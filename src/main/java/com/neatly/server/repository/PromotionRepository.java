package com.neatly.server.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.neatly.server.domain.Promotion;

public interface PromotionRepository extends JpaRepository<Promotion, UUID> {

	boolean existsByCode(String code);

	Optional<Promotion> findByCodeIgnoreCase(String code);

	/**
	 * Promotions valid on {@code onDate}: active flag, and date window when start/end are set.
	 * Null start/end treated as open-ended for that side (PgBouncer-safe via simple JPQL).
	 */
	@Query("""
			SELECT p FROM Promotion p
			WHERE p.isActive = true
			  AND (p.startDate IS NULL OR p.startDate <= :onDate)
			  AND (p.endDate IS NULL OR p.endDate >= :onDate)
			ORDER BY p.code ASC
			""")
	List<Promotion> findActivePromotions(@Param("onDate") LocalDate onDate);
}
