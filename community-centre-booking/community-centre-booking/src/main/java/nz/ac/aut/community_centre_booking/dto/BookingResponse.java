package nz.ac.aut.community_centre_booking.dto;

import nz.ac.aut.community_centre_booking.model.Booking;
import nz.ac.aut.community_centre_booking.model.BookingStatus;

import java.time.LocalDate;
import java.time.LocalTime;

// booking data returned to clients, without internal user details
public record BookingResponse(
        Long id,
        Long roomId,
        String roomName,
        String memberName,
        String memberEmail,
        String purpose,
        LocalDate bookingDate,
        LocalTime startTime,
        LocalTime endTime,
        BookingStatus status) {

    public static BookingResponse from(Booking booking) {
        return new BookingResponse(
                booking.getId(),
                booking.getRoom().getId(),
                booking.getRoom().getName(),
                booking.getCustomerName(),
                booking.getCustomerEmail(),
                booking.getPurpose(),
                booking.getBookingDate(),
                booking.getStartTime(),
                booking.getEndTime(),
                booking.getStatus());
    }
}
