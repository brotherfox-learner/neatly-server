package com.neatly.server.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.neatly.server.dto.CreatePromotionRequest;
import com.neatly.server.dto.PromotionResponse;
import com.neatly.server.service.PromotionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.UUID;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/promotion")
@RequiredArgsConstructor
@Slf4j
public class PromotionController {

    private final PromotionService promotionService;

    @PostMapping
    public ResponseEntity<PromotionResponse> createPromotion(
            @Valid @RequestBody CreatePromotionRequest request) {
        log.info("[CREATE_PROMOTION][REQUEST] code={}", request.code());
        PromotionResponse response = promotionService.createPromotion(request);
        log.info("[CREATE_PROMOTION][SUCCESS] id={}", response.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<PromotionResponse>> getPromotions() {
        log.info("[GET_PROMOTIONS][REQUEST]");
        List<PromotionResponse> response = promotionService.getPromotions();
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/toggle-active")
    public ResponseEntity<PromotionResponse> toggleActive(@PathVariable UUID id) {
        log.info("[TOGGLE_PROMOTION][REQUEST] id={}", id);
        PromotionResponse response = promotionService.toggleActive(id);
        return ResponseEntity.ok(response);
    }
}