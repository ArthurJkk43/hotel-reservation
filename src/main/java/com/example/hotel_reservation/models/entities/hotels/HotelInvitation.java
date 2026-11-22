package com.example.hotel_reservation.models.entities.hotels;

import com.example.hotel_reservation.models.entities.users.AppUser;
import com.example.hotel_reservation.models.enums.StaffRole;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/**
 * An invitation for a person (by e-mail) to join a hotel's staff.
 * Accepting it creates the {@link HotelStaff} row.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "hotel_invitation")
public class HotelInvitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hotel_id", nullable = false)
    private Hotel hotel;

    @Column(nullable = false, length = 255, columnDefinition = "citext")
    private String email;

    /** Only MANAGER or RECEPTIONIST; the database rejects OWNER. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StaffRole staffRole;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;

    /**
     * The inviting user. The composite FK (hotel_id, invited_by) -> hotel_staff
     * guarantees in the database that they are staff of this same hotel.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invited_by", nullable = false)
    private AppUser invitedBy;

    @Column(nullable = false)
    private Instant expiresAt;

    private Instant acceptedAt;

    @Column(insertable = false, updatable = false)
    private Instant createdAt;
}
