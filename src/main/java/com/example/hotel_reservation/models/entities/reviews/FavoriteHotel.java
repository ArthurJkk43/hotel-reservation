package com.example.hotel_reservation.models.entities.reviews;

import com.example.hotel_reservation.models.entities.hotels.Hotel;
import com.example.hotel_reservation.models.entities.users.AppUser;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * A hotel saved by a user. Mapped as an entity (not a plain @ManyToMany)
 * because it carries its own created_at.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "favorite_hotel")
public class FavoriteHotel {

    @EmbeddedId
    private FavoriteHotelId id = new FavoriteHotelId();

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private AppUser user;

    @MapsId("hotelId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hotel_id")
    private Hotel hotel;

    @Column(insertable = false, updatable = false)
    private Instant createdAt;
}
