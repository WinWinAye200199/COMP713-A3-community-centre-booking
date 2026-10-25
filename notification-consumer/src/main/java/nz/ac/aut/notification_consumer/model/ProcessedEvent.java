package nz.ac.aut.notification_consumer.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

// records handled event IDs so a repeated event does not create a second notification
@Entity
@Table(name = "processed_events")
public class ProcessedEvent {

    @Id
    @Column(length = 36)
    private String eventId;

    @Column(nullable = false)
    private Instant processedAt;

    public ProcessedEvent() {
    }

    public ProcessedEvent(String eventId) {
        this.eventId = eventId;
        this.processedAt = Instant.now();
    }
}
