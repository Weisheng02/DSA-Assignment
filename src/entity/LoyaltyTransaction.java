package entity;

import java.time.LocalDateTime;

/**
 * Author: Tan Hock Siang
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

    public LoyaltyTransaction(String confirmationNumber, String guestName, String itemName, int pointsSpent,
            int validityMinutes) {
        this.transactionId = "TXN" + (idCounter++);
        this.confirmationNumber = confirmationNumber;
        this.guestName = guestName;
        this.itemName = itemName;
        this.pointsSpent = pointsSpent;
        this.startTime = LocalDateTime.now();
        this.endTime = this.startTime.plusMinutes(validityMinutes); // Inspired by point expiration/validity rules
        this.status = "ACTIVE";
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getConfirmationNumber() {
        return confirmationNumber;
    }

    public String getGuestName() {
        return guestName;
    }

    public String getItemName() {
        return itemName;
    }

    public int getPointsSpent() {
        return pointsSpent;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public int compareTo(LoyaltyTransaction other) {
        if (other == null || other.transactionId == null)
            return 1;
        if (this.transactionId == null)
            return -1;
        return this.transactionId.compareTo(other.transactionId);
    }

}
