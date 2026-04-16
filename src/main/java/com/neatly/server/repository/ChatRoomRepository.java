package com.neatly.server.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.neatly.server.domain.ChatRoom;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, UUID> {

	Optional<ChatRoom> findFirstByUser_IdAndStatusInOrderByUpdatedAtDesc(UUID userId, List<String> statuses);

	List<ChatRoom> findByStatusOrderByCreatedAtAsc(String status);

	List<ChatRoom> findByUserIdOrderByCreatedAtDesc(UUID userId);

	List<ChatRoom> findByAgent_IdAndStatusOrderByUpdatedAtDesc(UUID agentId, String status);
}
