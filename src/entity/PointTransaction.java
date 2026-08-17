package entity;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Author: Hock Siang
 * A typed loyalty-points ledger entry. Positive entries can also act as
 * spendable earning batches through their remainingPoints value.
 */
public class PointTransaction implements Comparable<PointTransaction> {
    public static final String OPENING_BALANCE = "OPENING_BALANCE";
    public static final String CHECKOUT_EARN = "CHECKOUT_EARN";
    public static final String DAILY_CHECK_IN = "DAILY_CHECK_IN";
    public static final String REDEMPTION = "REDEMPTION";
    public static final String EXPIRY = "EXPIRY";
    public static final String ADJUSTMENT = "ADJUSTMENT";

    public static final String ACTIVE = "ACTIVE";
    public static final String PARTIALLY_USED = "PARTIALLY_USED";
    public static final String CONSUMED = "CONSUMED";
    public static final String EXPIRED = "EXPIRED";
    public static final String COMPLETED = "COMPLETED";

    private static int nextId = 1;
    private static final DateTimeFormatter DISPLAY_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String transactionId;
    private String memberKey;
    private final String sourceConfirmationNumber;
    private final String guestNameSnapshot;
    private final String transactionType;
    private final String description;
    private final int pointsChange;
    private final int originalPoints;
    private int remainingPoints;
    private final int experienceEarned;
    private final LocalDateTime occurredAt;
    private final LocalDateTime expiresAt;
    private String status;
    private final String externalReference;
    private final String relatedTransactionId;

    public static PointTransaction earning(String memberKey, String confirmationNumber,
            String guestName, String type, String description, int points,
            int experienceEarned, LocalDateTime occurredAt, LocalDateTime expiresAt,
            String externalReference) {
        if (points <= 0) throw new IllegalArgumentException("Earning points must be positive.");
        return new PointTransaction(memberKey, confirmationNumber, guestName, type,
                description, points, points, points, experienceEarned, occurredAt,
                expiresAt, ACTIVE, externalReference, null);
    }

    public static PointTransaction deduction(String memberKey, String confirmationNumber,
            String guestName, String type, String description, int points,
            LocalDateTime occurredAt, String externalReference,
            String relatedTransactionId) {
        if (points >= 0) throw new IllegalArgumentException("Deduction points must be negative.");
        return new PointTransaction(memberKey, confirmationNumber, guestName, type,
                description, points, 0, 0, 0, occurredAt, null, COMPLETED,
                externalReference, relatedTransactionId);
    }

    private PointTransaction(String memberKey, String sourceConfirmationNumber,
            String guestNameSnapshot, String transactionType, String description,
            int pointsChange, int originalPoints, int remainingPoints,
            int experienceEarned, LocalDateTime occurredAt, LocalDateTime expiresAt,
            String status, String externalReference, String relatedTransactionId) {
        this.transactionId = String.format("PT%06d", nextId++);
        this.memberKey = memberKey;
        this.sourceConfirmationNumber = sourceConfirmationNumber;
        this.guestNameSnapshot = guestNameSnapshot;
        this.transactionType = transactionType;
        this.description = description;
        this.pointsChange = pointsChange;
        this.originalPoints = originalPoints;
        this.remainingPoints = remainingPoints;
        this.experienceEarned = experienceEarned;
        this.occurredAt = occurredAt;
        this.expiresAt = expiresAt;
        this.status = status;
        this.externalReference = externalReference;
        this.relatedTransactionId = relatedTransactionId;
    }

    public String getTransactionId() { return transactionId; }
    public String getMemberKey() { return memberKey; }
    public String getSourceConfirmationNumber() { return sourceConfirmationNumber; }
    public String getGuestNameSnapshot() { return guestNameSnapshot; }
    public String getTransactionType() { return transactionType; }
    public String getDescription() { return description; }
    public int getPointsChange() { return pointsChange; }
    public int getOriginalPoints() { return originalPoints; }
    public int getRemainingPoints() { return remainingPoints; }
    public int getExperienceEarned() { return experienceEarned; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public String getStatus() { return status; }
    public String getExternalReference() { return externalReference; }
    public String getRelatedTransactionId() { return relatedTransactionId; }

    public boolean reassignMemberKey(String expectedOldKey, String newMemberKey) {
        if (expectedOldKey == null || newMemberKey == null
                || !expectedOldKey.equals(memberKey) || newMemberKey.trim().isEmpty()) return false;
        memberKey = newMemberKey;
        return true;
    }

    public boolean isEarningBatch() { return originalPoints > 0; }

    public boolean isSpendableAt(LocalDateTime now) {
        return isEarningBatch() && remainingPoints > 0
                && !EXPIRED.equals(status)
                && (expiresAt == null || now.isBefore(expiresAt));
    }

    public int consume(int requestedPoints) {
        if (requestedPoints <= 0 || remainingPoints <= 0 || EXPIRED.equals(status)) return 0;
        int consumed = Math.min(requestedPoints, remainingPoints);
        remainingPoints -= consumed;
        status = remainingPoints == 0 ? CONSUMED : PARTIALLY_USED;
        return consumed;
    }

    public int expire() {
        if (!isEarningBatch() || remainingPoints <= 0 || EXPIRED.equals(status)) return 0;
        int expiredPoints = remainingPoints;
        remainingPoints = 0;
        status = EXPIRED;
        return expiredPoints;
    }

    public String getOccurredAtFormatted() {
        return occurredAt == null ? "-" : occurredAt.format(DISPLAY_FORMAT);
    }

    public String getExpiresAtFormatted() {
        return expiresAt == null ? "-" : expiresAt.format(DISPLAY_FORMAT);
    }

    @Override
    public int compareTo(PointTransaction other) {
        return transactionId.compareTo(other.transactionId);
    }

    @Override
    public String toString() {
        return String.format("[%s] %s %+d points | %s | %s", transactionId,
                transactionType, pointsChange, description, status);
    }
}
