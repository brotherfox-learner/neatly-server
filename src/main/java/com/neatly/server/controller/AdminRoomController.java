package com.neatly.server.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.multipart.MultipartFile;

import com.neatly.server.dto.AdminCreateRoomRequest;
import com.neatly.server.dto.AdminCreateRoomResponse;
import com.neatly.server.dto.AdminRoomDetailResponse;
import com.neatly.server.dto.AdminRoomListItemResponse;
import com.neatly.server.dto.AdminUpdateRoomStatusRequest;
import com.neatly.server.dto.RoomImageUploadResponse;
import com.neatly.server.service.AdminRoomService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin/rooms")
@RequiredArgsConstructor
public class AdminRoomController {

	private final AdminRoomService adminRoomService;

	@GetMapping
	public List<AdminRoomListItemResponse> list() {
		return adminRoomService.listRooms();
	}

	@GetMapping("/{roomId}")
	public AdminRoomDetailResponse detail(@PathVariable UUID roomId) {
		return adminRoomService.getRoomDetail(roomId);
	}

	@PostMapping("/images")
	public RoomImageUploadResponse uploadImage(@RequestParam("file") MultipartFile file) {
		String imageUrl = adminRoomService.uploadRoomImage(file);
		return new RoomImageUploadResponse(imageUrl);
	}

	@PostMapping
	public AdminCreateRoomResponse create(@Valid @RequestBody AdminCreateRoomRequest request) {
		return adminRoomService.createRoom(request);
	}

	@PutMapping("/{roomId}")
	public AdminRoomDetailResponse update(@PathVariable UUID roomId, @Valid @RequestBody AdminCreateRoomRequest request) {
		return adminRoomService.updateRoom(roomId, request);
	}

	@PatchMapping("/{roomId}/status")
	public void updateStatus(@PathVariable UUID roomId, @Valid @RequestBody AdminUpdateRoomStatusRequest request) {
		adminRoomService.updateRoomStatus(roomId, request.status());
	}

	@DeleteMapping("/{roomId}")
	public void delete(@PathVariable UUID roomId) {
		adminRoomService.deleteRoom(roomId);
	}
}
