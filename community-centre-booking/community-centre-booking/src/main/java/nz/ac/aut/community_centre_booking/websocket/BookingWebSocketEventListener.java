package nz.ac.aut.community_centre_booking.websocket;

import nz.ac.aut.community_centre_booking.event.BookingChangedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class BookingWebSocketEventListener {

    private final BookingWebSocketHandler webSocketHandler;

    public BookingWebSocketEventListener(
            BookingWebSocketHandler webSocketHandler) {

        this.webSocketHandler = webSocketHandler;
    }

    // Sends the notification only after the database change succeeds.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleBookingChanged(BookingChangedEvent event) {
        webSocketHandler.broadcastBookingChanged(
                event.bookingId(),
                event.changeType());
    }
}