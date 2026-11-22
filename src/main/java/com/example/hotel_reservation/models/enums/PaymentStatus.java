package com.example.hotel_reservation.models.enums;

// Must match the CHECK on payment.status
public enum PaymentStatus {
    PENDING,
    AUTHORIZED,
    CAPTURED,
    FAILED,
    CANCELLED
}
