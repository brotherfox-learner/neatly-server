package com.neatly.server.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AdminCustomerBookingListItemResponse(
		UUID bookingId,
		String customerName,
		String guestNumbers,
		String roomTypeName,
		String amountLabel,
		BigDecimal subtotal,
		BigDecimal discountAmount,
		BigDecimal totalAmount,
		String bedType,
		LocalDate checkInDate,
		LocalDate checkOutDate,
		Integer totalNights,
		String stayTotalLabel,
		Instant bookingCreatedAt,
		Instant updatedAt) {
}
