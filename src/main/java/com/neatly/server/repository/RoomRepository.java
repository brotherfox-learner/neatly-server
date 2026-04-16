package com.neatly.server.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.neatly.server.domain.Room;

public interface RoomRepository extends JpaRepository<Room, UUID> {

	List<Room> findByDeletedAtIsNullOrderByUpdatedAtDesc();

	java.util.Optional<Room> findByIdAndDeletedAtIsNull(UUID id);

	@Query("""
			SELECT r FROM Room r
			WHERE r.roomType.id = :roomTypeId
			  AND r.deletedAt IS NULL
			  AND r.status = 'Vacant Clean'
			  AND r.id NOT IN (
			    SELECT bi.room.id FROM BookingItem bi
			    WHERE bi.booking.checkInDate < :checkOut
			      AND bi.booking.checkOutDate > :checkIn
			      AND bi.booking.status NOT IN ('FAILED', 'CANCELLED')
			  )
			ORDER BY r.roomNumber ASC
			""")
	List<Room> findAvailableRooms(
			@Param("roomTypeId") UUID roomTypeId,
			@Param("checkIn") LocalDate checkIn,
			@Param("checkOut") LocalDate checkOut);
	/** Active rooms ordered by display room number (matches `rooms.room_number`). */
	java.util.List<Room> findByDeletedAtIsNullOrderByRoomNumberAsc();

	java.util.Optional<Room> findByIdAndDeletedAtIsNull(UUID id);

	/** Active rooms ordered by display room number (matches `rooms.room_number`). */
	java.util.List<Room> findByDeletedAtIsNullOrderByRoomNumberAsc();

	java.util.Optional<Room> findByIdAndDeletedAtIsNull(UUID id);

	long countByRoomType_Id(UUID roomTypeId);
}
