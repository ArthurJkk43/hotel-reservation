package com.example.hotel_reservation.models.entities.reviews;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * Composite primary key of favorite_hotel: (user_id, hotel_id).
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class FavoriteHotelId implements Serializable {

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "hotel_id")
    private Long hotelId;
}
