package com.neatly.server.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record AdminRoomDetailResponse(
		UUID roomId,
		String roomTypeName,
		String description,
		Integer maxOccupancy,
		BigDecimal basePrice,
		BigDecimal discountedPrice,
		String bedType,
		BigDecimal roomSizeSqm,
		List<String> amenities,
		String mainImageUrl,
		List<String> galleryImageUrls) {
}
