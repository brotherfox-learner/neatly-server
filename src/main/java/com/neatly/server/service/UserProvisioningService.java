package com.neatly.server.service;

import java.util.Map;
import java.util.UUID;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.neatly.server.domain.Profile;
import com.neatly.server.domain.User;
import com.neatly.server.repository.ProfileRepository;
import com.neatly.server.repository.UserRepository;
import com.neatly.server.security.SupabaseRoleResolver;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Ensures {@code public.users} and {@code public.profiles} rows exist for the Supabase JWT {@code sub}
 * (aligned with docs/sql.md).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserProvisioningService {

	static final String SUPABASE_MANAGED_PASSWORD_PLACEHOLDER = "";

	private final UserRepository userRepository;
	private final ProfileRepository profileRepository;
	private final SupabaseRoleResolver supabaseRoleResolver;

	@Transactional
	public User ensureUserForJwt(Jwt jwt) {
		UUID id = UUID.fromString(jwt.getSubject());
		return userRepository.findById(id).orElseGet(() -> createUserAndProfile(jwt, id));
	}

	private User createUserAndProfile(Jwt jwt, UUID id) {
		String email = jwt.getClaimAsString("email");
		if (!StringUtils.hasText(email)) {
			email = id + "@users.supabase.local";
		}
		String first = jwt.getClaimAsString("given_name");
		String last = jwt.getClaimAsString("family_name");

		if (!StringUtils.hasText(first)) {
			Map<String, Object> meta = readUserMetadata(jwt);
			if (meta != null) {
				Object fn = meta.get("first_name");
				Object ln = meta.get("last_name");
				if (fn instanceof String s && StringUtils.hasText(s)) {
					first = s;
				}
				if (ln instanceof String s && StringUtils.hasText(s)) {
					last = s;
				}
			}
		}
		if (!StringUtils.hasText(first) && StringUtils.hasText(email) && email.contains("@")) {
			first = email.substring(0, email.indexOf('@'));
		}
		if (!StringUtils.hasText(first)) {
			first = "User";
		}
		if (!StringUtils.hasText(last)) {
			last = "Member";
		}

		String role = supabaseRoleResolver.resolveRole(jwt);
		User user = User.provisionFromSupabaseAuth(id, email, role, SUPABASE_MANAGED_PASSWORD_PLACEHOLDER);
		userRepository.save(user);

		Profile profile = new Profile();
		profile.setUser(user);
		profile.setFirstName(first);
		profile.setLastName(last);
		profileRepository.save(profile);

		log.info("Provisioned users + profiles id={} (first API call after Supabase Auth)", id);
		return user;
	}

	private Map<String, Object> readUserMetadata(Jwt jwt) {
		Object raw = jwt.getClaim("user_metadata");
		if (raw instanceof Map<?, ?> map) {
			@SuppressWarnings("unchecked")
			Map<String, Object> cast = (Map<String, Object>) map;
			return cast;
		}
		return null;
	}
}
