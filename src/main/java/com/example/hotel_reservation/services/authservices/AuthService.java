package com.example.hotel_reservation.services.authservices;

import com.example.hotel_reservation.exception.EmailAlreadyExistsException;
import com.example.hotel_reservation.models.dtos.RegisterRequestDto;
import com.example.hotel_reservation.models.dtos.UserResponseDto;
import com.example.hotel_reservation.models.entities.users.AppUser;
import com.example.hotel_reservation.models.entities.users.Role;
import com.example.hotel_reservation.models.enums.UserStatus;
import com.example.hotel_reservation.repositories.AppUserRepository;
import com.example.hotel_reservation.repositories.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponseDto register(RegisterRequestDto dto) {
        String email = dto.email().trim().toLowerCase(Locale.ROOT);

        if (appUserRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException();
        }

        Role guestRole = roleRepository.findByCode("GUEST")
                .orElseThrow(() -> new IllegalStateException("GUEST role is missing"));

        AppUser user = new AppUser();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(dto.password()));
        user.setFirstName(dto.firstName().trim());
        user.setLastName(dto.lastName().trim());
        user.setPhone(dto.phone());
        user.setStatus(UserStatus.ACTIVE);
        user.getRoles().add(guestRole);

        AppUser saved = appUserRepository.save(user);

        return new UserResponseDto(
                saved.getId(),
                saved.getEmail(),
                saved.getFirstName(),
                saved.getLastName());
    }
}
