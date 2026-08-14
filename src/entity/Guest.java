package entity;

/**
 * Author: Weisheng
 * Entity class representing a Resort Guest Stay / Front Desk Check-In Record.
 *
 * Stores guest person identity (icNo) + stay/booking information.
 * Used as the BST node data, ordered by confirmationNumber (Stay Key).
 */
public class Guest implements Comparable<Guest> {

    // === Guest Person Identity (Permanent) ===
    private String guestName;
    private String icNo;            // IC / Passport Number (Person Identity Key)
    private String phoneNumber;
    private String gender;          // Male / Female / Other
    private String nationality;     // Malaysian / Foreigner / etc.
    private String email;           // Guest Email Address

    // === Stay / Booking Reference (BST Key) ===
    private String confirmationNumber;  // 8-digit unique Stay/Reservation Key (BST Search Key)
    private String bookingStatus;       // Registered, Reserved, CheckedIn, CheckedOut, Cancelled, NoShow
    private String checkInDate;
    private String checkOutDate;
    private int numberOfNights;

    // === Room Assignment ===
    private String assignedRoomNumber;
    private String roomType;            // Deluxe Suite, Presidential Suite, Standard Room
    private double roomRate;            // Nightly rate charged

    // === Loyalty (shared with Loyalty module) ===
    private String loyaltyTier;         // Platinum, Gold, Silver, Standard
    private int loyaltyPoints;

    // === Guest Preferences ===
    private String specialRequest;      // e.g., "Extra pillows", "High floor"

    // --- Default Constructor ---
    public Guest() {
    }

    // --- Convenience Constructor (minimal, for BST dummy/search key) ---
    public Guest(String guestName, String confirmationNumber, String loyaltyTier, int loyaltyPoints) {
        this(guestName, "N/A", "N/A", confirmationNumber, loyaltyTier, loyaltyPoints);
    }

    // --- Full Constructor ---
    public Guest(String guestName, String icNo, String confirmationNumber, String loyaltyTier, int loyaltyPoints) {
        this(guestName, icNo, "N/A", confirmationNumber, loyaltyTier, loyaltyPoints);
    }

    // --- Complete Constructor ---
    public Guest(String guestName, String icNo, String phoneNumber,
                 String confirmationNumber, String loyaltyTier, int loyaltyPoints) {
        this(guestName, icNo, phoneNumber, "N/A", "Malaysian", "N/A", confirmationNumber, loyaltyTier, loyaltyPoints);
    }

    // --- All-Fields Demographic Constructor ---
    public Guest(String guestName, String icNo, String phoneNumber, String gender,
                 String nationality, String email, String confirmationNumber,
                 String loyaltyTier, int loyaltyPoints) {
        this.guestName = guestName;
        this.icNo = (icNo != null && !icNo.trim().isEmpty()) ? icNo.trim() : "N/A";
        this.phoneNumber = (phoneNumber != null && !phoneNumber.trim().isEmpty()) ? phoneNumber.trim() : "N/A";
        this.gender = (gender != null && !gender.trim().isEmpty()) ? gender.trim() : "N/A";
        this.nationality = (nationality != null && !nationality.trim().isEmpty()) ? nationality.trim() : "Malaysian";
        this.email = (email != null && !email.trim().isEmpty()) ? email.trim() : "N/A";
        this.confirmationNumber = confirmationNumber;
        this.loyaltyTier = (loyaltyTier != null) ? loyaltyTier : "Standard";
        this.loyaltyPoints = loyaltyPoints;
        this.bookingStatus = "Registered";
        this.assignedRoomNumber = null;
        this.roomType = null;
        this.roomRate = 0.0;
        this.checkInDate = null;
        this.checkOutDate = null;
        this.numberOfNights = 0;
        this.specialRequest = null;
    }

    // ========== Getters & Setters ==========

    // --- Identity ---
    public String getGuestName() { return guestName; }
    public void setGuestName(String guestName) { this.guestName = guestName; }

    public String getIcNo() { return icNo; }
    public void setIcNo(String icNo) { this.icNo = icNo; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getNationality() { return nationality; }
    public void setNationality(String nationality) { this.nationality = nationality; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    // --- BST Key (immutable after creation) ---
    public String getConfirmationNumber() { return confirmationNumber; }
    // No setter — confirmation number should not change after creation

    // --- Stay Status ---
    public String getBookingStatus() { return bookingStatus; }
    public void setBookingStatus(String bookingStatus) { this.bookingStatus = bookingStatus; }

    /**
     * Convenience method: checks if bookingStatus equals "CheckedIn".
     * Preserves backward compatibility with existing controller logic.
     */
    public boolean isCheckedIn() {
        return "CheckedIn".equalsIgnoreCase(bookingStatus);
    }

    public boolean isReserved() {
        return "Reserved".equalsIgnoreCase(bookingStatus);
    }

    public boolean isRegistered() {
        return "Registered".equalsIgnoreCase(bookingStatus);
    }

    public boolean isCheckedOut() {
        return "CheckedOut".equalsIgnoreCase(bookingStatus);
    }

    public boolean isCancelled() {
        return "Cancelled".equalsIgnoreCase(bookingStatus);
    }

    /**
     * Convenience method: sets bookingStatus to "CheckedIn" or "Reserved".
     * Preserves backward compatibility with existing controller logic.
     */
    public void setCheckedIn(boolean checkedIn) {
        this.bookingStatus = checkedIn ? "CheckedIn" : "Reserved";
    }

    public String getCheckInDate() { return checkInDate; }
    public void setCheckInDate(String checkInDate) { this.checkInDate = checkInDate; }

    public String getCheckOutDate() { return checkOutDate; }
    public void setCheckOutDate(String checkOutDate) { this.checkOutDate = checkOutDate; }

    public int getNumberOfNights() { return numberOfNights; }
    public void setNumberOfNights(int numberOfNights) { this.numberOfNights = numberOfNights; }

    // --- Room ---
    public String getAssignedRoomNumber() { return assignedRoomNumber; }
    public void setAssignedRoomNumber(String assignedRoomNumber) { this.assignedRoomNumber = assignedRoomNumber; }

    public String getRoomType() { return roomType; }
    public void setRoomType(String roomType) { this.roomType = roomType; }

    public double getRoomRate() { return roomRate; }
    public void setRoomRate(double roomRate) { this.roomRate = roomRate; }

    // Backward compatibility aliases for effectiveRoomRate
    public double getEffectiveRoomRate() { return roomRate; }
    public void setEffectiveRoomRate(double effectiveRoomRate) { this.roomRate = effectiveRoomRate; }

    // --- Loyalty ---
    public String getLoyaltyTier() { return loyaltyTier; }
    public void setLoyaltyTier(String loyaltyTier) { this.loyaltyTier = loyaltyTier; }

    public int getLoyaltyPoints() { return loyaltyPoints; }
    public void setLoyaltyPoints(int loyaltyPoints) { this.loyaltyPoints = loyaltyPoints; }

    // --- Preferences ---
    public String getSpecialRequest() { return specialRequest; }
    public void setSpecialRequest(String specialRequest) { this.specialRequest = specialRequest; }

    // ========== Computed Methods ==========

    /**
     * Calculates total bill: nightly rate × number of nights.
     * Discounts should be applied by the Controller, not here.
     */
    public double getTotalBill() {
        return roomRate * numberOfNights;
    }

    // ========== BST Ordering (by Confirmation Number) ==========

    @Override
    public int compareTo(Guest other) {
        if (other == null || other.confirmationNumber == null) return 1;
        if (this.confirmationNumber == null) return -1;
        return this.confirmationNumber.compareToIgnoreCase(other.confirmationNumber);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Guest other = (Guest) obj;
        return confirmationNumber != null && confirmationNumber.equalsIgnoreCase(other.confirmationNumber);
    }

    @Override
    public int hashCode() {
        return (confirmationNumber != null) ? confirmationNumber.toLowerCase().hashCode() : 0;
    }

    // ========== Display ==========

    @Override
    public String toString() {
        return "Guest{" +
                "Name='" + guestName + '\'' +
                ", IC='" + icNo + '\'' +
                ", Phone='" + phoneNumber + '\'' +
                ", ConfirmNo='" + confirmationNumber + '\'' +
                ", Status='" + bookingStatus + '\'' +
                ", Room='" + (assignedRoomNumber != null ? assignedRoomNumber : "N/A") + '\'' +
                ", RoomType='" + (roomType != null ? roomType : "N/A") + '\'' +
                ", Rate=" + roomRate +
                ", Nights=" + numberOfNights +
                ", Tier='" + loyaltyTier + '\'' +
                ", Points=" + loyaltyPoints +
                '}';
    }
}
