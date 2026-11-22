package com.example.hotel_reservation.models.entities.rooms;

import com.example.hotel_reservation.models.entities.hotels.Hotel;
import com.example.hotel_reservation.models.entities.users.AppUser;
import com.example.hotel_reservation.models.enums.RoomBlockReason;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

/**
 * A period when a room is out of order. Blocks of one room cannot overlap (DB exclusion
 * constraint), but a block vs. a reservation must be checked in the service layer.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "room_block")
public class RoomBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hotel_id", nullable = false)
    private Hotel hotel;

    @Column(nullable = false)
    private LocalDate startDate;

    /** Exclusive, like a check-out date. */
    @Column(nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RoomBlockReason reason;

    @Column(length = 255)
    private String note;

    /** Staff member of the same hotel (composite FK to hotel_staff). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private AppUser createdBy;

    @Column(insertable = false, updatable = false)
    private Instant createdAt;
}
