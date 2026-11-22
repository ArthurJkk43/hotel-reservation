package com.example.hotel_reservation.models.entities.bookings;

import com.example.hotel_reservation.models.entities.hotels.Hotel;
import com.example.hotel_reservation.models.entities.pricing.RatePlan;
import com.example.hotel_reservation.models.entities.rooms.Room;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One booked room of a {@link Reservation}. Price and cancellation terms are snapshots
 * taken at booking time. While {@code isActive} is true the database forbids any other
 * active booking of the same room for overlapping dates (exclusion constraint).
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "reservation_room")
public class ReservationRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hotel_id", nullable = false)
    private Hotel hotel;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rate_plan_id", nullable = false)
    private RatePlan ratePlan;

    @Column(nullable = false)
    private LocalDate checkIn;

    @Column(nullable = false)
    private LocalDate checkOut;

    @Column(nullable = false)
    private Short adults;

    @Column(nullable = false)
    private Short children = 0;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal priceTotal;

    /** Snapshot of the policy: free cancellation until this date (null = never free). */
    private LocalDate cancelFreeUntil;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal penaltyPercent = BigDecimal.ZERO;

    /** Set to false when the reservation is cancelled or expires, which frees the room. */
    @Column(nullable = false)
    private Boolean isActive = true;
}
