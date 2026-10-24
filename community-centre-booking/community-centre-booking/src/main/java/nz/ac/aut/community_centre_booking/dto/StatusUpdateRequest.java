package nz.ac.aut.community_centre_booking.dto;

import jakarta.validation.constraints.NotNull;
import nz.ac.aut.community_centre_booking.model.BookingStatus;

// request body for staff approving or rejecting a booking
public record StatusUpdateRequest(
        @NotNull(message = "Status is required.") BookingStatus status) {
}
