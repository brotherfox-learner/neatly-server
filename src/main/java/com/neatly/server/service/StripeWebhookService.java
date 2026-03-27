package com.neatly.server.service;

import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import com.neatly.server.domain.WebhookEvent;
import com.neatly.server.repository.WebhookEventRepository;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class StripeWebhookService {

	private final WebhookEventRepository webhookEventRepository;
	private final String webhookSecret;

	public StripeWebhookService(
			WebhookEventRepository webhookEventRepository,
			@Value("${stripe.webhook-secret:}") String webhookSecret) {
		this.webhookEventRepository = webhookEventRepository;
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
	}
}
