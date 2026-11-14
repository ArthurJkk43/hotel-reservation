package com.example.hotel_reservation.services.authservices;

import com.example.hotel_reservation.models.dtos.LoginRequestDto;
import com.example.hotel_reservation.models.dtos.RegisterRequestDto;
import com.example.hotel_reservation.models.dtos.TokenResponseDto;
import com.example.hotel_reservation.models.dtos.UserResponseDto;

public interface AuthService {

    UserResponseDto register(RegisterRequestDto dto);

    TokenResponseDto login(LoginRequestDto dto);
}

