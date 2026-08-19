package boundary;

import adt.BSTInterface;
import adt.ListInterface;
import control.LoyaltyController;
import entity.Guest;
import entity.RewardItem;
import java.util.Scanner;

/**
 * Author: Hock Siang
 * Loyalty & Rewards module user interface.
 */
public class LoyaltyUI {
    private BSTInterface<Guest> masterGuestTree;
    private LoyaltyController loyaltyController;

    public LoyaltyUI(BSTInterface<Guest> masterGuestTree) {
        this.masterGuestTree = masterGuestTree;
        this.loyaltyController = new LoyaltyController();
    }
    
    public LoyaltyUI(BSTInterface<Guest> masterGuestTree, LoyaltyController loyaltyController) {
        this.masterGuestTree = masterGuestTree;
        this.loyaltyController = loyaltyController;
    }

    public void displayMenu() {
        displayMenu(null);
    }

    public void displayMenu(Scanner scanner) {
        Scanner scan = (scanner != null) ? scanner : new Scanner(System.in);
        String confirmNo;

        do {
            System.out.println("\n-----------------------------------------------");
            System.out.println("          LOYALTY & REWARDS MANAGEMENT           ");
            System.out.println("-----------------------------------------------");
            System.out.print("Enter Confirmation Number (0 to exit): ");
            
            confirmNo = scan.nextLine().trim();

            if (confirmNo.equals("0")) {
                System.out.println("Returning to main menu...");
                break;
            }

            if (confirmNo.isEmpty()) {
                System.out.println("Confirmation number cannot be empty!");
                continue;
            }

            Guest targetDummy = new Guest("", confirmNo, "", 0);
            Guest guest = masterGuestTree.search(targetDummy);

            if (guest != null) {
                guestProfile(guest, scan);
            } else {
                System.out.println("Error: No guest found with confirmation number '" + confirmNo + "'.");
            }
        } while (true);
    }
    
    public void guestProfile(Guest guest, Scanner scan) {
        String choice;

        do {
            loyaltyController.refreshPoints(guest);
            String tierInfo = loyaltyController.getNextTierInfo(guest);
            
            System.out.println("\n========================================================================");
            System.out.println("                MEMBER PROFILE: " + guest.getGuestName() + " - " + guest.getConfirmationNumber());
            System.out.println("========================================================================");
            System.out.println(loyaltyController.checkNotifications(guest));
            System.out.println("Current Tier    : " + guest.getLoyaltyTier());
            System.out.println("Loyalty Points  : " + guest.getLoyaltyPoints());
            System.out.println("Loyalty EXP     : " + tierInfo);
            System.out.println("------------------------------------------------------------------------");
            System.out.println("1. Daily Check-In (+700 pts)");
            System.out.println("2. Reward Catalog (Point Exchange)");
            System.out.println("3. Item Storage & Inventory");
            System.out.println("4. Point & Redemption History");
            System.out.println("5. Restock Item Stock");
            System.out.println("6. Member Point Activity & Expiry Audit Report");
            System.out.println("7. Reward Item Stock & Performance Report");
            System.out.println("0. Back to Confirmation Menu");
            System.out.println("========================================================================");

            while (true) {
                System.out.print("Select an option (0-7) > ");
                choice = scan.nextLine().trim();

                if (loyaltyController.isValidMenuChoice(choice, 0, 7)) break;
                System.out.println("Invalid selection. Please enter a number between 0 and 7.");
            }
            
            switch (choice) {
                case "1" -> System.out.println("\n" + loyaltyController.performDailyCheckIn(guest));
                case "2" -> rewardCatalog(scan, guest);
                case "3" -> viewInventory(scan, guest);
                case "4" -> System.out.println("\n--- TRANSACTION HISTORY ---\n" + loyaltyController.getFormattedTransactionHistory(guest));
                case "5" -> System.out.println(loyaltyController.resetAllRewardStocks());
                case "6" -> displayPointReport(scan);
                case "7" -> displayRewardReport(scan);
                case "0" -> System.out.println("Exiting guest profile...");
            }

        } while (!choice.equals("0"));
    }
    
    public void rewardCatalog(Scanner scan, Guest guest) {
        while (true) {
            System.out.println("\n========================================================================");
            System.out.println("                        REWARD CATALOG REDEMPTION                       ");
            System.out.println("========================================================================");
            System.out.println(" Available Points: " + guest.getLoyaltyPoints() + " pts");
            System.out.println("----------------------------------------------------------------------------------");
            System.out.printf(" %-4s | %-30s | %-13s | %-11s | %-14s\n", "No.", "Reward Description", "Cost (Pts)", "Stock", "Validity");
            System.out.println("----------------------------------------------------------------------------------");

            ListInterface<RewardItem> items = loyaltyController.getRewardCatalog();

            for (int i = 0; i < items.getNumberOfEntries(); i++) {
                RewardItem item = items.get(i);
                System.out.printf(" %-2d.  | %s\n", (i + 1), item.toString());
            }

            System.out.println("----------------------------------------------------------------------------------");
            System.out.println(" 0. Back to Profile Menu");
            System.out.println("==================================================================================");
            System.out.print("Select item number to redeem > ");
            String input = scan.nextLine().trim();

            if (input.equals("0")) {
                break;
            }

            if (loyaltyController.isValidMenuChoice(input, 1, items.getNumberOfEntries())) {
                int choice = Integer.parseInt(input);
                String resultMsg = loyaltyController.redeemRewardItem(guest, items.get(choice - 1));
                System.out.println("\n------------------------------------------------------------------------");
                System.out.println(" " + resultMsg);
                System.out.println("------------------------------------------------------------------------");
            } else {
                System.out.println("\n[ERROR] Invalid selection. Please enter a valid item number.");
            }
        }
    }
    
    public void viewInventory(Scanner scan, Guest guest) {
        while (true) {
            System.out.println("\n========================================================================");
            System.out.println("                    REDEEMED ITEM STORAGE & INVENTORY                   ");
            System.out.println("========================================================================");            
            System.out.println(loyaltyController.getFormattedInventory(guest));
            System.out.println("------------------------------------------------------------------------");
            System.out.print("Select option (0 to go back) > ");
            
            String input = scan.nextLine().trim();
            if (input.equals("0")) {
                break;
            }

            try {
                int choice = Integer.parseInt(input);
                String result = loyaltyController.useRedeemedItem(guest, choice);
                System.out.println("------------------------------------------------------------------------");
                System.out.println(" " + result);
                System.out.println("------------------------------------------------------------------------");
            } catch (NumberFormatException e) {
                System.out.println("\n[ERROR] Invalid input. Please enter a valid number.");
            }
        }
    }
    
    private void displayPointReport(Scanner scan) {
        System.out.print("Status (ACTIVE/EXPIRED/DEDUCTION/ALL): ");
        String status = scan.nextLine().trim();
        System.out.print("Sort (1.Asc, 2.Desc): ");
        boolean ascending = readIntInput(scan) != 2;

        ListInterface<String> report = loyaltyController.getFilteredPointReport(status, ascending);

        System.out.println("\n--- POINT ACTIVITY & EXPIRY REPORT ---");
        System.out.printf("%-10s | %-8s | %-40s | %-19s | %-19s | %-10s\n", "Confirm No", "Points", "Description", "Earned", "Expiry", "Status");
        System.out.println("--------------------------------------------------------------------------------------------------");

        if (report.isEmpty()) System.out.println("No matching records.");
        else for (int i = 0; i < report.getNumberOfEntries(); i++) {
            String[] p = report.get(i).split("\\|");
            System.out.printf("%-10s | %-8s | %-40s | %-19s | %-19s | %-10s\n",
                    p[4].replace("Conf: ", ""), (Integer.parseInt(p[0]) >= 0 ? "+" : "") + p[0], p[3], p[1], p[2], p[5]);
        }
    }

    private void displayRewardReport(Scanner scan) {
        System.out.print("\nMaximum Stock Filter: ");
        int maxStock = readIntInput(scan);

        System.out.print("Sort by Points Cost? (1 Ascending, 2 Descending): ");
        boolean ascending = readIntInput(scan) != 2;

        ListInterface<RewardItem> report = loyaltyController.getFilteredRewardReport(maxStock, ascending);

        System.out.println("\n==================================================================================");
        System.out.println("                        REWARD STOCK & PERFORMANCE REPORT                                 ");
        System.out.println("==================================================================================");
        System.out.printf(" %-24s | %-6s | %-5s | %-8s | %-8s | %-8s\n",
                "Reward Item", "Cost", "Stock", "Redeemed", "Active", "Expired");
        System.out.println("----------------------------------------------------------------------------------");

        int grandTotalRedemptions = 0;
        int grandTotalActive = 0;
        int grandTotalExpired = 0;

        for (int i = 0; i < report.getNumberOfEntries(); i++) {
            RewardItem r = report.get(i);
            int redeemedCount = loyaltyController.getTotalItemRedemptionCount(r.getItemName());
            int activeCount = loyaltyController.getActiveItemRedemptionCount(null, r.getItemName());
            int expiredCount = loyaltyController.getExpiredItemRedemptionCount(r.getItemName());

            grandTotalRedemptions += redeemedCount;
            grandTotalActive += activeCount;
            grandTotalExpired += expiredCount;

            System.out.printf(" %-24s | %-6d | %-5d | %-8d | %-8d | %-8d\n",
                    r.getItemName(), r.getPointsCost(), r.getStockQuantity(), redeemedCount, activeCount, expiredCount);
        }

        System.out.println("----------------------------------------------------------------------------------");
        if (report.isEmpty()) {
            System.out.println(" No items match the stock criteria.");
        } else {
            System.out.printf(" SUMMARY: Showing %d item(s) | Total: %d Redeemed | Active: %d | Expired: %d\n",
                    report.getNumberOfEntries(), grandTotalRedemptions, grandTotalActive, grandTotalExpired);
        }
        System.out.println("==================================================================================");
    }

    private int readIntInput(Scanner scan) {
        while (true) {
            try {
                return Integer.parseInt(scan.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.print("Invalid input. Please enter a valid number: ");
            }
        }
    }
}
