package com.neatly.server.domain;

import java.time.Instant;

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
@Table(name = "hotel_infos")
@Getter
@Setter
@NoArgsConstructor
public class HotelInfo {

	/** Singleton row id (always 1). */
	@Id
	private Short id;

	@Column(name = "hotel_name", nullable = false)
	private String hotelName;

	@Column(name = "about_description", nullable = false, columnDefinition = "text")
	private String aboutDescription;

	@Column(name = "logo_url", nullable = false)
	private String logoUrl;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@PrePersist
	void prePersist() {
		if (id == null) {
			id = 1;
		}
		if (updatedAt == null) {
			updatedAt = Instant.now();
		}
	}

	@PreUpdate
	void preUpdate() {
		updatedAt = Instant.now();
	}
}
