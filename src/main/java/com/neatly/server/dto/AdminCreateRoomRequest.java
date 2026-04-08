package com.neatly.server.dto;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminCreateRoomRequest(
		@NotBlank @Size(max = 200) String roomTypeName,
		@Size(max = 3000) String description,
		@NotNull @DecimalMin(value = "1") Integer maxOccupancy,
		@NotNull @DecimalMin(value = "0.01") BigDecimal basePrice,
		@DecimalMin(value = "0.00") BigDecimal discountedPrice,
		@NotBlank @Size(max = 100) String bedType,
		@NotNull @DecimalMin(value = "0.01") BigDecimal roomSizeSqm,
		@NotEmpty List<@NotBlank @Size(max = 120) String> amenities,
		@NotBlank @Size(max = 500) String mainImageUrl,
		@NotEmpty @Size(min = 4) List<@NotBlank @Size(max = 500) String> galleryImageUrls) {
}
