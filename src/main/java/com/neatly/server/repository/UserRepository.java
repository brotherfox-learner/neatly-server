package com.neatly.server.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.neatly.server.domain.User;

public interface UserRepository extends JpaRepository<User, UUID> {
}
