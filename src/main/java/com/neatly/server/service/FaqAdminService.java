package com.neatly.server.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.neatly.server.constant.ChatbotFaqKeys;
import com.neatly.server.domain.Faq;
import com.neatly.server.dto.ChatbotDefaultsDto;
import com.neatly.server.dto.ChatbotDefaultsUpdateRequest;
import com.neatly.server.dto.FaqAdminDto;
import com.neatly.server.dto.FaqWriteRequest;
import com.neatly.server.repository.FaqRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FaqAdminService {

	private static final Set<String> ALLOWED_RESPONSE_TYPES = Set.of(
			"text",
			"room_cards",
			"options",
			"promotion_cards",
			"contact_admin");

	private final FaqRepository faqRepository;
	private final ChatBotService chatBotService;

	public List<FaqAdminDto> listPresets() {
		return faqRepository.findByCategoryOrderBySortOrderAsc(ChatbotFaqKeys.CATEGORY_PRESET)
				.stream()
				.map(this::toDto)
				.toList();
	}

	public ChatbotDefaultsDto getSettings() {
		return chatBotService.getChatDefaults();
	}

	@Transactional
	public ChatbotDefaultsDto updateSettings(ChatbotDefaultsUpdateRequest req) {
		String greeting = req.greeting() != null ? req.greeting() : "";
		String autoReply = req.autoReply() != null ? req.autoReply() : "";
		upsertSettingsRow(ChatbotFaqKeys.QUESTION_GREETING, greeting);
		upsertSettingsRow(ChatbotFaqKeys.QUESTION_AUTO_REPLY, autoReply);
		return new ChatbotDefaultsDto(greeting, autoReply);
	}

	@Transactional
	public FaqAdminDto createPreset(FaqWriteRequest req) {
		if (req.getQuestion() == null || req.getQuestion().isBlank()) {
			throw new IllegalArgumentException("Topic (question) is required");
		}
		String responseType = normalizeResponseType(req.getResponseType());
		Faq f = new Faq();
		f.setCategory(ChatbotFaqKeys.CATEGORY_PRESET);
		f.setQuestion(req.getQuestion().trim());
		f.setAnswer(req.getAnswer() != null ? req.getAnswer() : "");
		f.setKeywords(toKeywordArray(req.getKeywords()));
		f.setResponseType(responseType);
		f.setIsActive(req.isActive());
		f.setShowInChat(req.isShowInChat());
		f.setSortOrder(req.getSortOrder() != null ? req.getSortOrder() : nextPresetSortOrder());
		return toDto(faqRepository.save(f));
	}

	@Transactional
	public FaqAdminDto updatePreset(UUID id, FaqWriteRequest req) {
		Faq f = loadPresetOrThrow(id);
		if (req.getQuestion() != null && !req.getQuestion().isBlank()) {
			f.setQuestion(req.getQuestion().trim());
		}
		if (req.getAnswer() != null) {
			f.setAnswer(req.getAnswer());
		}
		if (req.getKeywords() != null) {
			f.setKeywords(toKeywordArray(req.getKeywords()));
		}
		if (req.getResponseType() != null) {
			f.setResponseType(normalizeResponseType(req.getResponseType()));
		}
		if (req.getSortOrder() != null) {
			f.setSortOrder(req.getSortOrder());
		}
		f.setIsActive(req.isActive());
		f.setShowInChat(req.isShowInChat());
		return toDto(faqRepository.save(f));
	}

	@Transactional
	public void deletePreset(UUID id) {
		Faq f = loadPresetOrThrow(id);
		faqRepository.delete(f);
	}

	private Faq loadPresetOrThrow(UUID id) {
		Faq f = faqRepository.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("FAQ not found"));
		if (!ChatbotFaqKeys.CATEGORY_PRESET.equals(f.getCategory())) {
			throw new IllegalArgumentException("Not a chatbot preset row");
		}
		return f;
	}

	private void upsertSettingsRow(String questionKey, String answer) {
		Faq f = faqRepository
				.findFirstByCategoryAndQuestion(ChatbotFaqKeys.CATEGORY_SETTINGS, questionKey)
				.orElseGet(() -> newSettingsFaq(questionKey));
		f.setAnswer(answer);
		faqRepository.save(f);
	}

	private Faq newSettingsFaq(String questionKey) {
		Faq f = new Faq();
		f.setCategory(ChatbotFaqKeys.CATEGORY_SETTINGS);
		f.setQuestion(questionKey);
		f.setAnswer("");
		f.setKeywords(new String[0]);
		f.setResponseType("text");
		f.setIsActive(true);
		f.setShowInChat(true);
		f.setSortOrder(0);
		return f;
	}

	private int nextPresetSortOrder() {
		return faqRepository.findByCategoryOrderBySortOrderAsc(ChatbotFaqKeys.CATEGORY_PRESET).stream()
				.mapToInt(x -> x.getSortOrder() != null ? x.getSortOrder() : 0)
				.max()
				.orElse(0) + 1;
	}

	private String normalizeResponseType(String raw) {
		if (raw == null || raw.isBlank()) {
			return "text";
		}
		String t = raw.trim().toLowerCase(Locale.ROOT).replace('-', '_');
		if (!ALLOWED_RESPONSE_TYPES.contains(t)) {
			throw new IllegalArgumentException("Invalid responseType: " + raw.trim());
		}
		return t;
	}

	private static String[] toKeywordArray(List<String> list) {
		if (list == null || list.isEmpty()) {
			return new String[0];
		}
		return list.toArray(new String[0]);
	}

	private static List<String> keywordsAsList(String[] keywords) {
		if (keywords == null || keywords.length == 0) {
			return List.of();
		}
		return List.copyOf(Arrays.asList(keywords));
	}

	private FaqAdminDto toDto(Faq f) {
		return new FaqAdminDto(
				f.getId(),
				f.getQuestion(),
				f.getAnswer() != null ? f.getAnswer() : "",
				keywordsAsList(f.getKeywords()),
				Boolean.TRUE.equals(f.getIsActive()),
				!Boolean.FALSE.equals(f.getShowInChat()),
				f.getCategory() != null ? f.getCategory() : "general",
				f.getSortOrder() != null ? f.getSortOrder() : 0,
				f.getResponseType() != null ? f.getResponseType() : "text");
	}
}
