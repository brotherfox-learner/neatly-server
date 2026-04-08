package com.neatly.server.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.neatly.server.domain.Room;
import com.neatly.server.domain.RoomType;
import com.neatly.server.domain.RoomTypeImage;
import com.neatly.server.dto.AdminCreateRoomRequest;
import com.neatly.server.dto.AdminCreateRoomResponse;
import com.neatly.server.repository.RoomRepository;
import com.neatly.server.repository.RoomTypeImageRepository;
import com.neatly.server.repository.RoomTypeRepository;

@ExtendWith(MockitoExtension.class)
class AdminRoomServiceTest {

	@Mock
	private RoomTypeRepository roomTypeRepository;
	@Mock
	private RoomRepository roomRepository;
	@Mock
	private RoomTypeImageRepository roomTypeImageRepository;
	@Mock
	private CurrentUserService currentUserService;
	@Mock
	private SupabaseStorageService supabaseStorageService;

	private AdminRoomService adminRoomService;

	@BeforeEach
	void setUp() {
		adminRoomService = new AdminRoomService(
				roomTypeRepository,
				roomRepository,
				roomTypeImageRepository,
				currentUserService,
				supabaseStorageService);
	}

	@Test
	void createRoom_success_savesRoomTypeRoomAndImagesInOrder() {
		when(roomTypeRepository.save(any(RoomType.class))).thenAnswer(invocation -> {
			RoomType roomType = invocation.getArgument(0);
			roomType.setId(UUID.randomUUID());
			return roomType;
		});
		when(roomRepository.save(any(Room.class))).thenAnswer(invocation -> {
			Room room = invocation.getArgument(0);
			room.setId(UUID.randomUUID());
			return room;
		});

		AdminCreateRoomRequest request = new AdminCreateRoomRequest(
				"Deluxe",
				"Nice room",
				2,
				new BigDecimal("3500.00"),
				new BigDecimal("3000.00"),
				"double bed",
				new BigDecimal("32.50"),
				List.of("Wifi", "Bathtub"),
				"https://cdn/main.jpg",
				List.of(
						"https://cdn/g1.jpg",
						"https://cdn/g2.jpg",
						"https://cdn/g3.jpg",
						"https://cdn/g4.jpg"));

		AdminCreateRoomResponse response = adminRoomService.createRoom(request);

		assertNotNull(response.roomTypeId());
		assertNotNull(response.roomId());
		assertNotNull(response.roomNumber());

		ArgumentCaptor<RoomTypeImage> imageCaptor = ArgumentCaptor.forClass(RoomTypeImage.class);
		verify(roomTypeImageRepository, org.mockito.Mockito.times(5)).save(imageCaptor.capture());
		List<RoomTypeImage> savedImages = imageCaptor.getAllValues();

		assertEquals(true, savedImages.get(0).getIsPrimary());
		assertEquals(0, savedImages.get(0).getSortOrder());
		assertEquals("https://cdn/main.jpg", savedImages.get(0).getImageUrl());

		assertEquals(false, savedImages.get(1).getIsPrimary());
		assertEquals(1, savedImages.get(1).getSortOrder());
		assertEquals("https://cdn/g1.jpg", savedImages.get(1).getImageUrl());
		assertEquals(4, savedImages.get(4).getSortOrder());
		assertEquals("https://cdn/g4.jpg", savedImages.get(4).getImageUrl());
	}

	@Test
	void createRoom_fail_whenGalleryLessThanFour() {
		AdminCreateRoomRequest request = new AdminCreateRoomRequest(
				"Deluxe",
				"Nice room",
				2,
				new BigDecimal("3500.00"),
				null,
				"double bed",
				new BigDecimal("32.50"),
				List.of("Wifi"),
				"https://cdn/main.jpg",
				List.of("https://cdn/g1.jpg", "https://cdn/g2.jpg", "https://cdn/g3.jpg"));

		ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> adminRoomService.createRoom(request));
		assertEquals(400, exception.getStatusCode().value());
	}

	@Test
	void createRoom_fail_whenDiscountGreaterThanBasePrice() {
		AdminCreateRoomRequest request = new AdminCreateRoomRequest(
				"Deluxe",
				"Nice room",
				2,
				new BigDecimal("3500.00"),
				new BigDecimal("4000.00"),
				"double bed",
				new BigDecimal("32.50"),
				List.of("Wifi"),
				"https://cdn/main.jpg",
				List.of(
						"https://cdn/g1.jpg",
						"https://cdn/g2.jpg",
						"https://cdn/g3.jpg",
						"https://cdn/g4.jpg"));

		ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> adminRoomService.createRoom(request));
		assertEquals(400, exception.getStatusCode().value());
	}
}
