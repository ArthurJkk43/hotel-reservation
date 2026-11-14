package com.example.hotel_reservation.controllers;

import com.example.hotel_reservation.models.dtos.LoginRequestDto;
import com.example.hotel_reservation.models.dtos.RegisterRequestDto;
import com.example.hotel_reservation.models.dtos.TokenResponseDto;
import com.example.hotel_reservation.models.dtos.UserResponseDto;
import com.example.hotel_reservation.services.authservices.AuthService;
import com.example.hotel_reservation.services.authservices.impl.AuthServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthServiceImpl authServiceImpl;
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<UserResponseDto> register(@Valid @RequestBody RegisterRequestDto dto) {
        UserResponseDto created = authServiceImpl.register(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponseDto> login(@Valid @RequestBody LoginRequestDto dto) {
        return ResponseEntity.ok(authService.login(dto));
    }
}