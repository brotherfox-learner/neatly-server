package com.neatly.server.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.neatly.server.domain.ExtraService;

public interface ExtraServiceRepository extends JpaRepository<ExtraService, UUID> {

    List<ExtraService> findAllByIsActiveTrue();
}
