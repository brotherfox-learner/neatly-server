package com.neatly.server.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Size;

public record MeProfileUpdateRequest(
		@Size(max = 100) String firstName,
		@Size(max = 100) String lastName,
		@Size(max = 50) String phone,
		@Size(max = 100) String country,
		LocalDate dateOfBirth,
		@Size(max = 500) String avatarUrl) {
}
