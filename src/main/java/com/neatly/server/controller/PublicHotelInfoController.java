package com.neatly.server.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.neatly.server.dto.HotelInfoResponse;
import com.neatly.server.service.HotelInfoService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/public/hotel-info")
@RequiredArgsConstructor
public class PublicHotelInfoController {

	private final HotelInfoService hotelInfoService;

	@GetMapping
	public HotelInfoResponse get() {
		return hotelInfoService.getHotelInfo();
	}
}
