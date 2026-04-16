package com.neatly.server.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.neatly.server.domain.Promotion;

public record PromotionResponse(
    UUID id,
    String code,
    String discountType,
    BigDecimal discountValue,
    BigDecimal maxDiscount,
    BigDecimal minSpend,
    Integer usageLimit,
    Integer perUserLimit,
    Integer usedCount,
    LocalDate startDate,
    LocalDate endDate,
    Boolean isActive,
    Instant createdAt
) {
    public static PromotionResponse from(Promotion p) {
        return new PromotionResponse(
            p.getId(),
            p.getCode(),
            p.getDiscountType(),
            p.getDiscountValue(),
            p.getMaxDiscount(),
            p.getMinSpend(),
            p.getUsageLimit(),
            p.getPerUserLimit(),
            p.getUsedCount(),
            p.getStartDate(),
            p.getEndDate(),
            p.getIsActive(),
            p.getCreatedAt()
        );
    }
}