package nz.ac.aut.communitycentrebooking.network;

import java.util.List;

import nz.ac.aut.communitycentrebooking.model.Room;
import nz.ac.aut.communitycentrebooking.model.UserResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;

// Defines the HTTP requests made to the Spring Boot API.
public interface ApiService {

    @GET("api/auth/me")
    Call<UserResponse> getCurrentUser(
            @Header("Authorization") String authorization
    );

    @GET("api/rooms")
    Call<List<Room>> getRooms();
}