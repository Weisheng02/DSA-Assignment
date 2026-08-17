package boundary;

import control.LoyaltyController;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

/**
 * Author: Hock Siang
 * Console boundary for member loyalty, reward redemption and management reports.
 * All searching, ADT traversal and business mutations are delegated to Control.
 */
public class LoyaltyUI {
    private final LoyaltyController controller;
    private Scanner scanner;

    public LoyaltyUI() {
        this(new LoyaltyController());
    }

    public LoyaltyUI(LoyaltyController controller) {
        if (controller == null) throw new IllegalArgumentException("LoyaltyController is required.");
        this.controller = controller;
    }

    public void displayMenu() {
        displayMenu(new Scanner(System.in));
    }

    public void displayMenu(Scanner scanner) {
        this.scanner = scanner != null ? scanner : new Scanner(System.in);
        int choice;
        do {
            System.out.println("\n============================================================");
            System.out.println("             LOYALTY & REWARDS SERVICE");
            System.out.println("============================================================");
            System.out.println("1. Member Loyalty Service");
            System.out.println("2. Report: Member Point Activity");
            System.out.println("3. Report: Reward Stock & Performance");
            System.out.println("4. Management: Restock Reward Catalog");
            System.out.println("0. Back to Main Menu");
            System.out.println("------------------------------------------------------------");
            choice = readInt("Select an option (0-4): ", 0, 4);

            switch (choice) {
                case 1:
                    openMemberService();
                    break;
                case 2:
                    displayPointActivityReport();
                    break;
                case 3:
                    displayRewardPerformanceReport();
                    break;
                case 4:
                    restockRewards();
                    break;
                case 0:
                    System.out.println("Returning to main menu...");
                    break;
                default:
                    break;
            }
        } while (choice != 0);
    }

    private void openMemberService() {
        System.out.print("Enter member confirmation number: ");
        String confirmation = scanner.nextLine().trim();
        if (!controller.memberExists(confirmation)) {
            System.out.println("Error: No member found for confirmation number '" + confirmation + "'.");
            return;
        }

        int choice;
        do {
            System.out.println("\n============================================================");
            System.out.println("                 MEMBER LOYALTY SERVICE");
            System.out.println("============================================================");
            System.out.println(controller.getMemberProfileText(confirmation));
            System.out.println("------------------------------------------------------------");
            System.out.println(controller.getNotificationsText(confirmation));
            System.out.println("------------------------------------------------------------");
            System.out.println("1. Refresh Member Profile");
            System.out.println("2. Daily Check-In (+10 points and EXP)");
            System.out.println("3. View Reward Catalog");
            System.out.println("4. Redeem Reward");
            System.out.println("5. View Redeemed Reward Inventory");
            System.out.println("6. Use Redeemed Reward");
            System.out.println("7. View Point Transaction History");
            System.out.println("0. Back to Loyalty Menu");
            choice = readInt("Select an option (0-7): ", 0, 7);

            switch (choice) {
                case 1:
                    System.out.println("\n" + controller.getMemberProfileText(confirmation));
                    break;
                case 2:
                    printAward(controller.claimDailyReward(confirmation));
                    break;
                case 3:
                    printCatalog();
                    break;
                case 4:
                    redeemReward(confirmation);
                    break;
                case 5:
                    viewInventory(confirmation);
                    break;
                case 6:
                    useReward(confirmation);
                    break;
                case 7:
                    System.out.println("\n--- POINT TRANSACTION HISTORY ---");
                    System.out.println(controller.getPointHistoryText(confirmation));
                    break;
                case 0:
                    System.out.println("Returning to Loyalty menu...");
                    break;
                default:
                    break;
            }
        } while (choice != 0);
    }

    private void printCatalog() {
        System.out.println("\n--- REWARD CATALOG ---");
        System.out.println(controller.getRewardCatalogText());
    }

    private void redeemReward(String confirmation) {
        printCatalog();
        System.out.print("Enter Reward Item ID to redeem (0 to cancel): ");
        String itemId = scanner.nextLine().trim();
        if ("0".equals(itemId)) return;
        LoyaltyController.RedemptionResult result = controller.redeemReward(confirmation, itemId);
        System.out.println((result.isSuccess() ? "SUCCESS: " : "ERROR: ") + result.getMessage());
        if (result.isSuccess()) {
            System.out.printf("Transaction: %s | Points spent: %d | New balance: %d%n",
                    result.getTransactionId(), result.getPointsSpent(), result.getNewBalance());
        }
    }

    private void viewInventory(String confirmation) {
        System.out.print("Status filter (ACTIVE / USED / EXPIRED / ALL): ");
        String status = defaultAll(scanner.nextLine());
        System.out.println("\n--- REDEEMED REWARD INVENTORY ---");
        System.out.println(controller.getRewardInventoryText(confirmation, status));
    }

    private void useReward(String confirmation) {
        System.out.println("\n--- ACTIVE REDEEMED REWARDS ---");
        System.out.println(controller.getRewardInventoryText(confirmation, "ACTIVE"));
        System.out.print("Enter redemption Transaction ID to use (0 to cancel): ");
        String transactionId = scanner.nextLine().trim();
        if ("0".equals(transactionId)) return;
        LoyaltyController.UseRewardResult result =
                controller.useRedeemedReward(confirmation, transactionId);
        System.out.println((result.isSuccess() ? "SUCCESS: " : "ERROR: ") + result.getMessage());
    }

    private void displayPointActivityReport() {
        System.out.println("\n--- MEMBER POINT ACTIVITY REPORT CRITERIA ---");
        System.out.print("Search member/confirmation/name/description (blank = ALL): ");
        String search = scanner.nextLine().trim();
        System.out.print("Transaction type (CHECKOUT_EARN / DAILY_CHECK_IN / REDEMPTION / EXPIRY / ALL): ");
        String type = defaultAll(scanner.nextLine());
        System.out.print("Status (ACTIVE / PARTIALLY_USED / CONSUMED / EXPIRED / DEDUCTION / ALL): ");
        String status = defaultAll(scanner.nextLine());
        LocalDate from = readOptionalDate("From date yyyy-MM-dd (blank = ALL): ");
        LocalDate to = readOptionalDate("To date yyyy-MM-dd (blank = ALL): ");
        while (from != null && to != null && to.isBefore(from)) {
            System.out.println("To date cannot be before From date.");
            to = readOptionalDate("To date yyyy-MM-dd (blank = ALL): ");
        }
        int minPoints = readInt("Minimum absolute points (0 or more): ", 0, Integer.MAX_VALUE);
        System.out.print("Sort field (DATE / POINTS / CONFIRMATION / TYPE / STATUS): ");
        String sortField = defaultValue(scanner.nextLine(), "DATE");
        boolean ascending = readSortDirection();

        System.out.println("\n" + controller.getPointActivityReportText(search, type, status,
                from, to, minPoints, sortField, ascending));
    }

    private void displayRewardPerformanceReport() {
        System.out.println("\n--- REWARD PERFORMANCE REPORT CRITERIA ---");
        System.out.print("Search Reward ID/name (blank = ALL): ");
        String search = scanner.nextLine().trim();
        System.out.print("Stock status (IN_STOCK / LOW_STOCK / OUT_OF_STOCK / ALL): ");
        String stockStatus = defaultAll(scanner.nextLine());
        int minCost = readInt("Minimum points cost (0 or more): ", 0, Integer.MAX_VALUE);
        int maxCost = readInt("Maximum points cost (-1 = ALL): ", -1, Integer.MAX_VALUE);
        while (maxCost >= 0 && maxCost < minCost) {
            System.out.println("Maximum cost must be -1 or at least the minimum cost.");
            maxCost = readInt("Maximum points cost (-1 = ALL): ", -1, Integer.MAX_VALUE);
        }
        int minRedeemed = readInt("Minimum redemption count (0 or more): ", 0, Integer.MAX_VALUE);
        System.out.print("Sort field (COST / STOCK / REDEEMED / NAME / STATUS): ");
        String sortField = defaultValue(scanner.nextLine(), "COST");
        boolean ascending = readSortDirection();

        System.out.println("\n" + controller.getRewardPerformanceReportText(search, stockStatus,
                minCost, maxCost, minRedeemed, sortField, ascending));
    }

    private void restockRewards() {
        System.out.print("Restock every reward to its default quantity? (Y/N): ");
        if ("Y".equalsIgnoreCase(scanner.nextLine().trim())) {
            System.out.println(controller.resetAllRewardStocks());
        } else {
            System.out.println("Restock cancelled.");
        }
    }

    private void printAward(LoyaltyController.AwardResult result) {
        System.out.println((result.isSuccess() ? "SUCCESS: " : "WARNING: ") + result.getMessage());
        if (result.isSuccess()) {
            System.out.printf("Points awarded: %d | New balance: %d%n",
                    result.getPointsAwarded(), result.getNewBalance());
            if (result.isTierUpgraded()) {
                System.out.println("Tier upgraded: " + result.getOldTier() + " -> " + result.getNewTier());
            }
        }
    }

    private int readInt(String prompt, int minimum, int maximum) {
        while (true) {
            System.out.print(prompt);
            try {
                int value = Integer.parseInt(scanner.nextLine().trim());
                if (value >= minimum && value <= maximum) return value;
            } catch (NumberFormatException ignored) {
                // Fall through to the common validation message.
            }
            System.out.printf("Invalid input. Enter a value from %d to %d.%n", minimum, maximum);
        }
    }

    private LocalDate readOptionalDate(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) return null;
            try {
                return LocalDate.parse(input);
            } catch (DateTimeParseException e) {
                System.out.println("Invalid date. Use yyyy-MM-dd, for example 2026-08-17.");
            }
        }
    }

    private boolean readSortDirection() {
        return readInt("Sort direction (1 = Ascending, 2 = Descending): ", 1, 2) == 1;
    }

    private String defaultAll(String value) {
        return defaultValue(value, "ALL");
    }

    private String defaultValue(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    public static void main(String[] args) {
        new LoyaltyUI().displayMenu();
    }
}
