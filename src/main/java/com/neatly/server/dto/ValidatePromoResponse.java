package com.neatly.server.dto;

import java.math.BigDecimal;

public record ValidatePromoResponse(
    String code,
    BigDecimal discountAmount,
    String discountType
) {}
