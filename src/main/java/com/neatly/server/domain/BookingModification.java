package com.neatly.server.domain;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "booking_modifications")
@Getter
@Setter
@NoArgsConstructor
public class BookingModification {

	@Id
	@UuidGenerator
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "booking_id", nullable = false, columnDefinition = "uuid")
	private Booking booking;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "modified_by", nullable = false, columnDefinition = "uuid")
	private User modifiedBy;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "old_data", columnDefinition = "jsonb")
	private String oldData;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "new_data", columnDefinition = "jsonb")
	private String newData;

	@Column(columnDefinition = "text")
	private String reason;

	@Column(name = "created_at")
	private Instant createdAt;

	@PrePersist
	void prePersist() {
		if (createdAt == null) {
			createdAt = Instant.now();
		}
	}
}
