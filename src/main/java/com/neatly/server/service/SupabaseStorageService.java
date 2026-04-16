package com.neatly.server.service;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SupabaseStorageService {

	private static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024;
	private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

	private final String supabaseUrl;
	private final String supabaseApiKey;
	private final String bucket;
	private final RestClient restClient;

	public SupabaseStorageService(
			@Value("${supabase.url:}") String supabaseUrl,
			@Value("${supabase.api-key:}") String supabaseApiKey,
			@Value("${supabase.bucket:uploads}") String bucket) {
		this.supabaseUrl = trimTrailingSlash(supabaseUrl);
		this.supabaseApiKey = supabaseApiKey != null ? supabaseApiKey.trim() : "";
		this.bucket = StringUtils.hasText(bucket) ? bucket.trim() : "uploads";
		this.restClient = RestClient.builder().baseUrl(this.supabaseUrl).build();
		log.info(
				"SupabaseStorageService init: bucket='{}', supabase.url configured={}, supabase.api-key configured={}",
				this.bucket,
				StringUtils.hasText(this.supabaseUrl),
				StringUtils.hasText(this.supabaseApiKey));
	}

	public String uploadProfileAvatar(UUID userId, MultipartFile file, String userAccessToken) {
		validateStorageConfigured();
		validateImageFile(file, "Avatar");
		if (!StringUtils.hasText(userAccessToken)) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
		}

		String contentType = file.getContentType().trim().toLowerCase(Locale.ROOT);
		String extension = extensionFor(contentType);
		String objectPath = "profiles/" + userId + "/" + System.currentTimeMillis() + "-" + UUID.randomUUID() + extension;
		return uploadToBucket(objectPath, file, contentType, userAccessToken, "Avatar upload failed.", "Invalid avatar file.");
	}

	/**
	 * Room / gallery images for admin room form; stored under {@code rooms/{userId}/...}.
	 */
	public String uploadRoomImage(UUID userId, MultipartFile file, String userAccessToken) {
		validateStorageConfigured();
		validateImageFile(file, "Image");
		if (!StringUtils.hasText(userAccessToken)) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
		}

		String contentType = file.getContentType().trim().toLowerCase(Locale.ROOT);
		String extension = extensionFor(contentType);
		String objectPath = "rooms/" + userId + "/" + System.currentTimeMillis() + "-" + UUID.randomUUID() + extension;
		return uploadToBucket(objectPath, file, contentType, userAccessToken, "Image upload failed.", "Invalid image file.");
	}

	private String uploadToBucket(
			String objectPath,
			MultipartFile file,
			String contentType,
			String userAccessToken,
			String remoteFailureMessage,
			String readFailureMessage) {
		String uploadPath = "/storage/v1/object/" + bucket + "/" + objectPath;

		try {
			byte[] bytes = file.getBytes();
			RestClient.RequestBodySpec request = restClient.post()
					.uri(uploadPath)
					.header("authorization", "Bearer " + userAccessToken.trim())
					.header("apikey", supabaseApiKey)
					.header("x-upsert", "true")
					.contentType(MediaType.parseMediaType(contentType));

			request.body(bytes)
					.retrieve()
					.toBodilessEntity();

			return supabaseUrl + "/storage/v1/object/public/" + bucket + "/" + objectPath;
		}
		catch (RestClientResponseException e) {
			log.warn("Supabase Storage upload failed status={} body={}", e.getStatusCode().value(), e.getResponseBodyAsString());
			throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, remoteFailureMessage);
		}
		catch (RestClientException e) {
			log.warn("Supabase Storage upload failed: {}", e.getMessage());
			throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, remoteFailureMessage);
		}
		catch (IOException e) {
			log.warn("File read failed: {}", e.getMessage());
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, readFailureMessage);
		}
	}

	private void validateStorageConfigured() {
		if (!StringUtils.hasText(supabaseUrl) || !StringUtils.hasText(supabaseApiKey)) {
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Supabase Storage is not configured.");
		}
	}

	private static void validateImageFile(MultipartFile file, String label) {
		if (file == null || file.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + " file is required.");
		}
		if (file.getSize() > MAX_FILE_SIZE_BYTES) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + " must be 5MB or smaller.");
		}
		String contentType = file.getContentType();
		if (!StringUtils.hasText(contentType) || !ALLOWED_IMAGE_TYPES.contains(contentType.trim().toLowerCase(Locale.ROOT))) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + " must be JPG, PNG, or WEBP.");
		}
	}

	private static String extensionFor(String contentType) {
		return switch (contentType) {
			case "image/png" -> ".png";
			case "image/webp" -> ".webp";
			case "image/jpeg" -> ".jpg";
			default -> ".bin";
		};
	}

	private static String trimTrailingSlash(String value) {
		if (value == null) {
			return "";
		}
		String out = value.trim();
		while (out.endsWith("/")) {
			out = out.substring(0, out.length() - 1);
		}
		return out;
	}
}
