package com.example.hotel_reservation.models.dtos;

import com.example.hotel_reservation.models.enums.HotelStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;

/**
 * Full details of one hotel (hotel page, manager edit screen). No validation annotations:
 * responses are built by the server, not sent by clients.
 */
public record HotelResponseDto(
        Long id,
        String name,
        String slug,
        String description,
        Short starRating,
        String addressLine1,
        String addressLine2,
        String city,
        String region,
        String postalCode,
        String countryCode,
        BigDecimal latitude,
        BigDecimal longitude,
        String timezone,
        String currency,
        String contactEmail,
        String contactPhone,
        LocalTime checkInTime,
        LocalTime checkOutTime,
        BigDecimal avgRating,
        Integer reviewCount,
        HotelStatus status,
        Instant createdAt,
        Instant updatedAt
) {}
