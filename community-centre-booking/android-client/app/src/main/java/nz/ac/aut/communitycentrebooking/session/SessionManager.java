package nz.ac.aut.communitycentrebooking.session;

import nz.ac.aut.communitycentrebooking.model.UserResponse;

// Keeps the logged-in member and authentication header while the app is open.
public final class SessionManager {

    private static String authorization;
    private static UserResponse currentUser;

    private SessionManager() {
        // Prevents this utility class from being instantiated.
    }

    public static void startSession(
            String authorizationHeader,
            UserResponse user) {

        authorization = authorizationHeader;
        currentUser = user;
    }

    public static String getAuthorization() {
        return authorization;
    }

    public static UserResponse getCurrentUser() {
        return currentUser;
    }

    public static boolean isLoggedIn() {
        return authorization != null && currentUser != null;
    }

    public static void clearSession() {
        authorization = null;
        currentUser = null;
    }
}