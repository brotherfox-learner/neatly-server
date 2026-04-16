package com.neatly.server.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.neatly.server.domain.PromotionUsage;

public interface PromotionUsageRepository extends JpaRepository<PromotionUsage, UUID> {

    int countByPromotionIdAndUserId(UUID promotionId, UUID userId);

    boolean existsByBookingId(UUID bookingId);
}
