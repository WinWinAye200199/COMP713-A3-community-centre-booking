package nz.ac.aut.communitycentrebooking.network;

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
}