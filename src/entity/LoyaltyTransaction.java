package entity;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Author: Hock Siang
 * Represents a loyalty reward redemption transaction.
 */
public class LoyaltyTransaction implements Comparable<LoyaltyTransaction> {
    private static int idCounter = 1000;
    private String transactionId;
    private String confirmationNumber;
    private String guestName;
    private String itemName;
    private int pointsSpent;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status; // ACTIVE, USED, EXPIRED
    
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public LoyaltyTransaction(String confirmationNumber, String guestName, String itemName, int pointsSpent, int validityMinutes) {
        this.transactionId = "TXN" + (idCounter++);
        this.confirmationNumber = confirmationNumber;
        this.guestName = guestName;
        this.itemName = itemName;
        this.pointsSpent = pointsSpent;
        this.startTime = LocalDateTime.now();
        this.endTime = this.startTime.plusMinutes(validityMinutes); // Inspired by point expiration/validity rules
        this.status = "ACTIVE";
    }

    public String getTransactionId() { return transactionId; }
    public String getConfirmationNumber() { return confirmationNumber; }
    public String getGuestName() { return guestName; }
    public String getItemName() { return itemName; }
    public int getPointsSpent() { return pointsSpent; }
    public String getStartTimeFormatted() { return startTime.format(FMT); }
    public String getEndTimeFormatted() { return endTime.format(FMT); }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public int compareTo(LoyaltyTransaction other) {
        return this.transactionId.compareTo(other.transactionId);
    }

    @Override
    public String toString() {
        return String.format("[%s] %s | '%s' (%d pts) | Valid: %s to %s | Status: %s", 
                transactionId, guestName, itemName, pointsSpent, getStartTimeFormatted(), getEndTimeFormatted(), status);
    }
}
