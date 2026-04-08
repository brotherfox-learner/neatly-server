package com.neatly.server.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.neatly.server.dto.AvatarUploadResponse;
import com.neatly.server.dto.MeProfileUpdateRequest;
import com.neatly.server.dto.UserResponse;
import com.neatly.server.service.MeService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
@Slf4j
public class MeController {

	private final MeService meService;

	@GetMapping
	public UserResponse me() {
		log.trace("GET /api/v1/me");
		return meService.getCurrentUserProfile();
	}

	@PutMapping
	public UserResponse update(@Valid @RequestBody MeProfileUpdateRequest request) {
		log.trace("PUT /api/v1/me");
		return meService.updateCurrentUserProfile(request);
	}

	@PostMapping("/avatar")
	public AvatarUploadResponse uploadAvatar(@RequestParam("file") MultipartFile file) {
		log.trace("POST /api/v1/me/avatar");
		String avatarUrl = meService.uploadCurrentUserAvatar(file);
		return new AvatarUploadResponse(avatarUrl);
	}
}
