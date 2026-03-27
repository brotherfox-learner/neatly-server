package com.neatly.server.dto;

import jakarta.validation.constraints.NotBlank;

public record GeminiPromptRequest(@NotBlank String prompt) {
}
