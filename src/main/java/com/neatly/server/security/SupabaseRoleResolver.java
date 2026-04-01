package com.neatly.server.security;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Resolves application role from Supabase JWT claims.
 */
@Component
public class SupabaseRoleResolver {

	private static final String DEFAULT_ROLE = "user";
	private static final Set<String> SUPPORTED_ROLES = Set.of("admin", "user");

	public String resolveRole(Jwt jwt) {
		String fromAppMetadata = readRoleFromAppMetadata(jwt);
		if (StringUtils.hasText(fromAppMetadata)) {
			return normalizeRole(fromAppMetadata);
		}

		String directRole = jwt.getClaimAsString("role");
		if (StringUtils.hasText(directRole)) {
			return normalizeRole(directRole);
		}

		return DEFAULT_ROLE;
	}

	private String readRoleFromAppMetadata(Jwt jwt) {
		Object raw = jwt.getClaim("app_metadata");
		if (!(raw instanceof Map<?, ?> metadata)) {
			return null;
		}
		Object role = metadata.get("role");
		return role instanceof String s ? s : null;
	}

	private String normalizeRole(String role) {
		String normalized = role.trim().toLowerCase(Locale.ROOT);
		if ("customer".equals(normalized)) {
			return DEFAULT_ROLE;
		}
		if (SUPPORTED_ROLES.contains(normalized)) {
			return normalized;
		}
		return DEFAULT_ROLE;
	}
}
