package com.neatly.server.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Role-protected endpoint to verify admin authorization.
 */
@RestController
@RequestMapping("/api/v1/admin")
public class AdminAuthController {

	@GetMapping("/auth-check")
	public Map<String, Object> authCheck() {
		return Map.of(
				"ok", true,
				"message", "Admin access granted");
	}
}
