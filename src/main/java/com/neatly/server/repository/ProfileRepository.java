package com.neatly.server.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.neatly.server.domain.Profile;

public interface ProfileRepository extends JpaRepository<Profile, UUID> {

	Optional<Profile> findByUser_Id(UUID userId);
}
