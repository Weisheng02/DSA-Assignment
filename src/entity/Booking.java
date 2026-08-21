package entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Author: Zhi Xuan
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
    private String bookingStatus; // Confirmed, CheckedIn, CheckedOut, Cancelled, NoShow
    private String specialRequest;
    private String bookingCreatedDate;
    private String cancellationReason;
    private String cancelledBy;
    private String cancellationDate;
    private String actualCheckInDate;
    private String actualCheckOutDate;
    private String noShowDate;

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
        this.specialRequest = "None";
        this.bookingCreatedDate = LocalDateTime.now().withNano(0).toString();
        this.cancellationReason = "N/A";
        this.cancelledBy = "N/A";
        this.cancellationDate = "N/A";
        this.actualCheckInDate = "N/A";
        this.actualCheckOutDate = "N/A";
        this.noShowDate = "N/A";
    }

    public String getBookingId() {
        return bookingId;
    }

    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }

    public String getGuestConfirmationNumber() {
        return guestConfirmationNumber;
    }

    public void setGuestConfirmationNumber(String guestConfirmationNumber) {
        this.guestConfirmationNumber = guestConfirmationNumber;
    }

    public String getGuestName() {
        return guestName;
    }

    public void setGuestName(String guestName) {
        this.guestName = guestName;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public String getRoomType() {
        return roomType;
    }

    public void setRoomType(String roomType) {
        this.roomType = roomType;
    }

    public double getRoomPrice() {
        return roomPrice;
    }

    public void setRoomPrice(double roomPrice) {
        this.roomPrice = roomPrice;
    }

    public String getCheckInDate() {
        return checkInDate;
    }

    public void setCheckInDate(String checkInDate) {
        this.checkInDate = checkInDate;
    }

    public int getNumberOfNights() {
        return numberOfNights;
    }

    public void setNumberOfNights(int numberOfNights) {
        this.numberOfNights = numberOfNights;
    }

    public String getBookingStatus() {
        return bookingStatus;
    }

    public void setBookingStatus(String bookingStatus) {
        this.bookingStatus = bookingStatus;
    }

    public String getSpecialRequest() {
        return specialRequest;
    }

    public void setSpecialRequest(String specialRequest) {
        this.specialRequest = (specialRequest == null || specialRequest.trim().isEmpty())
                ? "None"
                : specialRequest.trim();
    }

    public String getBookingCreatedDate() {
        return bookingCreatedDate;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public String getCancelledBy() {
        return cancelledBy;
    }

    public String getCancellationDate() {
        return cancellationDate;
    }

    public String getActualCheckInDate() {
        return actualCheckInDate;
    }

    public String getActualCheckOutDate() {
        return actualCheckOutDate;
    }

    public String getNoShowDate() {
        return noShowDate;
    }

    /**
     * Records a recoverable cancellation audit trail instead of deleting the
     * booking.
     */
    public void recordCancellation(String reason, String staffName) {
        this.bookingStatus = "Cancelled";
        this.cancellationReason = (reason == null || reason.trim().isEmpty()) ? "No reason recorded" : reason.trim();
        this.cancelledBy = (staffName == null || staffName.trim().isEmpty()) ? "Booking Staff" : staffName.trim();
        this.cancellationDate = LocalDateTime.now().withNano(0).toString();
    }

    public void recordCheckIn() {
        recordCheckIn(LocalDate.now());
    }

    /**
     * Records an explicit actual date, which is also useful when loading history.
     */
    public void recordCheckIn(LocalDate actualDate) {
        this.bookingStatus = "CheckedIn";
        this.actualCheckInDate = (actualDate != null ? actualDate : LocalDate.now()).toString();
    }

    public void recordCheckOut() {
        recordCheckOut(LocalDate.now());
    }

    /**
     * Records an explicit actual date, which is also useful when loading history.
     */
    public void recordCheckOut(LocalDate actualDate) {
        this.bookingStatus = "CheckedOut";
        this.actualCheckOutDate = (actualDate != null ? actualDate : LocalDate.now()).toString();
    }

    public void recordNoShow() {
        recordNoShow(LocalDate.now());
    }

    /**
     * Records an explicit no-show date while preserving the normal no-argument API.
     */
    public void recordNoShow(LocalDate recordedDate) {
        this.bookingStatus = "NoShow";
        this.noShowDate = (recordedDate != null ? recordedDate : LocalDate.now()).toString();
    }

    /**
     * Computes the departure date from the inclusive check-in date and stay length.
     */
    public String getCheckOutDate() {
        try {
            return LocalDate.parse(checkInDate).plusDays(numberOfNights).toString();
        } catch (Exception e) {
            return "N/A";
        }
    }

    public int getActualNightsStayed() {
        if ("N/A".equals(actualCheckInDate) || "N/A".equals(actualCheckOutDate))
            return 0;
        long nights = ChronoUnit.DAYS.between(LocalDate.parse(actualCheckInDate), LocalDate.parse(actualCheckOutDate));
        return (int) Math.max(1, nights);
    }

    /** Reserved nights remain chargeable; an overstay adds elapsed extra nights. */
    public int getBillableNightsAsOf(LocalDate asOfDate) {
        try {
            long elapsed = ChronoUnit.DAYS.between(LocalDate.parse(checkInDate), asOfDate);
            return Math.max(numberOfNights, (int) Math.max(1, elapsed));
        } catch (Exception e) {
            return numberOfNights;
        }
    }

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
                ", CheckOut='" + getCheckOutDate() + '\'' +
                ", Nights=" + numberOfNights +
                ", Status='" + bookingStatus + '\'' +
                '}';
    }
}
