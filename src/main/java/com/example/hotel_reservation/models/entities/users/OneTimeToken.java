package com.example.hotel_reservation.models.entities.users;

import com.example.hotel_reservation.models.enums.OneTimeTokenType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/**
 * Single-use token for e-mail verification or password reset. Only the hash is stored.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "one_time_token")
public class OneTimeToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OneTimeTokenType type;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(nullable = false)
    private Instant expiresAt;

    /** Set when the token is consumed; a used token can never be used again. */
    private Instant usedAt;

    @Column(insertable = false, updatable = false)
    private Instant createdAt;
}
