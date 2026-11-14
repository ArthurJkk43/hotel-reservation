package com.example.hotel_reservation.config.JWT;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, long expirationMinutes) {}
