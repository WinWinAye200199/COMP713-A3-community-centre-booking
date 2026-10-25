package nz.ac.aut.community_centre_booking.repository;

import nz.ac.aut.community_centre_booking.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// read access to notifications created by the consumer
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByMemberIdOrderByCreatedAtDesc(Long memberId);
}
