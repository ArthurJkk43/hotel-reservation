package com.example.hotel_reservation.models.entities.pricing;

import com.example.hotel_reservation.models.entities.hotels.Hotel;
import com.example.hotel_reservation.models.enums.DiscountType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A discount code. {@code hotel} is null for a platform-wide code.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "promo_code")
public class PromoCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id")
    private Hotel hotel;

    @Column(nullable = false, unique = true, length = 40)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private DiscountType discountType;

    /** A percentage for PERCENT, an amount in the hotel currency for FIXED. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal discountValue;

    @Column(nullable = false)
    private Instant validFrom;

    @Column(nullable = false)
    private Instant validUntil;

    /** Null means unlimited. */
    private Integer maxRedemptions;

    @Column(nullable = false)
    private Integer redemptionCount = 0;

    @Column(nullable = false)
    private Boolean isActive = true;
}
