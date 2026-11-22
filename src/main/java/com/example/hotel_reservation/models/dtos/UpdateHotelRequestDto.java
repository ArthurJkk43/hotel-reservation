package com.example.hotel_reservation.models.dtos;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * Body of "update hotel" as a partial update (PATCH): every field is optional and
 * null means "leave unchanged". Rules still apply to the fields that are sent.
 * {@code @Size(min = 1)} rejects an empty string for required columns, which would
 * otherwise wipe them. Like the create request, it cannot change id, slug, status,
 * ratings or timestamps; status changes have their own endpoints (publish, suspend).
 */
public record UpdateHotelRequestDto(
        @Size(min = 1, max = 150) String name,
        String description,
        @Min(1) @Max(5) Short starRating,
        @Size(min = 1, max = 255) String addressLine1,
        @Size(max = 255) String addressLine2,
        @Size(min = 1, max = 100) String city,
        @Size(max = 100) String region,
        @Size(max = 20) String postalCode,
        @Pattern(regexp = "^[A-Z]{2}$", message = "must be a 2-letter ISO country code, e.g. KG")
        String countryCode,
        @DecimalMin("-90.0") @DecimalMax("90.0") @Digits(integer = 3, fraction = 6) BigDecimal latitude,
        @DecimalMin("-180.0") @DecimalMax("180.0") @Digits(integer = 3, fraction = 6) BigDecimal longitude,
        @Size(min = 1, max = 50) String timezone,
        @Pattern(regexp = "^[A-Z]{3}$", message = "must be a 3-letter ISO currency code, e.g. USD")
        String currency,
        @Email @Size(max = 255) String contactEmail,
        @Size(max = 30) String contactPhone,
        LocalTime checkInTime,
        LocalTime checkOutTime
) {}
