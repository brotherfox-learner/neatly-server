package com.neatly.server.service;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import com.neatly.server.domain.WebhookEvent;
import com.neatly.server.repository.BookingRepository;
import com.neatly.server.repository.WebhookEventRepository;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.stripe.net.Webhook;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class StripeWebhookService {

	private final WebhookEventRepository webhookEventRepository;
	private final BookingRepository bookingRepository;
	private final String webhookSecret;

	public StripeWebhookService(
			WebhookEventRepository webhookEventRepository,
			BookingRepository bookingRepository,
			@Value("${stripe.webhook-secret:}") String webhookSecret) {
		this.webhookEventRepository = webhookEventRepository;
		this.bookingRepository = bookingRepository;
		this.webhookSecret = webhookSecret;
	}

	@Transactional
	public void processStripeEvent(byte[] payload, String signatureHeader) {
		if (payload == null || payload.length == 0) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing request body.");
		}
		if (!StringUtils.hasText(webhookSecret)) {
			log.warn("Stripe webhook received but STRIPE_WEBHOOK_SECRET is not configured");
			throw new ResponseStatusException(
					HttpStatus.SERVICE_UNAVAILABLE,
					"Stripe webhook is not configured (STRIPE_WEBHOOK_SECRET).");
		}
		if (!StringUtils.hasText(signatureHeader)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing Stripe-Signature header.");
		}
		String body = new String(payload, StandardCharsets.UTF_8);
		Event event;
		try {
			event = Webhook.constructEvent(body, signatureHeader.trim(), webhookSecret.trim());
		}
		catch (SignatureVerificationException e) {
			log.debug("Invalid Stripe webhook signature: {}", e.getMessage());
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid signature.");
		}
		catch (Exception e) {
			log.warn("Invalid Stripe webhook payload: {}", e.getMessage());
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid payload.");
		}
		if (webhookEventRepository.findByStripeEventId(event.getId()).isPresent()) {
			log.debug("Skip duplicate Stripe webhook event id={}", event.getId());
			return;
		}
		try {
			WebhookEvent row = new WebhookEvent();
			row.setStripeEventId(event.getId());
			row.setEventType(event.getType());
			row.setPayload(body);
			row.setProcessed(false);
			webhookEventRepository.save(row);
			log.info("Stored webhook_events stripe_event_id={} type={}", event.getId(), event.getType());
		}
		catch (Exception e) {
			log.error("Failed to persist webhook_events", e);
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to persist webhook.");
		}

		// Process booking status update based on event type
		handleBookingStatusUpdate(event);
	}

	private void handleBookingStatusUpdate(Event event) {
		String eventType = event.getType();
		if (!"payment_intent.succeeded".equals(eventType) && !"payment_intent.payment_failed".equals(eventType)) {
			return;
		}

		StripeObject stripeObject = event.getDataObjectDeserializer().getObject().orElse(null);
		if (!(stripeObject instanceof PaymentIntent paymentIntent)) {
			log.warn("[WEBHOOK] Could not deserialize PaymentIntent for event={}", event.getId());
			return;
		}

		String bookingIdStr = paymentIntent.getMetadata().get("bookingId");
		if (!StringUtils.hasText(bookingIdStr)) {
			log.warn("[WEBHOOK] No bookingId in PaymentIntent metadata, intentId={}", paymentIntent.getId());
			return;
		}

		UUID bookingId;
		try {
			bookingId = UUID.fromString(bookingIdStr);
		} catch (IllegalArgumentException e) {
			log.warn("[WEBHOOK] Invalid bookingId format={}", bookingIdStr);
			return;
		}

		String newStatus = "payment_intent.succeeded".equals(eventType) ? "PAID" : "FAILED";

		bookingRepository.findById(bookingId).ifPresentOrElse(
				booking -> {
					booking.setStatus(newStatus);
					bookingRepository.save(booking);
					log.info("[WEBHOOK] Updated booking id={} status={}", bookingId, newStatus);
				},
				() -> log.warn("[WEBHOOK] Booking not found for id={}", bookingId)
		);
	}
}
