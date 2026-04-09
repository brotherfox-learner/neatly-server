package com.neatly.server.dto;

import java.util.List;
import java.util.Map;

public record PresetAnswerDto(
		String answer,
		String responseType,
		List<Map<String, Object>> cards,
		List<Map<String, Object>> options) {
}
