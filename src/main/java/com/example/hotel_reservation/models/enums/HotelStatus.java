package com.example.hotel_reservation.models.enums;

// Must match the CHECK constraint on hotel.status in V1__init_schema.sql
public enum HotelStatus {
    DRAFT,
    ACTIVE,
    SUSPENDED,
    CLOSED
}
