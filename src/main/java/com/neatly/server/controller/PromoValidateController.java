package com.neatly.server.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.neatly.server.dto.ValidatePromoRequest;
import com.neatly.server.dto.ValidatePromoResponse;
import com.neatly.server.service.PromotionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/promotions")
@RequiredArgsConstructor
@Slf4j
public class PromoValidateController {

    private final PromotionService promotionService;

    @PostMapping("/validate")
    public ResponseEntity<ValidatePromoResponse> validate(@RequestBody ValidatePromoRequest request) {
        log.info("[PROMO_VALIDATE][REQUEST] code={}", request.code());
        ValidatePromoResponse response = promotionService.validatePromo(request);
        return ResponseEntity.ok(response);
    }
}
