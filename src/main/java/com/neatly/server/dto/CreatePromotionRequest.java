package com.neatly.server.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.*;

public record CreatePromotionRequest(

    @NotBlank(message = "code is required")
    @Size(max = 255)
    String code,

    @NotBlank(message = "discountType is required")
    @Pattern(
        regexp = "PERCENTAGE|FIXED",
        message = "discountType must be PERCENTAGE or FIXED"
    )
    String discountType,

    @NotNull(message = "discountValue is required")
    @DecimalMin(value = "0.00", inclusive = false, message = "discountValue must be greater than 0")
    BigDecimal discountValue,

    BigDecimal maxDiscount,

    @DecimalMin(value = "0.00", message = "minSpend must be >= 0")
    BigDecimal minSpend,

    @Min(value = 1, message = "usageLimit must be >= 1")
    Integer usageLimit,

    @Min(value = 1, message = "perUserLimit must be >= 1")
    Integer perUserLimit,

    LocalDate startDate,
    LocalDate endDate,

    Boolean isActive
) {}