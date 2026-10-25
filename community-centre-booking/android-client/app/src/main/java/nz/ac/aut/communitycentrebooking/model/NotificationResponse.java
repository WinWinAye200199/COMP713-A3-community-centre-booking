package nz.ac.aut.communitycentrebooking.model;

public class NotificationResponse {

    private Long id;
    private Long bookingId;
    private String message;
    private String createdAt;

    public Long getId() {
        return id;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public String getMessage() {
        return message;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}