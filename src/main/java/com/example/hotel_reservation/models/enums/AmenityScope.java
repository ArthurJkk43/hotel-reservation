package com.example.hotel_reservation.models.enums;

// Whether an amenity describes the whole hotel (pool) or a room type (minibar).
// Must match the CHECK on amenity.scope.
public enum AmenityScope {
    HOTEL,
    ROOM
}
