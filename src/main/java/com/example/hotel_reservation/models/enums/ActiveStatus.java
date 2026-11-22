package com.example.hotel_reservation.models.enums;

// Shared by room_type.status, room.status and rate_plan.status,
// which all have CHECK (status IN ('ACTIVE','INACTIVE')).
public enum ActiveStatus {
    ACTIVE,
    INACTIVE
}
