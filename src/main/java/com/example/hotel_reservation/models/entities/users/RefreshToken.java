package com.example.hotel_reservation.models.entities.users;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.net.InetAddress;
import java.time.Instant;

/**
 * A JWT refresh-token session. Only the SHA-256 hash of the token is stored.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "refresh_token")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    /** SHA-256 hex digest of the raw token (64 chars). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(nullable = false)
    private Instant expiresAt;

    /** Set on logout or rotation. */
    private Instant revokedAt;

    @Column(length = 255)
    private String userAgent;

    @JdbcTypeCode(SqlTypes.INET)
    private InetAddress ipAddress;

    @Column(insertable = false, updatable = false)
    private Instant createdAt;
}
