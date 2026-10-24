package nz.ac.aut.community_centre_booking.controller;

import jakarta.validation.Valid;
import nz.ac.aut.community_centre_booking.dto.BookingRequest;
import nz.ac.aut.community_centre_booking.dto.BookingResponse;
import nz.ac.aut.community_centre_booking.dto.StatusUpdateRequest;
import nz.ac.aut.community_centre_booking.model.BookingStatus;
import nz.ac.aut.community_centre_booking.service.BookingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

// controller for handling booking-related requests
@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    // staff: all bookings, optionally filtered by status (e.g. ?status=PENDING)
    @GetMapping
    @PreAuthorize("hasRole('STAFF')")
    public List<BookingResponse> getBookings(
            @RequestParam(required = false) BookingStatus status) {
        return bookingService.getBookings(status);
    }

    // member: bookings made by the logged-in member
    @GetMapping("/my")
    @PreAuthorize("hasRole('MEMBER')")
    public List<BookingResponse> getMyBookings(Authentication authentication) {
        return bookingService.getMyBookings(authentication.getName());
    }

    // member: creates a pending booking and returns its location with a 201 response
    @PostMapping
    @PreAuthorize("hasRole('MEMBER')")
    public ResponseEntity<BookingResponse> createBooking(
            @Valid @RequestBody BookingRequest request,
            Authentication authentication) {

        BookingResponse createdBooking = bookingService.createBooking(request, authentication.getName());

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdBooking.id())
                .toUri();

        return ResponseEntity
                .created(location)
                .body(createdBooking);
    }

    // staff: approves or rejects a pending booking
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('STAFF')")
    public BookingResponse updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusUpdateRequest request) {
        return bookingService.updateStatus(id, request.status());
    }

    // member cancels own booking, staff can cancel any booking
    @PatchMapping("/{id}/cancel")
    public BookingResponse cancelBooking(
            @PathVariable Long id,
            Authentication authentication) {

        boolean isStaff = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_STAFF".equals(authority.getAuthority()));

        return bookingService.cancelBooking(id, authentication.getName(), isStaff);
    }
}
