package com.example.hotel_reservation.models.dtos;

public record TokenResponseDto(
        String accessToken,
        String tokenType,
        long expiresInSeconds
) {}
