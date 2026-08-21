package boundary;

import control.LoyaltyController;
import entity.Guest;
import entity.LoyaltyTransaction;
import entity.RewardItem;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

/**
 * Author: Tan Hock Siang
 * Console boundary for Loyalty & Rewards.
 * It collects input and formats the domain data/results returned by Control.
 */
public class LoyaltyUI {
    private static final DateTimeFormatter TRANSACTION_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final LoyaltyController controller;

    public LoyaltyUI(LoyaltyController controller) {
        if (controller == null)
            throw new IllegalArgumentException("LoyaltyController is required.");
        this.controller = controller;
    }

    public void displayMenu(Scanner scanner) {
        Scanner scan = scanner != null ? scanner : new Scanner(System.in);
        while (true) {
            System.out.println("\n-----------------------------------------------");
            System.out.println("          LOYALTY & REWARDS MANAGEMENT           ");
            System.out.println("-----------------------------------------------");
            System.out.print("Enter Confirmation Number (0 to exit): ");
            String confirmationNumber = scan.nextLine().trim();

            if ("0".equals(confirmationNumber)) {
                System.out.println("Returning to main menu...");
                return;
            }
            if (confirmationNumber.isEmpty()) {
                System.out.println("Wrong input");
                System.out.println("Enter a confirmation number, or 0 to return.");
                continue;
            }
            if (!controller.memberExists(confirmationNumber)) {
                System.out.println("Wrong input");
                System.out.println("No guest found. Enter again, or 0 to return.");
                continue;
            }
            displayMemberMenu(scan, confirmationNumber);
        }
    }

    private void displayMemberMenu(Scanner scan, String confirmationNumber) {
        while (true) {
            displayMemberProfile(confirmationNumber);
            System.out.println("1. Daily Check-In (+" + LoyaltyController.DAILY_CHECK_IN_POINTS + " pts)");
            System.out.println("2. Reward Catalog (Point Exchange)");
            System.out.println("3. Item Storage & Inventory");
            System.out.println("4. Point Transaction & Expiry History");
            System.out.println("5. Settle Pending Reward Queue");
            System.out.println("6. Restock Item Stock");
            System.out.println("7. Member Point Activity & Expiry Audit Report");
            System.out.println("8. Reward Item Stock & Performance Report");
            System.out.println("0. Back to Confirmation Menu");
            System.out.println("========================================================================");
            System.out.print("Select an option (0-8) > ");

            switch (readMenuChoice(scan, 0, 8)) {
                case 1:
                    displayDailyCheckInResult(controller.claimDailyCheckIn(confirmationNumber));
                    pauseForEnter(scan);
                    break;
                case 2:
                    displayRewardCatalog(scan, confirmationNumber);
                    break;
                case 3:
                    displayInventory(scan, confirmationNumber);
                    break;
                case 4:
                    displayTransactionHistory(confirmationNumber);
                    pauseForEnter(scan);
                    break;
                case 5:
                    displaySettledTransactions(controller.settlePendingRewardRedemptionsData());
                    pauseForEnter(scan);
                    break;
                case 6:
                    controller.restockAllRewardItems();
                    System.out.println("All reward items were successfully restocked!");
                    pauseForEnter(scan);
                    break;
                case 7:
                    if (displayPointReport(scan))
                        pauseForEnter(scan);
                    break;
                case 8:
                    if (displayRewardReport(scan))
                        pauseForEnter(scan);
                    break;
                case 0:
                    System.out.println("Exiting guest profile...");
                    return;
                default:
                    break;
            }
        }
    }

    private void displayMemberProfile(String confirmationNumber) {
        Guest guest = controller.findGuestByConfirmationNumber(confirmationNumber);
        if (guest == null)
            return;
        controller.refreshPoints(guest);

        String currentTier = guest.getLoyaltyTier() == null ? "Standard" : guest.getLoyaltyTier();
        String calculatedTier = controller.getCalculatedTier(guest);
        int expired = controller.getExpiredRewardCount(guest);
        int expiring = controller.getExpiringSoonRewardCount(guest);
        String[] offers = controller.getUpcomingDiscountRewardNames(guest);

        System.out.println("\n========================================================================");
        System.out.println("                MEMBER PROFILE: " + guest.getGuestName() + " - "
                + guest.getConfirmationNumber());
        System.out.println("========================================================================");
        System.out.println("                    NOTIFICATION ALERTS");
        System.out.println("------------------------------------------------------------------------");
        boolean hasAlert = false;
        if (!calculatedTier.equalsIgnoreCase(currentTier)) {
            System.out.println(" [ALERT] Tier upgrade from " + currentTier + " to " + calculatedTier + " available!");
            hasAlert = true;
        }
        if (expired > 0) {
            System.out.println(" [EXPIRY ALERT] " + expired
                    + " redeemed item(s) expired. View Option 3 > Expired Items.");
            hasAlert = true;
        }
        if (expiring > 0) {
            System.out.println(" [EXPIRY ALERT] " + expiring
                    + " redeemed item(s) expiring within 1 minute.");
            hasAlert = true;
        }
        for (String itemName : offers) {
            System.out.println(" [ALERT] Personalized Offer: 50% discount on next " + itemName + "!");
            hasAlert = true;
        }
        if (!hasAlert)
            System.out.println(" No new notifications.");
        System.out.println("------------------------------------------------------------------------");

        controller.updateMemberTier(guest);
        int exp = guest.getLoyaltyExperiences();
        int threshold = controller.getNextTierThreshold(guest);
        System.out.println("Current Tier    : " + guest.getLoyaltyTier());
        System.out.println("Loyalty Points  : " + guest.getLoyaltyPoints());
        if (threshold == -1) {
            System.out.println("Loyalty EXP     : " + exp + " EXP (Max Tier Reached)");
        } else {
            String nextTier = threshold == 200 ? "Silver" : threshold == 500 ? "Gold" : "Platinum";
            String shownThreshold = threshold == 1200 ? "1,200" : String.valueOf(threshold);
            System.out.println("Loyalty EXP     : " + exp + " / " + shownThreshold + " EXP (" + nextTier
                    + ") | Need: " + Math.max(0, threshold - exp) + " EXP");
        }
        System.out.println("------------------------------------------------------------------------");
    }

    private void displayDailyCheckInResult(int result) {
        if (result == LoyaltyController.RESULT_SUCCESS) {
            System.out.println("\nSUCCESS: Daily Check-In complete! +"
                    + LoyaltyController.DAILY_CHECK_IN_POINTS + " Points & EXP.");
        } else if (result == LoyaltyController.RESULT_ALREADY_CLAIMED) {
            System.out.println("\nWARNING: You have already claimed today.");
        } else {
            System.out.println("\nERROR: Guest not found.");
        }
    }

    private void displayTransactionHistory(String confirmationNumber) {
        String[] records = controller.getMemberPointHistoryRecords(confirmationNumber);
        System.out.println("\nPOINT TRANSACTION & EXPIRY HISTORY");
        if (records.length == 0) {
            System.out.println("No point transactions yet.");
            return;
        }
        int[] widths = { 4, 12, 36, 19, 19, 10 };
        ConsoleTable.printHeader(new String[] { "No.", "Points", "Description", "Earned", "Expiry", "Status" },
                widths);
        for (int i = 0; i < records.length; i++) {
            String[] point = records[i].split("\\|");
            int points = Integer.parseInt(point[0]);
            ConsoleTable.printRow(new String[] { String.valueOf(i + 1),
                    (points >= 0 ? "+" : "-") + Math.abs(points), point[3], point[1],
                    "-".equals(point[2]) ? "N/A" : point[2], point[5] }, widths);
        }
        ConsoleTable.printFooter(widths);
    }

    private void displaySettledTransactions(LoyaltyTransaction[] processed) {
        if (processed.length == 0) {
            System.out.println("No pending item transactions in the queue.");
            return;
        }
        System.out.println("PROCESSING PENDING ITEM QUEUE");
        int[] widths = { 4, 12, 24, 18, 10 };
        ConsoleTable.printHeader(new String[] { "No.", "Confirm No", "Reward Item", "Guest Name", "Tier" }, widths);
        for (int i = 0; i < processed.length; i++) {
            LoyaltyTransaction transaction = processed[i];
            Guest guest = controller.findGuestByConfirmationNumber(transaction.getConfirmationNumber());
            String tier = guest == null ? "N/A" : guest.getLoyaltyTier();
            ConsoleTable.printRow(new String[] { String.valueOf(i + 1), transaction.getConfirmationNumber(),
                    transaction.getItemName(), transaction.getGuestName(), tier }, widths);
        }
        ConsoleTable.printFooter(widths);
        System.out.println("All pending items successfully dequeued and stored!");
    }

    private void displayRewardCatalog(Scanner scan, String confirmationNumber) {
        while (true) {
            Guest guest = controller.findGuestByConfirmationNumber(confirmationNumber);
            RewardItem[] items = controller.getRewardCatalogArray();
            System.out.println("\nREWARD CATALOG REDEMPTION");
            System.out.println("Available Points: " + guest.getLoyaltyPoints() + " pts");
            int[] widths = { 4, 30, 10, 8, 12 };
            ConsoleTable.printHeader(new String[] { "No.", "Reward Description", "Cost", "Stock", "Validity" },
                    widths);
            for (int i = 0; i < items.length; i++) {
                RewardItem item = items[i];
                ConsoleTable.printRow(new String[] { String.valueOf(i + 1), item.getItemName(),
                        item.getPointsCost() + " pts", String.valueOf(item.getStockQuantity()),
                        item.getValidityMinutes() + " mins" }, widths);
            }
            ConsoleTable.printFooter(widths);
            System.out.println("0. Back to Profile Menu");
            System.out.print("Select item number to redeem > ");
            int choice = readMenuChoice(scan, 0, Integer.MAX_VALUE);
            if (choice == 0)
                return;
            System.out.println("\n------------------------------------------------------------------------");
            int requiredCost = controller.getEffectiveRewardCost(confirmationNumber, choice);
            int result = controller.requestRewardRedemption(confirmationNumber, choice);
            if (result != LoyaltyController.RESULT_SUCCESS)
                System.out.println("Wrong input");
            if (result == LoyaltyController.RESULT_SUCCESS) {
                System.out.println(" SUCCESS: Item '" + items[choice - 1].getItemName()
                        + "' added to transaction queue. Please proceed to Option 5 to process.");
            } else if (result == LoyaltyController.RESULT_INSUFFICIENT_POINTS) {
                System.out.println(" ERROR: Insufficient points! (Required: " + requiredCost + ")");
            } else if (result == LoyaltyController.RESULT_OUT_OF_STOCK) {
                System.out.println(" ERROR: Item out of stock!");
            } else if (result == LoyaltyController.RESULT_INVALID_SELECTION) {
                System.out.println(" ERROR: Invalid item selection.");
            } else {
                System.out.println(" ERROR: Guest not found.");
            }
            System.out.println("------------------------------------------------------------------------");
            pauseForEnter(scan);
        }
    }

    private void displayInventory(Scanner scan, String confirmationNumber) {
        while (true) {
            System.out.println("\nREDEEMED ITEM STORAGE & HISTORY");
            System.out.println("1. View / Use Active Items");
            System.out.println("2. View Used Items");
            System.out.println("3. View Expired Items");
            System.out.println("0. Return");
            System.out.print("Enter choice (0-3): ");
            int choice = readMenuChoice(scan, 0, 3);
            if (choice == 0)
                return;
            if (choice == 1)
                displayActiveInventory(scan, confirmationNumber);
            else if (choice == 2)
                displayRedemptionRecordsByStatus(scan, confirmationNumber, "USED");
            else
                displayRedemptionRecordsByStatus(scan, confirmationNumber, "EXPIRED");
        }
    }

    private void displayActiveInventory(Scanner scan, String confirmationNumber) {
        LoyaltyTransaction[] items = controller.getActiveInventoryArray(confirmationNumber);
        System.out.println("\nACTIVE REDEEMED ITEMS");
        if (items.length == 0) {
            System.out.println("No active redeemed items are available to use.");
            pauseForEnter(scan);
            return;
        }
        int[] widths = { 4, 24, 10, 19, 19 };
        ConsoleTable.printHeader(new String[] { "No.", "Reward Item", "Txn ID", "Start Time", "Expiry Time" },
                widths);
        for (int i = 0; i < items.length; i++) {
            LoyaltyTransaction item = items[i];
            ConsoleTable.printRow(new String[] { String.valueOf(i + 1), item.getItemName(),
                    item.getTransactionId(), item.getStartTime().format(TRANSACTION_TIME_FORMAT),
                    item.getEndTime().format(TRANSACTION_TIME_FORMAT) }, widths);
        }
        ConsoleTable.printFooter(widths);
        System.out.print("Select item to use (0 to return): ");
        int choice = readMenuChoice(scan, 0, items.length);
        if (choice == 0)
            return;
        String itemName = items[choice - 1].getItemName();
        int result = controller.useInventoryItem(confirmationNumber, choice);
        if (result != LoyaltyController.RESULT_SUCCESS)
            System.out.println("Wrong input");
        System.out.println(result == LoyaltyController.RESULT_SUCCESS
                ? "SUCCESS: Used '" + itemName + "'!"
                : "ERROR: Item is no longer active. Please review the refreshed inventory.");
        pauseForEnter(scan);
    }

    private void displayRedemptionRecordsByStatus(Scanner scan, String confirmationNumber, String status) {
        LoyaltyTransaction[] history = controller.getMemberRedemptionHistoryArray(confirmationNumber);
        int count = 0;
        for (LoyaltyTransaction item : history)
            if (status.equalsIgnoreCase(item.getStatus()))
                count++;
        System.out.println("\n" + status + " REDEEMED ITEMS");
        if (count == 0) {
            System.out.println("No " + status.toLowerCase() + " redeemed items found.");
            pauseForEnter(scan);
            return;
        }
        int[] widths = { 4, 24, 10, 12, 19, 19 };
        ConsoleTable.printHeader(new String[] { "No.", "Reward Item", "Txn ID", "Points", "Start Time", "End Time" },
                widths);
        int number = 0;
        for (LoyaltyTransaction item : history) {
            if (!status.equalsIgnoreCase(item.getStatus()))
                continue;
            ConsoleTable.printRow(new String[] { String.valueOf(++number), item.getItemName(),
                    item.getTransactionId(), String.valueOf(item.getPointsSpent()),
                    item.getStartTime().format(TRANSACTION_TIME_FORMAT),
                    item.getEndTime().format(TRANSACTION_TIME_FORMAT) }, widths);
        }
        ConsoleTable.printFooter(widths);
        pauseForEnter(scan);
    }

    private boolean displayPointReport(Scanner scan) {
        System.out.println("Status filter:");
        System.out.println("1. Active");
        System.out.println("2. Consumed");
        System.out.println("3. Expired");
        System.out.println("4. Deduction");
        System.out.println("5. All");
        System.out.print("Enter status number (1-5, or 0 to return): ");
        int statusChoice = readMenuChoice(scan, 0, 5);
        if (statusChoice == 0)
            return false;
        String[] statuses = { "ACTIVE", "CONSUMED", "EXPIRED", "DEDUCTION", "ALL" };
        String status = statuses[statusChoice - 1];

        System.out.println("Confirmation filter:");
        System.out.println("1. All confirmations");
        System.out.println("2. Enter an 8-digit confirmation number");
        System.out.println("0. Return");
        System.out.print("Enter choice (0-2): ");
        int confirmationChoice = readMenuChoice(scan, 0, 2);
        if (confirmationChoice == 0)
            return false;
        String confirmationFilter = "ALL";
        if (confirmationChoice == 2) {
            while (true) {
                System.out.print("Enter 8-digit confirmation number (0 to return): ");
                confirmationFilter = scan.nextLine().trim();
                if ("0".equals(confirmationFilter))
                    return false;
                if (confirmationFilter.matches("\\d{8}"))
                    break;
                System.out.println("Wrong input");
            }
        }
        int minimumPoints;
        while (true) {
            System.out.print("Minimum absolute point amount (0 for no minimum): ");
            minimumPoints = readIntInput(scan);
            if (minimumPoints >= 0)
                break;
            System.out.println("Wrong input");
        }
        String startDate = readDateFilter(scan, "Earned start date");
        if (startDate == null)
            return false;
        String endDate;
        while (true) {
            endDate = readDateFilter(scan, "Earned end date");
            if (endDate == null)
                return false;
            if (!"ALL".equals(startDate) && !"ALL".equals(endDate)
                    && !LocalDate.parse(startDate).isAfter(LocalDate.parse(endDate)))
                break;
            if ("ALL".equals(startDate) || "ALL".equals(endDate))
                break;
            System.out.println("Wrong input");
            System.out.println("End date cannot be before start date.");
        }
        System.out.print("Sort by points (1=Ascending, 2=Descending, 0=Return): ");
        int sort = readMenuChoice(scan, 0, 2);
        if (sort == 0)
            return false;
        boolean ascending = sort == 1;
        String[] report = controller.getFilteredPointReportArray(status, confirmationFilter,
                minimumPoints, startDate, endDate, ascending);
        System.out.println("\nPOINT ACTIVITY & EXPIRY REPORT");
        if (report.length == 0) {
            System.out.println("No matching records.");
        } else {
            int[] widths = { 12, 8, 36, 19, 19, 10 };
            ConsoleTable.printHeader(new String[] { "Confirm No", "Points", "Description", "Earned", "Expiry",
                    "Status" }, widths);
            for (String record : report) {
                String[] point = record.split("\\|");
                ConsoleTable.printRow(new String[] { point[4].replace("Conf: ", ""),
                        (Integer.parseInt(point[0]) >= 0 ? "+" : "") + point[0],
                        point[3], point[1], point[2], point[5] }, widths);
            }
            ConsoleTable.printFooter(widths);
        }
        return true;
    }

    private boolean displayRewardReport(Scanner scan) {
        int maxStock;
        while (true) {
            System.out.print("\nMaximum Stock Filter (>0, 0 to return): ");
            maxStock = readIntInput(scan);
            if (maxStock == 0)
                return false;
            if (maxStock > 0)
                break;
            System.out.println("Wrong input");
        }
        int minimumRedeemed;
        while (true) {
            System.out.print("Minimum completed redemptions (0 for no minimum): ");
            minimumRedeemed = readIntInput(scan);
            if (minimumRedeemed >= 0)
                break;
            System.out.println("Wrong input");
        }
        System.out.print("Sort by Points Cost? (1=Ascending, 2=Descending, 0=Return): ");
        int sort = readMenuChoice(scan, 0, 2);
        if (sort == 0)
            return false;
        boolean ascending = sort == 1;
        RewardItem[] report = controller.getFilteredRewardReportArray(maxStock, minimumRedeemed, ascending);
        System.out.println("\nREWARD STOCK & PERFORMANCE REPORT");
        int[] widths = { 28, 8, 7, 10, 8, 8 };
        ConsoleTable.printHeader(new String[] { "Reward Item", "Cost", "Stock", "Redeemed", "Active", "Expired" },
                widths);
        int redeemed = 0, active = 0, expired = 0;
        for (RewardItem item : report) {
            int itemRedeemed = item.getTotalRedeemed();
            int itemActive = controller.getActiveItemRedemptionCount(null, item.getItemName());
            int itemExpired = controller.getExpiredItemRedemptionCount(item.getItemName());
            redeemed += itemRedeemed;
            active += itemActive;
            expired += itemExpired;
            ConsoleTable.printRow(new String[] { item.getItemName(), String.valueOf(item.getPointsCost()),
                    String.valueOf(item.getStockQuantity()), String.valueOf(itemRedeemed),
                    String.valueOf(itemActive), String.valueOf(itemExpired) }, widths);
        }
        ConsoleTable.printFooter(widths);
        if (report.length == 0) {
            System.out.println("No items match the stock criteria.");
        } else {
            System.out.printf("Summary: Showing %d item(s) | Total: %d Redeemed | Active: %d | Expired: %d%n",
                    report.length, redeemed, active, expired);
        }
        return true;
    }

    private int readMenuChoice(Scanner scan, int min, int max) {
        while (true) {
            try {
                int value = Integer.parseInt(scan.nextLine().trim());
                if (value >= min && value <= max)
                    return value;
            } catch (NumberFormatException ignored) {
                // Display the common validation message below.
            }
            System.out.println("Wrong input");
            System.out.print("Enter again, or enter 0 to return: ");
        }
    }

    private int readIntInput(Scanner scan) {
        while (true) {
            try {
                return Integer.parseInt(scan.nextLine().trim());
            } catch (NumberFormatException ignored) {
                System.out.println("Wrong input");
                System.out.print("Enter again, or enter 0 to return: ");
            }
        }
    }

    private String readDateFilter(Scanner scan, String label) {
        System.out.println(label + " filter:");
        System.out.println("1. All dates");
        System.out.println("2. Enter a date");
        System.out.println("0. Return");
        System.out.print("Enter choice (0-2): ");
        int choice = readMenuChoice(scan, 0, 2);
        if (choice == 0)
            return null;
        if (choice == 1)
            return "ALL";

        while (true) {
            System.out.print("Enter date (YYYY-MM-DD, or 0 to return): ");
            String value = scan.nextLine().trim();
            if ("0".equals(value))
                return null;
            try {
                LocalDate.parse(value);
                return value;
            } catch (DateTimeParseException ignored) {
                System.out.println("Wrong input");
            }
        }
    }

    private void pauseForEnter(Scanner scan) {
        System.out.print("\nPress Enter to return to the previous menu...");
        scan.nextLine();
    }
}
