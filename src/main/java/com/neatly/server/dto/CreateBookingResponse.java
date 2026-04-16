package com.neatly.server.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CreateBookingResponse {
    private UUID bookingId;
}