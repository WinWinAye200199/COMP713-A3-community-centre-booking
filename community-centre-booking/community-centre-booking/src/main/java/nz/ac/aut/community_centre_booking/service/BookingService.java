package nz.ac.aut.community_centre_booking.service;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import nz.ac.aut.community_centre_booking.dto.BookingRequest;
import nz.ac.aut.community_centre_booking.dto.BookingResponse;
import nz.ac.aut.community_centre_booking.event.BookingStatusChangedEvent;
import nz.ac.aut.community_centre_booking.model.AppUser;
import nz.ac.aut.community_centre_booking.model.Booking;
import nz.ac.aut.community_centre_booking.model.BookingStatus;
import nz.ac.aut.community_centre_booking.model.Room;
import nz.ac.aut.community_centre_booking.repository.AppUserRepository;
import nz.ac.aut.community_centre_booking.repository.BookingRepository;
import nz.ac.aut.community_centre_booking.repository.RoomRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import nz.ac.aut.community_centre_booking.event.BookingChangedEvent;

import java.util.List;
import java.util.NoSuchElementException;

// contains main booking rules: creating, approving, rejecting and cancelling bookings
@Service
public class BookingService {

    // bookings with these statuses block the room time slot
    private static final List<BookingStatus> ACTIVE_STATUSES = List.of(BookingStatus.PENDING, BookingStatus.APPROVED);

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final AppUserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    public BookingService(
            BookingRepository bookingRepository,
            RoomRepository roomRepository,
            AppUserRepository userRepository,
            ApplicationEventPublisher eventPublisher) {

        this.bookingRepository = bookingRepository;
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
    }

    // staff view: all bookings, or only bookings with the given status
    @Transactional(readOnly = true)
    public List<BookingResponse> getBookings(BookingStatus status) {
        List<Booking> bookings = (status == null)
                ? bookingRepository.findAllByOrderByBookingDateAscStartTimeAsc()
                : bookingRepository.findByStatusOrderByBookingDateAscStartTimeAsc(status);

        return bookings.stream().map(BookingResponse::from).toList();
    }

    // member view: bookings made by the logged-in member
    @Transactional(readOnly = true)
    public List<BookingResponse> getMyBookings(String email) {
        AppUser member = findUser(email);

        return bookingRepository.findByMemberOrderByBookingDateAscStartTimeAsc(member)
                .stream()
                .map(BookingResponse::from)
                .toList();
    }

    // creates a pending booking after checking the time and room availability
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public BookingResponse createBooking(BookingRequest request, String email) {
        AppUser member = findUser(email);

        Room room = roomRepository.findByIdForUpdate(request.roomId())
                .orElseThrow(() -> new NoSuchElementException("Room not found."));

        if (!request.startTime().isBefore(request.endTime())) {
            throw new IllegalArgumentException("End time must be after start time.");
        }

        long overlapping = bookingRepository.countOverlapping(
                room,
                request.bookingDate(),
                request.startTime(),
                request.endTime(),
                ACTIVE_STATUSES);

        if (overlapping > 0) {
            throw new IllegalStateException("The room is already booked during this time.");
        }

        Booking booking = new Booking(
                member,
                request.purpose(),
                request.bookingDate(),
                request.startTime(),
                request.endTime(),
                room);

        Booking savedBooking = bookingRepository.save(booking);

        eventPublisher.publishEvent(
                new BookingChangedEvent(
                        savedBooking.getId(),
                        "CREATED"));

        return BookingResponse.from(savedBooking);
    }

    // staff approves or rejects a pending booking
    @Transactional
    public BookingResponse updateStatus(Long id, BookingStatus newStatus) {
        if (newStatus != BookingStatus.APPROVED && newStatus != BookingStatus.REJECTED) {
            throw new IllegalArgumentException("Status must be APPROVED or REJECTED.");
        }

        Booking booking = findBooking(id);

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalStateException(
                    "Only pending bookings can be approved or rejected. Current status: " + booking.getStatus());
        }

        BookingStatus oldStatus = booking.getStatus();
        booking.setStatus(newStatus);
        publishStatusChange(booking, oldStatus);

        eventPublisher.publishEvent(
                new BookingChangedEvent(
                        booking.getId(),
                        "STATUS_CHANGED"));

        return BookingResponse.from(booking);
    }

    // member cancels their own booking, or staff cancels any booking
    @Transactional
    public BookingResponse cancelBooking(Long id, String email, boolean isStaff) {
        Booking booking = findBooking(id);

        boolean isOwner = booking.getMember() != null
                && booking.getMember().getEmail().equals(email);

        if (!isStaff && !isOwner) {
            throw new AccessDeniedException("You can only cancel your own bookings.");
        }

        if (!ACTIVE_STATUSES.contains(booking.getStatus())) {
            throw new IllegalStateException(
                    "Only pending or approved bookings can be cancelled. Current status: " + booking.getStatus());
        }

        BookingStatus oldStatus = booking.getStatus();
        booking.setStatus(BookingStatus.CANCELLED);
        publishStatusChange(booking, oldStatus);

        eventPublisher.publishEvent(
                new BookingChangedEvent(
                        booking.getId(),
                        "CANCELLED"));

        return BookingResponse.from(booking);
    }

    // A3: raises an internal event; it is sent to RabbitMQ only after the
    // transaction commits
    private void publishStatusChange(Booking booking, BookingStatus oldStatus) {
        if (booking.getMember() == null) {
            return; // A2 bookings without a member cannot receive notifications
        }
        eventPublisher.publishEvent(BookingStatusChangedEvent.from(booking, oldStatus));
    }

    private AppUser findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NoSuchElementException("User not found."));
    }

    private Booking findBooking(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Booking not found."));
    }
}
