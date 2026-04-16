package com.neatly.server.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.neatly.server.domain.Room;

public interface RoomRepository extends JpaRepository<Room, UUID> {
	/** Active rooms ordered by display room number (matches `rooms.room_number`). */
	java.util.List<Room> findByDeletedAtIsNullOrderByRoomNumberAsc();

	java.util.Optional<Room> findByIdAndDeletedAtIsNull(UUID id);

	long countByRoomType_Id(UUID roomTypeId);
}
