package nz.ac.aut.community_centre_booking.service;

import nz.ac.aut.community_centre_booking.model.AppUser;
import nz.ac.aut.community_centre_booking.repository.AppUserRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

// loads users from the database for Spring Security login
@Service
public class AppUserDetailsService implements UserDetailsService {

    private final AppUserRepository userRepository;

    public AppUserDetailsService(AppUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found."));

        return User.withUsername(user.getEmail())
                .password(user.getPasswordHash())
                .roles(user.getRole().name())
                .build();
    }
}