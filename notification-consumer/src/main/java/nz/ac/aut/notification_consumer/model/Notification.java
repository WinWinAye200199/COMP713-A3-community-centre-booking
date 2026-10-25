package nz.ac.aut.notification_consumer.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

// notification for a member, written by this consumer
@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long memberId;

    @Column(nullable = false)
    private Long bookingId;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(nullable = false)
    private Instant createdAt;

    public Notification() {
    }

    public Notification(Long memberId, Long bookingId, String message) {
        this.memberId = memberId;
        this.bookingId = bookingId;
        this.message = message;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }
}
