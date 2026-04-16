package com.neatly.server.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.neatly.server.domain.HotelInfo;

public interface HotelInfoRepository extends JpaRepository<HotelInfo, Short> {
}
