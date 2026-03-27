package com.neatly.server.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.neatly.server.service.StripeWebhookService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
@Slf4j
public class StripeWebhookController {

	private final StripeWebhookService stripeWebhookService;

	@PostMapping("/stripe")
	public ResponseEntity<String> handleStripe(
			@RequestBody byte[] payload,
			@RequestHeader(value = "Stripe-Signature", required = false) String sigHeader) {
		log.debug("Stripe webhook POST, payload bytes={}", payload != null ? payload.length : 0);
		stripeWebhookService.processStripeEvent(payload, sigHeader);
		return ResponseEntity.ok("ok");
	}
}
