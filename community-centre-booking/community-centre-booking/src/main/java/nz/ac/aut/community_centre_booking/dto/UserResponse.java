package nz.ac.aut.community_centre_booking.dto;

import nz.ac.aut.community_centre_booking.model.Role;

// user data returned to clients, without the password hash
public record UserResponse(Long id, String name, String email, Role role) {
}