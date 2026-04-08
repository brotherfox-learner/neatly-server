package com.neatly.server.dto;

import java.util.UUID;

public record AdminCreateRoomResponse(
		UUID roomTypeId,
		UUID roomId,
		String roomNumber) {
}
