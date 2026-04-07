package com.neatly.server.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CreatePaymentIntentResponse {
    private String clientSecret;
    private String paymentIntentId;
}
