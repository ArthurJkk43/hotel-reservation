package com.example.hotel_reservation.models.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequestDto (
        @NotBlank @Email @Size(max=255) String email,
        @NotBlank @Size(min=8, max=72) String password,
        @NotBlank @Size(max=100) String firstName,
        @NotBlank @Size(max=100) String lastName,
        @Size(max=30) String phone
) {}
