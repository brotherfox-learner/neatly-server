package com.neatly.server.dto;

import java.util.List;

public record FaqWriteRequest(
		String question,
		String answer,
		List<String> keywords,
		String responseType,
		Integer sortOrder,
		Boolean active) {
}
