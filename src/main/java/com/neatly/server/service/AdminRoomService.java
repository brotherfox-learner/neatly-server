package com.neatly.server.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.time.Instant;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.neatly.server.domain.Room;
import com.neatly.server.domain.RoomType;
import com.neatly.server.domain.RoomTypeImage;
import com.neatly.server.dto.AdminCreateRoomRequest;
import com.neatly.server.dto.AdminCreateRoomResponse;
import com.neatly.server.dto.AdminRoomDetailResponse;
import com.neatly.server.dto.AdminRoomListItemResponse;
import com.neatly.server.repository.RoomRepository;
import com.neatly.server.repository.RoomTypeImageRepository;
import com.neatly.server.repository.RoomTypeRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminRoomService {

	private final RoomTypeRepository roomTypeRepository;
	private final RoomRepository roomRepository;
	private final RoomTypeImageRepository roomTypeImageRepository;
	private final CurrentUserService currentUserService;
	private final SupabaseStorageService supabaseStorageService;

	public String uploadRoomImage(MultipartFile file) {
		return supabaseStorageService.uploadRoomImage(
				currentUserService.requireUserId(),
				file,
				currentUserService.requireAccessToken());
	}

	@Transactional(readOnly = true)
	public List<AdminRoomListItemResponse> listRooms() {
		return roomRepository.findByDeletedAtIsNullOrderByUpdatedAtDesc().stream()
				.map(room -> {
					RoomType roomType = room.getRoomType();
					String imageUrl = roomTypeImageRepository
							.findFirstByRoomType_IdAndIsPrimaryTrueOrderBySortOrderAsc(roomType.getId())
							.or(() -> roomTypeImageRepository.findFirstByRoomType_IdOrderBySortOrderAsc(roomType.getId()))
							.map(RoomTypeImage::getImageUrl)
							.orElse("");
					return new AdminRoomListItemResponse(
							room.getId(),
							imageUrl,
							roomType.getName(),
							roomType.getBasePrice(),
							roomType.getDiscountedPrice(),
							roomType.getMaxOccupancy(),
							room.getBedType(),
							room.getRoomSizeSqm(),
							room.getUpdatedAt());
				})
				.toList();
	}

	@Transactional(readOnly = true)
	public AdminRoomDetailResponse getRoomDetail(UUID roomId) {
		Room room = roomRepository.findByIdAndDeletedAtIsNull(roomId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));
		RoomType roomType = room.getRoomType();
		List<RoomTypeImage> images = roomTypeImageRepository.findByRoomType_IdOrderBySortOrderAsc(roomType.getId());
		String mainImageUrl = images.stream()
				.filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
				.findFirst()
				.or(() -> images.stream().findFirst())
				.map(RoomTypeImage::getImageUrl)
				.orElse("");
		List<String> galleryImageUrls = images.stream()
				.filter(img -> !Boolean.TRUE.equals(img.getIsPrimary()))
				.sorted(Comparator.comparing(img -> img.getSortOrder() == null ? Integer.MAX_VALUE : img.getSortOrder()))
				.map(RoomTypeImage::getImageUrl)
				.toList();
		return new AdminRoomDetailResponse(
				room.getId(),
				roomType.getName(),
				roomType.getDescription(),
				roomType.getMaxOccupancy(),
				roomType.getBasePrice(),
				roomType.getDiscountedPrice(),
				room.getBedType(),
				room.getRoomSizeSqm(),
				new ArrayList<>(roomType.getAmenities() == null ? List.of() : roomType.getAmenities()),
				mainImageUrl,
				galleryImageUrls);
	}

	@Transactional
	public AdminCreateRoomResponse createRoom(AdminCreateRoomRequest request) {
		validateRequest(request);

		RoomType roomType = new RoomType();
		roomType.setName(request.roomTypeName().trim());
		roomType.setDescription(trimToNull(request.description()));
		roomType.setMaxOccupancy(request.maxOccupancy());
		roomType.setBasePrice(request.basePrice());
		roomType.setDiscountedPrice(request.discountedPrice());
		roomType.setAmenities(new ArrayList<>(request.amenities().stream()
				.map(String::trim)
				.filter(s -> !s.isBlank())
				.toList()));
		roomTypeRepository.save(roomType);

		Room room = new Room();
		room.setRoomType(roomType);
		room.setRoomNumber(generateRoomNumber());
		room.setBedType(request.bedType().trim());
		room.setRoomSizeSqm(request.roomSizeSqm());
		roomRepository.save(room);

		createRoomTypeImages(roomType, request.mainImageUrl(), request.galleryImageUrls());

		return new AdminCreateRoomResponse(roomType.getId(), room.getId(), room.getRoomNumber());
	}

	@Transactional
	public AdminRoomDetailResponse updateRoom(UUID roomId, AdminCreateRoomRequest request) {
		validateRequest(request);
		Room room = roomRepository.findByIdAndDeletedAtIsNull(roomId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));
		RoomType roomType = room.getRoomType();

		roomType.setName(request.roomTypeName().trim());
		roomType.setDescription(trimToNull(request.description()));
		roomType.setMaxOccupancy(request.maxOccupancy());
		roomType.setBasePrice(request.basePrice());
		roomType.setDiscountedPrice(request.discountedPrice());
		roomType.setAmenities(new ArrayList<>(request.amenities().stream()
				.map(String::trim)
				.filter(s -> !s.isBlank())
				.toList()));
		roomTypeRepository.save(roomType);

		room.setBedType(request.bedType().trim());
		room.setRoomSizeSqm(request.roomSizeSqm());
		roomRepository.save(room);

		roomTypeImageRepository.deleteByRoomType_Id(roomType.getId());
		// Force DELETE to hit DB before INSERT to avoid unique(room_type_id, sort_order) collisions.
		roomTypeImageRepository.flush();
		createRoomTypeImages(roomType, request.mainImageUrl(), request.galleryImageUrls());
		return getRoomDetail(roomId);
	}

	@Transactional
	public void deleteRoom(UUID roomId) {
		Room room = roomRepository.findByIdAndDeletedAtIsNull(roomId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));
		room.setDeletedAt(Instant.now());
		roomRepository.save(room);
	}

	private void createRoomTypeImages(RoomType roomType, String mainImageUrl, List<String> galleryImageUrls) {
		RoomTypeImage mainImage = new RoomTypeImage();
		mainImage.setRoomType(roomType);
		mainImage.setImageUrl(mainImageUrl.trim());
		mainImage.setIsPrimary(true);
		mainImage.setSortOrder(0);
		roomTypeImageRepository.save(mainImage);

		for (int i = 0; i < galleryImageUrls.size(); i++) {
			RoomTypeImage galleryImage = new RoomTypeImage();
			galleryImage.setRoomType(roomType);
			galleryImage.setImageUrl(galleryImageUrls.get(i).trim());
			galleryImage.setIsPrimary(false);
			galleryImage.setSortOrder(i + 1);
			roomTypeImageRepository.save(galleryImage);
		}
	}

	private void validateRequest(AdminCreateRoomRequest request) {
		if (request.galleryImageUrls().size() < 4) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image gallery must contain at least 4 images.");
		}
		if (request.discountedPrice() != null && request.discountedPrice().compareTo(request.basePrice()) > 0) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Discounted price must be less than or equal to base price.");
		}
	}

	private String generateRoomNumber() {
		return "AUTO-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
	}

	private static String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
