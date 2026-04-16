package com.neatly.server.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.neatly.server.dto.CreatePaymentIntentRequest;
import com.neatly.server.dto.CreatePaymentIntentResponse;
import com.neatly.server.dto.UpdateBookingStatusRequest;
import com.neatly.server.service.PaymentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/create-intent")
    public ResponseEntity<CreatePaymentIntentResponse> createPaymentIntent(
            @RequestBody CreatePaymentIntentRequest request) {

        log.info("[PAYMENT_INTENT][REQUEST] bookingId={}", request.getBookingId());
        CreatePaymentIntentResponse response = paymentService.createIntent(request.getBookingId());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{bookingId}/status")
    public ResponseEntity<Void> updateBookingStatus(
            @PathVariable UUID bookingId,
            @RequestBody UpdateBookingStatusRequest request) {

        log.info("[PAYMENT_STATUS][REQUEST] bookingId={} status={}", bookingId, request.getStatus());
        paymentService.updateStatus(bookingId, request.getStatus());
        return ResponseEntity.noContent().build();
    }
}
