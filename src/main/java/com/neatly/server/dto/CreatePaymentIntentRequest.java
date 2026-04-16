package com.neatly.server.dto;

import java.util.UUID;

import lombok.Data;

@Data
public class CreatePaymentIntentRequest {
    private UUID bookingId;
}
