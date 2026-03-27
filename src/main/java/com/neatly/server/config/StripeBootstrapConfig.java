package com.neatly.server.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import com.stripe.Stripe;

import jakarta.annotation.PostConstruct;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class StripeBootstrapConfig {

	@Value("${stripe.secret-key:}")
	private String stripeSecretKey;

	@PostConstruct
	void initStripeApiKey() {
		if (StringUtils.hasText(stripeSecretKey)) {
			Stripe.apiKey = stripeSecretKey.trim();
			log.info("Stripe API key configured");
		}
		else {
			log.warn("STRIPE_SECRET_KEY is empty; Stripe API calls will fail until configured");
		}
	}
}
