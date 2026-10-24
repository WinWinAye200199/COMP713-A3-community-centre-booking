package nz.ac.aut.community_centre_booking.repository;

import nz.ac.aut.community_centre_booking.model.AppUser;
import nz.ac.aut.community_centre_booking.model.Booking;
import nz.ac.aut.community_centre_booking.model.BookingStatus;
import nz.ac.aut.community_centre_booking.model.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;

// repository for Booking entity
public interface BookingRepository extends JpaRepository<Booking, Long> {

    // bookings made by one member
    List<Booking> findByMemberOrderByBookingDateAscStartTimeAsc(AppUser member);

    // bookings with a given status, for the staff view
    List<Booking> findByStatusOrderByBookingDateAscStartTimeAsc(BookingStatus status);

    // all bookings in date order, for the staff view
    List<Booking> findAllByOrderByBookingDateAscStartTimeAsc();

    // counts active bookings that overlap the requested time in the same room
    @Query("""
            SELECT COUNT(b) FROM Booking b
            WHERE b.room = :room
              AND b.bookingDate = :bookingDate
              AND b.status IN :activeStatuses
              AND b.startTime < :endTime
              AND b.endTime > :startTime
            """)
    long countOverlapping(
            @Param("room") Room room,
            @Param("bookingDate") LocalDate bookingDate,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("activeStatuses") Collection<BookingStatus> activeStatuses);
}
