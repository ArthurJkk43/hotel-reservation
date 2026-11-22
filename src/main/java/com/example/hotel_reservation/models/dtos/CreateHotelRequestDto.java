package com.example.hotel_reservation.models.dtos;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * Body of "create hotel". On purpose it has NO id, slug, status, avgRating, reviewCount or
 * timestamps (mass-assignment protection): the service generates the slug, starts the hotel
 * as DRAFT, and the rest is owned by the server.
 * timezone, currency and check-in/out times are optional; null means the entity default
 * (UTC, USD, 14:00, 12:00).
 */
public record CreateHotelRequestDto(
        @NotBlank @Size(max = 150) String name,
        String description,
        @Min(1) @Max(5) Short starRating,
        @NotBlank @Size(max = 255) String addressLine1,
        @Size(max = 255) String addressLine2,
        @NotBlank @Size(max = 100) String city,
        @Size(max = 100) String region,
        @Size(max = 20) String postalCode,
        @NotBlank @Pattern(regexp = "^[A-Z]{2}$", message = "must be a 2-letter ISO country code, e.g. KG")
        String countryCode,
        @DecimalMin("-90.0") @DecimalMax("90.0") @Digits(integer = 3, fraction = 6) BigDecimal latitude,
        @DecimalMin("-180.0") @DecimalMax("180.0") @Digits(integer = 3, fraction = 6) BigDecimal longitude,
        @Size(max = 50) String timezone,
        @Pattern(regexp = "^[A-Z]{3}$", message = "must be a 3-letter ISO currency code, e.g. USD")
        String currency,
        @Email @Size(max = 255) String contactEmail,
        @Size(max = 30) String contactPhone,
        LocalTime checkInTime,
        LocalTime checkOutTime
) {}
