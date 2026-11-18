package com.auca.classroom;

import com.auca.exceptions.ValidationException;
import com.auca.interfaces.Identifiable;
import com.auca.interfaces.Registrable;

/** A physical classroom; "active" means currently available. */
public class Classroom implements Identifiable, Registrable {

    private String classroomId;
    private String buildingName;
    private String roomNumber;
    private int capacity;
    private int floorNumber;
    private boolean available;

    public Classroom(String classroomId, String buildingName, String roomNumber,
                     int capacity, int floorNumber, boolean available) {
        this.classroomId = classroomId;
        this.buildingName = buildingName;
        this.roomNumber = roomNumber;
        this.capacity = capacity;
        this.floorNumber = floorNumber;
        this.available = available;
    }

    @Override
    public String getId() { return classroomId; }

    @Override
    public String getDisplayName() { return buildingName + " " + roomNumber; }

    @Override
    public boolean isActive() { return available; }

    @Override
    public void validate() throws ValidationException {
        Registrable.requireId("Classroom ID", classroomId);
        Registrable.requireText("Building name", buildingName, 2, 60);
        Registrable.requireText("Room number", roomNumber, 1, 10);
        if (capacity <= 0) {
            throw new ValidationException("Capacity must be greater than zero.");
        }
        if (capacity > 1000) {
            throw new ValidationException("Capacity cannot exceed 1000.");
        }
        if (floorNumber < 0) {
            throw new ValidationException("Floor number must be zero or greater.");
        }
        if (floorNumber > 100) {
            throw new ValidationException("Floor number cannot exceed 100.");
        }
    }

    @Override
    public void printDetails() {
        System.out.println("  Classroom ID        : " + classroomId);
        System.out.println("  Building name       : " + buildingName);
        System.out.println("  Room number         : " + roomNumber);
        System.out.println("  Capacity            : " + capacity);
        System.out.println("  Floor number        : " + floorNumber);
        System.out.println("  Available           : " + (available ? "YES" : "NO"));
    }

    @Override
    public String getRegistrationSummary() {
        return "[" + classroomId + "] " + getDisplayName() + " - floor " + floorNumber + " - capacity "
                + capacity + " - " + (available ? "available" : "unavailable");
    }

    public String getClassroomId() { return classroomId; }
    public void setClassroomId(String classroomId) { this.classroomId = classroomId; }
    public String getBuildingName() { return buildingName; }
    public void setBuildingName(String buildingName) { this.buildingName = buildingName; }
    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }
    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }
    public int getFloorNumber() { return floorNumber; }
    public void setFloorNumber(int floorNumber) { this.floorNumber = floorNumber; }
    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }

    @Override
    public String toString() {
        return "Classroom{id='" + classroomId + "', room='" + getDisplayName() + "', capacity=" + capacity
                + ", floor=" + floorNumber + ", available=" + available + "}";
    }
}
