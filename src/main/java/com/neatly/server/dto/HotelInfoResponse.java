package com.neatly.server.dto;

public record HotelInfoResponse(
		String hotelName,
		String aboutDescription,
		String logoUrl) {
}
