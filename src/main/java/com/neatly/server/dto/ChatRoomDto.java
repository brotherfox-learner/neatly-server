package com.neatly.server.dto;

import java.time.Instant;
import java.util.UUID;

public record ChatRoomDto(
		UUID id,
		UUID userId,
		String userName,
		String userAvatarUrl,
		UUID agentId,
		String agentName,
		String agentAvatarUrl,
		String status,
		Instant createdAt,
		Instant updatedAt) {
}
