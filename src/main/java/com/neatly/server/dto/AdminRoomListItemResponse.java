package com.neatly.server.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AdminRoomListItemResponse(
		UUID roomId,
		String imageUrl,
		String roomType,
		BigDecimal price,
		BigDecimal promotionPrice,
		Integer guests,
		String bedType,
		BigDecimal roomSizeSqm,
		Instant updatedAt) {
}
