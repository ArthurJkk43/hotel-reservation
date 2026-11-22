package com.example.hotel_reservation.models.entities.audit;

import com.example.hotel_reservation.models.entities.hotels.Hotel;
import com.example.hotel_reservation.models.entities.users.AppUser;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.net.InetAddress;
import java.time.Instant;

/**
 * Who changed what, and when. Written by the service layer; rows are never updated.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "audit_log")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Null when the actor was deleted (ON DELETE SET NULL) or for system events. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private AppUser actor;

    /** Null for platform-level events. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id")
    private Hotel hotel;

    /** e.g. "RESERVATION_CANCELLED", "HOTEL_UPDATED". */
    @Column(nullable = false, length = 50)
    private String action;

    @Column(nullable = false, length = 50)
    private String entityType;

    @Column(nullable = false)
    private Long entityId;

    /** Free-form JSON (before/after values, reason, ...), stored as JSONB. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String details;

    @JdbcTypeCode(SqlTypes.INET)
    private InetAddress ipAddress;

    @Column(insertable = false, updatable = false)
    private Instant createdAt;
}
