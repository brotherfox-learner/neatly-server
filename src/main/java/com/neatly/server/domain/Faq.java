package com.neatly.server.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "faqs")
@Getter
@Setter
@NoArgsConstructor
public class Faq {

	@Id
	@UuidGenerator
	private UUID id;

	@Column(columnDefinition = "text")
	private String question;

	@Column(columnDefinition = "text")
	private String answer;

	@JdbcTypeCode(SqlTypes.ARRAY)
	@Column(columnDefinition = "text[]")
	private List<String> keywords = new ArrayList<>();

	@Column(name = "is_active")
	private Boolean isActive = true;

	@Column
	private String category = "general";

	@Column(name = "sort_order")
	private Integer sortOrder = 0;

	@Column(name = "response_type")
	private String responseType = "text";

	@Column(name = "created_at")
	private Instant createdAt;

	@PrePersist
	void prePersist() {
		if (createdAt == null) {
			createdAt = Instant.now();
		}
	}
}
