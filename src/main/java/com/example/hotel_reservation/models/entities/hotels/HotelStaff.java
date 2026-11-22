package com.example.hotel_reservation.models.entities.hotels;

import com.example.hotel_reservation.models.entities.users.AppUser;
import com.example.hotel_reservation.models.enums.StaffRole;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Who may manage which hotel, and in what capacity. This is the basis of hotel isolation:
 * a user can act on a hotel only if a row exists here for (hotel, user).
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "hotel_staff")
public class HotelStaff {

    @EmbeddedId
    private HotelStaffId id = new HotelStaffId();

    // @MapsId fills the matching part of the embedded id from the associated entity.
    @MapsId("hotelId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hotel_id")
    private Hotel hotel;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private AppUser user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StaffRole staffRole;

    @Column(insertable = false, updatable = false)
    private Instant createdAt;
}
