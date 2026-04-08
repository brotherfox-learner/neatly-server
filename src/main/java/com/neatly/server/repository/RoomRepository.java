package com.neatly.server.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.neatly.server.domain.Room;

public interface RoomRepository extends JpaRepository<Room, UUID> {
	java.util.List<Room> findByDeletedAtIsNullOrderByUpdatedAtDesc();

	java.util.Optional<Room> findByIdAndDeletedAtIsNull(UUID id);
}
