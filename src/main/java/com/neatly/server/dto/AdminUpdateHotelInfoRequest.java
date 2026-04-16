package com.neatly.server.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminUpdateHotelInfoRequest(
		@NotBlank(message = "Hotel name is required")
		String hotelName,
		@NotBlank(message = "Hotel description is required")
		String aboutDescription,
		String logoUrl) {
}
