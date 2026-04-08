package com.neatly.server.service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.neatly.server.domain.Booking;
import com.neatly.server.dto.CreatePaymentIntentResponse;
import com.neatly.server.repository.BookingRepository;
import com.stripe.Stripe;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    @Value("${stripe.secret-key}")
    private String stripeSecretKey;

    private final BookingRepository bookingRepository;

    public void updateStatus(UUID bookingId, String status) {
        log.info("[PAYMENT_STATUS][UPDATE] bookingId={} status={}", bookingId, status);
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found: " + bookingId));
        booking.setStatus(status);
        bookingRepository.save(booking);
        log.info("[PAYMENT_STATUS][DONE] bookingId={} newStatus={}", bookingId, status);
    }

    public CreatePaymentIntentResponse createIntent(UUID bookingId) {
        log.info("[PAYMENT_INTENT][START] bookingId={}", bookingId);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found: " + bookingId));

        // Convert THB to satang (smallest unit) — 1 THB = 100 satang
        long amountSatang = booking.getTotalAmount().multiply(java.math.BigDecimal.valueOf(100)).longValue();

        try {
            Stripe.apiKey = stripeSecretKey;

            Map<String, String> metadata = new HashMap<>();
            metadata.put("bookingId", bookingId.toString());
            metadata.put("bookingReference", booking.getBookingReference());

            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amountSatang)
                    .setCurrency("thb")
                    .putAllMetadata(metadata)
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .build())
                    .build();

            PaymentIntent intent = PaymentIntent.create(params);

            log.info("[PAYMENT_INTENT][SUCCESS] bookingId={} intentId={}", bookingId, intent.getId());
            return new CreatePaymentIntentResponse(intent.getClientSecret(), intent.getId());

        } catch (com.stripe.exception.StripeException e) {
            log.error("[PAYMENT_INTENT][ERROR] bookingId={} error={}", bookingId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Stripe error: " + e.getMessage());
        }
    }
}
