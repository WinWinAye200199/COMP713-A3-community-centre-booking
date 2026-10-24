package nz.ac.aut.communitycentrebooking.network;

import java.util.List;

import nz.ac.aut.communitycentrebooking.model.BookingRequest;
import nz.ac.aut.communitycentrebooking.model.BookingResponse;
import nz.ac.aut.communitycentrebooking.model.Room;
import nz.ac.aut.communitycentrebooking.model.UserResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PATCH;
import retrofit2.http.Path;

// Defines the HTTP requests made to the Spring Boot API.
public interface ApiService {

    @GET("api/auth/me")
    Call<UserResponse> getCurrentUser(
            @Header("Authorization") String authorization
    );

    @GET("api/rooms")
    Call<List<Room>> getRooms();

    @POST("api/bookings")
    Call<BookingResponse> createBooking(
            @Header("Authorization") String authorization,
            @Body BookingRequest bookingRequest
    );

    @GET("api/bookings/my")
    Call<List<BookingResponse>> getMyBookings(
            @Header("Authorization") String authorization
    );

    @PATCH("api/bookings/{id}/cancel")
    Call<BookingResponse> cancelBooking(
            @Header("Authorization") String authorization,
            @Path("id") Long bookingId
    );
}