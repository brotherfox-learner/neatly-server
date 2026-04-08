package com.neatly.server.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.neatly.server.domain.RoomTypeImage;

public interface RoomTypeImageRepository extends JpaRepository<RoomTypeImage, UUID> {
	java.util.Optional<RoomTypeImage> findFirstByRoomType_IdAndIsPrimaryTrueOrderBySortOrderAsc(UUID roomTypeId);

	java.util.Optional<RoomTypeImage> findFirstByRoomType_IdOrderBySortOrderAsc(UUID roomTypeId);

	java.util.List<RoomTypeImage> findByRoomType_IdOrderBySortOrderAsc(UUID roomTypeId);

	void deleteByRoomType_Id(UUID roomTypeId);
}
