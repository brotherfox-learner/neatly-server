package com.neatly.server.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.neatly.server.dto.AdminCustomerBookingListItemResponse;
import com.neatly.server.service.AdminCustomerBookingService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin/bookings")
@RequiredArgsConstructor
public class AdminCustomerBookingController {

	private final AdminCustomerBookingService adminCustomerBookingService;

	@GetMapping
	public List<AdminCustomerBookingListItemResponse> list() {
		return adminCustomerBookingService.listBookings();
	}
}
