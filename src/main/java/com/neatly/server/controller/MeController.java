package com.neatly.server.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.neatly.server.dto.UserResponse;
import com.neatly.server.service.MeService;

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
}
