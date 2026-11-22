package com.example.hotel_reservation.models.entities.pricing;

import com.example.hotel_reservation.models.entities.hotels.Hotel;
import com.example.hotel_reservation.models.enums.ActiveStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A sellable offer ("Flexible + Breakfast", "Saver, non-refundable").
 * Prices per room type and date range live in {@link RoomRate}.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "rate_plan")
public class RatePlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hotel_id", nullable = false)
    private Hotel hotel;

    /** Composite FK (cancellation_policy_id, hotel_id) keeps the policy inside the same hotel. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cancellation_policy_id", nullable = false)
    private CancellationPolicy cancellationPolicy;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private Boolean breakfastIncluded = false;

    @Column(nullable = false)
    private Short minNights = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ActiveStatus status = ActiveStatus.ACTIVE;
}
