package control;

import adt.BSTInterface;
import adt.BinarySearchTree;
import adt.ListInterface;
import adt.MyArrayList;
import entity.Guest;
import entity.LoyaltyTransaction;
import entity.PointTransaction;
import entity.RewardItem;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Author: Hock Siang
 * Controls member identity, points, rewards, expiry, promotions and reports.
 */
public final class LoyaltyController {
    private static final int POINT_VALIDITY_DAYS = 365;
    private static final int DAILY_REWARD = 10;

    private final BSTInterface<Guest> masterGuestRegistry;
    private final BSTInterface<RewardItem> rewardCatalog = new BinarySearchTree<>();
    private final ListInterface<PointTransaction> pointHistory = new MyArrayList<>();
    private final ListInterface<LoyaltyTransaction> redemptionHistory = new MyArrayList<>();
    private final ListInterface<String> initializedMemberKeys = new MyArrayList<>();

    public LoyaltyController() { this(null); }

    public LoyaltyController(BSTInterface<Guest> masterGuestRegistry) {
        this.masterGuestRegistry = masterGuestRegistry;
        initializeDefaultRewards();
        initializeOpeningBalances();
    }

    public BSTInterface<Guest> getMasterGuestRegistry() { return masterGuestRegistry; }

    public static class AwardResult {
        private final boolean success;
        private final String code, message, oldTier, newTier;
        private final int pointsAwarded, newBalance;
        private AwardResult(boolean success, String code, String message, int pointsAwarded,
                int newBalance, String oldTier, String newTier) {
            this.success = success; this.code = code; this.message = message;
            this.pointsAwarded = pointsAwarded; this.newBalance = newBalance;
            this.oldTier = oldTier; this.newTier = newTier;
        }
        public boolean isSuccess() { return success; }
        public String getCode() { return code; }
        public String getMessage() { return message; }
        public int getPointsAwarded() { return pointsAwarded; }
        public int getNewBalance() { return newBalance; }
        public String getOldTier() { return oldTier; }
        public String getNewTier() { return newTier; }
        public boolean isTierUpgraded() { return tierRank(newTier) > tierRank(oldTier); }
    }

    public static class RedemptionResult {
        private final boolean success, promotionApplied;
        private final String code, message, transactionId;
        private final int pointsSpent, newBalance;
        private RedemptionResult(boolean success, String code, String message,
                String transactionId, int pointsSpent, int newBalance, boolean promotionApplied) {
            this.success = success; this.code = code; this.message = message;
            this.transactionId = transactionId; this.pointsSpent = pointsSpent;
            this.newBalance = newBalance; this.promotionApplied = promotionApplied;
        }
        public boolean isSuccess() { return success; }
        public String getCode() { return code; }
        public String getMessage() { return message; }
        public String getTransactionId() { return transactionId; }
        public int getPointsSpent() { return pointsSpent; }
        public int getNewBalance() { return newBalance; }
        public boolean isPromotionApplied() { return promotionApplied; }
    }

    public static class UseRewardResult {
        private final boolean success;
        private final String code, message;
        private UseRewardResult(boolean success, String code, String message) {
            this.success = success; this.code = code; this.message = message;
        }
        public boolean isSuccess() { return success; }
        public String getCode() { return code; }
        public String getMessage() { return message; }
    }

    public static class RewardPerformanceRow {
        private final RewardItem rewardItem;
        private final int totalRedeemed, activeCount, usedCount, expiredCount;
        private RewardPerformanceRow(RewardItem item, int total, int active, int used, int expired) {
            rewardItem = item; totalRedeemed = total; activeCount = active;
            usedCount = used; expiredCount = expired;
        }
        public RewardItem getRewardItem() { return rewardItem; }
        public int getTotalRedeemed() { return totalRedeemed; }
        public int getActiveCount() { return activeCount; }
        public int getUsedCount() { return usedCount; }
        public int getExpiredCount() { return expiredCount; }
        public int getCurrentStock() { return rewardItem.getStockQuantity(); }
        public String getStockStatus() { return rewardItem.getStockStatus(); }
    }

    // Member identity: confirmation locates a stay, IC/passport owns loyalty data.
    public Guest findGuestByConfirmationNumber(String confirmationNumber) {
        if (masterGuestRegistry == null || blank(confirmationNumber)) return null;
        return masterGuestRegistry.search(new Guest("", confirmationNumber.trim(), "", 0));
    }

    public boolean memberExists(String confirmationNumber) {
        return findGuestByConfirmationNumber(confirmationNumber) != null;
    }

    public String resolveMemberKey(Guest guest) {
        if (guest == null) return "";
        return resolveMemberKey(guest.getIcNo(), guest.getConfirmationNumber());
    }

    private String resolveMemberKey(String icNo, String confirmationNumber) {
        String identity = normalize(icNo);
        if (!identity.isEmpty() && !"na".equals(identity)) return "ID:" + identity;
        return "CONF:" + normalize(confirmationNumber);
    }

    private String normalize(String value) {
        return value == null ? "" : value.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
    }

    /**
     * Atomically migrates a member's stable loyalty ledger when Front Desk corrects
     * the IC/passport used as the member identity key.
     */
    public boolean migrateMemberIdentity(Guest sourceGuest, String newIcNo) {
        if (sourceGuest == null || blank(newIcNo)) return false;
        ensureMemberInitialized(sourceGuest);
        String oldKey = resolveMemberKey(sourceGuest);
        String newKey = resolveMemberKey(newIcNo, sourceGuest.getConfirmationNumber());
        if (oldKey.equals(newKey)) return true;

        ListInterface<Guest> all = masterGuestRegistry == null
                ? new MyArrayList<>() : masterGuestRegistry.inOrderTraversal();
        for (int i = 0; i < all.getNumberOfEntries(); i++) {
            String candidateKey = resolveMemberKey(all.get(i));
            if (newKey.equals(candidateKey) && !oldKey.equals(candidateKey)) return false;
        }

        int balance = availableBalance(oldKey);
        int experience = memberExperience(oldKey);
        for (int i = 0; i < pointHistory.getNumberOfEntries(); i++)
            pointHistory.get(i).reassignMemberKey(oldKey, newKey);
        for (int i = 0; i < redemptionHistory.getNumberOfEntries(); i++)
            redemptionHistory.get(i).reassignMemberKey(oldKey, newKey);
        ListInterface<String> migratedKeys = new MyArrayList<>();
        for (int i = 0; i < initializedMemberKeys.getNumberOfEntries(); i++) {
            String key = initializedMemberKeys.get(i);
            if (!oldKey.equals(key) && !contains(migratedKeys, key)) migratedKeys.add(key);
        }
        if (!contains(migratedKeys, newKey)) migratedKeys.add(newKey);
        initializedMemberKeys.clear();
        for (int i = 0; i < migratedKeys.getNumberOfEntries(); i++)
            initializedMemberKeys.add(migratedKeys.get(i));

        for (int i = 0; i < all.getNumberOfEntries(); i++) {
            Guest guest = all.get(i);
            if (oldKey.equals(resolveMemberKey(guest))) guest.setIcNo(newIcNo.trim());
        }
        syncMember(newKey, balance, experience);
        return true;
    }

    private void initializeOpeningBalances() {
        if (masterGuestRegistry == null) return;
        ListInterface<Guest> guests = masterGuestRegistry.inOrderTraversal();
        for (int i = 0; i < guests.getNumberOfEntries(); i++) ensureMemberInitialized(guests.get(i));
    }

    private void ensureMemberInitialized(Guest guest) {
        if (guest == null) return;
        String key = resolveMemberKey(guest);
        if (contains(initializedMemberKeys, key)) {
            syncMember(key, availableBalance(key), memberExperience(key));
            return;
        }
        int points = Math.max(0, guest.getLoyaltyPoints());
        int exp = Math.max(guest.getLoyaltyExperience(), tierMinimum(guest.getLoyaltyTier()));
        Guest snapshot = guest;
        if (masterGuestRegistry != null) {
            ListInterface<Guest> all = masterGuestRegistry.inOrderTraversal();
            for (int i = 0; i < all.getNumberOfEntries(); i++) {
                Guest candidate = all.get(i);
                if (!key.equals(resolveMemberKey(candidate))) continue;
                if (candidate.getLoyaltyPoints() > points) {
                    points = candidate.getLoyaltyPoints(); snapshot = candidate;
                }
                exp = Math.max(exp, Math.max(candidate.getLoyaltyExperience(),
                        tierMinimum(candidate.getLoyaltyTier())));
            }
        }
        initializedMemberKeys.add(key);
        if (points > 0) pointHistory.add(PointTransaction.earning(key,
                snapshot.getConfirmationNumber(), snapshot.getGuestName(),
                PointTransaction.OPENING_BALANCE, "Opening loyalty balance", points, 0,
                LocalDateTime.now(), null, "OPENING:" + key));
        syncMember(key, points, exp);
    }

    private void syncMember(String key, int points, int exp) {
        if (masterGuestRegistry == null) return;
        ListInterface<Guest> all = masterGuestRegistry.inOrderTraversal();
        for (int i = 0; i < all.getNumberOfEntries(); i++) {
            Guest guest = all.get(i);
            if (key.equals(resolveMemberKey(guest))) {
                guest.setLoyaltyPoints(Math.max(0, points));
                guest.setLoyaltyExperience(Math.max(0, exp));
                guest.setLoyaltyTier(calculateTier(exp));
            }
        }
    }

    private int memberExperience(String key) {
        int result = 0;
        if (masterGuestRegistry != null) {
            ListInterface<Guest> all = masterGuestRegistry.inOrderTraversal();
            for (int i = 0; i < all.getNumberOfEntries(); i++)
                if (key.equals(resolveMemberKey(all.get(i))))
                    result = Math.max(result, all.get(i).getLoyaltyExperience());
        }
        return result;
    }

    public String calculateTier(int experience) {
        if (experience >= 1000) return "Platinum";
        if (experience >= 500) return "Gold";
        if (experience >= 200) return "Silver";
        return "Standard";
    }

    private int tierMinimum(String tier) {
        if ("Platinum".equalsIgnoreCase(tier)) return 1000;
        if ("Gold".equalsIgnoreCase(tier)) return 500;
        if ("Silver".equalsIgnoreCase(tier)) return 200;
        return 0;
    }

    private static int tierRank(String tier) {
        if ("Platinum".equalsIgnoreCase(tier)) return 3;
        if ("Gold".equalsIgnoreCase(tier)) return 2;
        if ("Silver".equalsIgnoreCase(tier)) return 1;
        return 0;
    }

    public String getNextTierInfo(Guest guest) {
        if (guest == null) return "Guest not found.";
        ensureMemberInitialized(guest);
        int exp = guest.getLoyaltyExperience();
        String tier = calculateTier(exp);
        if ("Standard".equals(tier)) return exp + " / 200 EXP (Silver) | Need: " + (200-exp) + " EXP";
        if ("Silver".equals(tier)) return exp + " / 500 EXP (Gold) | Need: " + (500-exp) + " EXP";
        if ("Gold".equals(tier)) return exp + " / 1,000 EXP (Platinum) | Need: " + (1000-exp) + " EXP";
        return exp + " EXP (Max Tier Reached)";
    }

    // Earning and daily claim.
    public AwardResult validateCheckoutAward(String confirmation, String bookingId, int points) {
        Guest guest = findGuestByConfirmationNumber(confirmation);
        if (guest == null) return awardError("GUEST_NOT_FOUND", "Guest not found.");
        if (blank(bookingId)) return awardError("INVALID_REFERENCE", "Booking ID is required.");
        if (points <= 0) return awardError("INVALID_POINTS", "Earned points must be positive.");
        ensureMemberInitialized(guest);
        String key = resolveMemberKey(guest);
        expireBatches(key, LocalDateTime.now(), guest);
        String reference = "CHECKOUT:" + bookingId.trim().toUpperCase();
        if (hasReference(key, PointTransaction.CHECKOUT_EARN, reference))
            return awardError("ALREADY_AWARDED", "Checkout points were already awarded.");
        String tier = calculateTier(memberExperience(key));
        return new AwardResult(true, "READY", "Checkout reward is ready.", 0,
                availableBalance(key), tier, tier);
    }

    public AwardResult awardCheckoutPoints(String confirmation, String bookingId, int points) {
        AwardResult validation = validateCheckoutAward(confirmation, bookingId, points);
        if (!validation.isSuccess()) return validation;
        Guest guest = findGuestByConfirmationNumber(confirmation);
        String key = resolveMemberKey(guest);
        LocalDateTime now = LocalDateTime.now();
        String reference = "CHECKOUT:" + bookingId.trim().toUpperCase();
        int oldExp = memberExperience(key);
        String oldTier = calculateTier(oldExp);
        addEarning(guest, PointTransaction.CHECKOUT_EARN,
                "Checkout reward for " + bookingId.trim(), points, points, reference, now);
        int balance = availableBalance(key), newExp = oldExp + points;
        String newTier = calculateTier(newExp);
        syncMember(key, balance, newExp);
        String message = "Checkout completed: +" + points + " points and EXP.";
        if (tierRank(newTier) > tierRank(oldTier)) message += " Tier upgraded to " + newTier + ".";
        return new AwardResult(true, "SUCCESS", message, points, balance, oldTier, newTier);
    }

    public AwardResult awardCheckoutPoints(Guest guest, String bookingId, int points) {
        return guest == null ? awardError("GUEST_NOT_FOUND", "Guest not found.")
                : awardCheckoutPoints(guest.getConfirmationNumber(), bookingId, points);
    }

    public AwardResult claimDailyReward(String confirmation) {
        Guest guest = findGuestByConfirmationNumber(confirmation);
        if (guest == null) return awardError("GUEST_NOT_FOUND", "Guest not found.");
        ensureMemberInitialized(guest);
        String key = resolveMemberKey(guest);
        LocalDateTime now = LocalDateTime.now();
        expireBatches(key, now, guest);
        for (int i = 0; i < pointHistory.getNumberOfEntries(); i++) {
            PointTransaction t = pointHistory.get(i);
            if (key.equals(t.getMemberKey())
                    && PointTransaction.DAILY_CHECK_IN.equals(t.getTransactionType())
                    && t.getOccurredAt().toLocalDate().equals(now.toLocalDate()))
                return awardError("ALREADY_CLAIMED", "Daily reward has already been claimed today.");
        }
        int oldExp = memberExperience(key);
        String oldTier = calculateTier(oldExp);
        addEarning(guest, PointTransaction.DAILY_CHECK_IN, "Daily check-in", DAILY_REWARD,
                DAILY_REWARD, "DAILY:" + key + ":" + now.toLocalDate(), now);
        int balance = availableBalance(key), newExp = oldExp + DAILY_REWARD;
        syncMember(key, balance, newExp);
        return new AwardResult(true, "SUCCESS", "Daily check-in complete: +10 points and EXP.",
                DAILY_REWARD, balance, oldTier, calculateTier(newExp));
    }

    public String performDailyCheckIn(Guest guest) {
        if (guest == null) return "ERROR: Guest not found.";
        AwardResult r = claimDailyReward(guest.getConfirmationNumber());
        return (r.isSuccess() ? "SUCCESS: " : "WARNING: ") + r.getMessage();
    }

    private AwardResult awardError(String code, String message) {
        return new AwardResult(false, code, message, 0, 0, "Standard", "Standard");
    }

    private void addEarning(Guest guest, String type, String description, int points,
            int experience, String reference, LocalDateTime now) {
        pointHistory.add(PointTransaction.earning(resolveMemberKey(guest),
                guest.getConfirmationNumber(), guest.getGuestName(), type, description,
                points, experience, now, now.plusDays(POINT_VALIDITY_DAYS), reference));
    }

    public void recordPointTransaction(Guest guest, String description, int points) {
        if (guest == null || points == 0) return;
        ensureMemberInitialized(guest);
        String key = resolveMemberKey(guest);
        LocalDateTime now = LocalDateTime.now();
        expireBatches(key, now, guest);
        int exp = memberExperience(key);
        if (points > 0) {
            addEarning(guest, PointTransaction.ADJUSTMENT, description, points, points, null, now);
            exp += points;
        } else {
            if (availableBalance(key) < -points) return;
            consumeFifo(key, -points, now);
            pointHistory.add(PointTransaction.deduction(key, guest.getConfirmationNumber(),
                    guest.getGuestName(), PointTransaction.ADJUSTMENT, description,
                    points, now, null, null));
        }
        syncMember(key, availableBalance(key), exp);
    }

    public void refreshPoints(Guest guest) {
        if (guest != null) { ensureMemberInitialized(guest); expireBatches(resolveMemberKey(guest), LocalDateTime.now(), guest); }
    }

    public int expirePointsForMember(String confirmation) {
        Guest guest = findGuestByConfirmationNumber(confirmation);
        if (guest == null) return 0;
        ensureMemberInitialized(guest);
        return expireBatches(resolveMemberKey(guest), LocalDateTime.now(), guest);
    }

    public int expireAllPoints() {
        if (masterGuestRegistry == null) return 0;
        int total = 0;
        ListInterface<Guest> all = masterGuestRegistry.inOrderTraversal();
        for (int i = 0; i < all.getNumberOfEntries(); i++) {
            Guest guest = all.get(i); ensureMemberInitialized(guest);
            total += expireBatches(resolveMemberKey(guest), LocalDateTime.now(), guest);
        }
        return total;
    }

    private int expireBatches(String key, LocalDateTime now, Guest source) {
        int total = 0, originalSize = pointHistory.getNumberOfEntries();
        for (int i = 0; i < originalSize; i++) {
            PointTransaction batch = pointHistory.get(i);
            if (!key.equals(batch.getMemberKey()) || !batch.isEarningBatch()
                    || batch.getExpiresAt() == null || now.isBefore(batch.getExpiresAt())) continue;
            int expired = batch.expire();
            if (expired > 0) {
                total += expired;
                pointHistory.add(PointTransaction.deduction(key, source.getConfirmationNumber(),
                        source.getGuestName(), PointTransaction.EXPIRY,
                        "Expired points from " + batch.getTransactionId(), -expired, now,
                        "EXPIRY:" + batch.getTransactionId(), batch.getTransactionId()));
            }
        }
        if (total > 0) syncMember(key, availableBalance(key), memberExperience(key));
        return total;
    }

    private int availableBalance(String key) {
        int total = 0;
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < pointHistory.getNumberOfEntries(); i++) {
            PointTransaction batch = pointHistory.get(i);
            if (key.equals(batch.getMemberKey()) && batch.isSpendableAt(now))
                total += batch.getRemainingPoints();
        }
        return total;
    }

    private void consumeFifo(String key, int points, LocalDateTime now) {
        ListInterface<PointTransaction> batches = new MyArrayList<>();
        for (int i = 0; i < pointHistory.getNumberOfEntries(); i++) {
            PointTransaction batch = pointHistory.get(i);
            if (key.equals(batch.getMemberKey()) && batch.isSpendableAt(now)) batches.add(batch);
        }
        batches.sort((a, b) -> {
            if (a.getExpiresAt() == null && b.getExpiresAt() != null) return 1;
            if (a.getExpiresAt() != null && b.getExpiresAt() == null) return -1;
            if (a.getExpiresAt() != null) {
                int c = a.getExpiresAt().compareTo(b.getExpiresAt()); if (c != 0) return c;
            }
            int c = a.getOccurredAt().compareTo(b.getOccurredAt());
            return c != 0 ? c : a.getTransactionId().compareTo(b.getTransactionId());
        });
        int left = points;
        for (int i = 0; i < batches.getNumberOfEntries() && left > 0; i++)
            left -= batches.get(i).consume(left);
    }

    private boolean hasReference(String key, String type, String reference) {
        for (int i = 0; i < pointHistory.getNumberOfEntries(); i++) {
            PointTransaction t = pointHistory.get(i);
            if (key.equals(t.getMemberKey()) && type.equals(t.getTransactionType())
                    && reference.equalsIgnoreCase(text(t.getExternalReference()))) return true;
        }
        return false;
    }

    // Reward catalog and redemption.
    public final void initializeDefaultRewards() {
        if (!rewardCatalog.isEmpty()) return;
        rewardCatalog.add(new RewardItem("RW001", "Free Coffee Drink", 20, 10, 30));
        rewardCatalog.add(new RewardItem("RW002", "Complimentary Breakfast", 50, 5, 30));
        rewardCatalog.add(new RewardItem("RW003", "Spa Discount Voucher", 100, 3, 60));
        rewardCatalog.add(new RewardItem("RW004", "20% Dining Discount", 500, 1, 90));
    }

    public ListInterface<RewardItem> getRewardCatalog() { return rewardCatalog.inOrderTraversal(); }

    public RewardItem findRewardById(String itemId) {
        return blank(itemId) ? null : rewardCatalog.search(new RewardItem(itemId.trim(), "", 0, 0, 1));
    }

    public RedemptionResult redeemReward(String confirmation, String rewardId) {
        Guest guest = findGuestByConfirmationNumber(confirmation);
        if (guest == null) return redeemError("GUEST_NOT_FOUND", "Guest not found.");
        RewardItem item = findRewardById(rewardId);
        if (item == null || !item.isActive()) return redeemError("REWARD_NOT_FOUND", "Reward item is unavailable.");
        if (!item.hasStock()) return redeemError("OUT_OF_STOCK", "Reward item is out of stock.");
        ensureMemberInitialized(guest);
        String key = resolveMemberKey(guest);
        LocalDateTime now = LocalDateTime.now();
        expireBatches(key, now, guest);
        int previous = memberItemCount(key, item.getItemId());
        boolean promotion = (previous + 1) % 5 == 0;
        int cost = promotion ? (item.getPointsCost() + 1) / 2 : item.getPointsCost();
        int balance = availableBalance(key);
        if (balance < cost) return redeemError("INSUFFICIENT_POINTS",
                "Insufficient points. Required: " + cost + ", available: " + balance + ".");
        LoyaltyTransaction redemption = new LoyaltyTransaction(key, guest.getConfirmationNumber(),
                guest.getGuestName(), item.getItemId(), item.getItemName(), item.getPointsCost(),
                cost, promotion, now, now.plusDays(item.getValidityDays()));
        if (!item.recordRedemption()) return redeemError("OUT_OF_STOCK", "Reward item is out of stock.");
        consumeFifo(key, cost, now);
        redemptionHistory.add(redemption);
        pointHistory.add(PointTransaction.deduction(key, guest.getConfirmationNumber(),
                guest.getGuestName(), PointTransaction.REDEMPTION,
                "Redeemed: " + item.getItemName(), -cost, now,
                "REWARD:" + redemption.getTransactionId(), redemption.getTransactionId()));
        int newBalance = availableBalance(key);
        syncMember(key, newBalance, memberExperience(key));
        String message = "Redeemed '" + item.getItemName() + "' for " + cost + " points.";
        if (promotion) message += " 50% loyalty offer applied.";
        return new RedemptionResult(true, "SUCCESS", message, redemption.getTransactionId(),
                cost, newBalance, promotion);
    }

    public String redeemRewardItem(Guest guest, RewardItem item) {
        if (guest == null || item == null) return "ERROR: Invalid guest or reward item.";
        RedemptionResult r = redeemReward(guest.getConfirmationNumber(), item.getItemId());
        return (r.isSuccess() ? "SUCCESS: " : "ERROR: ") + r.getMessage();
    }

    private RedemptionResult redeemError(String code, String message) {
        return new RedemptionResult(false, code, message, null, 0, 0, false);
    }

    public ListInterface<LoyaltyTransaction> getRewardInventory(String confirmation, String status) {
        ListInterface<LoyaltyTransaction> result = new MyArrayList<>();
        Guest guest = findGuestByConfirmationNumber(confirmation);
        if (guest == null) return result;
        ensureMemberInitialized(guest);
        String key = resolveMemberKey(guest);
        refreshRewardExpiry(key, LocalDateTime.now());
        for (int i = 0; i < redemptionHistory.getNumberOfEntries(); i++) {
            LoyaltyTransaction t = redemptionHistory.get(i);
            if (key.equals(t.getMemberKey()) && filter(t.getStatus(), status)) result.add(t);
        }
        return result;
    }

    public UseRewardResult useRedeemedReward(String confirmation, String transactionId) {
        Guest guest = findGuestByConfirmationNumber(confirmation);
        if (guest == null) return useError("GUEST_NOT_FOUND", "Guest not found.");
        if (blank(transactionId)) return useError("INVALID_ID", "Transaction ID is required.");
        ensureMemberInitialized(guest);
        String key = resolveMemberKey(guest);
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < redemptionHistory.getNumberOfEntries(); i++) {
            LoyaltyTransaction t = redemptionHistory.get(i);
            if (!key.equals(t.getMemberKey()) || !transactionId.trim().equalsIgnoreCase(t.getTransactionId())) continue;
            t.refreshExpiry(now);
            if (LoyaltyTransaction.EXPIRED.equals(t.getStatus())) return useError("EXPIRED", "Redeemed reward has expired.");
            if (LoyaltyTransaction.USED.equals(t.getStatus())) return useError("ALREADY_USED", "Redeemed reward was already used.");
            if (t.markUsed(now)) return new UseRewardResult(true, "SUCCESS", "Used '" + t.getItemName() + "'.");
        }
        return useError("NOT_FOUND", "Active redeemed reward not found.");
    }

    private UseRewardResult useError(String code, String message) {
        return new UseRewardResult(false, code, message);
    }

    public String useRedeemedItem(Guest guest, int choice) {
        if (guest == null || choice <= 0) return "ERROR: Invalid item selection.";
        ListInterface<LoyaltyTransaction> active = getRewardInventory(guest.getConfirmationNumber(), LoyaltyTransaction.ACTIVE);
        if (choice > active.getNumberOfEntries()) return "ERROR: Invalid item selection.";
        UseRewardResult r = useRedeemedReward(guest.getConfirmationNumber(), active.get(choice-1).getTransactionId());
        return (r.isSuccess() ? "SUCCESS: " : "ERROR: ") + r.getMessage();
    }

    public String getFormattedInventory(Guest guest) {
        if (guest == null) return "No items in storage yet.";
        ListInterface<LoyaltyTransaction> items = getRewardInventory(guest.getConfirmationNumber(), "ALL");
        if (items.isEmpty()) return "No items in storage yet.";
        StringBuilder out = new StringBuilder(); int activeNo = 0;
        for (int i = 0; i < items.getNumberOfEntries(); i++) {
            LoyaltyTransaction t = items.get(i);
            String no = LoyaltyTransaction.ACTIVE.equals(t.getStatus()) ? String.valueOf(++activeNo) : "-";
            out.append(String.format(" %-3s %-25s (%s) | %s to %s | Status: %-8s%n",
                    no + ".", t.getItemName(), t.getTransactionId(), t.getStartTimeFormatted(),
                    t.getEndTimeFormatted(), t.getStatus()));
        }
        return out.toString();
    }

    private void refreshRewardExpiry(String key, LocalDateTime now) {
        for (int i = 0; i < redemptionHistory.getNumberOfEntries(); i++) {
            LoyaltyTransaction t = redemptionHistory.get(i);
            if (key == null || key.equals(t.getMemberKey())) t.refreshExpiry(now);
        }
    }

    private int memberItemCount(String key, String itemId) {
        int count = 0;
        for (int i = 0; i < redemptionHistory.getNumberOfEntries(); i++) {
            LoyaltyTransaction t = redemptionHistory.get(i);
            if (key.equals(t.getMemberKey()) && itemId.equalsIgnoreCase(t.getRewardItemId())) count++;
        }
        return count;
    }

    public String resetAllRewardStocks() {
        ListInterface<RewardItem> items = getRewardCatalog();
        for (int i = 0; i < items.getNumberOfEntries(); i++) items.get(i).resetStockToDefault();
        return "All reward items were restocked to their default quantities.";
    }

    // Member histories and notifications.
    public ListInterface<PointTransaction> getMemberPointHistory(String confirmation) {
        ListInterface<PointTransaction> result = new MyArrayList<>();
        Guest guest = findGuestByConfirmationNumber(confirmation);
        if (guest == null) return result;
        ensureMemberInitialized(guest); expireBatches(resolveMemberKey(guest), LocalDateTime.now(), guest);
        String key = resolveMemberKey(guest);
        for (int i = 0; i < pointHistory.getNumberOfEntries(); i++)
            if (key.equals(pointHistory.get(i).getMemberKey())) result.add(pointHistory.get(i));
        result.sort((a,b) -> b.getOccurredAt().compareTo(a.getOccurredAt()));
        return result;
    }

    public String getFormattedTransactionHistory(Guest guest) {
        if (guest == null) return "No point transactions yet.";
        ListInterface<PointTransaction> rows = getMemberPointHistory(guest.getConfirmationNumber());
        if (rows.isEmpty()) return "No point transactions yet.";
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < rows.getNumberOfEntries(); i++) {
            PointTransaction t = rows.get(i);
            out.append(String.format(" %3d. %+-6d pts | %-18s | %-36s | %s | %-14s%n",
                    i+1, t.getPointsChange(), t.getTransactionType(), t.getDescription(),
                    t.getOccurredAtFormatted(), t.getStatus()));
        }
        return out.toString();
    }

    public String checkNotifications(Guest guest) {
        if (guest == null) return "No notifications.";
        ensureMemberInitialized(guest);
        String key = resolveMemberKey(guest); LocalDateTime now = LocalDateTime.now();
        expireBatches(key, now, guest); refreshRewardExpiry(key, now);
        int expired = 0, expiring = 0, expiringPoints = 0;
        for (int i = 0; i < redemptionHistory.getNumberOfEntries(); i++) {
            LoyaltyTransaction t = redemptionHistory.get(i);
            if (!key.equals(t.getMemberKey())) continue;
            if (LoyaltyTransaction.EXPIRED.equals(t.getStatus())) expired++;
            else if (LoyaltyTransaction.ACTIVE.equals(t.getStatus())
                    && Duration.between(now, t.getExpiresAt()).toDays() <= 7) expiring++;
        }
        for (int i = 0; i < pointHistory.getNumberOfEntries(); i++) {
            PointTransaction t = pointHistory.get(i);
            if (key.equals(t.getMemberKey()) && t.isSpendableAt(now) && t.getExpiresAt() != null
                    && Duration.between(now, t.getExpiresAt()).toDays() <= 30)
                expiringPoints += t.getRemainingPoints();
        }
        StringBuilder out = new StringBuilder();
        if (expired > 0) out.append("[ALERT] ").append(expired).append(" redeemed reward(s) expired.\n");
        if (expiring > 0) out.append("[ALERT] ").append(expiring).append(" reward(s) expire within 7 days.\n");
        if (expiringPoints > 0) out.append("[ALERT] ").append(expiringPoints).append(" points expire within 30 days.\n");
        ListInterface<RewardItem> catalog = getRewardCatalog();
        for (int i = 0; i < catalog.getNumberOfEntries(); i++) {
            RewardItem item = catalog.get(i);
            if ((memberItemCount(key, item.getItemId()) + 1) % 5 == 0)
                out.append("[OFFER] Next ").append(item.getItemName()).append(" redemption is 50% off.\n");
        }
        out.append("Tier progress: ").append(getNextTierInfo(guest));
        return out.toString();
    }

    // Boundary-facing formatted views keep ADT traversal and entity mutation in Control.
    public String getMemberProfileText(String confirmation) {
        Guest guest = findGuestByConfirmationNumber(confirmation);
        if (guest == null) return "ERROR: Member confirmation number was not found.";
        refreshPoints(guest);
        return String.format(
                "Member Name       : %s%nConfirmation No.  : %s%nMember Key        : %s%n"
                + "Current Tier      : %s%nRedeemable Points : %d%nLifetime EXP      : %d%nTier Progress     : %s",
                guest.getGuestName(), guest.getConfirmationNumber(), resolveMemberKey(guest),
                guest.getLoyaltyTier(), guest.getLoyaltyPoints(), guest.getLoyaltyExperience(),
                getNextTierInfo(guest));
    }

    public String getNotificationsText(String confirmation) {
        Guest guest = findGuestByConfirmationNumber(confirmation);
        return guest == null ? "ERROR: Member confirmation number was not found."
                : checkNotifications(guest);
    }

    public String getRewardCatalogText() {
        ListInterface<RewardItem> items = getRewardCatalog();
        StringBuilder out = new StringBuilder();
        out.append(String.format("%-6s | %-30s | %-10s | %-8s | %-13s | %-9s%n",
                "ID", "Reward", "Cost", "Stock", "Stock Status", "Validity"));
        out.append("------------------------------------------------------------------------------------------\n");
        for (int i = 0; i < items.getNumberOfEntries(); i++) {
            RewardItem item = items.get(i);
            out.append(String.format("%-6s | %-30s | %7d pts | %8d | %-13s | %6d days%n",
                    item.getItemId(), item.getItemName(), item.getPointsCost(),
                    item.getStockQuantity(), item.getStockStatus(), item.getValidityDays()));
        }
        return out.toString();
    }

    public String getRewardInventoryText(String confirmation, String status) {
        ListInterface<LoyaltyTransaction> items = getRewardInventory(confirmation, status);
        if (findGuestByConfirmationNumber(confirmation) == null)
            return "ERROR: Member confirmation number was not found.";
        if (items.isEmpty()) return "No redeemed rewards match the selected status.";
        StringBuilder out = new StringBuilder();
        out.append(String.format("%-9s | %-6s | %-28s | %-8s | %-19s | %-19s%n",
                "Txn ID", "Item", "Reward", "Status", "Redeemed", "Expires"));
        out.append("------------------------------------------------------------------------------------------------\n");
        for (int i = 0; i < items.getNumberOfEntries(); i++) {
            LoyaltyTransaction item = items.get(i);
            out.append(String.format("%-9s | %-6s | %-28s | %-8s | %-19s | %-19s%n",
                    item.getTransactionId(), item.getRewardItemId(), item.getItemName(),
                    item.getStatus(), item.getStartTimeFormatted(), item.getEndTimeFormatted()));
        }
        return out.toString();
    }

    public String getPointHistoryText(String confirmation) {
        Guest guest = findGuestByConfirmationNumber(confirmation);
        return guest == null ? "ERROR: Member confirmation number was not found."
                : getFormattedTransactionHistory(guest);
    }

    // Reports: search + multiple filters + custom MyArrayList sorting.
    public ListInterface<PointTransaction> generatePointActivityReport(String search,
            String typeFilter, String statusFilter, LocalDate fromDate, LocalDate toDate,
            int minimumAbsolutePoints, String sortField, boolean ascending) {
        expireAllPoints();
        ListInterface<PointTransaction> result = new MyArrayList<>();
        Guest searchedGuest = findGuestByConfirmationNumber(search);
        String searchedMemberKey = searchedGuest == null ? "" : resolveMemberKey(searchedGuest);
        for (int i = 0; i < pointHistory.getNumberOfEntries(); i++) {
            PointTransaction t = pointHistory.get(i); LocalDate date = t.getOccurredAt().toLocalDate();
            boolean searchOk = blank(search)
                    || (!searchedMemberKey.isEmpty() && searchedMemberKey.equals(t.getMemberKey()))
                    || has(t.getMemberKey(), search)
                    || has(t.getSourceConfirmationNumber(), search) || has(t.getGuestNameSnapshot(), search)
                    || has(t.getDescription(), search) || has(t.getExternalReference(), search);
            boolean statusOk = "DEDUCTION".equalsIgnoreCase(statusFilter)
                    ? t.getPointsChange() < 0 : filter(t.getStatus(), statusFilter);
            if (searchOk && filter(t.getTransactionType(), typeFilter) && statusOk
                    && (fromDate == null || !date.isBefore(fromDate))
                    && (toDate == null || !date.isAfter(toDate))
                    && Math.abs(t.getPointsChange()) >= Math.max(0, minimumAbsolutePoints)) result.add(t);
        }
        final int direction = ascending ? 1 : -1;
        result.sort((a,b) -> direction * comparePoint(a,b,sortField));
        return result;
    }

    public String getPointActivityReportText(String search, String typeFilter,
            String statusFilter, LocalDate fromDate, LocalDate toDate,
            int minimumAbsolutePoints, String sortField, boolean ascending) {
        ListInterface<PointTransaction> rows = generatePointActivityReport(search, typeFilter,
                statusFilter, fromDate, toDate, minimumAbsolutePoints, sortField, ascending);
        StringBuilder out = new StringBuilder();
        out.append("MEMBER POINT ACTIVITY REPORT\n");
        out.append("Criteria: search=").append(blank(search) ? "ALL" : search)
                .append(", type=").append(blank(typeFilter) ? "ALL" : typeFilter)
                .append(", status=").append(blank(statusFilter) ? "ALL" : statusFilter)
                .append(", from=").append(fromDate == null ? "ALL" : fromDate)
                .append(", to=").append(toDate == null ? "ALL" : toDate)
                .append(", min |points|=").append(Math.max(0, minimumAbsolutePoints))
                .append(", sort=").append(blank(sortField) ? "DATE" : sortField)
                .append(ascending ? " ASC\n" : " DESC\n");
        out.append(String.format("%-9s | %-10s | %-18s | %-7s | %-14s | %-19s | %s%n",
                "Txn ID", "Confirm", "Type", "Points", "Status", "Date", "Description"));
        out.append("------------------------------------------------------------------------------------------------------------------\n");
        int positive = 0, negative = 0;
        for (int i = 0; i < rows.getNumberOfEntries(); i++) {
            PointTransaction row = rows.get(i);
            out.append(String.format("%-9s | %-10s | %-18s | %+7d | %-14s | %-19s | %s%n",
                    row.getTransactionId(), text(row.getSourceConfirmationNumber()),
                    row.getTransactionType(), row.getPointsChange(), row.getStatus(),
                    row.getOccurredAtFormatted(), row.getDescription()));
            if (row.getPointsChange() >= 0) positive += row.getPointsChange();
            else negative += row.getPointsChange();
        }
        out.append("------------------------------------------------------------------------------------------------------------------\n");
        out.append(String.format("Rows: %d | Positive: +%d | Deductions: %d | Net: %+d",
                rows.getNumberOfEntries(), positive, negative, positive + negative));
        return out.toString();
    }

    private int comparePoint(PointTransaction a, PointTransaction b, String field) {
        if ("POINTS".equalsIgnoreCase(field)) return Integer.compare(a.getPointsChange(), b.getPointsChange());
        if ("CONFIRMATION".equalsIgnoreCase(field)) return text(a.getSourceConfirmationNumber()).compareToIgnoreCase(text(b.getSourceConfirmationNumber()));
        if ("TYPE".equalsIgnoreCase(field)) return a.getTransactionType().compareToIgnoreCase(b.getTransactionType());
        if ("STATUS".equalsIgnoreCase(field)) return a.getStatus().compareToIgnoreCase(b.getStatus());
        return a.getOccurredAt().compareTo(b.getOccurredAt());
    }

    public ListInterface<String> getFilteredPointReport(String status, boolean ascending) {
        ListInterface<PointTransaction> rows = generatePointActivityReport("", "ALL", status,
                null, null, 0, "POINTS", ascending);
        ListInterface<String> result = new MyArrayList<>();
        for (int i = 0; i < rows.getNumberOfEntries(); i++) {
            PointTransaction t = rows.get(i);
            result.add(String.format("%d|%s|%s|%s|Conf: %s|%s", t.getPointsChange(),
                    t.getOccurredAtFormatted(), t.getExpiresAtFormatted(), t.getDescription(),
                    t.getSourceConfirmationNumber(), t.getStatus()));
        }
        return result;
    }

    public ListInterface<RewardPerformanceRow> generateRewardPerformanceReport(String search,
            String stockStatus, int minCost, int maxCost, int minRedeemed,
            String sortField, boolean ascending) {
        refreshRewardExpiry(null, LocalDateTime.now());
        ListInterface<RewardPerformanceRow> result = new MyArrayList<>();
        ListInterface<RewardItem> items = getRewardCatalog();
        for (int i = 0; i < items.getNumberOfEntries(); i++) {
            RewardItem item = items.get(i); RewardPerformanceRow row = performanceRow(item);
            if ((blank(search) || has(item.getItemId(), search) || has(item.getItemName(), search))
                    && filter(item.getStockStatus(), stockStatus)
                    && item.getPointsCost() >= Math.max(0, minCost)
                    && (maxCost < 0 || item.getPointsCost() <= maxCost)
                    && row.getTotalRedeemed() >= Math.max(0, minRedeemed)) result.add(row);
        }
        final int direction = ascending ? 1 : -1;
        result.sort((a,b) -> direction * compareReward(a,b,sortField));
        return result;
    }

    public String getRewardPerformanceReportText(String search, String stockStatus,
            int minCost, int maxCost, int minRedeemed, String sortField, boolean ascending) {
        ListInterface<RewardPerformanceRow> rows = generateRewardPerformanceReport(search,
                stockStatus, minCost, maxCost, minRedeemed, sortField, ascending);
        StringBuilder out = new StringBuilder();
        out.append("REWARD STOCK & PERFORMANCE REPORT\n");
        out.append("Criteria: search=").append(blank(search) ? "ALL" : search)
                .append(", stock=").append(blank(stockStatus) ? "ALL" : stockStatus)
                .append(", cost=").append(Math.max(0, minCost)).append("..")
                .append(maxCost < 0 ? "ALL" : maxCost)
                .append(", min redeemed=").append(Math.max(0, minRedeemed))
                .append(", sort=").append(blank(sortField) ? "COST" : sortField)
                .append(ascending ? " ASC\n" : " DESC\n");
        out.append(String.format("%-6s | %-28s | %-6s | %-5s | %-13s | %-8s | %-6s | %-7s%n",
                "ID", "Reward", "Cost", "Stock", "Stock Status", "Redeemed", "Active", "Used/Exp"));
        out.append("-----------------------------------------------------------------------------------------------------\n");
        int total = 0, active = 0, used = 0, expired = 0;
        for (int i = 0; i < rows.getNumberOfEntries(); i++) {
            RewardPerformanceRow row = rows.get(i);
            out.append(String.format("%-6s | %-28s | %6d | %5d | %-13s | %8d | %6d | %3d/%-3d%n",
                    row.getRewardItem().getItemId(), row.getRewardItem().getItemName(),
                    row.getRewardItem().getPointsCost(), row.getCurrentStock(), row.getStockStatus(),
                    row.getTotalRedeemed(), row.getActiveCount(), row.getUsedCount(), row.getExpiredCount()));
            total += row.getTotalRedeemed(); active += row.getActiveCount();
            used += row.getUsedCount(); expired += row.getExpiredCount();
        }
        out.append("-----------------------------------------------------------------------------------------------------\n");
        out.append(String.format("Rows: %d | Redeemed: %d | Active: %d | Used: %d | Expired: %d",
                rows.getNumberOfEntries(), total, active, used, expired));
        return out.toString();
    }

    private RewardPerformanceRow performanceRow(RewardItem item) {
        int total=0, active=0, used=0, expired=0;
        for (int i = 0; i < redemptionHistory.getNumberOfEntries(); i++) {
            LoyaltyTransaction t = redemptionHistory.get(i);
            if (!sameReward(t,item)) continue;
            total++;
            if (LoyaltyTransaction.ACTIVE.equals(t.getStatus())) active++;
            else if (LoyaltyTransaction.USED.equals(t.getStatus())) used++;
            else if (LoyaltyTransaction.EXPIRED.equals(t.getStatus())) expired++;
        }
        return new RewardPerformanceRow(item,total,active,used,expired);
    }

    private int compareReward(RewardPerformanceRow a, RewardPerformanceRow b, String field) {
        if ("STOCK".equalsIgnoreCase(field)) return Integer.compare(a.getCurrentStock(), b.getCurrentStock());
        if ("REDEEMED".equalsIgnoreCase(field)) return Integer.compare(a.getTotalRedeemed(), b.getTotalRedeemed());
        if ("NAME".equalsIgnoreCase(field)) return a.getRewardItem().getItemName().compareToIgnoreCase(b.getRewardItem().getItemName());
        if ("STATUS".equalsIgnoreCase(field)) return a.getStockStatus().compareToIgnoreCase(b.getStockStatus());
        return Integer.compare(a.getRewardItem().getPointsCost(), b.getRewardItem().getPointsCost());
    }

    public ListInterface<RewardItem> getFilteredRewardReport(int maxStock, boolean ascending) {
        ListInterface<RewardPerformanceRow> rows = generateRewardPerformanceReport("", "ALL", 0, -1, 0, "COST", ascending);
        ListInterface<RewardItem> result = new MyArrayList<>();
        for (int i=0; i<rows.getNumberOfEntries(); i++) if (rows.get(i).getCurrentStock() <= maxStock) result.add(rows.get(i).getRewardItem());
        return result;
    }

    public int getTotalItemRedemptionCount(String name) { return countStatus(name,"ALL",null); }
    public int getActiveItemRedemptionCount(Guest guest,String name) { return countStatus(name,LoyaltyTransaction.ACTIVE,guest==null?null:resolveMemberKey(guest)); }
    public int getUsedItemRedemptionCount(String name) { return countStatus(name,LoyaltyTransaction.USED,null); }
    public int getExpiredItemRedemptionCount(String name) { return countStatus(name,LoyaltyTransaction.EXPIRED,null); }

    private int countStatus(String name, String status, String key) {
        refreshRewardExpiry(key, LocalDateTime.now()); int count=0;
        for (int i=0; i<redemptionHistory.getNumberOfEntries(); i++) {
            LoyaltyTransaction t=redemptionHistory.get(i);
            if (t.getItemName().equalsIgnoreCase(name) && (key==null || key.equals(t.getMemberKey()))
                    && filter(t.getStatus(),status)) count++;
        }
        return count;
    }

    public boolean isValidMenuChoice(String choice, int min, int max) {
        try { int n=Integer.parseInt(choice.trim()); return n>=min && n<=max; }
        catch (Exception e) { return false; }
    }

    private boolean sameReward(LoyaltyTransaction t, RewardItem item) {
        return !blank(t.getRewardItemId()) ? item.getItemId().equalsIgnoreCase(t.getRewardItemId())
                : item.getItemName().equalsIgnoreCase(t.getItemName());
    }
    private boolean filter(String value,String wanted) { return blank(wanted)||"ALL".equalsIgnoreCase(wanted)||text(value).equalsIgnoreCase(wanted.trim()); }
    private boolean has(String value,String query) { return text(value).toLowerCase().contains(text(query).trim().toLowerCase()); }
    private boolean contains(ListInterface<String> values,String target) { for(int i=0;i<values.getNumberOfEntries();i++) if(values.get(i).equals(target)) return true; return false; }
    private boolean blank(String value) { return value==null||value.trim().isEmpty(); }
    private String text(String value) { return value==null?"":value; }
}
