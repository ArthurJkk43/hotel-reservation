package com.example.hotel_reservation.repositories;

import com.example.hotel_reservation.models.entities.hotels.Hotel;
import com.example.hotel_reservation.models.enums.HotelStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class HotelRepositoryTest {

    @Autowired
    private HotelRepository hotelRepository;

    @Test
    void findsHotelBySlug() {
        Hotel saved = hotelRepository.saveAndFlush(hotel("Repo Test Hotel", "repo-test-hotel"));

        Hotel found = hotelRepository.findBySlug("repo-test-hotel").orElseThrow();

        assertThat(found.getId()).isEqualTo(saved.getId());
        assertThat(found.getName()).isEqualTo("Repo Test Hotel");
        assertThat(found.getStatus()).isEqualTo(HotelStatus.DRAFT);   // new hotels start as DRAFT
    }

    @Test
    void findBySlugIsEmptyForUnknownSlug() {
        assertThat(hotelRepository.findBySlug("no-such-hotel-slug")).isEmpty();
    }

    @Test
    void existsBySlugReportsTakenSlugs() {
        hotelRepository.saveAndFlush(hotel("Repo Test Hotel", "repo-test-hotel"));

        assertThat(hotelRepository.existsBySlug("repo-test-hotel")).isTrue();
        assertThat(hotelRepository.existsBySlug("repo-test-hotel-2")).isFalse();
    }

    private Hotel hotel(String name, String slug) {
        Hotel hotel = new Hotel();
        hotel.setName(name);
        hotel.setSlug(slug);
        hotel.setAddressLine1("1 Test Street");
        hotel.setCity("Bishkek");
        hotel.setCountryCode("KG");
        return hotel;
    }
}
