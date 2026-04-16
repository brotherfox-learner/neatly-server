package com.neatly.server.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminUpdateRoomStatusRequest(@NotBlank String status) {
}
