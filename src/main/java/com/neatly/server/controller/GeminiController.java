package com.neatly.server.controller;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.neatly.server.dto.GeminiPromptRequest;
import com.neatly.server.dto.GeminiTextResponse;
import com.neatly.server.service.CurrentUserService;
import com.neatly.server.service.GeminiService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/ai/gemini")
@RequiredArgsConstructor
@Validated
@Slf4j
public class GeminiController {

	private final GeminiService geminiService;
	private final CurrentUserService currentUserService;

	@PostMapping("/generate")
	public GeminiTextResponse generate(@Valid @RequestBody GeminiPromptRequest request) {
		currentUserService.requireUserId();
		log.trace("POST /api/v1/ai/gemini/generate");
		String text = geminiService.generateText(request.prompt());
		return new GeminiTextResponse(text);
	}
}
