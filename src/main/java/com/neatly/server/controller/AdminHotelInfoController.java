package com.neatly.server.controller;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.neatly.server.dto.AdminUpdateHotelInfoRequest;
import com.neatly.server.dto.HotelInfoResponse;
import com.neatly.server.service.HotelInfoService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin/hotel-info")
@RequiredArgsConstructor
@Validated
public class AdminHotelInfoController {

	private final HotelInfoService hotelInfoService;

	@GetMapping
	public HotelInfoResponse get() {
		return hotelInfoService.getHotelInfo();
	}

	@PutMapping
	public HotelInfoResponse update(@Valid @RequestBody AdminUpdateHotelInfoRequest request) {
		return hotelInfoService.updateHotelInfo(request);
	}
}
