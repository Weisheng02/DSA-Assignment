package entity;

/**
 * Author: Weisheng
 * Entity class representing a Guest Booking / Reservation
 */
public class Booking {
    private String bookingId;
    private String guestConfirmationNumber;
    private String guestName;
    private String roomNumber;
    private String roomType;
    private double roomPrice;
    private String checkInDate;
    private int numberOfNights;
    private String bookingStatus; // Confirmed, Cancelled

    public Booking(String bookingId, String guestConfirmationNumber, String guestName,
                   String roomNumber, String roomType, double roomPrice,
                   String checkInDate, int numberOfNights) {
        this.bookingId = bookingId;
        this.guestConfirmationNumber = guestConfirmationNumber;
        this.guestName = guestName;
        this.roomNumber = roomNumber;
        this.roomType = roomType;
        this.roomPrice = roomPrice;
        this.checkInDate = checkInDate;
        this.numberOfNights = numberOfNights;
        this.bookingStatus = "Confirmed";
    }

    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }

    public String getGuestConfirmationNumber() { return guestConfirmationNumber; }
    public void setGuestConfirmationNumber(String guestConfirmationNumber) {
        this.guestConfirmationNumber = guestConfirmationNumber;
    }

    public String getGuestName() { return guestName; }
    public void setGuestName(String guestName) { this.guestName = guestName; }

    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }

    public String getRoomType() { return roomType; }
    public void setRoomType(String roomType) { this.roomType = roomType; }

    public double getRoomPrice() { return roomPrice; }
    public void setRoomPrice(double roomPrice) { this.roomPrice = roomPrice; }

    public String getCheckInDate() { return checkInDate; }
    public void setCheckInDate(String checkInDate) { this.checkInDate = checkInDate; }

    public int getNumberOfNights() { return numberOfNights; }
    public void setNumberOfNights(int numberOfNights) { this.numberOfNights = numberOfNights; }

    public String getBookingStatus() { return bookingStatus; }
    public void setBookingStatus(String bookingStatus) { this.bookingStatus = bookingStatus; }

    public double getTotalPrice() {
        return roomPrice * numberOfNights;
    }

    @Override
    public String toString() {
        return "Booking{" +
                "ID='" + bookingId + '\'' +
                ", Guest='" + guestName + '\'' +
                ", ConfirmNo='" + guestConfirmationNumber + '\'' +
                ", RoomNo='" + roomNumber + '\'' +
                ", Type='" + roomType + '\'' +
                ", CheckIn='" + checkInDate + '\'' +
                ", Nights=" + numberOfNights +
                ", Status='" + bookingStatus + '\'' +
                '}';
    }
}
