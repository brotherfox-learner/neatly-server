package com.neatly.server.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class GeminiService {

	private final String apiKey;
	private final String model;
	private final RestClient geminiClient;

	public GeminiService(
			@Value("${gemini.api-key:}") String apiKey,
			@Value("${gemini.model:gemini-2.0-flash}") String model) {
		this.apiKey = apiKey;
		this.model = model.trim();
		this.geminiClient = RestClient.builder()
				.baseUrl("https://generativelanguage.googleapis.com")
				.build();
	}

	public boolean isConfigured() {
		return StringUtils.hasText(apiKey);
	}

	public String maskedKeyHint() {
		if (!isConfigured()) {
			log.trace("Gemini API key not configured");
			return "";
		}
		String k = apiKey.trim();
		if (k.length() <= 8) {
			return "****";
		}
		return k.substring(0, 4) + "…" + k.substring(k.length() - 4);
	}

	@SuppressWarnings("unchecked")
	public String generateText(String prompt) {
		if (!isConfigured()) {
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Gemini is not configured (GEMINI_API_KEY).");
		}
		Map<String, Object> body = Map.of(
				"contents", List.of(
						Map.of("parts", List.of(
								Map.of("text", prompt)))));
		try {
			Map<String, Object> response = geminiClient.post()
					.uri(uriBuilder -> uriBuilder
							.path("/v1beta/models/{model}:generateContent")
							.queryParam("key", apiKey.trim())
							.build(model))
					.contentType(MediaType.APPLICATION_JSON)
					.body(body)
					.retrieve()
					.body(Map.class);
			return extractText(response);
		}
		catch (RestClientResponseException e) {
			log.warn("Gemini API error status={} body={}", e.getStatusCode().value(), e.getResponseBodyAsString());
			throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Gemini request failed.");
		}
		catch (RestClientException e) {
			log.warn("Gemini request failed: {}", e.getMessage());
			throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Gemini request failed.");
		}
	}

	@SuppressWarnings("unchecked")
	private static String extractText(Map<String, Object> root) {
		if (root == null) {
			return "";
		}
		Object candidates = root.get("candidates");
		if (!(candidates instanceof List<?> list) || list.isEmpty()) {
			return "";
		}
		Object first = list.get(0);
		if (!(first instanceof Map<?, ?> cand)) {
			return "";
		}
		Object content = cand.get("content");
		if (!(content instanceof Map<?, ?> contentMap)) {
			return "";
		}
		Object parts = contentMap.get("parts");
		if (!(parts instanceof List<?> partsList) || partsList.isEmpty()) {
			return "";
		}
		Object part0 = partsList.get(0);
		if (!(part0 instanceof Map<?, ?> partMap)) {
			return "";
		}
		Object text = partMap.get("text");
		return text != null ? text.toString() : "";
	}
}
