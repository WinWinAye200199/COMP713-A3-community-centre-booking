package nz.ac.aut.notification_consumer.repository;

import nz.ac.aut.notification_consumer.model.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, String> {
}
