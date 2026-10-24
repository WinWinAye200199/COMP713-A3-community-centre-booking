package nz.ac.aut.community_centre_booking.event;

import nz.ac.aut.community_centre_booking.model.Booking;
import nz.ac.aut.community_centre_booking.model.BookingStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

// domain event sent when a booking status changes (approved, rejected or cancelled)
public record BookingStatusChangedEvent(
        String eventId,
        Long bookingId,
        Long memberId,
        String roomName,
        LocalDate bookingDate,
        LocalTime startTime,
        BookingStatus oldStatus,
        BookingStatus newStatus,
        Instant occurredAt) {

    public static BookingStatusChangedEvent from(Booking booking, BookingStatus oldStatus) {
        return new BookingStatusChangedEvent(
                UUID.randomUUID().toString(),
                booking.getId(),
                booking.getMember().getId(),
                booking.getRoom().getName(),
                booking.getBookingDate(),
                booking.getStartTime(),
                oldStatus,
                booking.getStatus(),
                Instant.now());
    }
}
