package com.example.hotel_reservation.models.entities.hotels;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * Composite primary key of hotel_staff: (hotel_id, user_id).
 * Embedded ids need equals/hashCode on all fields and must be Serializable.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class HotelStaffId implements Serializable {

    @Column(name = "hotel_id")
    private Long hotelId;

    @Column(name = "user_id")
    private Long userId;
}
