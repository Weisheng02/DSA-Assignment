package entity;

/**
 * Author: Weisheng
 * Entity class representing a Resort Guest
 */
public class Guest implements Comparable<Guest> {
    private String guestName;
    private String confirmationNumber; // 8-digit unique ID
    private String loyaltyTier;        // Platinum, Gold, Silver, Standard
    private int loyaltyPoints;
    private boolean checkedIn;
    private String assignedRoomNumber;
    private double effectiveRoomRate; // Actual rate charged (preserves upgrade benefits)

    public Guest(){
    }
    
    public Guest(String guestName, String confirmationNumber, String loyaltyTier, int loyaltyPoints) {
        this.guestName = guestName;
        this.confirmationNumber = confirmationNumber;
        this.loyaltyTier = loyaltyTier;
        this.loyaltyPoints = loyaltyPoints;
        this.checkedIn = false;
        this.assignedRoomNumber = null;
        this.effectiveRoomRate = 0.0;
    }

    public boolean isCheckedIn() { return checkedIn; }
    public void setCheckedIn(boolean checkedIn) { this.checkedIn = checkedIn; }

    public String getAssignedRoomNumber() { return assignedRoomNumber; }
    public void setAssignedRoomNumber(String assignedRoomNumber) { this.assignedRoomNumber = assignedRoomNumber; }

    public double getEffectiveRoomRate() { return effectiveRoomRate; }
    public void setEffectiveRoomRate(double effectiveRoomRate) { this.effectiveRoomRate = effectiveRoomRate; }

    public String getGuestName() { return guestName; }
    public void setGuestName(String guestName) { this.guestName = guestName; }

    public String getConfirmationNumber() { return confirmationNumber; }
    public void setConfirmationNumber(String confirmationNumber) { this.confirmationNumber = confirmationNumber; }

    public String getLoyaltyTier() { return loyaltyTier; }
    public void setLoyaltyTier(String loyaltyTier) { this.loyaltyTier = loyaltyTier; }

    public int getLoyaltyPoints() { return loyaltyPoints; }
    public void setLoyaltyPoints(int loyaltyPoints) { this.loyaltyPoints = loyaltyPoints; }

    @Override
    public int compareTo(Guest other) {
        if (other == null || other.confirmationNumber == null) return 1;
        if (this.confirmationNumber == null) return -1;
        return this.confirmationNumber.compareToIgnoreCase(other.confirmationNumber);
    }

    @Override
    public String toString() {
        return "Guest{" +
                "Name='" + guestName + '\'' +
                ", ConfirmNo='" + confirmationNumber + '\'' +
                ", Tier='" + loyaltyTier + '\'' +
                ", Points=" + loyaltyPoints +
                '}';
    }
}

