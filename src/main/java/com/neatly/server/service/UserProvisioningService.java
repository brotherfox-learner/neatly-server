package com.neatly.server.service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
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
		return userRepository.findById(id)
				.map(existing -> {
					backfillProfileFromMetadata(jwt, id);
					return existing;
				})
				.orElseGet(() -> createUserAndProfile(jwt, id));
	}

	private void backfillProfileFromMetadata(Jwt jwt, UUID id) {
		Map<String, Object> meta = readUserMetadata(jwt);
		if (meta == null) return;

		profileRepository.findByUser_Id(id).ifPresent(profile -> {
			boolean changed = false;

			if (profile.getPhone() == null) {
				String phone = metaString(meta, "phone_number");
				if (phone != null) {
					profile.setPhone(phone);
					changed = true;
				}
			}
			if (profile.getDateOfBirth() == null) {
				LocalDate dob = metaDate(meta, "birth_date");
				if (dob != null) {
					profile.setDateOfBirth(dob);
					changed = true;
				}
			}
			if (profile.getCountry() == null) {
				String country = metaString(meta, "country");
				if (country != null) {
					profile.setCountry(country);
					changed = true;
				}
			}
			if (changed) {
				profileRepository.save(profile);
				log.info("Backfilled profile from JWT metadata user_id={}", id);
			}
		});
	}

    private User createUserAndProfile(Jwt jwt, UUID id) {
        String email = jwt.getClaimAsString("email");
        if (!StringUtils.hasText(email)) {
            email = id + "@users.supabase.local";
        }
        String first = jwt.getClaimAsString("given_name");
        String last = jwt.getClaimAsString("family_name");

		Map<String, Object> meta = readUserMetadata(jwt);
		log.info("Provisioning user={} user_metadata keys={} values={}", id,
				meta != null ? meta.keySet() : "null",
				meta != null ? meta : "null");
		if (!StringUtils.hasText(first) && meta != null) {
			Object fn = meta.get("first_name");
			Object ln = meta.get("last_name");
			if (fn instanceof String s && StringUtils.hasText(s)) {
				first = s;
			}
			if (ln instanceof String s && StringUtils.hasText(s)) {
				last = s;
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

		String phone = metaString(meta, "phone_number");
		String country = metaString(meta, "country");
		LocalDate dateOfBirth = metaDate(meta, "birth_date");

		String role = supabaseRoleResolver.resolveRole(jwt);
		User user = User.provisionFromSupabaseAuth(id, email, role, SUPABASE_MANAGED_PASSWORD_PLACEHOLDER);
		userRepository.save(user);

		Profile profile = new Profile();
		profile.setUser(user);
		profile.setFirstName(first);
		profile.setLastName(last);
		if (StringUtils.hasText(phone)) {
			profile.setPhone(phone);
		}
		if (StringUtils.hasText(country)) {
			profile.setCountry(country);
		}
		if (dateOfBirth != null) {
			profile.setDateOfBirth(dateOfBirth);
		}
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

	private static String metaString(Map<String, Object> meta, String key) {
		if (meta == null) return null;
		Object val = meta.get(key);
		if (val instanceof String s && StringUtils.hasText(s)) {
			return s.trim();
		}
		return null;
	}

	private static LocalDate metaDate(Map<String, Object> meta, String key) {
		String raw = metaString(meta, key);
		if (raw == null) return null;
		try {
			return LocalDate.parse(raw);
		} catch (DateTimeParseException e) {
			return null;
		}
	}
}
