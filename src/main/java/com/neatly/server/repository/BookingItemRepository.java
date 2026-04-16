package com.neatly.server.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.neatly.server.domain.BookingItem;

public interface BookingItemRepository extends JpaRepository<BookingItem, UUID> {

	/** Load items for many bookings; touch {@code room} / {@code roomType} in a transactional service. */
	List<BookingItem> findByBooking_IdInOrderByIdAsc(Collection<UUID> bookingIds);
}
