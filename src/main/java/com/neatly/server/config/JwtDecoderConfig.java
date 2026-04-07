package com.neatly.server.config;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.util.StringUtils;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class JwtDecoderConfig {

	@Bean
	JwtDecoder jwtDecoder(
			Environment environment,
			@Value("${supabase.jwks.url:}") String jwksUrl,
			@Value("${supabase.jwt.issuer:}") String issuer,
			@Value("${supabase.jwt.secret:}") String jwtSecret) {
		// HS256 is only for automated tests (see application-test.properties).
		// Profile "local" is the default for spring-boot:run; real Supabase tokens require JWKS + issuer.
		// If JWKS env is missing, do not fall back to HS256 under "local" — that makes every Supabase JWT fail with 401.
		boolean hmacDevProfile = environment.acceptsProfiles(Profiles.of("test", "local"));

		if (StringUtils.hasText(jwksUrl)) {
			if (!StringUtils.hasText(issuer)) {
				throw new IllegalStateException(
						"SUPABASE_JWT_ISSUER is required when SUPABASE_JWKS_URL is set (Neatly rule.md: verify via JWKS + issuer).");
			}
			log.info("JWT decoder: JWKS + issuer validation (ES256)");
			NimbusJwtDecoder decoder = NimbusJwtDecoder
					.withJwkSetUri(jwksUrl.trim())
					.jwsAlgorithm(SignatureAlgorithm.ES256)
					.build();
			decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer.trim()));
			return decoder;
		}

		if (hmacDevProfile && StringUtils.hasText(jwtSecret)) {
			byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
			SecretKey key = new SecretKeySpec(keyBytes, "HmacSHA256");
			log.warn("JWT decoder: HS256 (profiles test/local only; not for production)");
			NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key)
					.macAlgorithm(MacAlgorithm.HS256)
					.build();
			if (StringUtils.hasText(issuer)) {
				decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer.trim()));
			}
			return decoder;
		}

		throw new IllegalStateException(
				"Set SUPABASE_JWKS_URL and SUPABASE_JWT_ISSUER for Supabase JWT verification (see rule.md). "
						+ "HS256 via supabase.jwt.secret is only used when spring.profiles.active includes 'test' or 'local'.");
	}
}
