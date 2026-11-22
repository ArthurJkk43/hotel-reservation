package com.example.hotel_reservation.models.enums;

// Hotel-level roles. Must match the CHECK on hotel_staff.staff_role.
// hotel_invitation.staff_role only allows MANAGER and RECEPTIONIST (owners are not invited).
public enum StaffRole {
    OWNER,
    MANAGER,
    RECEPTIONIST
}
