package nz.ac.aut.notification_consumer.listener;

import nz.ac.aut.notification_consumer.event.BookingStatusChangedEvent;
import nz.ac.aut.notification_consumer.model.Notification;
import nz.ac.aut.notification_consumer.model.ProcessedEvent;
import nz.ac.aut.notification_consumer.repository.NotificationRepository;
import nz.ac.aut.notification_consumer.repository.ProcessedEventRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// unit tests for the consumer logic, using mocked repositories (no database or broker needed)
class BookingEventListenerTest {

    private final NotificationRepository notificationRepository = mock(NotificationRepository.class);
    private final ProcessedEventRepository processedEventRepository = mock(ProcessedEventRepository.class);

    private final BookingStatusChangedEvent event = new BookingStatusChangedEvent(
            "event-1", 3L, 1L, "Meeting Room",
            LocalDate.of(2026, 10, 31), LocalTime.of(9, 0),
            "PENDING", "APPROVED", Instant.now());

    @Test
    void newEventCreatesOneNotification() {
        BookingEventListener listener =
                new BookingEventListener(notificationRepository, processedEventRepository, false);
        when(processedEventRepository.existsById("event-1")).thenReturn(false);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(call -> call.getArgument(0));

        listener.handle(event);

        verify(notificationRepository).save(any(Notification.class));
        verify(processedEventRepository).save(any(ProcessedEvent.class));
    }

    @Test
    void repeatedEventIsIgnored() {
        BookingEventListener listener =
                new BookingEventListener(notificationRepository, processedEventRepository, false);
        when(processedEventRepository.existsById("event-1")).thenReturn(true);

        listener.handle(event);

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void failureThrowsSoTheMessageIsRetried() {
        BookingEventListener listener =
                new BookingEventListener(notificationRepository, processedEventRepository, true);

        assertThrows(IllegalStateException.class, () -> listener.handle(event));
        verify(notificationRepository, never()).save(any());
    }
}
