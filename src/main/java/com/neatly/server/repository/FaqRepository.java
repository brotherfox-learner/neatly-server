package com.neatly.server.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.neatly.server.domain.Faq;

public interface FaqRepository extends JpaRepository<Faq, UUID> {

	List<Faq> findByCategoryAndIsActiveTrueOrderBySortOrder(String category);

	List<Faq> findByIsActiveTrue();
}
