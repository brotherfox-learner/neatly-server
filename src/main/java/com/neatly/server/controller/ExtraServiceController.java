package com.neatly.server.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.neatly.server.dto.ExtraServiceResponse;
import com.neatly.server.repository.ExtraServiceRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/extra-services")
@RequiredArgsConstructor
public class ExtraServiceController {

    private final ExtraServiceRepository extraServiceRepository;

    @GetMapping
    public ResponseEntity<List<ExtraServiceResponse>> getExtraServices() {
        List<ExtraServiceResponse> services = extraServiceRepository.findAllByIsActiveTrue()
                .stream()
                .map(s -> new ExtraServiceResponse(s.getId(), s.getName(), s.getDescription(), s.getType(), s.getPrice()))
                .toList();

        return ResponseEntity.ok(services);
    }
}
