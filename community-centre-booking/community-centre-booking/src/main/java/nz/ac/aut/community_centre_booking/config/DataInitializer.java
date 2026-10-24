package nz.ac.aut.community_centre_booking.config;

import nz.ac.aut.community_centre_booking.model.AppUser;
import nz.ac.aut.community_centre_booking.model.Role;
import nz.ac.aut.community_centre_booking.model.Room;
import nz.ac.aut.community_centre_booking.repository.AppUserRepository;
import nz.ac.aut.community_centre_booking.repository.RoomRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

// adds initial rooms and test accounts to the database if none exist
@Component
public class DataInitializer implements CommandLineRunner {

    private final RoomRepository roomRepository;
    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(RoomRepository roomRepository,
            AppUserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // check if the room repository is empty and add initial rooms if so
        if (roomRepository.count() == 0) {
            roomRepository.save(
                    new Room(
                            "Main Hall",
                            120,
                            "Large room suitable for events and group activities."));

            roomRepository.save(
                    new Room(
                            "Meeting Room",
                            20,
                            "Smaller room suitable for meetings and workshops."));

            roomRepository.save(
                    new Room(
                            "Activity Room",
                            40,
                            "General-purpose room for classes and community activities."));
        }

        // add test accounts for each role if no users exist
        if (userRepository.count() == 0) {
            userRepository.save(new AppUser("Test Member", "member@test.com",
                    passwordEncoder.encode("member123"), Role.MEMBER));
            userRepository.save(new AppUser("Second Member", "member2@test.com",
                    passwordEncoder.encode("member123"), Role.MEMBER));
            userRepository.save(new AppUser("Centre Staff", "staff@test.com",
                    passwordEncoder.encode("staff123"), Role.STAFF));
        }
    }
}