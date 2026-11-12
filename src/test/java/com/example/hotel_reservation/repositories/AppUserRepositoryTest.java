package com.example.hotel_reservation.repositories;

import com.example.hotel_reservation.models.enums.UserStatus;
import com.example.hotel_reservation.models.users.AppUser;
import com.example.hotel_reservation.models.users.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class AppUserRepositoryTest {

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Test
    void savesUserWithGuestRole() {
        Role guest = roleRepository.findByCode("GUEST").orElseThrow();

        AppUser user = new AppUser();
        user.setEmail("test.user@example.com");
        user.setPasswordHash("x");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setStatus(UserStatus.ACTIVE);
        user.getRoles().add(guest);

        AppUser saved = appUserRepository.save(user);

        assertThat(saved.getId()).isNotNull();

        AppUser found = appUserRepository.findByEmail("test.user@example.com").orElseThrow();
        assertThat(found.getRoles()).hasSize(1);
        assertThat(found.getRoles().iterator().next().getCode()).isEqualTo("GUEST");
    }
}