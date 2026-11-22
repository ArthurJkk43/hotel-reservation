package com.example.hotel_reservation.models.entities.hotels;

import com.example.hotel_reservation.models.enums.AmenityScope;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Platform-wide amenity catalog (WIFI, POOL, MINIBAR, ...), seeded by the V1 migration.
 * Linked to hotels through hotel_amenity and to room types through room_type_amenity.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "amenity")
public class Amenity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AmenityScope scope;

    @Column(length = 50)
    private String icon;
}
