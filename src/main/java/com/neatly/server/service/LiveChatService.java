package com.neatly.server.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.neatly.server.domain.ChatMessage;
import com.neatly.server.domain.ChatRoom;
import com.neatly.server.dto.ChatMessageDto;
import com.neatly.server.dto.ChatRoomDto;
import com.neatly.server.repository.ChatMessageRepository;
import com.neatly.server.repository.ChatRoomRepository;
import com.neatly.server.repository.ProfileRepository;
import com.neatly.server.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class LiveChatService {

	private final ChatRoomRepository chatRoomRepository;
	private final ChatMessageRepository chatMessageRepository;
	private final UserRepository userRepository;
	private final ProfileRepository profileRepository;
	private final SimpMessagingTemplate messagingTemplate;

	/**
	 * User requests a live agent. Status → WAITING_AGENT.
	 * Broadcasts to all admins via WebSocket topic.
	 */
	@Transactional
	public ChatRoomDto requestAgent(UUID chatRoomId, UUID userId) {
		ChatRoom room = chatRoomRepository.findById(chatRoomId)
				.orElseThrow(() -> new RuntimeException("Chat room not found"));

		if (!room.getUser().getId().equals(userId)) {
			throw new RuntimeException("Not your chat room");
		}

		if (!"OPEN".equals(room.getStatus()) && !"WAITING_AGENT".equals(room.getStatus())) {
			throw new RuntimeException("Chat room cannot request agent in current status: " + room.getStatus());
		}

		room.setStatus("WAITING_AGENT");
		room.setUpdatedAt(Instant.now());
		chatRoomRepository.save(room);

		// System message in chat
		saveSystemMessage(room, "Connecting you to our team...");

		// Broadcast to all admins
		ChatRoomDto dto = toDto(room);
		messagingTemplate.convertAndSend("/topic/admin/chat-requests", (Object) dto);
		log.info("Chat room {} requesting agent, broadcast to admins", chatRoomId);

		return dto;
	}

	/**
	 * Admin accepts a chat. First-come-first-served with optimistic locking.
	 * Returns 409-like exception if already taken.
	 */
	@Transactional
	public ChatRoomDto acceptChat(UUID chatRoomId, UUID agentId) {
		ChatRoom room = chatRoomRepository.findById(chatRoomId)
				.orElseThrow(() -> new RuntimeException("Chat room not found"));

		if (!"WAITING_AGENT".equals(room.getStatus())) {
			throw new IllegalStateException("Chat room already accepted or closed");
		}

		room.setAgent(userRepository.getReferenceById(agentId));
		room.setStatus("ACTIVE");
		room.setUpdatedAt(Instant.now());
		chatRoomRepository.save(room);

		// Get admin profile name
		String agentName = profileRepository.findByUser_Id(agentId)
				.map(p -> {
					String fn = p.getFirstName() != null ? p.getFirstName() : "";
					String ln = p.getLastName() != null ? p.getLastName() : "";
					return (fn + " " + ln).trim();
				})
				.filter(n -> !n.isEmpty())
				.orElse("Admin");

		// System message: agent joined (formal, no emoji)
		saveSystemMessage(room, agentName + " has joined the chat.");

		// Notify user
		ChatRoomDto dto = toDto(room);
		messagingTemplate.convertAndSendToUser(
				room.getUser().getId().toString(),
				"/queue/chat-events",
				Map.of("event", "AGENT_JOINED", "room", dto, "agentName", agentName));

		// Notify all admins: this room is taken
		messagingTemplate.convertAndSend("/topic/admin/chat-taken",
				(Object) Map.of("chatRoomId", chatRoomId.toString(), "agentId", agentId.toString()));

		// Formal first reply from the agent (persisted + pushed like any AGENT message)
		sendMessage(chatRoomId, agentId, "AGENT", buildAgentJoinGreeting(agentName));

		log.info("Chat room {} accepted by agent {}", chatRoomId, agentId);
		return dto;
	}

	private static String buildAgentJoinGreeting(String agentDisplayName) {
		String name = (agentDisplayName != null && !agentDisplayName.isBlank()) ? agentDisplayName.trim()
				: "a member of our guest services team";
		return "Good day, and welcome to Neatly Hotel. I am " + name
				+ ". How may I assist you with your stay, reservation, or any questions about our property today?";
	}

	/**
	 * Assigned agent leaves: room returns to OPEN, history kept, user returns to assistant mode.
	 */
	@Transactional
	public void agentLeaveChat(UUID chatRoomId, UUID requesterId) {
		ChatRoom room = chatRoomRepository.findById(chatRoomId)
				.orElseThrow(() -> new RuntimeException("Chat room not found"));
		if (room.getAgent() == null || !room.getAgent().getId().equals(requesterId)) {
			throw new AccessDeniedException("Only the assigned team member can leave this chat");
		}
		if (!"ACTIVE".equals(room.getStatus())) {
			throw new IllegalStateException("Only an active chat can be left");
		}

		String agentName = profileRepository.findByUser_Id(requesterId)
				.map(p -> {
					String fn = p.getFirstName() != null ? p.getFirstName() : "";
					String ln = p.getLastName() != null ? p.getLastName() : "";
					return (fn + " " + ln).trim();
				})
				.filter(n -> !n.isEmpty())
				.orElse("Our guest services team");

		room.setAgent(null);
		room.setStatus("OPEN");
		room.setUpdatedAt(Instant.now());
		chatRoomRepository.save(room);

		saveSystemMessage(room, agentName
				+ " has left the conversation. Your message history above is unchanged. You may continue by choosing a topic below, or request to speak with our team again at any time.");

		Map<String, Object> leftPayload = Map.of("event", "AGENT_LEFT", "chatRoomId", chatRoomId.toString());
		messagingTemplate.convertAndSendToUser(room.getUser().getId().toString(), "/queue/chat-events", leftPayload);
		messagingTemplate.convertAndSendToUser(requesterId.toString(), "/queue/chat-events", leftPayload);

		log.info("Agent {} left chat room {} (room now OPEN)", requesterId, chatRoomId);
	}

	/**
	 * Send a message in a live chat room.
	 */
	@Transactional
	public ChatMessageDto sendMessage(UUID chatRoomId, UUID senderId, String senderType, String message) {
		ChatRoom room = chatRoomRepository.findById(chatRoomId)
				.orElseThrow(() -> new RuntimeException("Chat room not found"));

		ChatMessage msg = new ChatMessage();
		msg.setChatRoom(room);
		msg.setSender(userRepository.getReferenceById(senderId));
		msg.setSenderType(senderType);
		msg.setMessage(message);
		msg.setMessageType("TEXT");
		chatMessageRepository.save(msg);

		room.setUpdatedAt(Instant.now());
		chatRoomRepository.save(room);

		ChatMessageDto dto = toMessageDto(msg);

		// Send to user
		messagingTemplate.convertAndSendToUser(
				room.getUser().getId().toString(),
				"/queue/chat",
				dto);

		// Send to agent if present
		if (room.getAgent() != null) {
			messagingTemplate.convertAndSendToUser(
					room.getAgent().getId().toString(),
					"/queue/chat",
					dto);
		}

		return dto;
	}

	/**
	 * Close a chat room.
	 */
	@Transactional
	public void closeChat(UUID chatRoomId, UUID requesterId, boolean requesterIsAdmin) {
		ChatRoom room = chatRoomRepository.findById(chatRoomId)
				.orElseThrow(() -> new RuntimeException("Chat room not found"));
		assertCanAccessRoom(room, requesterId, requesterIsAdmin);

		room.setStatus("CLOSED");
		room.setUpdatedAt(Instant.now());
		chatRoomRepository.save(room);

		saveSystemMessage(room, "Chat has been closed.");

		// Notify user
		messagingTemplate.convertAndSendToUser(
				room.getUser().getId().toString(),
				"/queue/chat-events",
				Map.of("event", "CHAT_CLOSED", "chatRoomId", chatRoomId.toString()));

		// Notify agent
		if (room.getAgent() != null) {
			messagingTemplate.convertAndSendToUser(
					room.getAgent().getId().toString(),
					"/queue/chat-events",
					Map.of("event", "CHAT_CLOSED", "chatRoomId", chatRoomId.toString()));
		}
	}

	public List<ChatMessageDto> getChatHistory(UUID chatRoomId) {
		return chatMessageRepository.findByChatRoomIdOrderByCreatedAtAsc(chatRoomId)
				.stream()
				.map(this::toMessageDto)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<ChatMessageDto> getChatHistoryForParticipant(UUID chatRoomId, UUID requesterId,
			boolean requesterIsAdmin) {
		ChatRoom room = chatRoomRepository.findById(chatRoomId)
				.orElseThrow(() -> new RuntimeException("Chat room not found"));
		assertCanAccessRoom(room, requesterId, requesterIsAdmin);
		return getChatHistory(chatRoomId);
	}

	public List<ChatRoomDto> getActiveRoomsForAgent(UUID agentId) {
		return chatRoomRepository.findByAgent_IdAndStatusOrderByUpdatedAtDesc(agentId, "ACTIVE")
				.stream()
				.map(this::toDto)
				.toList();
	}

	private void assertCanAccessRoom(ChatRoom room, UUID requesterId, boolean requesterIsAdmin) {
		if (requesterIsAdmin) {
			return;
		}
		if (room.getUser().getId().equals(requesterId)) {
			return;
		}
		if (room.getAgent() != null && room.getAgent().getId().equals(requesterId)) {
			return;
		}
		throw new AccessDeniedException("Not allowed to access this chat room");
	}

	public List<ChatRoomDto> getPendingRooms() {
		return chatRoomRepository.findByStatusOrderByCreatedAtAsc("WAITING_AGENT")
				.stream()
				.map(this::toDto)
				.toList();
	}

	private void saveSystemMessage(ChatRoom room, String text) {
		ChatMessage msg = new ChatMessage();
		msg.setChatRoom(room);
		msg.setSenderType("SYSTEM");
		msg.setMessage(text);
		msg.setMessageType("SYSTEM");
		chatMessageRepository.save(msg);

		ChatMessageDto dto = toMessageDto(msg);
		messagingTemplate.convertAndSendToUser(
				room.getUser().getId().toString(),
				"/queue/chat",
				dto);
		if (room.getAgent() != null) {
			messagingTemplate.convertAndSendToUser(
					room.getAgent().getId().toString(),
					"/queue/chat",
					dto);
		}
	}

	public ChatRoomDto toDto(ChatRoom room) {
		String userName = "User";
		String userAvatar = null;
		
		var userProfile = profileRepository.findByUser_Id(room.getUser().getId()).orElse(null);
		if (userProfile != null) {
			String fn = userProfile.getFirstName() != null ? userProfile.getFirstName() : "";
			String ln = userProfile.getLastName() != null ? userProfile.getLastName() : "";
			String name = (fn + " " + ln).trim();
			if (!name.isEmpty()) userName = name;
			userAvatar = userProfile.getAvatarUrl();
		}

		String agentName = "Admin";
		String agentAvatar = null;
		if (room.getAgent() != null) {
			var agentProfile = profileRepository.findByUser_Id(room.getAgent().getId()).orElse(null);
			if (agentProfile != null) {
				String fn = agentProfile.getFirstName() != null ? agentProfile.getFirstName() : "";
				String ln = agentProfile.getLastName() != null ? agentProfile.getLastName() : "";
				String name = (fn + " " + ln).trim();
				if (!name.isEmpty()) agentName = name;
				agentAvatar = agentProfile.getAvatarUrl();
			}
		}

		return new ChatRoomDto(
				room.getId(),
				room.getUser().getId(),
				userName,
				userAvatar,
				room.getAgent() != null ? room.getAgent().getId() : null,
				agentName,
				agentAvatar,
				room.getStatus(),
				room.getCreatedAt(),
				room.getUpdatedAt());
	}

	public ChatMessageDto toMessageDto(ChatMessage msg) {
		String senderName = "System";
		String senderAvatarUrl = null;

		if (msg.getSender() != null) {
			var profile = profileRepository.findByUser_Id(msg.getSender().getId()).orElse(null);
			if (profile != null) {
				String fn = profile.getFirstName() != null ? profile.getFirstName() : "";
				String ln = profile.getLastName() != null ? profile.getLastName() : "";
				String name = (fn + " " + ln).trim();
				if (!name.isEmpty()) senderName = name;
				senderAvatarUrl = profile.getAvatarUrl();
			} else {
				senderName = "AGENT".equals(msg.getSenderType()) ? "Admin" : "User";
			}
		}

		return new ChatMessageDto(
				msg.getId(),
				msg.getChatRoom().getId(),
				msg.getSender() != null ? msg.getSender().getId() : null,
				senderName,
				senderAvatarUrl,
				msg.getSenderType(),
				msg.getMessage(),
				msg.getMessageType(),
				msg.getMetadata(),
				msg.getCreatedAt());
	}
}
