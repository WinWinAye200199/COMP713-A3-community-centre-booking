package nz.ac.aut.communitycentrebooking.model;

// Represents a community centre room returned by the backend.
public class Room {

    private Long id;
    private String name;
    private int capacity;
    private String description;

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getCapacity() {
        return capacity;
    }

    public String getDescription() {
        return description;
    }
}