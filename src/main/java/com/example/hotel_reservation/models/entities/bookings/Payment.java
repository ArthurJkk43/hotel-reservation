package com.example.hotel_reservation.models.entities.bookings;

import com.example.hotel_reservation.models.entities.hotels.Hotel;
import com.example.hotel_reservation.models.enums.PaymentKind;
import com.example.hotel_reservation.models.enums.PaymentMethod;
import com.example.hotel_reservation.models.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A payment or refund of a {@link Reservation}. {@code idempotencyKey} is unique so a
 * retried client request can never charge twice.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "payment")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    /** Denormalized so staff can list their hotel's payments without a join. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hotel_id", nullable = false)
    private Hotel hotel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PaymentKind kind = PaymentKind.PAYMENT;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status = PaymentStatus.PENDING;

    /** e.g. "STRIPE", "SIMULATED". */
    @Column(length = 30)
    private String provider;

    /** The gateway's transaction id. */
    @Column(length = 100)
    private String providerRef;

    @Column(nullable = false, unique = true, length = 64)
    private String idempotencyKey;

    @Column(length = 255)
    private String failureReason;

    @Column(insertable = false, updatable = false)
    private Instant createdAt;

    @Column(insertable = false, updatable = false)
    private Instant updatedAt;
}
