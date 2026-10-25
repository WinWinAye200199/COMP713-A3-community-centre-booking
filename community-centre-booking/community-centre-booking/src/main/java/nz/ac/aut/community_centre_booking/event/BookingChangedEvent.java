package nz.ac.aut.community_centre_booking.event;

// Represents a successful booking change that dashboards should know about.
public record BookingChangedEvent(
        Long bookingId,
        String changeType) {
}