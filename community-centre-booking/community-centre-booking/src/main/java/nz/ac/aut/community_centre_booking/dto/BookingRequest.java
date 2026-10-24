package nz.ac.aut.community_centre_booking.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.time.LocalTime;

// DTO for booking request data
// A3: customer name and email now come from the logged-in member
public record BookingRequest(

        @NotBlank(message = "A booking purpose is required.") String purpose,

        @NotNull(message = "Booking date is required.") @FutureOrPresent(message = "Booking date cannot be in the past.") LocalDate bookingDate,

        @NotNull(message = "Start time is required.") LocalTime startTime,

        @NotNull(message = "End time is required.") LocalTime endTime,

        @NotNull(message = "Room ID is required.") @Positive(message = "Room ID must be positive.") Long roomId

) {
}
