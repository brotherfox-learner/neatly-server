package com.neatly.server.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "promotions")
@Getter
@Setter
@NoArgsConstructor
public class Promotion {

	@Id
	@UuidGenerator
	private UUID id;

	@Column(nullable = false, unique = true)
	private String code;

	@Column(name = "discount_type")
	private String discountType;

	@Column(name = "discount_value", precision = 12, scale = 2)
	private BigDecimal discountValue;

	@Column(name = "max_discount", precision = 12, scale = 2)
	private BigDecimal maxDiscount;

	@Column(name = "min_spend", precision = 12, scale = 2)
	private BigDecimal minSpend = BigDecimal.ZERO;

	@Column(name = "usage_limit")
	private Integer usageLimit;

	@Column(name = "per_user_limit")
	private Integer perUserLimit = 1;

	@Column(name = "used_count")
	private Integer usedCount = 0;

	@Column(name = "start_date")
	private LocalDate startDate;

	@Column(name = "end_date")
	private LocalDate endDate;

	@Column(name = "is_active")
	private Boolean isActive = true;

	@Column(name = "created_at")
	private Instant createdAt;

	@Column(name = "updated_at")
	private Instant updatedAt;

	@PrePersist
	void prePersist() {
		Instant now = Instant.now();
		if (createdAt == null) {
			createdAt = now;
		}
		if (updatedAt == null) {
			updatedAt = now;
		}
	}

	@PreUpdate
	void preUpdate() {
		updatedAt = Instant.now();
	}
}
