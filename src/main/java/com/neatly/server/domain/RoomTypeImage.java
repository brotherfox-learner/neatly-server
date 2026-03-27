package com.neatly.server.domain;

import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "room_type_images")
@Getter
@Setter
@NoArgsConstructor
public class RoomTypeImage {

	@Id
	@UuidGenerator
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "room_type_id", nullable = false, columnDefinition = "uuid")
	private RoomType roomType;

	@Column(name = "image_url", nullable = false)
	private String imageUrl;

	@Column(name = "is_primary")
	private Boolean isPrimary = false;

	@Column(name = "sort_order")
	private Integer sortOrder = 0;
}
