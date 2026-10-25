package nz.ac.aut.community_centre_booking.repository;

import jakarta.persistence.LockModeType;
import nz.ac.aut.community_centre_booking.model.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

// repository for Room entity
public interface RoomRepository extends JpaRepository<Room, Long> {

    // A3: SELECT ... FOR UPDATE - other bookings for the same room wait until this transaction ends
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Room r WHERE r.id = :id")
    Optional<Room> findByIdForUpdate(@Param("id") Long id);
}
