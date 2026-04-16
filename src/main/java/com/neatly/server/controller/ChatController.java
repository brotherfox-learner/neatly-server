package com.neatly.server.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.neatly.server.dto.ChatbotDefaultsDto;
import com.neatly.server.dto.ChatMessageDto;
import com.neatly.server.dto.ChatRoomDto;
import com.neatly.server.dto.PresetAnswerDto;
import com.neatly.server.dto.PresetQuestionDto;
import com.neatly.server.dto.SendMessageRequest;
import com.neatly.server.service.ChatBotService;
import com.neatly.server.service.LiveChatService;
import com.neatly.server.security.SupabaseRoleResolver;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

	private final ChatBotService chatBotService;
	private final LiveChatService liveChatService;
	private final SupabaseRoleResolver supabaseRoleResolver;

	// ─── Public endpoints (preset Q&A) ───────────────────────────

	@GetMapping("/presets")
	public List<PresetQuestionDto> getPresets() {
		return chatBotService.getPresetQuestions();
	}

	/**
	 * Shortcut for the "Promotion" preset chip when no {@code promotion_cards} FAQ exists, or to reuse the same payload.
	 */
	@GetMapping("/presets/promotions")
	public PresetAnswerDto getPromotionPresetAnswer() {
		return chatBotService.getPromotionPresetAnswer();
	}

	@GetMapping("/presets/{id}/answer")
	public PresetAnswerDto getPresetAnswer(@PathVariable UUID id) {
		return chatBotService.getPresetAnswer(id);
	}

	@GetMapping("/search")
	public ResponseEntity<PresetAnswerDto> searchFaq(@RequestParam String q) {
		PresetAnswerDto answer = chatBotService.searchFaq(q);
		if (answer != null) {
			return ResponseEntity.ok(answer);
		}
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/defaults")
	public ChatbotDefaultsDto getChatDefaults() {
		return chatBotService.getChatDefaults();
	}

	// ─── Authenticated endpoints (chat rooms) ────────────────────

	@PostMapping("/rooms")
	public ChatRoomDto createOrReuseRoom(@AuthenticationPrincipal Jwt jwt) {
		UUID userId = UUID.fromString(jwt.getSubject());
		return liveChatService.toDto(chatBotService.createOrReuseChatRoom(userId));
	}

	/**
	 * Current non-closed chat for the logged-in user (restore after refresh).
	 */
	@GetMapping("/rooms/session")
	public ResponseEntity<ChatRoomDto> getSessionRoom(@AuthenticationPrincipal Jwt jwt) {
		UUID userId = UUID.fromString(jwt.getSubject());
		return chatBotService.findCurrentUserSessionRoom(userId)
				.map(liveChatService::toDto)
				.map(ResponseEntity::ok)
				.orElseGet(() -> ResponseEntity.noContent().build());
	}

	@PostMapping("/rooms/{id}/request-agent")
	public ChatRoomDto requestAgent(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
		UUID userId = UUID.fromString(jwt.getSubject());
		return liveChatService.requestAgent(id, userId);
	}

	@GetMapping("/rooms/{id}/messages")
	public List<ChatMessageDto> getMessages(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
		UUID requesterId = UUID.fromString(jwt.getSubject());
		boolean admin = "admin".equalsIgnoreCase(supabaseRoleResolver.resolveRole(jwt));
		return liveChatService.getChatHistoryForParticipant(id, requesterId, admin);
	}

	@PostMapping("/rooms/{id}/messages")
	public ChatMessageDto sendMessage(
			@PathVariable UUID id,
			@AuthenticationPrincipal Jwt jwt,
			@RequestBody SendMessageRequest request) {
		UUID userId = UUID.fromString(jwt.getSubject());
		// Determine sender type from role claim robustly using Resolver
		String role = supabaseRoleResolver.resolveRole(jwt);
		String senderType = "admin".equalsIgnoreCase(role) ? "AGENT" : "USER";
		return liveChatService.sendMessage(id, userId, senderType, request.message());
	}

	@PostMapping("/rooms/{id}/close")
	public ResponseEntity<Void> closeChat(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
		UUID requesterId = UUID.fromString(jwt.getSubject());
		boolean admin = "admin".equalsIgnoreCase(supabaseRoleResolver.resolveRole(jwt));
		liveChatService.closeChat(id, requesterId, admin);
		return ResponseEntity.ok().build();
	}

	@PostMapping("/rooms/{id}/leave")
	public ResponseEntity<Void> agentLeaveChat(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
		UUID agentId = UUID.fromString(jwt.getSubject());
		liveChatService.agentLeaveChat(id, agentId);
		return ResponseEntity.ok().build();
	}

	// ─── Admin endpoints ─────────────────────────────────────────

	@GetMapping("/rooms/pending")
	public List<ChatRoomDto> getPendingRooms() {
		return liveChatService.getPendingRooms();
	}

	@GetMapping("/rooms/my-active")
	public List<ChatRoomDto> getMyActiveRooms(@AuthenticationPrincipal Jwt jwt) {
		UUID agentId = UUID.fromString(jwt.getSubject());
		return liveChatService.getActiveRoomsForAgent(agentId);
	}

	@PostMapping("/rooms/{id}/accept")
	public ResponseEntity<?> acceptChat(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
		UUID agentId = UUID.fromString(jwt.getSubject());
		try {
			ChatRoomDto dto = liveChatService.acceptChat(id, agentId);
			return ResponseEntity.ok(dto);
		} catch (IllegalStateException e) {
			return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
		}
	}

	// ─── Helper ──────────────────────────────────────────────────

}
