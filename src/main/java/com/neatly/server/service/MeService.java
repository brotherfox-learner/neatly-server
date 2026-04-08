package com.neatly.server.service;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;

import com.neatly.server.domain.Profile;
import com.neatly.server.domain.User;
import com.neatly.server.dto.MeProfileUpdateRequest;
import com.neatly.server.dto.UserResponse;
import com.neatly.server.repository.ProfileRepository;
import com.neatly.server.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MeService {

	private final UserRepository userRepository;
	private final ProfileRepository profileRepository;
	private final CurrentUserService currentUserService;
	private final SupabaseStorageService supabaseStorageService;

	@Transactional(readOnly = true)
	public UserResponse getCurrentUserProfile() {
		UUID id = currentUserService.requireUserId();
		User user = userRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
		Profile profile = profileRepository.findByUser_Id(id).orElse(null);
		return UserResponse.from(user, profile);
	}

	@Transactional
	public UserResponse updateCurrentUserProfile(MeProfileUpdateRequest request) {
		UUID id = currentUserService.requireUserId();
		User user = userRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
		Profile profile = profileRepository.findByUser_Id(id).orElseGet(() -> {
			Profile p = new Profile();
			p.setUser(user);
			return p;
		});

		profile.setFirstName(normalize(request.firstName()));
		profile.setLastName(normalize(request.lastName()));
		profile.setPhone(normalize(request.phone()));
		profile.setCountry(normalize(request.country()));
		profile.setDateOfBirth(request.dateOfBirth());
		profile.setAvatarUrl(normalize(request.avatarUrl()));

		Profile saved = profileRepository.save(profile);
		return UserResponse.from(user, saved);
	}

	public String uploadCurrentUserAvatar(MultipartFile file) {
		UUID id = currentUserService.requireUserId();
		String accessToken = currentUserService.requireAccessToken();
		return supabaseStorageService.uploadProfileAvatar(id, file, accessToken);
	}

	private static String normalize(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
