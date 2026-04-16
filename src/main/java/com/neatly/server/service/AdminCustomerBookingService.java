package com.neatly.server.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.neatly.server.domain.Booking;
import com.neatly.server.domain.BookingItem;
import com.neatly.server.domain.Profile;
import com.neatly.server.domain.User;
import com.neatly.server.dto.AdminCustomerBookingListItemResponse;
import com.neatly.server.repository.BookingItemRepository;
import com.neatly.server.repository.BookingRepository;
import com.neatly.server.repository.ProfileRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminCustomerBookingService {

	private final BookingRepository bookingRepository;
	private final BookingItemRepository bookingItemRepository;
	private final ProfileRepository profileRepository;

	@Transactional(readOnly = true)
	public List<AdminCustomerBookingListItemResponse> listBookings() {
		List<Booking> bookings = bookingRepository.findAllByOrderByUpdatedAtDesc();
		if (bookings.isEmpty()) {
			return List.of();
		}
		List<UUID> ids = bookings.stream().map(Booking::getId).toList();
		List<BookingItem> items = bookingItemRepository.findByBooking_IdInOrderByIdAsc(ids);
		Map<UUID, List<BookingItem>> byBooking = items.stream()
				.collect(Collectors.groupingBy(bi -> bi.getBooking().getId()));
		for (List<BookingItem> list : byBooking.values()) {
			list.sort(Comparator.comparing(BookingItem::getId));
		}

		List<AdminCustomerBookingListItemResponse> out = new ArrayList<>(bookings.size());
		for (Booking b : bookings) {
			List<BookingItem> rowItems = byBooking.getOrDefault(b.getId(), List.of());
			out.add(toResponse(b, rowItems));
		}
		return out;
	}

	private AdminCustomerBookingListItemResponse toResponse(Booking booking, List<BookingItem> items) {
		User user = booking.getUser();
		String customerName = resolveCustomerName(user);
		String amountLabel = amountLabel(items.size());
		BookingItem first = items.isEmpty() ? null : items.get(0);
		String roomTypeName = first == null ? "-" : nullToDash(first.getRoomType().getName());
		String bedType = first == null ? "-" : formatBedType(first.getRoom().getBedType());
		int nights = booking.getTotalNights() == null ? 0 : booking.getTotalNights();
		String stayTotalLabel = nights == 1 ? "1 night" : nights + " nights";
		BigDecimal subtotal = booking.getSubtotal() != null ? booking.getSubtotal() : BigDecimal.ZERO;
		BigDecimal discount = booking.getDiscountAmount() != null ? booking.getDiscountAmount() : BigDecimal.ZERO;
		BigDecimal total = booking.getTotalAmount() != null ? booking.getTotalAmount() : BigDecimal.ZERO;

		return new AdminCustomerBookingListItemResponse(
				booking.getId(),
				customerName,
				nullToDash(booking.getGuestNumbers()),
				roomTypeName,
				amountLabel,
				subtotal,
				discount,
				total,
				bedType,
				booking.getCheckInDate(),
				booking.getCheckOutDate(),
				booking.getTotalNights(),
				stayTotalLabel,
				booking.getCreatedAt(),
				booking.getUpdatedAt());
	}

	private String resolveCustomerName(User user) {
		return profileRepository.findByUser_Id(user.getId())
				.map(p -> formatProfileName(p, user.getEmail()))
				.orElse(user.getEmail());
	}

	private static String formatProfileName(Profile p, String emailFallback) {
		String fn = p.getFirstName() == null ? "" : p.getFirstName().trim();
		String ln = p.getLastName() == null ? "" : p.getLastName().trim();
		String full = (fn + " " + ln).trim();
		return full.isEmpty() ? emailFallback : full;
	}

	private static String amountLabel(int count) {
		if (count <= 0) {
			return "-";
		}
		return count == 1 ? "1 room" : count + " rooms";
	}

	private static String nullToDash(String s) {
		if (s == null || s.isBlank()) {
			return "-";
		}
		return s;
	}

	private static String formatBedType(String raw) {
		String s = raw == null ? "" : raw.trim();
		if (s.isEmpty()) {
			return "-";
		}
		return java.util.Arrays.stream(s.split("\\s+"))
				.filter(w -> !w.isEmpty())
				.map(w -> w.substring(0, 1).toUpperCase() + w.substring(1).toLowerCase())
				.collect(Collectors.joining(" "));
	}
}
