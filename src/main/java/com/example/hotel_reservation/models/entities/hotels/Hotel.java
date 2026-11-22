package com.example.hotel_reservation.models.entities.hotels;

import com.example.hotel_reservation.models.enums.HotelStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;

import static jakarta.persistence.GenerationType.IDENTITY;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "hotel")
public class Hotel {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, unique = true, length = 160)
    private String slug;

    @Column(columnDefinition = "text")
    private String description;

    private Short starRating;

    @Column(nullable = false, length = 255)
    private String addressLine1;

    @Column(length = 255)
    private String addressLine2;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(length = 100)
    private String region;

    @Column(length = 20)
    private String postalCode;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 2)
    private String countryCode;

    @Column(precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(precision = 9, scale = 6)
    private BigDecimal longitude;

    // Defaults below mirror the DB defaults: Hibernate always sends every column,
    // so a null field would become an explicit NULL and break NOT NULL.
    @Column(nullable = false, length = 50)
    private String timezone = "UTC";

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 3)
    private String currency = "USD";

    @Column(length = 255)
    private String contactEmail;

    @Column(length = 30)
    private String contactPhone;

    @Column(nullable = false)
    private LocalTime checkInTime = LocalTime.of(14, 0);

    @Column(nullable = false)
    private LocalTime checkOutTime = LocalTime.of(12, 0);

    @Column(nullable = false, precision = 3, scale = 2)
    private BigDecimal avgRating = BigDecimal.ZERO;

    @Column(nullable = false)
    private Integer reviewCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private HotelStatus status = HotelStatus.DRAFT;

    // Filled by the database (default now() / update trigger), never written by Hibernate.
    @Column(insertable = false, updatable = false)
    private Instant createdAt;

    @Column(insertable = false, updatable = false)
    private Instant updatedAt;

    private Instant deletedAt;
}
