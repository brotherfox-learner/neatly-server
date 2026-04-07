package com.neatly.server.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.neatly.server.dto.CreatePaymentIntentRequest;
import com.neatly.server.dto.CreatePaymentIntentResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    // TODO: inject PaymentService เมื่อ Stripe integration พร้อม
    // private final PaymentService paymentService;

    @PostMapping("/create-intent")
    public ResponseEntity<CreatePaymentIntentResponse> createPaymentIntent(
            @RequestBody CreatePaymentIntentRequest request) {

        log.info("[PAYMENT_INTENT][REQUEST] bookingId={}", request.getBookingId());

        // TODO: implement Stripe PaymentIntent creation
        // CreatePaymentIntentResponse response = paymentService.createIntent(request.getBookingId());
        // return ResponseEntity.ok(response);

        throw new UnsupportedOperationException("Stripe PaymentIntent not yet implemented");
    }
}
