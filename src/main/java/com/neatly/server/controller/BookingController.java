package com.neatly.server.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.neatly.server.dto.CreateBookingRequest;
import com.neatly.server.dto.CreateBookingResponse;
import com.neatly.server.service.BookingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
@Slf4j
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<CreateBookingResponse> createBooking(
            @RequestBody CreateBookingRequest request) {

        log.info("[CREATE_BOOKING][REQUEST] request received: {}", request);

        UUID bookingId = bookingService.createBooking(request);

        log.info("[CREATE_BOOKING][SUCCESS] booking created id={}", bookingId);

        return ResponseEntity.ok(new CreateBookingResponse(bookingId));
    }
}