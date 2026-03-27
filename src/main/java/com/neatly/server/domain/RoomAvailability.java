package com.neatly.server.domain;

import java.time.LocalDate;
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
@Table(name = "room_availability")
@Getter
@Setter
@NoArgsConstructor
public class RoomAvailability {

	@Id
	@UuidGenerator
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "room_id", nullable = false, columnDefinition = "uuid")
	private Room room;

	@Column(nullable = false)
	private LocalDate date;

	@Column(name = "is_available", nullable = false)
	private boolean isAvailable = true;
}
