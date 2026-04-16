package com.neatly.server.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.neatly.server.domain.Booking;

public interface BookingRepository extends JpaRepository<Booking, UUID> {

	/** Latest activity first; lazy-load {@code user} inside {@code @Transactional} service code. */
	List<Booking> findAllByOrderByUpdatedAtDesc();
}
