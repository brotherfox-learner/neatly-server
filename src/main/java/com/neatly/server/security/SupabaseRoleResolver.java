package com.neatly.server.security;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.neatly.server.domain.User;
import com.neatly.server.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Resolves application role from Supabase JWT claims.
 */
@Component
@RequiredArgsConstructor
public class SupabaseRoleResolver {

	private static final String DEFAULT_ROLE = "user";
	private static final Set<String> SUPPORTED_ROLES = Set.of("admin", "user");
	private final UserRepository userRepository;

	public String resolveRole(Jwt jwt) {
		String fromDatabase = readRoleFromDatabase(jwt);
		if (StringUtils.hasText(fromDatabase)) {
			return normalizeRole(fromDatabase);
		}

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

	private String readRoleFromDatabase(Jwt jwt) {
		try {
			UUID id = UUID.fromString(jwt.getSubject());
			return userRepository.findById(id)
					.map(User::getRole)
					.orElse(null);
		} catch (Exception ignored) {
			return null;
		}
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
