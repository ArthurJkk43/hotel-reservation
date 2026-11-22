package com.example.hotel_reservation.models.enums;

// Reservation lifecycle. Must match the CHECK on reservation.status.
public enum ReservationStatus {
    PENDING,
    CONFIRMED,
    CHECKED_IN,
    CHECKED_OUT,
    CANCELLED,
    NO_SHOW,
    EXPIRED
}
