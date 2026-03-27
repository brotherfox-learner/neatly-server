package com.neatly.server.service;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.neatly.server.domain.Profile;
import com.neatly.server.domain.User;
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

	@Transactional(readOnly = true)
	public UserResponse getCurrentUserProfile() {
		UUID id = currentUserService.requireUserId();
		User user = userRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
		Profile profile = profileRepository.findByUser_Id(id).orElse(null);
		return UserResponse.from(user, profile);
	}
}
