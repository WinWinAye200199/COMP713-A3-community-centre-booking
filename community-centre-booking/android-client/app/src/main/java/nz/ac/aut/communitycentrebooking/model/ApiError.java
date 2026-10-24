package nz.ac.aut.communitycentrebooking.model;

// Error information returned by the Spring Boot API.
public class ApiError {

    private String code;
    private String message;
    private String path;

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public String getPath() {
        return path;
    }
}