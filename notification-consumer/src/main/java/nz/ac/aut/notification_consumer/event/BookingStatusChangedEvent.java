package nz.ac.aut.notification_consumer.event;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

// JSON contract of the event published by the booking API
public record BookingStatusChangedEvent(
        String eventId,
        Long bookingId,
        Long memberId,
        String roomName,
        LocalDate bookingDate,
        LocalTime startTime,
        String oldStatus,
        String newStatus,
        Instant occurredAt) {
}
