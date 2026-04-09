package com.neatly.server.controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.neatly.server.dto.ChatbotDefaultsDto;
import com.neatly.server.dto.ChatbotDefaultsUpdateRequest;
import com.neatly.server.dto.FaqAdminDto;
import com.neatly.server.dto.FaqWriteRequest;
import com.neatly.server.service.FaqAdminService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin/chatbot")
@RequiredArgsConstructor
public class AdminChatbotController {

	private final FaqAdminService faqAdminService;

	@GetMapping("/presets")
	public List<FaqAdminDto> listPresets() {
		return faqAdminService.listPresets();
	}

	@PostMapping("/presets")
	@ResponseStatus(HttpStatus.CREATED)
	public FaqAdminDto createPreset(@RequestBody FaqWriteRequest body) {
		return faqAdminService.createPreset(body);
	}

	@PutMapping("/presets/{id}")
	public FaqAdminDto updatePreset(@PathVariable UUID id, @RequestBody FaqWriteRequest body) {
		return faqAdminService.updatePreset(id, body);
	}

	@DeleteMapping("/presets/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deletePreset(@PathVariable UUID id) {
		faqAdminService.deletePreset(id);
	}

	@GetMapping("/settings")
	public ChatbotDefaultsDto getSettings() {
		return faqAdminService.getSettings();
	}

	@PutMapping("/settings")
	public ChatbotDefaultsDto putSettings(@RequestBody ChatbotDefaultsUpdateRequest body) {
		return faqAdminService.updateSettings(body);
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException ex) {
		return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
	}
}
