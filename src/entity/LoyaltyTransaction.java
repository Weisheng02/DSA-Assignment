package entity;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Author: Hock Siang
 * A redeemed reward kept in a member's reward inventory.
 */
public class LoyaltyTransaction implements Comparable<LoyaltyTransaction> {
    public static final String ACTIVE = "ACTIVE";
    public static final String USED = "USED";
    public static final String EXPIRED = "EXPIRED";

    private static int nextId = 1000;
    private static final DateTimeFormatter DISPLAY_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String transactionId;
    private String memberKey;
    private final String sourceConfirmationNumber;
    private final String guestNameSnapshot;
    private final String rewardItemId;
    private final String itemNameSnapshot;
    private final int basePointsCost;
    private final int pointsSpent;
    private final boolean promotionApplied;
    private final LocalDateTime redeemedAt;
    private final LocalDateTime expiresAt;
    private LocalDateTime usedAt;
    private String status;

    public LoyaltyTransaction(String memberKey, String sourceConfirmationNumber,
            String guestNameSnapshot, String rewardItemId, String itemNameSnapshot,
            int basePointsCost, int pointsSpent, boolean promotionApplied,
            LocalDateTime redeemedAt, LocalDateTime expiresAt) {
        this.transactionId = "TXN" + nextId++;
        this.memberKey = memberKey;
        this.sourceConfirmationNumber = sourceConfirmationNumber;
        this.guestNameSnapshot = guestNameSnapshot;
        this.rewardItemId = rewardItemId;
        this.itemNameSnapshot = itemNameSnapshot;
        this.basePointsCost = basePointsCost;
        this.pointsSpent = pointsSpent;
        this.promotionApplied = promotionApplied;
        this.redeemedAt = redeemedAt;
        this.expiresAt = expiresAt;
        this.status = ACTIVE;
    }

    public LoyaltyTransaction(String confirmationNumber, String guestName,
            String itemName, int pointsSpent, int validityMinutes) {
        this(confirmationNumber, confirmationNumber, guestName, "", itemName,
                pointsSpent, pointsSpent, false, LocalDateTime.now(),
                LocalDateTime.now().plusMinutes(Math.max(1, validityMinutes)));
    }

    public String getTransactionId() { return transactionId; }
    public String getMemberKey() { return memberKey; }
    public String getSourceConfirmationNumber() { return sourceConfirmationNumber; }
    public String getConfirmationNumber() { return sourceConfirmationNumber; }
    public String getGuestNameSnapshot() { return guestNameSnapshot; }
    public String getGuestName() { return guestNameSnapshot; }
    public String getRewardItemId() { return rewardItemId; }
    public String getItemNameSnapshot() { return itemNameSnapshot; }
    public String getItemName() { return itemNameSnapshot; }
    public int getBasePointsCost() { return basePointsCost; }
    public int getPointsSpent() { return pointsSpent; }
    public boolean isPromotionApplied() { return promotionApplied; }
    public LocalDateTime getRedeemedAt() { return redeemedAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public LocalDateTime getUsedAt() { return usedAt; }
    public String getStatus() { return status; }

    public boolean reassignMemberKey(String expectedOldKey, String newMemberKey) {
        if (expectedOldKey == null || newMemberKey == null
                || !expectedOldKey.equals(memberKey) || newMemberKey.trim().isEmpty()) return false;
        memberKey = newMemberKey;
        return true;
    }

    public boolean refreshExpiry(LocalDateTime now) {
        if (ACTIVE.equals(status) && expiresAt != null && !now.isBefore(expiresAt)) {
            status = EXPIRED;
            return true;
        }
        return false;
    }

    public boolean markUsed(LocalDateTime now) {
        refreshExpiry(now);
        if (!ACTIVE.equals(status)) return false;
        status = USED;
        usedAt = now;
        return true;
    }

    public void setStatus(String status) {
        if (ACTIVE.equalsIgnoreCase(status)) this.status = ACTIVE;
        else if (USED.equalsIgnoreCase(status)) this.status = USED;
        else if (EXPIRED.equalsIgnoreCase(status)) this.status = EXPIRED;
    }

    public String getStartTimeFormatted() { return format(redeemedAt); }
    public String getEndTimeFormatted() { return format(expiresAt); }
    private String format(LocalDateTime value) {
        return value == null ? "-" : value.format(DISPLAY_FORMAT);
    }

    @Override
    public int compareTo(LoyaltyTransaction other) {
        return transactionId.compareTo(other.transactionId);
    }

    @Override
    public String toString() {
        return String.format("[%s] %s | '%s' (%d pts) | Valid: %s to %s | Status: %s",
                transactionId, guestNameSnapshot, itemNameSnapshot, pointsSpent,
                getStartTimeFormatted(), getEndTimeFormatted(), status);
    }
}
