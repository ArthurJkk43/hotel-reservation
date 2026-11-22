package com.example.hotel_reservation.models.dtos;

import com.example.hotel_reservation.models.enums.HotelStatus;

import java.math.BigDecimal;

/**
 * Short form of a hotel for lists (search results, "my hotels"). Keeps list
 * responses small; the full data comes from {@link HotelResponseDto}.
 */
public record HotelSummaryDto(
        Long id,
        String name,
        String slug,
        String city,
        String countryCode,
        Short starRating,
        BigDecimal avgRating,
        Integer reviewCount,
        HotelStatus status
) {}
