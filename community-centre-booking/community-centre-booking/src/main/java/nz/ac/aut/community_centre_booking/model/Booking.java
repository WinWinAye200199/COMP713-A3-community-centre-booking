package nz.ac.aut.community_centre_booking.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalTime;

//represents a booking for a room in the community centre
@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String customerName;
    private String customerEmail;
    private String purpose;
    private LocalDate bookingDate;
    private LocalTime startTime;
    private LocalTime endTime;

    // many to one relationship with room
    @ManyToOne(optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    // A3: member who made the booking
    @ManyToOne
    @JoinColumn(name = "member_id")
    private AppUser member;

    // A3: approval status of the booking
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status = BookingStatus.PENDING;

    public Booking() {
    }

    // A3: creates a new pending booking for the logged-in member
    public Booking(
            AppUser member,
            String purpose,
            LocalDate bookingDate,
            LocalTime startTime,
            LocalTime endTime,
            Room room) {

        this.member = member;
        this.customerName = member.getName();
        this.customerEmail = member.getEmail();
        this.purpose = purpose;
        this.bookingDate = bookingDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.room = room;
        this.status = BookingStatus.PENDING;
    }

    public Long getId() {
        return id;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public String getPurpose() {
        return purpose;
    }

    public LocalDate getBookingDate() {
        return bookingDate;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public Room getRoom() {
        return room;
    }

    public AppUser getMember() {
        return member;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
    }
}
