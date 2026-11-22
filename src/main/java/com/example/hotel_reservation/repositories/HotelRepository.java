package com.example.hotel_reservation.repositories;

import com.example.hotel_reservation.models.entities.hotels.Hotel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HotelRepository extends JpaRepository<Hotel, Long> {
    Optional<Hotel> findBySlug(String slug);

    /** Used when generating a slug: "grand-plaza" taken -> try "grand-plaza-2". */
    boolean existsBySlug(String slug);
}
