package com.neatly.server.dto;

import java.time.LocalDate;
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
		String phone,
		String country,
		LocalDate dateOfBirth,
		String avatarUrl) {

	public static UserResponse from(User user, Profile profile) {
		String first = profile != null ? profile.getFirstName() : null;
		String last = profile != null ? profile.getLastName() : null;
		String phone = profile != null ? profile.getPhone() : null;
		String country = profile != null ? profile.getCountry() : null;
		LocalDate dob = profile != null ? profile.getDateOfBirth() : null;
		String avatar = profile != null ? profile.getAvatarUrl() : null;
		return new UserResponse(
				user.getId(),
				user.getEmail(),
				user.getRole(),
				user.isActive(),
				first,
				last,
				phone,
				country,
				dob,
				avatar);
	}
}
