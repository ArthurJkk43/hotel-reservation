package com.example.hotel_reservation.services.authservices.impl;

import com.example.hotel_reservation.config.JWT.JwtProperties;
import com.example.hotel_reservation.exception.EmailAlreadyExistsException;
import com.example.hotel_reservation.models.dtos.LoginRequestDto;
import com.example.hotel_reservation.models.dtos.RegisterRequestDto;
import com.example.hotel_reservation.models.dtos.TokenResponseDto;
import com.example.hotel_reservation.models.dtos.UserResponseDto;
import com.example.hotel_reservation.models.entities.users.AppUser;
import com.example.hotel_reservation.models.entities.users.Role;
import com.example.hotel_reservation.models.enums.UserStatus;
import com.example.hotel_reservation.repositories.AppUserRepository;
import com.example.hotel_reservation.repositories.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.hotel_reservation.services.authservices.AuthService;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AppUserRepository appUserRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtEncoder jwtEncoder;
    private final JwtProperties jwtProperties;

    @Override
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

    @Override
    public TokenResponseDto login(LoginRequestDto dto) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        dto.email().trim().toLowerCase(Locale.ROOT), dto.password()));

        Instant now = Instant.now();
        Instant expiresAt = now.plus(jwtProperties.expirationMinutes(), ChronoUnit.MINUTES);

        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .toList();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("hotel-reservation")
                .subject(authentication.getName())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .claim("roles", roles)
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

        return new TokenResponseDto(token, "Bearer", jwtProperties.expirationMinutes() * 60);
    }
}
