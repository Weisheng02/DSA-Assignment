package control;

import adt.BSTInterface;
import adt.BinarySearchTree;
import adt.ListInterface;
import adt.MyArrayList;
import entity.Booking;
import entity.Guest;
import entity.LoyaltyTransaction;
import entity.RewardItem;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Author: Hock Siang
 * Loyalty & Rewards module business logic controller.
 */
public class LoyaltyController {
    private static final int POINT_VALIDITY_MINUTES = 1;
    private static final DateTimeFormatter BATCH_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static ListInterface<String> pointHistory = new MyArrayList<>();
    private static ListInterface<LoyaltyTransaction> redemptionHistory = new MyArrayList<>();
    private static ListInterface<String> dailyClaimedGuests = new MyArrayList<>();
    private BSTInterface<RewardItem> rewardCatalog = new BinarySearchTree<>();
    private BSTInterface<Guest> masterGuestTree;

    /** Minimal result type required by the existing Front Desk checkout hook. */
    public static class AwardResult {
        private final boolean success;
        private final int pointsAwarded;
        public AwardResult(boolean success, int pointsAwarded) {
            this.success = success;
            this.pointsAwarded = pointsAwarded;
        }
        public boolean isSuccess() { return success; }
        public int getPointsAwarded() { return pointsAwarded; }
    }

    public LoyaltyController() {
        initializeDefaultRewards();
    }

    /** Keeps the supplied Loyalty module connected to the shared Guest BST. */
    public LoyaltyController(BSTInterface<Guest> masterGuestTree) {
        this();
        this.masterGuestTree = masterGuestTree;
    }

    private Guest findGuest(String confirmationNumber) {
        if (masterGuestTree == null || confirmationNumber == null) return null;
        return masterGuestTree.search(new Guest("", confirmationNumber, "", 0));
    }

    /** Compatibility validation for the existing Front Desk checkout flow. */
    public AwardResult validateCheckoutAward(String confirmationNumber,
            String sourceReference, int points) {
        return new AwardResult(findGuest(confirmationNumber) != null && points >= 0, 0);
    }

    /** Sends a successful Front Desk checkout into the supplied point-history flow. */
    public AwardResult awardCheckoutPoints(String confirmationNumber,
            String sourceReference, int points) {
        Guest guest = findGuest(confirmationNumber);
        if (guest == null || points < 0) return new AwardResult(false, 0);
        recordPointTransaction(guest, "Checkout Reward (" + sourceReference + ")", points);
        return new AwardResult(true, points);
    }

    /** Existing transactions are tied to confirmation number, so no migration is needed. */
    public boolean migrateMemberIdentity(Guest guest, String newIcNo) {
        return guest != null && newIcNo != null && !newIcNo.trim().isEmpty();
    }

    public boolean isValidMenuChoice(String choice, int min, int max) {
        if ("0".equals(choice)) return true;
        try { int n = Integer.parseInt(choice); return n >= min && n <= max; }
        catch (NumberFormatException e) { return false; }
    }

    private String calculateTier(int exp) {
        if (exp >= 1200) return "Platinum";
        if (exp >= 500) return "Gold";
        if (exp >= 200) return "Silver";
        return "Standard";
    }

    public String getNextTierInfo(Guest guest) {
        int exp = guest.getLoyaltyExperiences();
        String tier = calculateTier(exp);
        guest.setLoyaltyTier(tier);
        return switch (tier) {
            case "Standard" -> exp + " / 200 EXP (Silver) | Need: " + Math.max(0, 200 - exp) + " EXP";
            case "Silver" -> exp + " / 500 EXP (Gold) | Need: " + Math.max(0, 500 - exp) + " EXP";
            case "Gold" -> exp + " / 1,200 EXP (Platinum) | Need: " + Math.max(0, 1200 - exp) + " EXP";
            default -> exp + " EXP (Max Tier Reached)";
        };
    }

    public void refreshPoints(Guest guest) { checkExpiredPoints(guest); }

    public void recordPointTransaction(Guest guest, String desc, int pts) {
        checkExpiredPoints(guest);
        guest.setLoyaltyPoints(guest.getLoyaltyPoints() + pts);
        if (pts > 0) guest.setLoyaltyExperiences(guest.getLoyaltyExperiences() + pts);
        
        LocalDateTime now = LocalDateTime.now();
        pointHistory.add(String.format("%d|%s|%s|%s|Conf: %s|%s", pts, now.format(BATCH_FMT), 
                now.plusMinutes(POINT_VALIDITY_MINUTES).format(BATCH_FMT), desc, guest.getConfirmationNumber(), pts > 0 ? "ACTIVE" : "DEDUCTION"));
    }

    private void checkExpiredPoints(Guest guest) {
        if (guest == null || guest.getConfirmationNumber() == null) return;
        LocalDateTime now = LocalDateTime.now();
        String conf = "Conf: " + guest.getConfirmationNumber();
        ListInterface<String> updated = new MyArrayList<>();

        for (int i = 0; i < pointHistory.getNumberOfEntries(); i++) {
            String rec = pointHistory.get(i);
            String[] p = rec.split("\\|");
            if (p.length >= 6 && "ACTIVE".equalsIgnoreCase(p[5]) && p[4].trim().equals(conf) && !"-".equals(p[2])) {
                try {
                    if (now.isAfter(LocalDateTime.parse(p[2], BATCH_FMT))) {
                        int pts = Integer.parseInt(p[0]);
                        guest.setLoyaltyPoints(Math.max(0, guest.getLoyaltyPoints() - pts));
                        updated.add(String.format("%s|%s|%s|%s|%s|EXPIRED", p[0], p[1], p[2], p[3], p[4]));
                        updated.add(String.format("-%d|%s|%s|%s (Expired)|%s|DEDUCTION", pts, p[1], p[2], p[3], p[4]));
                        continue;
                    }
                } catch (Exception ignored) {}
            }
            updated.add(rec);
        }
        pointHistory = updated;
    }

    public void processRoomBooking(Guest guest, Booking booking, boolean isCancel) {
        int pts = "Presidential Suite".equals(booking.getRoomType()) ? 30 : ("Deluxe Suite".equals(booking.getRoomType()) ? 20 : 10);
        recordPointTransaction(guest, (isCancel ? "Cancelled " : "") + booking.getRoomType() + " (" + booking.getBookingId() + ")", isCancel ? -pts : pts);
    }

    public String performDailyCheckIn(Guest guest) {
        String id = guest.getConfirmationNumber();
        for (int i = 0; i < dailyClaimedGuests.getNumberOfEntries(); i++)
            if (dailyClaimedGuests.get(i).equals(id)) return "WARNING: You have already claimed today.";
        dailyClaimedGuests.add(id);
        recordPointTransaction(guest, "Daily Check-In", 700);
        return "SUCCESS: Daily Check-In complete! +700 Points & EXP.";
    }

    public String getFormattedTransactionHistory(Guest guest) {
        checkExpiredPoints(guest);
        StringBuilder sb = new StringBuilder();
        int count = 0, len = pointHistory.getNumberOfEntries();
        String conf = "Conf: " + guest.getConfirmationNumber();

        for (int i = len - 1; i >= 0; i--) {
            String[] p = pointHistory.get(i).split("\\|");
            if (p.length >= 6 && p[4].trim().equals(conf)) {
                int pts = Integer.parseInt(p[0]);
                sb.append(String.format(" %-3d. %-13s | %-40s | %-8s: %s | Expires: %s\n",
                        ++count, (pts >= 0 ? "+" : "-") + Math.abs(pts) + " Points", p[3], 
                        "EXPIRED".equals(p[5]) ? "Earned" : "Date", p[1], p[2]));
            }
        }
        return count == 0 ? "No point transactions yet." : sb.toString();
    }

    public void initializeDefaultRewards() {
        rewardCatalog.add(new RewardItem("Free Coffee Drink", 20, 10, 1));
        rewardCatalog.add(new RewardItem("Complimentary Breakfast", 50, 5, 120));
        rewardCatalog.add(new RewardItem("Spa Discount Voucher", 100, 3, 120));
        rewardCatalog.add(new RewardItem("Free 20% Discount Dining", 500, 1, 120));
    }

    public ListInterface<RewardItem> getRewardCatalog() { return rewardCatalog.inOrderTraversal(); }

    private int countMatches(Guest guest, String itemName, boolean matchGuest) {
        int count = 0;
        for (int i = 0; i < redemptionHistory.getNumberOfEntries(); i++) {
            LoyaltyTransaction t = redemptionHistory.get(i);
            boolean matchesGuest = !matchGuest || t.getConfirmationNumber().equals(guest.getConfirmationNumber());
            if (matchesGuest && t.getItemName().equals(itemName)) count++;
        }
        return count;
    }

    private int getGuestItemRedemptionCount(Guest guest, String itemName) {
        return countMatches(guest, itemName, true);
    }

    public int getTotalItemRedemptionCount(String itemName) {
        return countMatches(null, itemName, false);
    }

    public String redeemRewardItem(Guest guest, RewardItem item) {
        int count = getGuestItemRedemptionCount(guest, item.getItemName());
        int cost = (count > 0 && count % 5 == 0) ? item.getPointsCost() / 2 : item.getPointsCost();

        if (guest.getLoyaltyPoints() < cost) return "ERROR: Insufficient points! (Required: " + cost + ")";
        if (item.getStockQuantity() <= 0) return "ERROR: Item out of stock!";

        recordPointTransaction(guest, "Redeemed: " + item.getItemName(), -cost);
        item.setStockQuantity(item.getStockQuantity() - 1);
        redemptionHistory.add(new LoyaltyTransaction(guest.getConfirmationNumber(), guest.getGuestName(), item.getItemName(), cost, item.getValidityMinutes()));
        return "SUCCESS: Redeemed '" + item.getItemName() + "' for " + cost + " points!";
    }

    public String getFormattedInventory(Guest guest) {
        StringBuilder sb = new StringBuilder();
        int count = 0;
        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < redemptionHistory.getNumberOfEntries(); i++) {
            LoyaltyTransaction t = redemptionHistory.get(i);
            if (t.getConfirmationNumber().equals(guest.getConfirmationNumber())) {
                checkAndUpdateExpiry(t, now);
                sb.append(String.format(" %-3d. %-25s (%s) | %-19s | %-19s | Status: %-8s\n",
                        ++count, t.getItemName(), t.getTransactionId(), t.getStartTimeFormatted(),
                        t.getEndTimeFormatted(), t.getStatus().replace("_ACK", "")));
            }
        }
        return count == 0 ? "No items in storage yet." : sb.toString();
    }

    public String useRedeemedItem(Guest guest, int choice) {
        int count = 0;
        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < redemptionHistory.getNumberOfEntries(); i++) {
            LoyaltyTransaction t = redemptionHistory.get(i);
            if (!t.getConfirmationNumber().equals(guest.getConfirmationNumber()) || !"ACTIVE".equals(t.getStatus())) continue;
            
            if (checkAndUpdateExpiry(t, now)) continue;
            if (++count == choice) {
                t.setStatus("USED");
                return "SUCCESS: Used '" + t.getItemName() + "'!";
            }
        }
        return "ERROR: Invalid item selection.";
    }

    public String resetAllRewardStocks() {
        ListInterface<RewardItem> items = getRewardCatalog();
        for (int i = 0; i < items.getNumberOfEntries(); i++) items.get(i).resetStockToDefault();
        return "All reward item been successfully restocks!";
    }

    public String checkNotifications(Guest guest) {
        StringBuilder sb = new StringBuilder("                    NOTIFICATION ALERTS\n------------------------------------------------------------------------\n");
        boolean alert = false;
        String conf = guest.getConfirmationNumber();
        String tier = guest.getLoyaltyTier() == null ? "standard" : guest.getLoyaltyTier().toLowerCase();
        String target = calculateTier(guest.getLoyaltyExperiences());

        if (!target.equalsIgnoreCase(tier) && ((tier.equals("standard") && "silver".equalsIgnoreCase(target)) ||
                 (tier.equals("silver") && "gold".equalsIgnoreCase(target)) || (tier.equals("gold") && "platinum".equalsIgnoreCase(target)))) {
            sb.append(" [ALERT] Tier upgrade to ").append(target).append(" available!\n");
            alert = true;
        }

        LocalDateTime now = LocalDateTime.now();
        int expired = 0, expiring = 0;

        for (int i = 0; i < redemptionHistory.getNumberOfEntries(); i++) {
            LoyaltyTransaction t = redemptionHistory.get(i);
            if (!t.getConfirmationNumber().equals(conf)) continue;

            LocalDateTime end = LocalDateTime.parse(t.getEndTimeFormatted(), BATCH_FMT);
            if ("ACTIVE".equals(t.getStatus()) && now.isAfter(end)) t.setStatus("EXPIRED");

            if ("EXPIRED".equals(t.getStatus())) expired++;
            else if ("ACTIVE".equals(t.getStatus()) && Duration.between(now, end).toSeconds() <= 60) expiring++;
        }

        if (expired > 0) { sb.append(" [EXPIRY ALERT] ").append(expired).append(" redeemed item(s) expired.\n"); alert = true; }
        if (expiring > 0) { sb.append(" [EXPIRY ALERT] ").append(expiring).append(" redeemed item(s) expiring within 1 minute.\n"); alert = true; }

        ListInterface<RewardItem> catalog = rewardCatalog.inOrderTraversal();
        for (int i = 0; i < catalog.getNumberOfEntries(); i++) {
            if (getGuestItemRedemptionCount(guest, catalog.get(i).getItemName()) >= 4) {
                sb.append(" [ALERT] Personalized Offer: 50% discount on next ").append(catalog.get(i).getItemName()).append("!\n");
                alert = true;
            }
        }

        if (!alert) sb.append(" No new notifications.\n");
        return sb.append("------------------------------------------------------------------------").toString();
    }

    public ListInterface<String> getFilteredPointReport(String status, boolean ascending) {
        ListInterface<String> result = new MyArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        ListInterface<String> updatedHistory = new MyArrayList<>();

        for (int i = 0; i < pointHistory.getNumberOfEntries(); i++) {
            String rec = pointHistory.get(i);
            String[] p = rec.split("\\|");
            if (p.length >= 6 && "ACTIVE".equalsIgnoreCase(p[5]) && !"-".equals(p[2])) {
                try {
                    if (now.isAfter(LocalDateTime.parse(p[2], BATCH_FMT)))
                        rec = String.format("%s|%s|%s|%s|%s|EXPIRED", p[0], p[1], p[2], p[3], p[4]);
                } catch (Exception ignored) {}
            }
            updatedHistory.add(rec);
        }
        pointHistory = updatedHistory;

        for (int i = 0; i < pointHistory.getNumberOfEntries(); i++) {
            String[] p = pointHistory.get(i).split("\\|");
            if (p.length >= 6 && ("ALL".equalsIgnoreCase(status) || p[5].equalsIgnoreCase(status)))
                result.add(pointHistory.get(i));
        }

        result.sort((a, b) -> {
            int x = Integer.parseInt(a.split("\\|")[0]), y = Integer.parseInt(b.split("\\|")[0]);
            return ascending ? Integer.compare(x, y) : Integer.compare(y, x);
        });
        return result;
    }

    public ListInterface<RewardItem> getFilteredRewardReport(int maxStock, boolean ascending) {
        ListInterface<RewardItem> catalog = getRewardCatalog();
        ListInterface<RewardItem> result = new MyArrayList<>();
        
        for (int i = 0; i < catalog.getNumberOfEntries(); i++)
            if (catalog.get(i).getStockQuantity() <= maxStock) result.add(catalog.get(i));
        
        result.sort((a, b) -> (ascending ? 1 : -1) * Integer.compare(a.getPointsCost(), b.getPointsCost()));
        return result;
    }
    
    private boolean checkAndUpdateExpiry(LoyaltyTransaction t, LocalDateTime now) {
        if ("ACTIVE".equalsIgnoreCase(t.getStatus())) {
            try {
                if (now.isAfter(LocalDateTime.parse(t.getEndTimeFormatted(), BATCH_FMT))) {
                    t.setStatus("EXPIRED");
                    return true;
                }
            } catch (Exception ignored) {}
        }
        return !"ACTIVE".equalsIgnoreCase(t.getStatus());
    }

    public int getActiveItemRedemptionCount(Guest guest, String itemName) {
        int count = 0;
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < redemptionHistory.getNumberOfEntries(); i++) {
            LoyaltyTransaction t = redemptionHistory.get(i);
            if (t.getItemName().equalsIgnoreCase(itemName) && !checkAndUpdateExpiry(t, now)) count++;
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
                if (!"ACTIVE".equalsIgnoreCase(t.getStatus())) count++;
            }
        }
        return count;
    }
}
