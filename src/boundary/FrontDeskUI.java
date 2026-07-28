package boundary;

import control.FrontDeskController;
import entity.Guest;
import entity.Room;
import adt.ListInterface;
import java.util.Scanner;

/**
 * Author: Weisheng
 * Boundary Class for Front-Desk User Interface
 */
public class FrontDeskUI {
    private FrontDeskController controller;
    private Scanner scanner;

    public FrontDeskUI() {
        controller = new FrontDeskController();
        scanner = new Scanner(System.in);
    }

    public void displayMenu() {
        displayMenu(this.scanner);
    }

    public void displayMenu(Scanner scanner) {
        if (scanner != null) {
            this.scanner = scanner;
        }
        int choice = -1;

        do {
            System.out.println("\n--------------------------------------------------");
            System.out.println("            FRONT DESK MANAGEMENT SYSTEM          ");
            System.out.println("--------------------------------------------------");
            System.out.println("1. Search Guest Information");
            System.out.println("2. Display All Room Statuses");
            System.out.println("3. Process Guest Check-In");
            System.out.println("4. Generate Billing & Receipt");
            System.out.println("5. Management Reports (Multi-Criteria & Sorted)");
            System.out.println("0. Exit Module");
            System.out.println("--------------------------------------------------");
            System.out.print("Enter choice (0-5): ");

            choice = readIntInput();
            System.out.println();

            switch (choice) {
                case 1:
                    handleGuestSearch();
                    break;
                case 2:
                    displayRoomList();
                    break;
                case 3:
                    handleCheckIn();
                    break;
                case 4:
                    handleBillingReceipt();
                    break;
                case 5:
                    displayReportsSubmenu();
                    break;
                case 0:
                    System.out.println("Exiting Front Desk System...");
                    break;
                default:
                    System.out.println("Invalid option! Please enter a number between 0 and 5.");
            }
        } while (choice != 0);
    }

    // Search guest by confirmation number or name
    private void handleGuestSearch() {
        System.out.println("\n--- Guest Search ---");
        System.out.println("1. Search by 8-Digit Confirmation Number");
        System.out.println("2. Search by Guest Name");
        System.out.print("Enter choice: ");
        int mode = readIntInput();

        if (mode == 1) {
            String confirmNo = readValidConfirmationNumber();
            Guest g = controller.searchGuestByConfirmationNumber(confirmNo);
            if (g != null) {
                printGuestCard(g);
            } else {
                System.out.println("\nNo record found for Confirmation No: " + confirmNo);
            }
        } else if (mode == 2) {
            System.out.print("Enter guest name: ");
            String name = scanner.nextLine().trim();
            ListInterface<Guest> results = controller.searchGuestsByName(name);
            if (results.isEmpty()) {
                System.out.println("\nNo guests found matching: \"" + name + "\"");
            } else {
                System.out.println("\nFound " + results.getNumberOfEntries() + " guest(s):");
                for (int i = 0; i < results.getNumberOfEntries(); i++) {
                    printGuestCard(results.get(i));
                }
            }
        } else {
            System.out.println("Invalid search mode.");
        }
    }

    // Display formatted room list
    private void displayRoomList() {
        ListInterface<Room> rooms = controller.getAllRooms();
        printRoomTable(rooms);
    }

    // Process check-in for guests
    private void handleCheckIn() {
        System.out.println("\n--- Guest Check-In ---");
        String confirmNo = readValidConfirmationNumber();

        Guest g = controller.searchGuestByConfirmationNumber(confirmNo);
        if (g == null) {
            System.out.println("Check-in failed: Confirmation number does not exist.");
            return;
        }
        System.out.println("Guest Found: " + g.getGuestName() + " (" + g.getLoyaltyTier() + " Member)");

        displayRoomList();
        System.out.print("Enter Room Number to assign: ");
        String roomNo = scanner.nextLine().trim();

        int result = controller.processCheckIn(confirmNo, roomNo);
        switch (result) {
            case 1:
                System.out.println("\nCheck-in successful! Room " + roomNo + " is now set to [Occupied].");
                break;
            case -2:
                System.out.println("\nCheck-in failed: Room " + roomNo + " does not exist.");
                break;
            case -3:
                Room r = controller.searchRoomByNumber(roomNo);
                System.out.println("\nCheck-in failed: Room " + roomNo + " is currently [" + r.getRoomStatus() + "].");
                System.out.println("Note: Only rooms with status [Ready for Check-In] can be assigned.");
                break;
        }
    }

    // Calculate billing and print invoice receipt
    private void handleBillingReceipt() {
        System.out.println("\n--- Billing & Receipt Generator ---");
        String confirmNo = readValidConfirmationNumber();
        Guest guest = controller.searchGuestByConfirmationNumber(confirmNo);

        if (guest == null) {
            System.out.println("Error: Guest record not found.");
            return;
        }

        System.out.print("Enter Room Number stayed: ");
        String roomNo = scanner.nextLine().trim();
        Room room = controller.searchRoomByNumber(roomNo);

        if (room == null) {
            System.out.println("Error: Invalid Room Number.");
            return;
        }

        System.out.print("Enter number of nights stayed: ");
        int nights = readPositiveIntInput();

        double discountRate = controller.getDiscountPercentage(guest.getLoyaltyTier());
        double subtotal = room.getPrice() * nights;
        double discountAmount = subtotal * discountRate;
        double finalTotal = subtotal - discountAmount;

        System.out.println("\n==================================================");
        System.out.println("                 RESORT INVOICE                   ");
        System.out.println("==================================================");
        System.out.printf(" Confirmation No : %s\n", guest.getConfirmationNumber());
        System.out.printf(" Guest Name      : %s\n", guest.getGuestName());
        System.out.printf(" Membership Tier : %s (%.0f%% Discount)\n", guest.getLoyaltyTier(), discountRate * 100);
        System.out.printf(" Room Assigned   : Room %s (%s)\n", room.getRoomNumber(), room.getRoomType());
        System.out.printf(" Stay Duration   : %d Night(s)\n", nights);
        System.out.println("--------------------------------------------------");
        System.out.printf(" Room Rate/Night : RM %8.2f\n", room.getPrice());
        System.out.printf(" Subtotal        : RM %8.2f\n", subtotal);
        System.out.printf(" Member Discount :-RM %8.2f\n", discountAmount);
        System.out.println("--------------------------------------------------");
        System.out.printf(" Total Payable   : RM %8.2f\n", finalTotal);
        System.out.println("==================================================");
    }

    // Reports Submenu to fulfill "At least two reports with searching, sorting &
    // filtering" requirement
    private void displayReportsSubmenu() {
        int reportChoice = -1;
        do {
            System.out.println("\n--------------------------------------------------");
            System.out.println("            MANAGEMENT REPORT ENGINE              ");
            System.out.println("--------------------------------------------------");
            System.out.println("1. Report 1: Daily Room Status & Occupancy Summary");
            System.out.println("2. Report 2: Multi-Criteria Filtered & Sorted Room Pricing Report");
            System.out.println("3. Report 3: High-Tier Member Rewards & Points Ranking Report");
            System.out.println("0. Back to Main Menu");
            System.out.println("--------------------------------------------------");
            System.out.print("Enter choice (0-3): ");

            reportChoice = readIntInput();
            System.out.println();

            switch (reportChoice) {
                case 1:
                    displayReport1();
                    break;
                case 2:
                    displayReport2();
                    break;
                case 3:
                    displayReport3();
                    break;
                case 0:
                    System.out.println("Returning to Main Menu...");
                    break;
                default:
                    System.out.println("Invalid choice. Please enter a number between 0 and 3.");
            }
        } while (reportChoice != 0);
    }

    // Report 1: Status & Occupancy Overview
    private void displayReport1() {
        int[] summary = controller.getRoomStatusSummary();
        double occupancyRate = (summary[0] == 0) ? 0.0 : ((double) summary[2] / summary[0]) * 100;

        System.out.println("\n==================================================");
        System.out.println("     REPORT 1: ROOM OCCUPANCY & STATUS OVERVIEW   ");
        System.out.println("==================================================");
        System.out.printf(" Total Rooms                : %d\n", summary[0]);
        System.out.printf(" Ready for Check-In Rooms  : %d\n", summary[1]);
        System.out.printf(" Occupied Rooms             : %d\n", summary[2]);
        System.out.printf(" Dirty Rooms                : %d\n", summary[3]);
        System.out.printf(" Cleaning In Progress       : %d\n", summary[4]);
        System.out.println("--------------------------------------------------");
        System.out.printf(" Current Occupancy Rate     : %.1f%%\n", occupancyRate);
        System.out.println("==================================================");
    }

    // Report 2: Multi-Criteria Filtered & Sorted Room Price Report
    private void displayReport2() {
        System.out.println("\n--- REPORT 2: FILTERED & SORTED ROOM PRICE ANALYSIS ---");
        System.out.print(
                "Enter Room Status filter (Ready for Check-In / Dirty / Occupied / Cleaning In Progress / ALL): ");
        String statusFilter = scanner.nextLine().trim();

        System.out.print("Enter Maximum Room Price per night (Enter 0 for no limit): ");
        double maxPrice = 0.0;
        try {
            maxPrice = Double.parseDouble(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            maxPrice = 0.0;
        }

        System.out.print("Sort by price ascending? (1 for Ascending, 2 for Descending): ");
        int sortChoice = readIntInput();
        boolean asc = (sortChoice != 2);

        ListInterface<Room> filteredRooms = controller.getFilteredAndSortedRooms(statusFilter, maxPrice, asc);

        System.out.println("\n==========================================================================");
        System.out.printf(" REPORT RESULTS: %d room(s) match criteria\n", filteredRooms.getNumberOfEntries());
        System.out.println("==========================================================================");
        printRoomTable(filteredRooms);
    }

    // Report 3: High-Tier Member Rewards & Points Ranking Report
    private void displayReport3() {
        System.out.println("\n--- REPORT 3: VIP MEMBER POINTS & RANKING REPORT ---");
        System.out.print("Enter Loyalty Tier filter (Platinum / Gold / Silver / Standard / ALL): ");
        String tierFilter = scanner.nextLine().trim();

        System.out.print("Enter Minimum Loyalty Points threshold: ");
        int minPoints = readIntInput();

        ListInterface<Guest> filteredGuests = controller.getFilteredAndSortedGuests(tierFilter, minPoints);

        System.out.println("\n==================================================");
        System.out.printf(" VIP RANKING RESULTS: %d member(s) found\n", filteredGuests.getNumberOfEntries());
        System.out.println("==================================================");
        for (int i = 0; i < filteredGuests.getNumberOfEntries(); i++) {
            Guest g = filteredGuests.get(i);
            System.out.printf(" Rank #%d | %-15s | ID: %s | Tier: %-8s | Points: %d pts\n",
                    (i + 1), g.getGuestName(), g.getConfirmationNumber(), g.getLoyaltyTier(), g.getLoyaltyPoints());
        }
        System.out.println("==================================================");
    }

    private void printRoomTable(ListInterface<Room> rooms) {
        System.out.println("==========================================================================");
        System.out.printf("%-10s | %-20s | %-22s | %-10s\n", "Room No", "Room Type", "Status", "Price/Night");
        System.out.println("==========================================================================");
        for (int i = 0; i < rooms.getNumberOfEntries(); i++) {
            Room r = rooms.get(i);
            System.out.printf("%-10s | %-20s | %-22s | RM %7.2f\n",
                    r.getRoomNumber(), r.getRoomType(), r.getRoomStatus(), r.getPrice());
        }
        System.out.println("==========================================================================");
    }

    // Helper method to validate 8-digit confirmation number
    private String readValidConfirmationNumber() {
        while (true) {
            System.out.print("Enter 8-digit Confirmation Number (e.g. 10000001): ");
            String input = scanner.nextLine().trim();
            if (input.matches("\\d{8}")) {
                return input;
            }
            System.out.println("Invalid format. Confirmation number must be exactly 8 digits.");
        }
    }

    // Helper method to read integer input safely
    private int readIntInput() {
        while (true) {
            try {
                String input = scanner.nextLine().trim();
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.print("Invalid input. Please enter a valid integer: ");
            }
        }
    }

    // Helper method to read positive integer
    private int readPositiveIntInput() {
        while (true) {
            int val = readIntInput();
            if (val > 0)
                return val;
            System.out.print("Value must be greater than 0. Please enter again: ");
        }
    }

    // Format guest detail card
    private void printGuestCard(Guest g) {
        System.out.println("--------------------------------------------------");
        System.out.printf(" Confirmation No : %s\n", g.getConfirmationNumber());
        System.out.printf(" Guest Name      : %s\n", g.getGuestName());
        System.out.printf(" Loyalty Tier    : %s\n", g.getLoyaltyTier());
        System.out.printf(" Reward Points   : %d pts\n", g.getLoyaltyPoints());
        System.out.println("--------------------------------------------------");
    }

    public static void main(String[] args) {
        FrontDeskUI ui = new FrontDeskUI();
        ui.displayMenu();
    }
}