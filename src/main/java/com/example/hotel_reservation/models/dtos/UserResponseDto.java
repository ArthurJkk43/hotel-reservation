package com.example.hotel_reservation.models.dtos;

public record UserResponseDto (
        Long id,
        String email,
        String firstname,
        String lastname
) {}
