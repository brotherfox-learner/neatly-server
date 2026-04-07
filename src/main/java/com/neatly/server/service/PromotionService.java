package com.neatly.server.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.neatly.server.domain.Promotion;
import com.neatly.server.dto.CreatePromotionRequest;
import com.neatly.server.dto.PromotionResponse;
import com.neatly.server.repository.PromotionRepository;
import java.util.List;
import java.time.Instant;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PromotionService {

    private final PromotionRepository promotionRepository;

    public PromotionResponse createPromotion(CreatePromotionRequest request) {

        log.info("[CREATE_PROMOTION][START] code={}", request.code());

        // เช็ค duplicate code
        if (promotionRepository.existsByCode(request.code())) {
            log.warn("[CREATE_PROMOTION][DUPLICATE] code={} already exists", request.code());
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Promotion code '" + request.code() + "' already exists"
            );
        }

        // เช็ค startDate < endDate
        if (request.startDate() != null && request.endDate() != null
                && !request.startDate().isBefore(request.endDate())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "startDate must be before endDate"
            );
        }

        Promotion promotion = new Promotion();
        promotion.setCode(request.code().toUpperCase());
        promotion.setDiscountType(request.discountType());
        promotion.setDiscountValue(request.discountValue());
        promotion.setMaxDiscount(request.maxDiscount());
        promotion.setMinSpend(request.minSpend());
        promotion.setUsageLimit(request.usageLimit());
        promotion.setPerUserLimit(request.perUserLimit());
        promotion.setStartDate(request.startDate());
        promotion.setEndDate(request.endDate());
        promotion.setIsActive(request.isActive() != null ? request.isActive() : true);

        promotionRepository.save(promotion);

        log.info("[CREATE_PROMOTION][SUCCESS] id={} code={}", promotion.getId(), promotion.getCode());

        return PromotionResponse.from(promotion);
    }

    public List<PromotionResponse> getPromotions() {
        log.info("[GET_PROMOTIONS][START]");
        List<PromotionResponse> result = promotionRepository.findAll()
                .stream()
                .map(PromotionResponse::from)
                .toList();
        log.info("[GET_PROMOTIONS][SUCCESS] count={}", result.size());
        return result;
    }

    public PromotionResponse toggleActive(UUID id) {
        log.info("[TOGGLE_PROMOTION][START] id={}", id);
    
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Promotion not found: " + id
                ));
    
        promotion.setIsActive(!promotion.getIsActive());
        promotion.setUpdatedAt(Instant.now());
        promotionRepository.save(promotion);
    
        log.info("[TOGGLE_PROMOTION][SUCCESS] id={} isActive={}", id, promotion.getIsActive());
    
        return PromotionResponse.from(promotion);
    }
}