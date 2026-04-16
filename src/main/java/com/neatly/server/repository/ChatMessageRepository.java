package com.neatly.server.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.neatly.server.domain.ChatMessage;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

	List<ChatMessage> findByChatRoomIdOrderByCreatedAtAsc(UUID chatRoomId);
}
