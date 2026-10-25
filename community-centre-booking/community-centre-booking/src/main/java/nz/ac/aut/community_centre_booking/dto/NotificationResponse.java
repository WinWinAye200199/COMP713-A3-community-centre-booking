package nz.ac.aut.community_centre_booking.dto;

import nz.ac.aut.community_centre_booking.model.Notification;

import java.time.Instant;

// notification data returned to the Android app
public record NotificationResponse(Long id, Long bookingId, String message, Instant createdAt) {

    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getBookingId(),
                notification.getMessage(),
                notification.getCreatedAt());
    }
}
