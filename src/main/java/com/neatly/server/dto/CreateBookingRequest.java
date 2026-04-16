package com.neatly.server.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class CreateBookingRequest {
    private UUID roomTypeId;
    private int roomCount;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private int guests;
    private GuestInfoDto guestInfo;
    private List<UUID> extraServiceIds;
    private List<String> standardRequests;
    private String additionalRequest;
    private String promoCode;
    private String paymentMethod; // "CARD" | "CASH"

    @Data
    public static class GuestInfoDto {
        private String firstName;
        private String lastName;
        private String email;
        private String phone;
        private String dateOfBirth;
        private String country;
    }
}