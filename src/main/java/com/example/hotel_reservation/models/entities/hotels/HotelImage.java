package com.example.hotel_reservation.models.entities.hotels;

import com.example.hotel_reservation.models.entities.rooms.RoomType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A photo of a hotel, or of one of its room types when {@code roomType} is set.
 * The database allows at most one primary hotel-level image per hotel.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "hotel_image")
public class HotelImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hotel_id", nullable = false)
    private Hotel hotel;

    /** Optional. The composite FK (room_type_id, hotel_id) keeps it inside the same hotel. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_type_id")
    private RoomType roomType;

    @Column(nullable = false, length = 500)
    private String url;

    @Column(length = 255)
    private String caption;

    @Column(nullable = false)
    private Integer sortOrder = 0;

    @Column(nullable = false)
    private Boolean isPrimary = false;
}
