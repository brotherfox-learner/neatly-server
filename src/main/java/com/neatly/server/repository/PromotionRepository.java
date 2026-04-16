package com.neatly.server.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.neatly.server.domain.Promotion;

public interface PromotionRepository extends JpaRepository<Promotion, UUID> {
    boolean existsByCode(String code);
    Optional<Promotion> findByCodeIgnoreCase(String code);
}
