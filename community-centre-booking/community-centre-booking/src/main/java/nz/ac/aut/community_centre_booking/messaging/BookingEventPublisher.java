package nz.ac.aut.community_centre_booking.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import nz.ac.aut.community_centre_booking.event.BookingStatusChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

// publishes booking events to RabbitMQ after the database transaction commits
@Component
public class BookingEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(BookingEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public BookingEventPublisher(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    // runs only if the status change was saved, so no event is sent for a rolled-back change
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookingStatusChanged(BookingStatusChangedEvent event) {
        try {
            rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.ROUTING_KEY, event);
            log.info("Published BookingStatusChanged event {} for booking {} ({} -> {}): {}",
                    event.eventId(), event.bookingId(), event.oldStatus(), event.newStatus(), toJson(event));
        } catch (AmqpException exception) {
            // the booking change is already saved; the event is lost (see report limitations)
            log.error("Broker unavailable - event {} for booking {} was not published: {}",
                    event.eventId(), event.bookingId(), exception.getMessage());
        }
    }

    private String toJson(BookingStatusChangedEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (Exception exception) {
            return event.toString();
        }
    }
}
