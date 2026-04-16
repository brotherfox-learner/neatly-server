package com.neatly.server.service;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.neatly.server.domain.Booking;
import com.neatly.server.domain.BookingExtraService;
import com.neatly.server.domain.BookingItem;
import com.neatly.server.domain.ExtraService;
import com.neatly.server.domain.Room;
import com.neatly.server.domain.RoomType;
import com.neatly.server.domain.User;
import com.neatly.server.dto.CreateBookingRequest;
import com.neatly.server.repository.BookingExtraServiceRepository;
import com.neatly.server.repository.BookingItemRepository;
import com.neatly.server.repository.BookingRepository;
import com.neatly.server.repository.ExtraServiceRepository;
import com.neatly.server.repository.PromotionRepository;
import com.neatly.server.repository.RoomRepository;
import com.neatly.server.repository.RoomTypeRepository;
import com.neatly.server.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final BookingItemRepository bookingItemRepository;
    private final BookingExtraServiceRepository bookingExtraServiceRepository;
    private final RoomRepository roomRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final ExtraServiceRepository extraServiceRepository;
    private final PromotionRepository promotionRepository;
    private final CurrentUserService currentUserService;

    public UUID createBooking(CreateBookingRequest request) {

        log.info("[CREATE_BOOKING][START] start createBooking");

        // STEP 0: get authenticated user from JWT
        UUID userId = currentUserService.requireUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        log.info("[CREATE_BOOKING][USER] userId={}", user.getId());

        Booking booking = new Booking();
        booking.setUser(user);

        // STEP 1: set basic info
        booking.setCheckInDate(request.getCheckInDate());
        booking.setCheckOutDate(request.getCheckOutDate());

        int nights = (int) ChronoUnit.DAYS.between(
                request.getCheckInDate(),
                request.getCheckOutDate());

        log.info("[CREATE_BOOKING][CALC] nights={}", nights);

        booking.setTotalNights(nights);
        booking.setGuestNumbers(String.valueOf(request.getGuests()));

        // STEP 2: calculate price — ดึงจาก RoomType ใช้ discountedPrice ถ้ามี ไม่งั้นใช้ basePrice
        RoomType roomType = roomTypeRepository.findById(request.getRoomTypeId())
                .orElseThrow(() -> new RuntimeException("RoomType not found: " + request.getRoomTypeId()));
        BigDecimal pricePerNight = roomType.getDiscountedPrice() != null
                ? roomType.getDiscountedPrice()
                : roomType.getBasePrice();
        int roomCount = request.getRoomCount() > 0 ? request.getRoomCount() : 1;
        BigDecimal subtotal = pricePerNight.multiply(BigDecimal.valueOf(nights)).multiply(BigDecimal.valueOf(roomCount));

        log.info("[CREATE_BOOKING][PRICE] {} x {} nights x {} rooms = {}", pricePerNight, nights, roomCount, subtotal);

        booking.setSubtotal(subtotal);

        // STEP 3: link promotion + calculate discount
        BigDecimal discountAmount = BigDecimal.ZERO;
        if (request.getPromoCode() != null && !request.getPromoCode().isBlank()) {
            var promoOpt = promotionRepository.findByCodeIgnoreCase(request.getPromoCode());
            if (promoOpt.isPresent()) {
                var promo = promoOpt.get();
                booking.setPromotion(promo);
                if ("PERCENTAGE".equalsIgnoreCase(promo.getDiscountType())) {
                    discountAmount = subtotal.multiply(promo.getDiscountValue())
                            .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
                    if (promo.getMaxDiscount() != null && discountAmount.compareTo(promo.getMaxDiscount()) > 0) {
                        discountAmount = promo.getMaxDiscount();
                    }
                } else {
                    discountAmount = promo.getDiscountValue();
                }
                booking.setDiscountAmount(discountAmount);
                log.info("[CREATE_BOOKING][PROMO] linked promotionId={} discountAmount={}", promo.getId(), discountAmount);
            }
        }

        // STEP 4: set status based on payment method
        String status = "CASH".equalsIgnoreCase(request.getPaymentMethod())
                ? "PENDING_CHECKIN"
                : "PENDING_PAYMENT";
        booking.setStatus(status);

        String ref = "BOOK-" + System.currentTimeMillis();
        booking.setBookingReference(ref);

        log.info("[CREATE_BOOKING][REF] bookingReference={}, status={}", ref, status);

        // STEP 5: save booking
        bookingRepository.save(booking);
        log.info("[CREATE_BOOKING][SUCCESS] saved booking id={}", booking.getId());

        // STEP 6: assign available rooms → create BookingItems
        List<Room> availableRooms = roomRepository.findAvailableRooms(
                request.getRoomTypeId(),
                request.getCheckInDate(),
                request.getCheckOutDate());

        if (availableRooms.size() < roomCount) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Not enough available rooms. Requested: " + roomCount + ", Available: " + availableRooms.size());
        }

        for (int i = 0; i < roomCount; i++) {
            Room room = availableRooms.get(i);
            BookingItem item = new BookingItem();
            item.setBooking(booking);
            item.setRoom(room);
            item.setRoomType(roomType);
            item.setPricePerNight(pricePerNight);
            item.setNights(nights);
            item.setSubtotal(pricePerNight.multiply(BigDecimal.valueOf(nights)));
            bookingItemRepository.save(item);
            log.info("[CREATE_BOOKING][ITEM] roomId={} roomNumber={}", room.getId(), room.getRoomNumber());
        }

        // STEP 7: save selected extra services + accumulate extrasTotal
        BigDecimal extrasTotal = BigDecimal.ZERO;
        if (request.getExtraServiceIds() != null && !request.getExtraServiceIds().isEmpty()) {
            for (UUID extraId : request.getExtraServiceIds()) {
                var extraOpt = extraServiceRepository.findById(extraId);
                if (extraOpt.isPresent()) {
                    var extra = extraOpt.get();
                    int pricingMultiplier = ("per_night".equals(extra.getPricingType()) || "per_day".equals(extra.getPricingType()))
                            ? nights : 1;
                    int chargeMultiplier = "per_person".equals(extra.getChargeUnit())
                            ? request.getGuests() : roomCount;
                    BigDecimal totalPrice = extra.getPrice()
                            .multiply(BigDecimal.valueOf(pricingMultiplier))
                            .multiply(BigDecimal.valueOf(chargeMultiplier));

                    BookingExtraService bes = new BookingExtraService();
                    bes.setBooking(booking);
                    bes.setExtraService(extra);
                    bes.setUnitPrice(extra.getPrice());
                    bes.setTotalPrice(totalPrice);
                    bookingExtraServiceRepository.save(bes);
                    extrasTotal = extrasTotal.add(totalPrice);
                    log.info("[CREATE_BOOKING][EXTRA] extraServiceId={} name={} totalPrice={}", extra.getId(), extra.getName(), totalPrice);
                }
            }
        }

        // STEP 8: update totalAmount = subtotal + extrasTotal - discountAmount
        BigDecimal totalAmount = subtotal.add(extrasTotal).subtract(discountAmount);
        if (totalAmount.compareTo(BigDecimal.ZERO) < 0) totalAmount = BigDecimal.ZERO;
        booking.setTotalAmount(totalAmount);
        bookingRepository.save(booking);
        log.info("[CREATE_BOOKING][TOTAL] subtotal={} extras={} discount={} total={}", subtotal, extrasTotal, discountAmount, totalAmount);

        return booking.getId();
    }
}
