package com.neatly.server.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record ChatMessageDto(
		UUID id,
		UUID chatRoomId,
		UUID senderId,
		String senderName,
		String senderAvatarUrl,
		String senderType,
		String message,
		String messageType,
		Map<String, Object> metadata,
		Instant createdAt) {
}
