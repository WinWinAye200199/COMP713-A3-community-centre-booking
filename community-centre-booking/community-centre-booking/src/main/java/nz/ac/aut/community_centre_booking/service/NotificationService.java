package nz.ac.aut.community_centre_booking.service;

import nz.ac.aut.community_centre_booking.dto.NotificationResponse;
import nz.ac.aut.community_centre_booking.model.AppUser;
import nz.ac.aut.community_centre_booking.repository.AppUserRepository;
import nz.ac.aut.community_centre_booking.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

// returns notifications for the logged-in member
@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final AppUserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository, AppUserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getMyNotifications(String email) {
        AppUser member = userRepository.findByEmail(email)
                .orElseThrow(() -> new NoSuchElementException("User not found."));

        return notificationRepository.findByMemberIdOrderByCreatedAtDesc(member.getId())
                .stream()
                .map(NotificationResponse::from)
                .toList();
    }
}
