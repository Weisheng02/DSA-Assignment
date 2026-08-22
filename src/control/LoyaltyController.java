package control;

import adt.BSTInterface;
import adt.BinarySearchTree;
import adt.ListInterface;
import adt.MyArrayList;
import adt.QueueInterface;
import adt.ArrayQueue;
import entity.Booking;
import entity.Guest;
import entity.LoyaltyTransaction;
import entity.PointBatch;
import entity.RewardItem;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Author: Tan Hock Siang
 * Loyalty & Rewards module business logic controller.
 */
public class LoyaltyController {
    private static final int POINT_VALIDITY_MINUTES = 365 * 24 * 60;
    private static final int NON_EXPIRING_POINTS = -1;
    private static final int OPENING_BALANCE_VALIDITY_MINUTES = POINT_VALIDITY_MINUTES;
    // Reward costs start at 20 points and tier thresholds are 200/500/1200 EXP.
    // Fifty points is meaningful without allowing one daily claim to skip tiers.
    public static final int DAILY_CHECK_IN_POINTS = 50;
    // Source of truth for earned points, remaining balances and expiry states.
    private ListInterface<PointBatch> pointBatches = new MyArrayList<>();
    private ListInterface<LoyaltyTransaction> redemptionHistory = new MyArrayList<>();
    private ListInterface<String> dailyClaimRecords = new MyArrayList<>();
    private QueueInterface<PendingItemRequest> pendingItemQueue = new ArrayQueue<>();
    private BSTInterface<RewardItem> rewardCatalog = new BinarySearchTree<>();
    private BSTInterface<Guest> masterGuestTree;

    /** One reward request held in the FIFO settlement queue. */
    public static class PendingItemRequest {
        private final Guest guest;
        private final RewardItem item;
        private final int effectiveCost;

        public PendingItemRequest(Guest guest, RewardItem item, int effectiveCost) {
            this.guest = guest;
            this.item = item;
            this.effectiveCost = effectiveCost;
        }

        public Guest getGuest() {
            return guest;
        }

        public RewardItem getItem() {
            return item;
        }

        public int getEffectiveCost() {
            return effectiveCost;
        }
    }

    /** Minimal result type required by the existing Front Desk checkout hook. */
    public static class AwardResult {
        private final boolean success;
        private final int pointsAwarded;

        public AwardResult(boolean success, int pointsAwarded) {
            this.success = success;
            this.pointsAwarded = pointsAwarded;
        }

        public boolean isSuccess() {
            return success;
        }

        public int getPointsAwarded() {
            return pointsAwarded;
        }
    }

    /** Keeps the supplied Loyalty module connected to the shared Guest BST. */
    public LoyaltyController(BSTInterface<Guest> masterGuestTree) {
        initializeDefaultRewards();
        this.masterGuestTree = masterGuestTree;
        initializeOpeningPointBatches();
    }

    /**
     * Converts seeded member balances into one traceable batch per person.
     * Repeat-stay Guest profiles share an IC/passport and must not duplicate the
     * same opening points.
     */
    private void initializeOpeningPointBatches() {
        if (masterGuestTree == null)
            return;
        ListInterface<String> initializedMembers = new MyArrayList<>();
        ListInterface<Guest> guests = masterGuestTree.inOrderTraversal();
        for (int i = 0; i < guests.getNumberOfEntries(); i++) {
            Guest guest = guests.get(i);
            String key = memberKey(guest);
            if (guest.getLoyaltyPoints() <= 0 || containsText(initializedMembers, key))
                continue;
            initializedMembers.add(key);
            PointBatch batch = new PointBatch(guest.getLoyaltyPoints(), "Opening Balance",
                    guest.getConfirmationNumber(), key, OPENING_BALANCE_VALIDITY_MINUTES);
            pointBatches.add(batch);
        }
    }

    private boolean containsText(ListInterface<String> values, String target) {
        for (int i = 0; i < values.getNumberOfEntries(); i++)
            if (values.get(i).equals(target))
                return true;
        return false;
    }

    private Guest findGuest(String confirmationNumber) {
        return ControllerDataSupport.findGuestByConfirmation(masterGuestTree, confirmationNumber);
    }

    /**
     * Finds a guest for the Loyalty boundary without exposing the Guest BST.
     * The tree remains an implementation detail of this Control class.
     */
    public Guest findGuestByConfirmationNumber(String confirmationNumber) {
        return findGuest(confirmationNumber);
    }

    // Simple operation results used by the console Boundary.
    public static final int RESULT_SUCCESS = 1;
    public static final int RESULT_ALREADY_CLAIMED = 0;
    public static final int RESULT_NOT_FOUND = -1;
    public static final int RESULT_INVALID_SELECTION = -2;
    public static final int RESULT_INSUFFICIENT_POINTS = -3;
    public static final int RESULT_OUT_OF_STOCK = -4;

    public boolean memberExists(String confirmationNumber) {
        return findGuest(confirmationNumber) != null;
    }

    /** Loyalty-owned room discount policy used by Front Desk billing. */
    public double getRoomDiscountRate(String loyaltyTier) {
        if (loyaltyTier == null)
            return 0.0;
        switch (loyaltyTier.trim().toUpperCase()) {
            case "PLATINUM":
                return 0.20;
            case "GOLD":
                return 0.10;
            case "SILVER":
                return 0.05;
            default:
                return 0.0;
        }
    }

    /** Compatibility validation for the existing Front Desk checkout flow. */
    public AwardResult validateCheckoutAward(String confirmationNumber,
            String sourceReference, int points) {
        return new AwardResult(findGuest(confirmationNumber) != null && points >= 0, 0);
    }

    /** Adds a successful Front Desk checkout reward as a new point batch. */
    public AwardResult awardCheckoutPoints(String confirmationNumber,
            String sourceReference, int points) {
        Guest guest = findGuest(confirmationNumber);
        if (guest == null || points < 0)
            return new AwardResult(false, 0);
        recordPointTransaction(guest, "Checkout Reward (" + sourceReference + ")", points,
                NON_EXPIRING_POINTS);
        return new AwardResult(true, points);
    }

    /** Migrates the identity shared by all stay profiles and loyalty batches. */
    public boolean migrateMemberIdentity(Guest guest, String newIcNo) {
        if (guest == null || newIcNo == null || newIcNo.trim().isEmpty())
            return false;
        String oldKey = memberKey(guest);
        String newIdentity = newIcNo.trim();
        String normalizedNewIdentity = ControllerDataSupport.normalizeIdentity(newIdentity);
        String newKey = (!normalizedNewIdentity.isEmpty() && !"na".equals(normalizedNewIdentity))
                ? "id:" + normalizedNewIdentity
                : "conf:" + guest.getConfirmationNumber().trim().toLowerCase();

        if (masterGuestTree != null) {
            ListInterface<Guest> profiles = masterGuestTree.inOrderTraversal();
            for (int i = 0; i < profiles.getNumberOfEntries(); i++) {
                Guest profile = profiles.get(i);
                if (oldKey.equals(memberKey(profile)))
                    profile.setIcNo(newIdentity);
            }
        }
        for (int i = 0; i < pointBatches.getNumberOfEntries(); i++) {
            PointBatch batch = pointBatches.get(i);
            if (oldKey.equals(batch.getMemberKey()))
                batch.setMemberKey(newKey);
        }
        ListInterface<String> migratedClaims = new MyArrayList<>();
        for (int i = 0; i < dailyClaimRecords.getNumberOfEntries(); i++) {
            String record = dailyClaimRecords.get(i);
            migratedClaims.add(record.startsWith(oldKey + "|")
                    ? newKey + record.substring(oldKey.length()) : record);
        }
        dailyClaimRecords = migratedClaims;
        return true;
    }

    private String calculateTier(int exp) {
        if (exp >= 1200)
            return "Platinum";
        if (exp >= 500)
            return "Gold";
        if (exp >= 200)
            return "Silver";
        return "Standard";
    }

    /** Returns the tier earned by the member's lifetime EXP without changing state. */
    public String getCalculatedTier(Guest guest) {
        return guest == null ? "Standard" : calculateTier(guest.getLoyaltyExperiences());
    }

    /** Applies the calculated tier and synchronizes every stay profile for the member. */
    public void updateMemberTier(Guest guest) {
        if (guest == null)
            return;
        guest.setLoyaltyTier(calculateTier(guest.getLoyaltyExperiences()));
        synchronizeMemberProfiles(guest);
    }

    /** Returns the next EXP threshold, or -1 when Platinum is already reached. */
    public int getNextTierThreshold(Guest guest) {
        String tier = getCalculatedTier(guest);
        if ("Standard".equals(tier))
            return 200;
        if ("Silver".equals(tier))
            return 500;
        if ("Gold".equals(tier))
            return 1200;
        return -1;
    }

    public void refreshPoints(Guest guest) {
        checkExpiredPoints(guest);
    }

    private String memberKey(Guest guest) {
        if (guest == null)
            return "";
        String identity = ControllerDataSupport.normalizeIdentity(guest.getIcNo());
        if (!identity.isEmpty() && !"na".equals(identity))
            return "id:" + identity;
        String confirmation = guest.getConfirmationNumber() == null ? ""
                : guest.getConfirmationNumber().trim().toLowerCase();
        return "conf:" + confirmation;
    }

    private boolean sameMember(Guest first, Guest second) {
        if (first == null || second == null)
            return false;
        return memberKey(first).equals(memberKey(second));
    }

    private boolean confirmationBelongsToMember(Guest member, String confirmationNumber) {
        Guest transactionOwner = findGuest(confirmationNumber);
        return transactionOwner != null && sameMember(member, transactionOwner);
    }

    /** Keeps every stay profile belonging to one IC/passport on one loyalty balance. */
    private void synchronizeMemberProfiles(Guest source) {
        if (source == null || masterGuestTree == null)
            return;
        ListInterface<Guest> profiles = masterGuestTree.inOrderTraversal();
        for (int i = 0; i < profiles.getNumberOfEntries(); i++) {
            Guest profile = profiles.get(i);
            if (sameMember(source, profile)) {
                profile.setLoyaltyPoints(source.getLoyaltyPoints());
                profile.setLoyaltyExperience(source.getLoyaltyExperience());
                profile.setLoyaltyTier(source.getLoyaltyTier());
            }
        }
    }

    /** Uses the oldest active earned batches first so spent points cannot expire twice. */
    private void consumePointBatches(Guest guest, int pointsToConsume) {
        int remaining = pointsToConsume;
        String key = memberKey(guest);
        for (int i = 0; i < pointBatches.getNumberOfEntries() && remaining > 0; i++) {
            PointBatch batch = pointBatches.get(i);
            if (!key.equals(batch.getMemberKey()))
                continue;
            int consumed = batch.consume(remaining);
            remaining -= consumed;
        }
    }

    public void recordPointTransaction(Guest guest, String desc, int pts) {
        recordPointTransaction(guest, desc, pts, POINT_VALIDITY_MINUTES);
    }

    private void recordPointTransaction(Guest guest, String desc, int pts, int validityMinutes) {
        if (guest == null)
            return;
        checkExpiredPoints(guest);
        int actualPoints = pts < 0 ? -Math.min(guest.getLoyaltyPoints(), -pts) : pts;
        if (actualPoints < 0)
            consumePointBatches(guest, -actualPoints);

        guest.setLoyaltyPoints(Math.max(0, guest.getLoyaltyPoints() + actualPoints));
        if (actualPoints > 0)
            guest.setLoyaltyExperiences(guest.getLoyaltyExperiences() + actualPoints);

        if (actualPoints > 0) {
            PointBatch batch = new PointBatch(actualPoints, desc, guest.getConfirmationNumber(),
                    memberKey(guest), validityMinutes);
            pointBatches.add(batch);
        }
        synchronizeMemberProfiles(guest);
    }

    private void checkExpiredPoints(Guest guest) {
        if (guest == null || guest.getConfirmationNumber() == null)
            return;
        LocalDateTime now = LocalDateTime.now();
        String key = memberKey(guest);
        int expiredTotal = 0;
        for (int i = 0; i < pointBatches.getNumberOfEntries(); i++) {
            PointBatch batch = pointBatches.get(i);
            if (!key.equals(batch.getMemberKey()))
                continue;
            int expired = batch.expire(now);
            if (expired > 0) {
                expiredTotal += expired;
            }
        }
        if (expiredTotal > 0) {
            guest.setLoyaltyPoints(Math.max(0, guest.getLoyaltyPoints() - expiredTotal));
            synchronizeMemberProfiles(guest);
        }
    }

    public void processRoomBooking(Guest guest, Booking booking, boolean isCancel) {
        if (guest == null || booking == null)
            return;
        int pts = "Presidential Suite".equals(booking.getRoomType()) ? 30
                : ("Deluxe Suite".equals(booking.getRoomType()) ? 20 : 10);
        recordPointTransaction(guest,
                (isCancel ? "Cancelled " : "") + booking.getRoomType() + " (" + booking.getBookingId() + ")",
                isCancel ? -pts : pts);
    }

    private int claimDailyCheckIn(Guest guest) {
        if (guest == null)
            return RESULT_NOT_FOUND;
        String claimKey = memberKey(guest) + "|" + LocalDate.now();
        for (int i = 0; i < dailyClaimRecords.getNumberOfEntries(); i++)
            if (dailyClaimRecords.get(i).equals(claimKey))
                return RESULT_ALREADY_CLAIMED;
        dailyClaimRecords.add(claimKey);
        recordPointTransaction(guest, "Daily Check-In", DAILY_CHECK_IN_POINTS);
        return RESULT_SUCCESS;
    }

    public int claimDailyCheckIn(String confirmationNumber) {
        return claimDailyCheckIn(findGuest(confirmationNumber));
    }

    /** Returns newest-first point batches for one loyalty member. */
    public PointBatch[] getMemberPointBatches(String confirmationNumber) {
        Guest guest = findGuest(confirmationNumber);
        if (guest == null)
            return new PointBatch[0];
        checkExpiredPoints(guest);
        ListInterface<PointBatch> records = new MyArrayList<>();
        int len = pointBatches.getNumberOfEntries();
        String key = memberKey(guest);
        for (int i = len - 1; i >= 0; i--) {
            PointBatch batch = pointBatches.get(i);
            if (key.equals(batch.getMemberKey()))
                records.add(batch);
        }
        PointBatch[] result = new PointBatch[records.getNumberOfEntries()];
        for (int i = 0; i < result.length; i++)
            result[i] = records.get(i);
        return result;
    }

    private void initializeDefaultRewards() {
        rewardCatalog.add(new RewardItem("Free Coffee Drink", 20, 10, 1));
        rewardCatalog.add(new RewardItem("Complimentary Breakfast", 50, 5, 120));
        rewardCatalog.add(new RewardItem("Spa Discount Voucher", 100, 3, 120));
        rewardCatalog.add(new RewardItem("Free 20% Discount Dining", 500, 1, 120));
        rewardCatalog.rebalance();
    }

    public ListInterface<RewardItem> getRewardCatalog() {
        return rewardCatalog.inOrderTraversal();
    }

    /** Returns the reward catalog in a simple form suitable for the Boundary. */
    public RewardItem[] getRewardCatalogArray() {
        ListInterface<RewardItem> items = getRewardCatalog();
        RewardItem[] result = new RewardItem[items.getNumberOfEntries()];
        for (int i = 0; i < result.length; i++)
            result[i] = items.get(i);
        return result;
    }

    private int countMatches(Guest guest, String itemName, boolean matchGuest) {
        int count = 0;
        for (int i = 0; i < redemptionHistory.getNumberOfEntries(); i++) {
            LoyaltyTransaction t = redemptionHistory.get(i);
            boolean matchesGuest = !matchGuest
                    || confirmationBelongsToMember(guest, t.getConfirmationNumber());
            if (matchesGuest && t.getItemName().equals(itemName))
                count++;
        }
        return count;
    }

    private int getGuestItemRedemptionCount(Guest guest, String itemName) {
        int count = countMatches(guest, itemName, true);
        QueueInterface<PendingItemRequest> temporaryQueue = new ArrayQueue<>();
        while (!pendingItemQueue.isEmpty()) {
            PendingItemRequest request = pendingItemQueue.dequeue();
            if (sameMember(guest, request.getGuest())
                    && itemName.equals(request.getItem().getItemName()))
                count++;
            temporaryQueue.enqueue(request);
        }
        while (!temporaryQueue.isEmpty()) {
            pendingItemQueue.enqueue(temporaryQueue.dequeue());
        }
        return count;
    }

    public int getTotalItemRedemptionCount(String itemName) {
        return countMatches(null, itemName, false);
    }

    private int calculateEffectiveRewardCost(Guest guest, RewardItem item) {
        if (guest == null || item == null)
            return -1;
        int count = getGuestItemRedemptionCount(guest, item.getItemName());
        return (count > 0 && (count + 1) % 5 == 0)
                ? item.getPointsCost() / 2 : item.getPointsCost();
    }

    public int getEffectiveRewardCost(String confirmationNumber, int itemNumber) {
        Guest guest = findGuest(confirmationNumber);
        RewardItem[] items = getRewardCatalogArray();
        if (guest == null || itemNumber < 1 || itemNumber > items.length)
            return -1;
        return calculateEffectiveRewardCost(guest, items[itemNumber - 1]);
    }

    private int requestRewardRedemption(Guest guest, RewardItem item) {
        if (guest == null || item == null)
            return RESULT_NOT_FOUND;
        checkExpiredPoints(guest);
        int cost = calculateEffectiveRewardCost(guest, item);

        if (guest.getLoyaltyPoints() < cost)
            return RESULT_INSUFFICIENT_POINTS;
        if (item.getStockQuantity() <= 0)
            return RESULT_OUT_OF_STOCK;

        recordPointTransaction(guest, "Redeemed: " + item.getItemName(), -cost);
        item.setStockQuantity(item.getStockQuantity() - 1);
        pendingItemQueue.enqueue(new PendingItemRequest(guest, item, cost));
        return RESULT_SUCCESS;
    }

    public int requestRewardRedemption(String confirmationNumber, int itemNumber) {
        Guest guest = findGuest(confirmationNumber);
        if (guest == null)
            return RESULT_NOT_FOUND;
        RewardItem[] items = getRewardCatalogArray();
        if (itemNumber < 1 || itemNumber > items.length)
            return RESULT_INVALID_SELECTION;
        return requestRewardRedemption(guest, items[itemNumber - 1]);
    }

    /** Processes the FIFO queue once and returns the transactions just stored. */
    public LoyaltyTransaction[] settlePendingRewardRedemptionsData() {
        LoyaltyTransaction[] processed = new LoyaltyTransaction[pendingItemQueue.getNumberOfEntries()];
        int index = 0;
        while (!pendingItemQueue.isEmpty()) {
            PendingItemRequest request = pendingItemQueue.dequeue();
            Guest guest = request.getGuest();
            RewardItem item = request.getItem();
            LoyaltyTransaction transaction = new LoyaltyTransaction(guest.getConfirmationNumber(),
                    guest.getGuestName(), item.getItemName(), request.getEffectiveCost(),
                    item.getValidityMinutes());
            redemptionHistory.add(transaction);
            item.recordRedemptionCompleted();
            processed[index++] = transaction;
        }
        return processed;
    }

    public int getPendingRewardRedemptionCount() {
        return pendingItemQueue.getNumberOfEntries();
    }

    public LoyaltyTransaction[] getActiveInventoryArray(String confirmationNumber) {
        Guest guest = findGuest(confirmationNumber);
        if (guest == null)
            return new LoyaltyTransaction[0];
        ListInterface<LoyaltyTransaction> activeItems = new MyArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < redemptionHistory.getNumberOfEntries(); i++) {
            LoyaltyTransaction t = redemptionHistory.get(i);
            if (confirmationBelongsToMember(guest, t.getConfirmationNumber())) {
                checkAndUpdateExpiry(t, now);
                if ("ACTIVE".equalsIgnoreCase(t.getStatus()))
                    activeItems.add(t);
            }
        }
        LoyaltyTransaction[] result = new LoyaltyTransaction[activeItems.getNumberOfEntries()];
        for (int i = 0; i < result.length; i++)
            result[i] = activeItems.get(i);
        return result;
    }

    /** Returns all settled reward records for one member, including terminal states. */
    public LoyaltyTransaction[] getMemberRedemptionHistoryArray(String confirmationNumber) {
        Guest guest = findGuest(confirmationNumber);
        if (guest == null)
            return new LoyaltyTransaction[0];
        ListInterface<LoyaltyTransaction> records = new MyArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < redemptionHistory.getNumberOfEntries(); i++) {
            LoyaltyTransaction transaction = redemptionHistory.get(i);
            if (confirmationBelongsToMember(guest, transaction.getConfirmationNumber())) {
                checkAndUpdateExpiry(transaction, now);
                records.add(transaction);
            }
        }
        LoyaltyTransaction[] result = new LoyaltyTransaction[records.getNumberOfEntries()];
        for (int i = 0; i < result.length; i++)
            result[i] = records.get(i);
        return result;
    }

    private int useRedeemedItemByChoice(Guest guest, int choice) {
        if (guest == null)
            return RESULT_NOT_FOUND;
        int count = 0;
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < redemptionHistory.getNumberOfEntries(); i++) {
            LoyaltyTransaction t = redemptionHistory.get(i);
            if (!confirmationBelongsToMember(guest, t.getConfirmationNumber())
                    || !"ACTIVE".equals(t.getStatus()))
                continue;

            if (checkAndUpdateExpiry(t, now))
                continue;
            if (++count == choice) {
                t.setStatus("USED");
                return RESULT_SUCCESS;
            }
        }
        return RESULT_INVALID_SELECTION;
    }

    public int useInventoryItem(String confirmationNumber, int choice) {
        return useRedeemedItemByChoice(findGuest(confirmationNumber), choice);
    }

    public int restockAllRewardItems() {
        ListInterface<RewardItem> items = getRewardCatalog();
        for (int i = 0; i < items.getNumberOfEntries(); i++)
            items.get(i).resetStockToDefault();
        return items.getNumberOfEntries();
    }

    private void refreshRewardExpiry(Guest guest) {
        if (guest == null)
            return;
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < redemptionHistory.getNumberOfEntries(); i++) {
            LoyaltyTransaction t = redemptionHistory.get(i);
            if (confirmationBelongsToMember(guest, t.getConfirmationNumber()))
                checkAndUpdateExpiry(t, now);
        }
    }

    public int getExpiredRewardCount(Guest guest) {
        refreshRewardExpiry(guest);
        int count = 0;
        for (int i = 0; i < redemptionHistory.getNumberOfEntries(); i++) {
            LoyaltyTransaction t = redemptionHistory.get(i);
            if (confirmationBelongsToMember(guest, t.getConfirmationNumber())
                    && "EXPIRED".equalsIgnoreCase(t.getStatus()))
                count++;
        }
        return count;
    }

    public int getExpiringSoonRewardCount(Guest guest) {
        refreshRewardExpiry(guest);
        int count = 0;
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < redemptionHistory.getNumberOfEntries(); i++) {
            LoyaltyTransaction t = redemptionHistory.get(i);
            if (!confirmationBelongsToMember(guest, t.getConfirmationNumber())
                    || !"ACTIVE".equalsIgnoreCase(t.getStatus()))
                continue;
            LocalDateTime end = t.getEndTime();
            long seconds = Duration.between(now, end).getSeconds();
            if (seconds >= 0 && seconds <= 60)
                count++;
        }
        return count;
    }

    public String[] getUpcomingDiscountRewardNames(Guest guest) {
        ListInterface<String> offers = new MyArrayList<>();
        ListInterface<RewardItem> catalog = rewardCatalog.inOrderTraversal();
        for (int i = 0; i < catalog.getNumberOfEntries(); i++) {
            int redemptionCount = getGuestItemRedemptionCount(guest, catalog.get(i).getItemName());
            if (redemptionCount > 0 && (redemptionCount + 1) % 5 == 0)
                offers.add(catalog.get(i).getItemName());
        }
        String[] result = new String[offers.getNumberOfEntries()];
        for (int i = 0; i < result.length; i++)
            result[i] = offers.get(i);
        return result;
    }

    public ListInterface<PointBatch> getFilteredPointBatchReport(String status, boolean ascending) {
        return getFilteredPointBatchReport(status, "ALL", 0, ascending);
    }

    /** Multi-criteria point-batch audit: status, stay confirmation and point amount. */
    public ListInterface<PointBatch> getFilteredPointBatchReport(String status, String confirmationFilter,
            int minimumAbsolutePoints, boolean ascending) {
        return getFilteredPointBatchReport(status, confirmationFilter, minimumAbsolutePoints,
                "ALL", "ALL", ascending);
    }

    /**
     * Multi-criteria point-batch audit: status, stay confirmation, point amount and
     * inclusive earned-date range.
     */
    public ListInterface<PointBatch> getFilteredPointBatchReport(String status, String confirmationFilter,
            int minimumAbsolutePoints, String earnedStartDate, String earnedEndDate,
            boolean ascending) {
        ListInterface<PointBatch> result = new MyArrayList<>();
        LocalDate start = parseOptionalReportDate(earnedStartDate);
        LocalDate end = parseOptionalReportDate(earnedEndDate);
        if (isInvalidOptionalReportDate(earnedStartDate, start)
                || isInvalidOptionalReportDate(earnedEndDate, end)
                || (start != null && end != null && start.isAfter(end)))
            return result;
        if (masterGuestTree != null) {
            ListInterface<Guest> guests = masterGuestTree.inOrderTraversal();
            for (int i = 0; i < guests.getNumberOfEntries(); i++)
                checkExpiredPoints(guests.get(i));
        }

        for (int i = 0; i < pointBatches.getNumberOfEntries(); i++) {
            PointBatch batch = pointBatches.get(i);
            String confirmation = batch.getConfirmationNumber();
            boolean statusMatches = "ALL".equalsIgnoreCase(status)
                    || batch.getStatus().equalsIgnoreCase(status);
            boolean confirmationMatches = confirmationFilter == null
                    || confirmationFilter.trim().isEmpty()
                    || "ALL".equalsIgnoreCase(confirmationFilter)
                    || confirmation.equalsIgnoreCase(confirmationFilter.trim());
            boolean pointsMatch = batch.getPoints() >= Math.max(0, minimumAbsolutePoints);
            LocalDate earnedDate = batch.getEarnedTime().toLocalDate();
            boolean earnedDateMatches = (start == null || !earnedDate.isBefore(start))
                    && (end == null || !earnedDate.isAfter(end));
            if (statusMatches && confirmationMatches && pointsMatch && earnedDateMatches) {
                result.add(batch);
            }
        }

        result.sort((a, b) -> {
            return ascending ? Integer.compare(a.getPoints(), b.getPoints())
                    : Integer.compare(b.getPoints(), a.getPoints());
        });
        return result;
    }

    private LocalDate parseOptionalReportDate(String value) {
        if (value == null || value.trim().isEmpty() || "ALL".equalsIgnoreCase(value.trim()))
            return null;
        try {
            return LocalDate.parse(value.trim());
        } catch (java.time.format.DateTimeParseException ignored) {
            return null;
        }
    }

    private boolean isInvalidOptionalReportDate(String value, LocalDate parsedDate) {
        return value != null && !value.trim().isEmpty() && !"ALL".equalsIgnoreCase(value.trim())
                && parsedDate == null;
    }

    /** Returns the filtered point-batch report without exposing the internal List ADT. */
    public PointBatch[] getFilteredPointBatchReportArray(String status, boolean ascending) {
        return getFilteredPointBatchReportArray(status, "ALL", 0, ascending);
    }

    public PointBatch[] getFilteredPointBatchReportArray(String status, String confirmationFilter,
            int minimumAbsolutePoints, boolean ascending) {
        return getFilteredPointBatchReportArray(status, confirmationFilter, minimumAbsolutePoints,
                "ALL", "ALL", ascending);
    }

    public PointBatch[] getFilteredPointBatchReportArray(String status, String confirmationFilter,
            int minimumAbsolutePoints, String earnedStartDate, String earnedEndDate,
            boolean ascending) {
        ListInterface<PointBatch> report = getFilteredPointBatchReport(status, confirmationFilter,
                minimumAbsolutePoints, earnedStartDate, earnedEndDate, ascending);
        PointBatch[] result = new PointBatch[report.getNumberOfEntries()];
        for (int i = 0; i < result.length; i++)
            result[i] = report.get(i);
        return result;
    }

    public ListInterface<RewardItem> getFilteredRewardReport(int maxStock, boolean ascending) {
        return getFilteredRewardReport(maxStock, 0, ascending);
    }

    /** Multi-criteria reward report: stock ceiling and completed redemptions. */
    public ListInterface<RewardItem> getFilteredRewardReport(int maxStock, int minimumRedeemed,
            boolean ascending) {
        ListInterface<RewardItem> catalog = getRewardCatalog();
        ListInterface<RewardItem> result = new MyArrayList<>();

        for (int i = 0; i < catalog.getNumberOfEntries(); i++)
            if (catalog.get(i).getStockQuantity() <= maxStock
                    && catalog.get(i).getTotalRedeemed() >= Math.max(0, minimumRedeemed))
                result.add(catalog.get(i));

        result.sort((a, b) -> (ascending ? 1 : -1) * Integer.compare(a.getPointsCost(), b.getPointsCost()));
        return result;
    }

    /**
     * Returns the filtered reward report without exposing the internal List ADT.
     */
    public RewardItem[] getFilteredRewardReportArray(int maxStock, boolean ascending) {
        return getFilteredRewardReportArray(maxStock, 0, ascending);
    }

    public RewardItem[] getFilteredRewardReportArray(int maxStock, int minimumRedeemed,
            boolean ascending) {
        ListInterface<RewardItem> report = getFilteredRewardReport(maxStock, minimumRedeemed, ascending);
        RewardItem[] result = new RewardItem[report.getNumberOfEntries()];
        for (int i = 0; i < result.length; i++)
            result[i] = report.get(i);
        return result;
    }

    private boolean checkAndUpdateExpiry(LoyaltyTransaction t, LocalDateTime now) {
        if ("ACTIVE".equalsIgnoreCase(t.getStatus())) {
            if (now.isAfter(t.getEndTime())) {
                t.setStatus("EXPIRED");
                return true;
            }
        }
        return !"ACTIVE".equalsIgnoreCase(t.getStatus());
    }

    public int getActiveItemRedemptionCount(Guest guest, String itemName) {
        int count = 0;
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < redemptionHistory.getNumberOfEntries(); i++) {
            LoyaltyTransaction t = redemptionHistory.get(i);
            boolean guestMatches = guest == null
                    || confirmationBelongsToMember(guest, t.getConfirmationNumber());
            if (guestMatches && t.getItemName().equalsIgnoreCase(itemName)
                    && !checkAndUpdateExpiry(t, now))
                count++;
        }
        return count;
    }

    public int getExpiredItemRedemptionCount(String itemName) {
        int count = 0;
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < redemptionHistory.getNumberOfEntries(); i++) {
            LoyaltyTransaction t = redemptionHistory.get(i);
            if (t.getItemName().equalsIgnoreCase(itemName)) {
                checkAndUpdateExpiry(t, now);
                if ("EXPIRED".equalsIgnoreCase(t.getStatus()))
                    count++;
            }
        }
        return count;
    }
}
