package com.neatly.server.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.neatly.server.domain.Promotion;

public interface PromotionRepository extends JpaRepository<Promotion, UUID> {

	@Query("SELECT p FROM Promotion p WHERE p.isActive = true AND p.startDate <= :today AND p.endDate >= :today")
	List<Promotion> findActivePromotions(@Param("today") LocalDate today);
}
