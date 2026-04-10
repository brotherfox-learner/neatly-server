package com.neatly.server.dto;

import java.util.List;
import java.util.UUID;

public record FaqAdminDto(
		UUID id,
		String question,
		String answer,
		List<String> keywords,
		boolean active,
		boolean showInChat,
		String category,
		int sortOrder,
		String responseType) {
}
