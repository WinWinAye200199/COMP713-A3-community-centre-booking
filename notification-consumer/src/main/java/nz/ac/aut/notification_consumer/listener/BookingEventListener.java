package nz.ac.aut.notification_consumer.listener;

import nz.ac.aut.notification_consumer.config.RabbitConfig;
import nz.ac.aut.notification_consumer.event.BookingStatusChangedEvent;
import nz.ac.aut.notification_consumer.model.Notification;
import nz.ac.aut.notification_consumer.model.ProcessedEvent;
import nz.ac.aut.notification_consumer.repository.NotificationRepository;
import nz.ac.aut.notification_consumer.repository.ProcessedEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// turns booking status events into member notifications
@Component
public class BookingEventListener {

    private static final Logger log = LoggerFactory.getLogger(BookingEventListener.class);

    private final NotificationRepository notificationRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final boolean simulateFailure;

    public BookingEventListener(
            NotificationRepository notificationRepository,
            ProcessedEventRepository processedEventRepository,
            @Value("${notification.simulate-failure:false}") boolean simulateFailure) {

        this.notificationRepository = notificationRepository;
        this.processedEventRepository = processedEventRepository;
        this.simulateFailure = simulateFailure;
    }

    // success -> message is acknowledged; exception -> retried, then sent to the dead-letter queue
    @RabbitListener(queues = RabbitConfig.QUEUE)
    @Transactional
    public void handle(BookingStatusChangedEvent event) {
        log.info("Received event {} for booking {} ({} -> {})",
                event.eventId(), event.bookingId(), event.oldStatus(), event.newStatus());

        if (simulateFailure) {
            log.warn("Simulated failure while processing event {}", event.eventId());
            throw new IllegalStateException("Simulated consumer failure");
        }

        // idempotency: a repeated event must not create a second notification
        if (processedEventRepository.existsById(event.eventId())) {
            log.warn("Duplicate event {} ignored", event.eventId());
            return;
        }

        Notification notification = notificationRepository.save(
                new Notification(event.memberId(), event.bookingId(), buildMessage(event)));
        processedEventRepository.save(new ProcessedEvent(event.eventId()));

        log.info("Notification {} created for member {}", notification.getId(), event.memberId());
    }

    private String buildMessage(BookingStatusChangedEvent event) {
        return "Your booking for " + event.roomName()
                + " on " + event.bookingDate()
                + " at " + event.startTime()
                + " is now " + event.newStatus() + ".";
    }
}
