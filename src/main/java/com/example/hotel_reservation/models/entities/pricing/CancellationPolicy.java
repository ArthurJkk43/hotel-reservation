package com.example.hotel_reservation.models.entities.pricing;

import com.example.hotel_reservation.models.entities.hotels.Hotel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Refund rules of a hotel ("Free cancellation", "Non-refundable"): free until
 * {@code freeCancelDays} before check-in, then {@code penaltyPercent} of the price is kept.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "cancellation_policy")
public class CancellationPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hotel_id", nullable = false)
    private Hotel hotel;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private Short freeCancelDays = 0;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal penaltyPercent = new BigDecimal("100");
}
