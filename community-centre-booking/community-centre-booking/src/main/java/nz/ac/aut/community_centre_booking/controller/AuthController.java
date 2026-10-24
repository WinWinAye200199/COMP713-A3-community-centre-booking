package nz.ac.aut.community_centre_booking.controller;

import nz.ac.aut.community_centre_booking.dto.UserResponse;
import nz.ac.aut.community_centre_booking.model.AppUser;
import nz.ac.aut.community_centre_booking.repository.AppUserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.NoSuchElementException;

// returns the logged-in user so clients can check login and role
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AppUserRepository userRepository;

    public AuthController(AppUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {
        AppUser user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new NoSuchElementException("User not found."));

        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole());
    }
}