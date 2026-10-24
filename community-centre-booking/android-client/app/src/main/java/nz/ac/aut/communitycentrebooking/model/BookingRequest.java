package nz.ac.aut.communitycentrebooking.model;

// Booking information sent to the Spring Boot API.
public class BookingRequest {

    private final String purpose;
    private final String bookingDate;
    private final String startTime;
    private final String endTime;
    private final Long roomId;

    public BookingRequest(
            String purpose,
            String bookingDate,
            String startTime,
            String endTime,
            Long roomId) {

        this.purpose = purpose;
        this.bookingDate = bookingDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.roomId = roomId;
    }

    public String getPurpose() {
        return purpose;
    }

    public String getBookingDate() {
        return bookingDate;
    }

    public String getStartTime() {
        return startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public Long getRoomId() {
        return roomId;
    }
}