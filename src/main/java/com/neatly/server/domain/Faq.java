package com.neatly.server.domain;

import java.time.Instant;
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

	/** PostgreSQL {@code text[]} — use {@code String[]} so Hibernate persists updates reliably. */
	@JdbcTypeCode(SqlTypes.ARRAY)
	@Column(name = "keywords", columnDefinition = "text[]")
	private String[] keywords = new String[0];

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
