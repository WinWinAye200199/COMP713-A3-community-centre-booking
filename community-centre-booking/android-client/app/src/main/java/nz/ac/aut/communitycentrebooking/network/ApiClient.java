package nz.ac.aut.communitycentrebooking.network;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

// Creates the Retrofit connection used by the Android app.
public class ApiClient {

    // Android emulators use 10.0.2.2 to access localhost on the host computer.
    private static final String BASE_URL = "http://10.0.2.2:8080/";

    private static final Retrofit retrofit = new Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build();

    private ApiClient() {
        // Prevents this utility class from being instantiated.
    }

    public static ApiService getService() {
        return retrofit.create(ApiService.class);
    }
}