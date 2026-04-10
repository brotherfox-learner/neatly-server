package com.neatly.server.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.neatly.server.constant.ChatbotFaqKeys;
import com.neatly.server.domain.ChatRoom;
import com.neatly.server.domain.Faq;
import com.neatly.server.domain.Promotion;
import com.neatly.server.domain.RoomType;
import com.neatly.server.domain.RoomTypeImage;
import com.neatly.server.domain.User;
import com.neatly.server.dto.ChatbotDefaultsDto;
import com.neatly.server.dto.PresetAnswerDto;
import com.neatly.server.dto.PresetQuestionDto;
import com.neatly.server.repository.ChatRoomRepository;
import com.neatly.server.repository.FaqRepository;
import com.neatly.server.repository.PromotionRepository;
import com.neatly.server.repository.RoomTypeImageRepository;
import com.neatly.server.repository.RoomTypeRepository;
import com.neatly.server.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatBotService {

	private final FaqRepository faqRepository;
	private final RoomTypeRepository roomTypeRepository;
	private final RoomTypeImageRepository roomTypeImageRepository;
	private final PromotionRepository promotionRepository;
	private final ChatRoomRepository chatRoomRepository;
	private final UserRepository userRepository;

	public List<PresetQuestionDto> getPresetQuestions() {
		return faqRepository.findByCategoryAndIsActiveTrueOrderBySortOrder("preset")
				.stream()
				.filter(faq -> !Boolean.FALSE.equals(faq.getShowInChat()))
				.map(faq -> new PresetQuestionDto(faq.getId(), faq.getQuestion(), faq.getResponseType()))
				.toList();
	}

	@Transactional(readOnly = true)
	public PresetAnswerDto getPresetAnswer(UUID faqId) {
		Faq faq = faqRepository.findById(faqId)
				.orElseThrow(() -> new RuntimeException("FAQ not found"));

		String responseType = faq.getResponseType();

		return switch (responseType) {
			case "room_cards" -> buildRoomCardsAnswer(faq);
			case "options" -> buildOptionsAnswer(faq);
			case "promotion_cards" -> buildPromotionCardsAnswer(faq);
			case "contact_admin" -> {
				String intro = faq.getAnswer() != null ? faq.getAnswer() : "";
				yield new PresetAnswerDto(intro, "contact_admin", null, null);
			}
			default -> new PresetAnswerDto(faq.getAnswer(), responseType, null, null);
		};
	}

	@Transactional(readOnly = true)
	public PresetAnswerDto searchFaq(String query) {
		if (query == null || query.trim().isEmpty()) {
			return null;
		}

		String normalizedQuery = query.toLowerCase();
		List<Faq> activeFaqs = faqRepository.findByIsActiveTrue();

		for (Faq faq : activeFaqs) {
			if (ChatbotFaqKeys.CATEGORY_SETTINGS.equals(faq.getCategory())) {
				continue;
			}
			String[] keywords = faq.getKeywords();
			if (keywords != null && keywords.length > 0) {
				for (String keyword : keywords) {
					if (keywordMatches(normalizedQuery, keyword)) {
						return getPresetAnswer(faq.getId());
					}
				}
			}

			if (faq.getQuestion() != null) {
				String q = faq.getQuestion().toLowerCase();
				if (normalizedQuery.contains(q) || keywordMatches(normalizedQuery, faq.getQuestion())) {
					return getPresetAnswer(faq.getId());
				}
			}
		}

		return null; // Return null if no match found
	}

	private static final String DEFAULT_GREETING = "Welcome to Neatly Hotel! 🌟 I'm your virtual assistant. "
			+ "Choose a topic you'd like to know more about. I'm here to help! 😊";

	private static final String DEFAULT_AUTO_REPLY = "Thanks for reaching out! If you need more help, call us at "
			+ "029872345 — we're happy to assist you! 😊";

	@Transactional(readOnly = true)
	public ChatbotDefaultsDto getChatDefaults() {
		String greeting = faqRepository
				.findFirstByCategoryAndQuestion(ChatbotFaqKeys.CATEGORY_SETTINGS, ChatbotFaqKeys.QUESTION_GREETING)
				.map(Faq::getAnswer)
				.filter(s -> s != null && !s.isBlank())
				.orElse(DEFAULT_GREETING);
		String autoReply = faqRepository
				.findFirstByCategoryAndQuestion(ChatbotFaqKeys.CATEGORY_SETTINGS, ChatbotFaqKeys.QUESTION_AUTO_REPLY)
				.map(Faq::getAnswer)
				.filter(s -> s != null && !s.isBlank())
				.orElse(DEFAULT_AUTO_REPLY);
		return new ChatbotDefaultsDto(greeting, autoReply);
	}

	/**
	 * Reuses the latest non-closed room (OPEN, WAITING_AGENT, ACTIVE) so refresh keeps the same session.
	 */
	@Transactional
	public ChatRoom createOrReuseChatRoom(UUID userId) {
		return chatRoomRepository
				.findFirstByUser_IdAndStatusInOrderByUpdatedAtDesc(userId,
						List.of("OPEN", "WAITING_AGENT", "ACTIVE"))
				.orElseGet(() -> {
					ChatRoom room = new ChatRoom();
					User user = userRepository.getReferenceById(userId);
					room.setUser(user);
					room.setStatus("OPEN");
					return chatRoomRepository.save(room);
				});
	}

	@Transactional(readOnly = true)
	public Optional<ChatRoom> findCurrentUserSessionRoom(UUID userId) {
		return chatRoomRepository.findFirstByUser_IdAndStatusInOrderByUpdatedAtDesc(userId,
				List.of("OPEN", "WAITING_AGENT", "ACTIVE"));
	}

	private PresetAnswerDto buildRoomCardsAnswer(Faq faq) {
		List<RoomType> roomTypes = roomTypeRepository.findAll();
		List<Map<String, Object>> cards = new ArrayList<>();

		for (RoomType rt : roomTypes) {
			Map<String, Object> card = new HashMap<>();
			card.put("id", rt.getId().toString());
			card.put("name", rt.getName());
			card.put("basePrice", rt.getBasePrice());
			card.put("discountedPrice", rt.getDiscountedPrice());

			// Get primary image
			List<RoomTypeImage> images = roomTypeImageRepository.findByRoomType_IdOrderBySortOrderAsc(rt.getId());
			String imageUrl = images.stream()
					.filter(RoomTypeImage::getIsPrimary)
					.findFirst()
					.or(() -> images.stream().findFirst())
					.map(RoomTypeImage::getImageUrl)
					.orElse(null);
			card.put("imageUrl", imageUrl);

			String desc = rt.getDescription();
			if (desc != null && !desc.isBlank()) {
				String t = desc.trim();
				if (t.length() > 110) {
					t = t.substring(0, 107) + "...";
				}
				card.put("description", t);
			} else {
				card.put("description", null);
			}

			cards.add(card);
		}

		return new PresetAnswerDto(faq.getAnswer(), faq.getResponseType(), cards, null);
	}

	private PresetAnswerDto buildOptionsAnswer(Faq faq) {
		List<Map<String, Object>> options = List.of(
				Map.of("label", "credit card", "key", "credit_card",
						"detail", "We accept major credit cards including Visa and MasterCard."),
				Map.of("label", "Cash", "key", "cash",
						"detail",
						"You may settle payment in cash or by cheque at the hotel. No payment is required until check-in."));

		String intro = "Here are the payment methods we accept. Tap to see more details 💳💵";
		String answer = faq.getAnswer() != null && !faq.getAnswer().isBlank() ? faq.getAnswer() : intro;

		return new PresetAnswerDto(answer, faq.getResponseType(), null, options);
	}

	private List<Map<String, Object>> mapPromotionsToCards(List<Promotion> promos) {
		List<Map<String, Object>> cards = new ArrayList<>();
		for (Promotion p : promos) {
			Map<String, Object> card = new HashMap<>();
			card.put("id", p.getId().toString());
			card.put("code", p.getCode());
			card.put("discountType", p.getDiscountType());
			card.put("discountValue", p.getDiscountValue());
			card.put("maxDiscount", p.getMaxDiscount());
			card.put("minSpend", p.getMinSpend());
			card.put("startDate", p.getStartDate() != null ? p.getStartDate().toString() : null);
			card.put("endDate", p.getEndDate() != null ? p.getEndDate().toString() : null);
			cards.add(card);
		}
		return cards;
	}

	private PresetAnswerDto buildPromotionCardsAnswer(Faq faq) {
		List<Promotion> promos = promotionRepository.findActivePromotions(LocalDate.now());
		List<Map<String, Object>> cards = mapPromotionsToCards(promos);
		String answer;
		if (promos.isEmpty()) {
			answer = "No promotions are currently available. Check back soon! 😊";
		} else {
			String faqAns = faq.getAnswer();
			answer = faqAns != null && !faqAns.isBlank() ? faqAns : "🎉 Here are our current promotions!";
		}
		return new PresetAnswerDto(answer, faq.getResponseType(), cards, null);
	}

	/**
	 * When no FAQ row exists for promotions, still return live promotion cards for the chat shortcut button.
	 */
	@Transactional(readOnly = true)
	public PresetAnswerDto getPromotionPresetAnswer() {
		return faqRepository.findByCategoryAndIsActiveTrueOrderBySortOrder("preset").stream()
				.filter(f -> "promotion_cards".equals(f.getResponseType()))
				.findFirst()
				.map(this::getPresetAnswer)
				.orElseGet(this::buildPromotionCardsAnswerStandalone);
	}

	private PresetAnswerDto buildPromotionCardsAnswerStandalone() {
		List<Promotion> promos = promotionRepository.findActivePromotions(LocalDate.now());
		List<Map<String, Object>> cards = mapPromotionsToCards(promos);
		String answer = promos.isEmpty()
				? "No promotions are currently available. Check back soon! 😊"
				: "🎉 Here are our current promotions!";
		return new PresetAnswerDto(answer, "promotion_cards", cards, null);
	}

	/**
	 * Match guest message to a keyword or topic phrase: full substring either way, with a short-query
	 * guard so typing "book" still matches a stored keyword "booking" (but not trivial single-letter noise).
	 */
	private boolean keywordMatches(String normalizedQuery, String keywordOrPhrase) {
		if (keywordOrPhrase == null || keywordOrPhrase.isBlank()) {
			return false;
		}
		String kw = keywordOrPhrase.toLowerCase().trim();
		if (kw.isEmpty()) {
			return false;
		}
		if (normalizedQuery.contains(kw)) {
			return true;
		}
		final int minLen = 3;
		return normalizedQuery.length() >= minLen && kw.contains(normalizedQuery);
	}
}
