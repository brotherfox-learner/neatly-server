package com.neatly.server.dto;

import java.util.UUID;

import com.neatly.server.domain.Profile;
import com.neatly.server.domain.User;

public record UserResponse(
		UUID id,
		String email,
		String role,
		boolean active,
		String firstName,
		String lastName,
		String avatarUrl) {

	public static UserResponse from(User user, Profile profile) {
		String first = profile != null ? profile.getFirstName() : null;
		String last = profile != null ? profile.getLastName() : null;
		String avatar = profile != null ? profile.getAvatarUrl() : null;
		return new UserResponse(
				user.getId(),
				user.getEmail(),
				user.getRole(),
				user.isActive(),
				first,
				last,
				avatar);
	}
}
