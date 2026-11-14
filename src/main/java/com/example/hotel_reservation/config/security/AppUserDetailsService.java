package com.example.hotel_reservation.config.security;

import com.example.hotel_reservation.models.entities.users.AppUser;
import com.example.hotel_reservation.models.enums.UserStatus;
import com.example.hotel_reservation.repositories.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AppUserDetailsService implements UserDetailsService {

    private final AppUserRepository appUserRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        AppUser user = appUserRepository.findByEmail(email.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        var authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getCode()))
                .toList();

        return User.withUsername(user.getEmail())
                .password(user.getPasswordHash())
                .authorities(authorities)
                .accountLocked(user.getStatus() == UserStatus.LOCKED)
                .disabled(user.getStatus() == UserStatus.DISABLED
                        || user.getStatus() == UserStatus.PENDING_VERIFICATION)
                .build();
    }
}
