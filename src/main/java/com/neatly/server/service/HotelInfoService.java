package com.neatly.server.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.neatly.server.domain.HotelInfo;
import com.neatly.server.dto.AdminUpdateHotelInfoRequest;
import com.neatly.server.dto.HotelInfoResponse;
import com.neatly.server.repository.HotelInfoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HotelInfoService {

	private static final short SINGLETON_ID = 1;
	private static final String DEFAULT_HOTEL_NAME = "Neatly Hotel";
	private static final String DEFAULT_HOTEL_DESCRIPTION = """
			Set in Bangkok, Thailand. Neatly Hotel offers 5-star accommodation with an outdoor pool, kids' club, sports facilities and a fitness centre. There is also a spa, an indoor pool and saunas.

			All units at the hotel are equipped with a seating area, a flat-screen TV with satellite channels, a dining area and a private bathroom with free toiletries, a bathtub and a hairdryer. Every room in Neatly Hotel features a furnished balcony. Some rooms are equipped with a coffee machine.

			Free WiFi and entertainment facilities are available at property and also rentals are provided to explore the area.
			""";
	private static final String DEFAULT_LOGO_URL = "/logo.svg";

	private final HotelInfoRepository hotelInfoRepository;

	@Transactional(readOnly = true)
	public HotelInfoResponse getHotelInfo() {
		return hotelInfoRepository.findById(SINGLETON_ID)
				.map(this::toResponse)
				.orElseGet(this::defaultResponse);
	}

	@Transactional
	public HotelInfoResponse updateHotelInfo(AdminUpdateHotelInfoRequest request) {
		HotelInfo hotelInfo = hotelInfoRepository.findById(SINGLETON_ID).orElseGet(HotelInfo::new);
		hotelInfo.setId(SINGLETON_ID);
		hotelInfo.setHotelName(request.hotelName().trim());
		hotelInfo.setAboutDescription(request.aboutDescription().trim());
		hotelInfo.setLogoUrl(normalizeLogoUrl(request.logoUrl()));
		hotelInfoRepository.save(hotelInfo);
		return toResponse(hotelInfo);
	}

	private String normalizeLogoUrl(String logoUrl) {
		return StringUtils.hasText(logoUrl) ? logoUrl.trim() : DEFAULT_LOGO_URL;
	}

	private HotelInfoResponse defaultResponse() {
		return new HotelInfoResponse(DEFAULT_HOTEL_NAME, DEFAULT_HOTEL_DESCRIPTION, DEFAULT_LOGO_URL);
	}

	private HotelInfoResponse toResponse(HotelInfo hotelInfo) {
		return new HotelInfoResponse(
				hotelInfo.getHotelName(),
				hotelInfo.getAboutDescription(),
				normalizeLogoUrl(hotelInfo.getLogoUrl()));
	}
}
