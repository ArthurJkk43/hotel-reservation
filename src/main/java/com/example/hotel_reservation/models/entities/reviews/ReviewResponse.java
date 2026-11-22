package com.example.hotel_reservation.models.entities.reviews;

import com.example.hotel_reservation.models.entities.hotels.Hotel;
import com.example.hotel_reservation.models.entities.users.AppUser;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * The hotel's single public reply to a {@link Review}. The composite FK
 * (hotel_id, staff_id) -> hotel_staff ensures only staff of the same hotel can reply.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "review_response")
public class ReviewResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "review_id", nullable = false, unique = true)
    private Review review;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hotel_id", nullable = false)
    private Hotel hotel;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "staff_id", nullable = false)
    private AppUser staff;

    @Column(nullable = false, columnDefinition = "text")
    private String body;

    @Column(insertable = false, updatable = false)
    private Instant createdAt;
}
