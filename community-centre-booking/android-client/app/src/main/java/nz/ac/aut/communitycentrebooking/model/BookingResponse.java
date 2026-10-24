package nz.ac.aut.communitycentrebooking.model;

// Booking data returned by the Spring Boot API.
public class BookingResponse {

    private Long id;
    private Long roomId;
    private String roomName;
    private String memberName;
    private String memberEmail;
    private String purpose;
    private String bookingDate;
    private String startTime;
    private String endTime;
    private String status;

    public Long getId() {
        return id;
    }

    public Long getRoomId() {
        return roomId;
    }

    public String getRoomName() {
        return roomName;
    }

    public String getMemberName() {
        return memberName;
    }

    public String getMemberEmail() {
        return memberEmail;
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

    public String getStatus() {
        return status;
    }
}