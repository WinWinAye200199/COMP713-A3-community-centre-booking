package nz.ac.aut.community_centre_booking.repository;

import nz.ac.aut.community_centre_booking.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// repository for AppUser entity
public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    // finds a user by email for login
    Optional<AppUser> findByEmail(String email);
}