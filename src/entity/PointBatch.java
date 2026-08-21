package entity;

import java.time.LocalDateTime;

/**
 * Author: Tan Hock Siang
 * A traceable batch of loyalty points with its own expiry and remaining balance.
 */
public class PointBatch {
    private static int idCounter = 1;

    private final String batchId;
    private final int points;
    private int remainingPoints;
    private final LocalDateTime earnedTime;
    private final LocalDateTime expiryTime;
    private final String description;
    private final String confirmationNumber;
    private String memberKey;
    private String status; // ACTIVE, CONSUMED, EXPIRED

    public PointBatch(int points, String description, String confirmationNumber,
            String memberKey, int validityMinutes) {
        this.batchId = String.format("PB%04d", idCounter++);
        this.points = points;
        this.remainingPoints = Math.max(0, points);
        this.earnedTime = LocalDateTime.now();
        this.expiryTime = validityMinutes < 0 ? null : earnedTime.plusMinutes(validityMinutes);
        this.description = description;
        this.confirmationNumber = confirmationNumber;
        this.memberKey = memberKey;
        this.status = points > 0 ? "ACTIVE" : "CONSUMED";
    }

    public boolean isExpired(LocalDateTime now) {
        return expiryTime != null && "ACTIVE".equalsIgnoreCase(status) && now.isAfter(expiryTime);
    }

    public int getPoints() {
        return points;
    }

    public String getBatchId() {
        return batchId;
    }

    public int getRemainingPoints() {
        return remainingPoints;
    }

    public LocalDateTime getEarnedTime() {
        return earnedTime;
    }

    public LocalDateTime getExpiryTime() {
        return expiryTime;
    }

    public String getDescription() {
        return description;
    }

    public String getConfirmationNumber() {
        return confirmationNumber;
    }

    public String getMemberKey() {
        return memberKey;
    }

    public void setMemberKey(String memberKey) {
        this.memberKey = memberKey;
    }

    public String getStatus() {
        return status;
    }

    /** Consumes up to the requested points and returns the amount consumed. */
    public int consume(int requestedPoints) {
        if (!"ACTIVE".equalsIgnoreCase(status) || requestedPoints <= 0)
            return 0;
        int consumed = Math.min(remainingPoints, requestedPoints);
        remainingPoints -= consumed;
        if (remainingPoints == 0)
            status = "CONSUMED";
        return consumed;
    }

    /** Expires the unused balance once and returns the points to deduct. */
    public int expire(LocalDateTime now) {
        if (!isExpired(now))
            return 0;
        int expiredPoints = remainingPoints;
        remainingPoints = 0;
        status = "EXPIRED";
        return expiredPoints;
    }
}
