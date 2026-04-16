package com.neatly.server.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.neatly.server.domain.BookingExtraService;

public interface BookingExtraServiceRepository extends JpaRepository<BookingExtraService, UUID> {
}
