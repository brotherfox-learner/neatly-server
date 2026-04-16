package com.neatly.server.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

/**
 * Mutable bean (not a record) so Jackson reliably binds JSON {@code false} for booleans on all JDK/Spring versions.
 */
@Data
public class FaqWriteRequest {

	private String question;
	private String answer;
	private List<String> keywords;
	private String responseType;
	private Integer sortOrder;

	@JsonProperty("active")
	@JsonAlias({ "is_active", "isActive" })
	private boolean active = true;

	@JsonProperty("showInChat")
	@JsonAlias({ "show_in_chat" })
	private boolean showInChat = true;
}
