package nz.ac.aut.notification_consumer.repository;

import nz.ac.aut.notification_consumer.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
}
