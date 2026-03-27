package com.neatly.server.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

	/** Supabase Auth user id — same as JWT {@code sub}. */
	@Id
	@Column(columnDefinition = "uuid")
	private UUID id;

	@Column(nullable = false, unique = true)
	private String email;

	@Column(name = "password_hash", nullable = false)
	private String passwordHash;

	@Column(nullable = false)
	private String role;

	@Column(name = "is_active", nullable = false)
	private boolean isActive = true;

	@Column(name = "email_verified_at")
	private Instant emailVerifiedAt;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Column(name = "deleted_at")
	private Instant deletedAt;

	/**
	 * Minimal row when identity is managed by Supabase Auth; password is not stored locally.
	 */
	public static User provisionFromSupabaseAuth(UUID id, String email, String role, String passwordHashPlaceholder) {
		User u = new User();
		u.setId(id);
		u.setEmail(email);
		u.setPasswordHash(passwordHashPlaceholder);
		u.setRole(role);
		u.setActive(true);
		Instant now = Instant.now();
		u.setCreatedAt(now);
		u.setUpdatedAt(now);
		return u;
	}

	public void setActive(boolean active) {
		this.isActive = active;
	}
}
