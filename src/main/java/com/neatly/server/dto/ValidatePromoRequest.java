package com.neatly.server.dto;

import java.math.BigDecimal;

public record ValidatePromoRequest(
    String code,
    BigDecimal orderTotal
) {}
