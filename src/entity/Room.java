package entity;

/**
 * Author: Weisheng
 * Entity class representing a Resort Room
 */
public class Room implements Comparable<Room> {
    private String roomNumber;
    private String roomType;   // Deluxe Suite, Presidential Suite, Standard Room
    private String roomStatus; // Dirty, Cleaning In Progress, Inspected, Ready for Check-In, Occupied
    private double price;

    public Room(){
    }
    
    public Room(String roomNumber, String roomType, String roomStatus, double price) {
        this.roomNumber = roomNumber;
        this.roomType = roomType;
        this.roomStatus = roomStatus;
        this.price = price;
    }

    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }

    public String getRoomType() { return roomType; }
    public void setRoomType(String roomType) { this.roomType = roomType; }

    public String getRoomStatus() { return roomStatus; }
    public void setRoomStatus(String roomStatus) { this.roomStatus = roomStatus; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    @Override
    public int compareTo(Room other) {
        if (other == null || other.roomNumber == null) return 1;
        if (this.roomNumber == null) return -1;
        return this.roomNumber.compareToIgnoreCase(other.roomNumber);
    }

    @Override
    public String toString() {
        return "Room{" +
                "RoomNo='" + roomNumber + '\'' +
                ", Type='" + roomType + '\'' +
                ", Status='" + roomStatus + '\'' +
                ", Price=" + price +
                '}';
    }
}

