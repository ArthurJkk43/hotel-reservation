package com.example.hotel_reservation.models.entities.rooms;

import com.example.hotel_reservation.models.entities.hotels.Amenity;
import com.example.hotel_reservation.models.entities.hotels.Hotel;
import com.example.hotel_reservation.models.enums.ActiveStatus;
import com.example.hotel_reservation.models.enums.BedType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * A category of room ("Deluxe King Sea View"). This is what guests book;
 * a {@link Room} is a physical unit of a room type.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "room_type")
public class RoomType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hotel_id", nullable = false)
    private Hotel hotel;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BedType bedType;

    @Column(nullable = false)
    private Short bedCount = 1;

    @Column(nullable = false)
    private Short maxAdults;

    @Column(nullable = false)
    private Short maxChildren = 0;

    @Column(precision = 6, scale = 2)
    private BigDecimal areaSqm;

    /** Fallback price per night when no room_rate matches the dates. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal basePrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ActiveStatus status = ActiveStatus.ACTIVE;

    @Column(insertable = false, updatable = false)
    private Instant createdAt;

    @Column(insertable = false, updatable = false)
    private Instant updatedAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "room_type_amenity",
            joinColumns = @JoinColumn(name = "room_type_id"),
            inverseJoinColumns = @JoinColumn(name = "amenity_id"))
    private Set<Amenity> amenities = new HashSet<>();
}
