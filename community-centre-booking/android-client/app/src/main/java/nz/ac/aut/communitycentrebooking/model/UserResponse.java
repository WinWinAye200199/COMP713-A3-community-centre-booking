package nz.ac.aut.communitycentrebooking.model;

// Represents the logged-in user returned by the backend.
public class UserResponse {

    private Long id;
    private String name;
    private String email;
    private String role;

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }
}