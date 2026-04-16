package com.neatly.server.dto;

import java.util.UUID;

public record PresetQuestionDto(
		UUID id,
		String question,
		String responseType) {
}
