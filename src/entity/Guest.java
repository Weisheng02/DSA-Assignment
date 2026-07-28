package entity;

/**
 * Author: Weisheng
 * Entity class representing a Resort Guest
 */
public class Guest {
    private String guestName;
    private String confirmationNumber; // 8-digit unique ID
    private String loyaltyTier;        // Platinum, Gold, Silver, Standard
    private int loyaltyPoints;

    public Guest(String guestName, String confirmationNumber, String loyaltyTier, int loyaltyPoints) {
        this.guestName = guestName;
        this.confirmationNumber = confirmationNumber;
        this.loyaltyTier = loyaltyTier;
        this.loyaltyPoints = loyaltyPoints;
    }

    public String getGuestName() { return guestName; }
    public void setGuestName(String guestName) { this.guestName = guestName; }

    public String getConfirmationNumber() { return confirmationNumber; }
    public void setConfirmationNumber(String confirmationNumber) { this.confirmationNumber = confirmationNumber; }

    public String getLoyaltyTier() { return loyaltyTier; }
    public void setLoyaltyTier(String loyaltyTier) { this.loyaltyTier = loyaltyTier; }

    public int getLoyaltyPoints() { return loyaltyPoints; }
    public void setLoyaltyPoints(int loyaltyPoints) { this.loyaltyPoints = loyaltyPoints; }

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