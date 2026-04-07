package com.neatly.server.dto;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ExtraServiceResponse {

    private UUID id;
    private String name;
    private String description;
    private String type;
    private BigDecimal price;
}
