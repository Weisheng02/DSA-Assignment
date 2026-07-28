package entity;

/**
 * Entity class representing a Guest Booking / Reservation
 */
public class Booking {
    private String bookingId;
    private String guestConfirmationNumber;
    private String roomNumber;
    private String checkInDate;
    private int numberOfNights;

    public Booking(String bookingId, String guestConfirmationNumber, String roomNumber, String checkInDate, int numberOfNights) {
        this.bookingId = bookingId;
        this.guestConfirmationNumber = guestConfirmationNumber;
        this.roomNumber = roomNumber;
        this.checkInDate = checkInDate;
        this.numberOfNights = numberOfNights;
    }

    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }

    public String getGuestConfirmationNumber() { return guestConfirmationNumber; }
    public void setGuestConfirmationNumber(String guestConfirmationNumber) { this.guestConfirmationNumber = guestConfirmationNumber; }

    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }

    public String getCheckInDate() { return checkInDate; }
    public void setCheckInDate(String checkInDate) { this.checkInDate = checkInDate; }

    public int getNumberOfNights() { return numberOfNights; }
    public void setNumberOfNights(int numberOfNights) { this.numberOfNights = numberOfNights; }

    @Override
    public String toString() {
        return "Booking{" +
                "ID='" + bookingId + '\'' +
                ", ConfirmNo='" + guestConfirmationNumber + '\'' +
                ", RoomNo='" + roomNumber + '\'' +
                ", CheckIn='" + checkInDate + '\'' +
                ", Nights=" + numberOfNights +
                '}';
    }
}
