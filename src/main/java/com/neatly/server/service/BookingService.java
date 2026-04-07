package com.neatly.server.service;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.neatly.server.domain.Booking;
import com.neatly.server.domain.User;
import com.neatly.server.dto.CreateBookingRequest;
import com.neatly.server.repository.BookingRepository;
import com.neatly.server.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final CurrentUserService currentUserService;

    public UUID createBooking(CreateBookingRequest request) {

        log.info("[CREATE_BOOKING][START] start createBooking");

        // STEP 0: get authenticated user from JWT
        UUID userId = currentUserService.requireUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        log.info("[CREATE_BOOKING][USER] userId={}", user.getId());

        Booking booking = new Booking();
        booking.setUser(user);

        // STEP 1: set basic info
        booking.setCheckInDate(request.getCheckInDate());
        booking.setCheckOutDate(request.getCheckOutDate());

        int nights = (int) ChronoUnit.DAYS.between(
                request.getCheckInDate(),
                request.getCheckOutDate());

        log.info("[CREATE_BOOKING][CALC] nights={}", nights);

        booking.setTotalNights(nights);
        booking.setGuestNumbers(String.valueOf(request.getGuests()));

        // STEP 2: calculate price
        // TODO: ดึง pricePerNight จาก RoomType ตาม roomTypeId เมื่อ room management พร้อม
        BigDecimal pricePerNight = BigDecimal.valueOf(1000);
        int roomCount = request.getRoomCount() > 0 ? request.getRoomCount() : 1;
        BigDecimal subtotal = pricePerNight.multiply(BigDecimal.valueOf(nights)).multiply(BigDecimal.valueOf(roomCount));

        log.info("[CREATE_BOOKING][PRICE] {} x {} nights x {} rooms = {}", pricePerNight, nights, roomCount, subtotal);

        booking.setSubtotal(subtotal);
        booking.setTotalAmount(subtotal);

        // STEP 3: set status based on payment method
        String status = "CASH".equalsIgnoreCase(request.getPaymentMethod())
                ? "PENDING_CHECKIN"
                : "PENDING_PAYMENT";
        booking.setStatus(status);

        String ref = "BOOK-" + System.currentTimeMillis();
        booking.setBookingReference(ref);

        log.info("[CREATE_BOOKING][REF] bookingReference={}, status={}", ref, status);

        // STEP 4: save DB
        bookingRepository.save(booking);
        log.info("[CREATE_BOOKING][SUCCESS] saved booking id={}", booking.getId());

        return booking.getId();
    }
}
