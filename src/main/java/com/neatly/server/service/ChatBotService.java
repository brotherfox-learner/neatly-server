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

import com.neatly.server.domain.ChatRoom;
import com.neatly.server.domain.Faq;
import com.neatly.server.domain.Promotion;
import com.neatly.server.domain.RoomType;
import com.neatly.server.domain.RoomTypeImage;
import com.neatly.server.domain.User;
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
			// Check if any keyword is present in the query
			if (faq.getKeywords() != null) {
				for (String keyword : faq.getKeywords()) {
					if (normalizedQuery.contains(keyword.toLowerCase())) {
						return getPresetAnswer(faq.getId());
					}
				}
			}
			
			// Simple fallback check against the question text itself
			if (faq.getQuestion() != null && normalizedQuery.contains(faq.getQuestion().toLowerCase())) {
				return getPresetAnswer(faq.getId());
			}
		}

		return null; // Return null if no match found
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

	private PresetAnswerDto buildPromotionCardsAnswer(Faq faq) {
		List<Promotion> promos = promotionRepository.findActivePromotions(LocalDate.now());
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

		String answer = promos.isEmpty()
				? "No promotions are currently available. Check back soon! 😊"
				: faq.getAnswer();

		return new PresetAnswerDto(answer, faq.getResponseType(), cards, null);
	}
}
