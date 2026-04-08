package com.neatly.server.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.neatly.server.domain.Promotion;
import com.neatly.server.dto.CreatePromotionRequest;
import com.neatly.server.dto.PromotionResponse;
import com.neatly.server.dto.ValidatePromoRequest;
import com.neatly.server.dto.ValidatePromoResponse;
import com.neatly.server.repository.PromotionRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
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

    public ValidatePromoResponse validatePromo(ValidatePromoRequest request) {
        String code = request.code();
        BigDecimal orderTotal = request.orderTotal() != null ? request.orderTotal() : BigDecimal.ZERO;

        log.info("[VALIDATE_PROMO][START] code={} orderTotal={}", code, orderTotal);

        Promotion promo = promotionRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Promo code not found"));

        if (!Boolean.TRUE.equals(promo.getIsActive())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Promo code is inactive");
        }

        LocalDate today = LocalDate.now();
        if (promo.getStartDate() != null && today.isBefore(promo.getStartDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Promo code is not yet valid");
        }
        if (promo.getEndDate() != null && today.isAfter(promo.getEndDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Promo code has expired");
        }

        if (promo.getUsageLimit() != null && promo.getUsedCount() >= promo.getUsageLimit()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Promo code usage limit reached");
        }

        BigDecimal minSpend = promo.getMinSpend() != null ? promo.getMinSpend() : BigDecimal.ZERO;
        if (orderTotal.compareTo(minSpend) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Minimum spend of " + minSpend.toPlainString() + " required");
        }

        BigDecimal discountAmount;
        if ("PERCENTAGE".equalsIgnoreCase(promo.getDiscountType())) {
            discountAmount = orderTotal.multiply(promo.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            if (promo.getMaxDiscount() != null && discountAmount.compareTo(promo.getMaxDiscount()) > 0) {
                discountAmount = promo.getMaxDiscount();
            }
        } else {
            discountAmount = promo.getDiscountValue();
        }

        log.info("[VALIDATE_PROMO][SUCCESS] code={} discountAmount={}", code, discountAmount);
        return new ValidatePromoResponse(promo.getCode(), discountAmount, promo.getDiscountType());
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