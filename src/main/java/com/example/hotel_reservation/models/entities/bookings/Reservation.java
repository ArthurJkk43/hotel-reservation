package com.example.hotel_reservation.models.entities.bookings;

import com.example.hotel_reservation.models.entities.hotels.Hotel;
import com.example.hotel_reservation.models.entities.pricing.PromoCode;
import com.example.hotel_reservation.models.entities.users.AppUser;
import com.example.hotel_reservation.models.enums.ReservationStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * The booking header: who booked, at which hotel, the money and the lifecycle.
 * The booked rooms are {@link ReservationRoom} rows.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "reservation")
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Human-friendly reference shown to the guest, e.g. "HX7K29QP". */
    @Column(nullable = false, unique = true, length = 12)
    private String bookingRef;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hotel_id", nullable = false)
    private Hotel hotel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status = ReservationStatus.PENDING;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promo_code_id")
    private PromoCode promoCode;

    /** The person staying; may differ from the account holder. */
    @Column(nullable = false, length = 200)
    private String guestName;

    @Column(nullable = false, length = 255)
    private String guestEmail;

    @Column(length = 30)
    private String guestPhone;

    @Column(columnDefinition = "text")
    private String specialRequests;

    /** A PENDING reservation expires at this time if it is not paid. */
    private Instant holdExpiresAt;

    private Instant cancelledAt;

    @Column(length = 255)
    private String cancellationReason;

    /** Optimistic locking: Hibernate increments it on every update. */
    @Version
    @Column(nullable = false)
    private Long version;

    @Column(insertable = false, updatable = false)
    private Instant createdAt;

    @Column(insertable = false, updatable = false)
    private Instant updatedAt;
}
