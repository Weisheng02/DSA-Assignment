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
            System.out.println("2. Manage Guest Records");
            System.out.println("3. Display All Room Statuses");
            System.out.println("4. Process Guest Check-In");
            System.out.println("5. Generate Billing & Receipt");
            System.out.println("6. Management Reports");
            System.out.println("7. View Tree Structure Info");
            System.out.println("0. Exit Module");
            System.out.println("--------------------------------------------------");
            System.out.print("Enter choice (0-7): ");

            choice = readIntInput();
            System.out.println();

            switch (choice) {
                case 1:
                    handleGuestSearch();
                    break;
                case 2:
                    handleGuestManagement();
                    break;
                case 3:
                    displayRoomList();
                    break;
                case 4:
                    handleCheckIn();
                    break;
                case 5:
                    handleBillingReceipt();
                    break;
                case 6:
                    displayReportsSubmenu();
                    break;
                case 7:
                    displayTreeDiagnostics();
                    break;
                case 0:
                    System.out.println("Exiting Front Desk System...");
                    break;
                default:
                    System.out.println("Invalid option! Please enter a number between 0 and 7.");
            }
        } while (choice != 0);
    }

    private void handleGuestSearch() {
        System.out.println("\n--- Guest Search ---");
        System.out.println("1. Search by Confirmation Number");
        System.out.println("2. Search by Guest Name");
        System.out.println("3. Search by Confirmation Number Range");
        System.out.print("Enter choice (1-3): ");
        int mode = readIntInput();

        if (mode == 1) {
            String confirmNo = readValidConfirmationNumber("Enter 8-digit Confirmation Number (e.g. 10000001): ");
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
        } else if (mode == 3) {
            System.out.println("\n--- Range Search ---");
            String startNo = readValidConfirmationNumber("Enter START Confirmation Number (e.g. 10000001): ");
            String endNo = readValidConfirmationNumber("Enter END Confirmation Number (e.g. 10000005): ");
            
            ListInterface<Guest> rangeResults = controller.searchGuestsByConfirmationRange(startNo, endNo);
            System.out.println("\n==================================================");
            System.out.printf(" RANGE SEARCH RESULTS: %d guest(s) found [%s - %s]\n", 
                    rangeResults.getNumberOfEntries(), startNo, endNo);
            System.out.println("==================================================");
            if (rangeResults.isEmpty()) {
                System.out.println(" No guests found within the specified range.");
            } else {
                for (int i = 0; i < rangeResults.getNumberOfEntries(); i++) {
                    printGuestCard(rangeResults.get(i));
                }
            }
        } else {
            System.out.println("Invalid search mode.");
        }
    }

    private void handleGuestManagement() {
        System.out.println("\n--- Manage Guest Records ---");
        System.out.println("1. Register New Guest");
        System.out.println("2. Remove Guest Record");
        System.out.print("Enter choice (1-2): ");
        int choice = readIntInput();

        if (choice == 1) {
            String confirmNo = readValidConfirmationNumber("Enter new 8-digit Confirmation Number: ");
            if (controller.searchGuestByConfirmationNumber(confirmNo) != null) {
                System.out.println("Error: Confirmation Number " + confirmNo + " already exists.");
                return;
            }
            System.out.print("Enter Guest Name: ");
            String name = scanner.nextLine().trim();

            System.out.println("Select Loyalty Tier (1. Platinum, 2. Gold, 3. Silver, 4. Standard): ");
            int tierChoice = readIntInput();
            String tier;
            switch (tierChoice) {
                case 1: tier = "Platinum"; break;
                case 2: tier = "Gold"; break;
                case 3: tier = "Silver"; break;
                default: tier = "Standard"; break;
            }

            System.out.print("Enter Initial Loyalty Points: ");
            int points = readPositiveIntInput();

            Guest newGuest = new Guest(name, confirmNo, tier, points);
            if (controller.registerGuest(newGuest)) {
                System.out.println("\nGuest " + name + " (" + confirmNo + ") registered successfully!");
            } else {
                System.out.println("\nFailed to register guest.");
            }
        } else if (choice == 2) {
            String confirmNo = readValidConfirmationNumber("Enter 8-digit Confirmation Number to remove: ");
            Guest removed = controller.removeGuest(confirmNo);
            if (removed != null) {
                System.out.println("\nRemoved guest: " + removed.getGuestName() + " (" + confirmNo + ")");
            } else {
                System.out.println("\nConfirmation Number " + confirmNo + " not found.");
            }
        } else {
            System.out.println("Invalid choice.");
        }
    }

    private void displayRoomList() {
        ListInterface<Room> rooms = controller.getAllRooms();
        printRoomTable(rooms);
    }

    private void handleCheckIn() {
        System.out.println("\n--- Guest Check-In ---");
        String confirmNo = readValidConfirmationNumber("Enter 8-digit Confirmation Number (e.g. 10000001): ");

        Guest g = controller.searchGuestByConfirmationNumber(confirmNo);
        if (g == null) {
            System.out.println("Check-in failed: Confirmation number does not exist.");
            return;
        }
        System.out.println("\nGuest Found: " + g.getGuestName() + " (" + g.getLoyaltyTier() + " Member)");

        displayRoomList();
        System.out.print("Enter Room Number to assign: ");
        String roomNo = scanner.nextLine().trim();

        // Suggest upgrade for Platinum/Gold members
        if ("Platinum".equalsIgnoreCase(g.getLoyaltyTier()) || "Gold".equalsIgnoreCase(g.getLoyaltyTier())) {
            Room upgrade = controller.suggestRoomUpgrade(roomNo);
            if (upgrade != null) {
                System.out.println("\n*** Room Upgrade Available ***");
                System.out.printf(" As a %s member, you may upgrade to Room %s (%s, RM %.2f/night)\n",
                        g.getLoyaltyTier(), upgrade.getRoomNumber(), upgrade.getRoomType(), upgrade.getPrice());
                System.out.print(" Accept upgrade? (Y/N): ");
                String ans = scanner.nextLine().trim();
                if (ans.equalsIgnoreCase("Y")) {
                    roomNo = upgrade.getRoomNumber();
                }
            }
        }

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

    private void handleBillingReceipt() {
        System.out.println("\n--- Billing & Receipt Generator ---");
        String confirmNo = readValidConfirmationNumber("Enter 8-digit Confirmation Number (e.g. 10000001): ");
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
        double discountAmt = subtotal * discountRate;
        double total = subtotal - discountAmt;

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
        System.out.printf(" Member Discount :-RM %8.2f\n", discountAmt);
        System.out.println("--------------------------------------------------");
        System.out.printf(" Total Payable   : RM %8.2f\n", total);
        System.out.println("==================================================");
    }

    private void displayReportsSubmenu() {
        int reportChoice = -1;
        do {
            System.out.println("\n--------------------------------------------------");
            System.out.println("            MANAGEMENT REPORTS                    ");
            System.out.println("--------------------------------------------------");
            System.out.println("1. Room Status & Occupancy Summary");
            System.out.println("2. Filtered & Sorted Room Pricing Report");
            System.out.println("3. VIP Member Points & Ranking Report");
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

    private void displayTreeDiagnostics() {
        String[] stats = controller.getGuestTreeDiagnostics();

        System.out.println("\n==================================================");
        System.out.println("         BST STRUCTURE INFORMATION                ");
        System.out.println("==================================================");
        System.out.printf(" Total Nodes      : %s\n", stats[0]);
        System.out.printf(" Tree Height      : %s\n", stats[1]);
        System.out.printf(" Smallest Key     : %s\n", stats[2]);
        System.out.printf(" Largest Key      : %s\n", stats[3]);
        System.out.println("==================================================");
    }

    private void displayReport1() {
        int[] summary = controller.getRoomStatusSummary();
        double occupancyRate = (summary[0] == 0) ? 0.0 : ((double) summary[2] / summary[0]) * 100;

        System.out.println("\n==================================================");
        System.out.println("     REPORT 1: ROOM OCCUPANCY & STATUS OVERVIEW   ");
        System.out.println("==================================================");
        System.out.printf(" Total Rooms                : %d\n", summary[0]);
        System.out.printf(" Ready for Check-In         : %d\n", summary[1]);
        System.out.printf(" Occupied                   : %d\n", summary[2]);
        System.out.printf(" Dirty                      : %d\n", summary[3]);
        System.out.printf(" Cleaning In Progress       : %d\n", summary[4]);
        System.out.println("--------------------------------------------------");
        System.out.printf(" Occupancy Rate             : %.1f%%\n", occupancyRate);
        System.out.println("==================================================");
    }

    private void displayReport2() {
        System.out.println("\n--- REPORT 2: FILTERED & SORTED ROOM PRICING ---");
        System.out.print(
                "Enter Room Status filter (Ready for Check-In / Dirty / Occupied / Cleaning In Progress / ALL): ");
        String statusFilter = scanner.nextLine().trim();

        System.out.print("Enter Maximum Price per night (0 for no limit): ");
        double maxPrice = 0.0;
        try {
            maxPrice = Double.parseDouble(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            maxPrice = 0.0;
        }

        System.out.print("Sort by price? (1=Ascending, 2=Descending): ");
        int sortChoice = readIntInput();
        boolean asc = (sortChoice != 2);

        ListInterface<Room> filteredRooms = controller.getFilteredAndSortedRooms(statusFilter, maxPrice, asc);

        System.out.println("\n==========================================================================");
        System.out.printf(" RESULTS: %d room(s) found\n", filteredRooms.getNumberOfEntries());
        System.out.println("==========================================================================");
        printRoomTable(filteredRooms);
    }

    private void displayReport3() {
        System.out.println("\n--- REPORT 3: VIP MEMBER RANKING ---");
        System.out.print("Enter Loyalty Tier filter (Platinum / Gold / Silver / Standard / ALL): ");
        String tierFilter = scanner.nextLine().trim();

        System.out.print("Enter Minimum Loyalty Points: ");
        int minPoints = readIntInput();

        ListInterface<Guest> filteredGuests = controller.getFilteredAndSortedGuests(tierFilter, minPoints);

        System.out.println("\n==================================================");
        System.out.printf(" VIP RANKING: %d member(s) found\n", filteredGuests.getNumberOfEntries());
        System.out.println("==================================================");
        for (int i = 0; i < filteredGuests.getNumberOfEntries(); i++) {
            Guest g = filteredGuests.get(i);
            System.out.printf(" #%d | %-15s | %s | %-8s | %d pts\n",
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

    private String readValidConfirmationNumber(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            if (input.matches("\\d{8}")) {
                return input;
            }
            System.out.println("Invalid format. Must be exactly 8 digits.");
        }
    }

    private int readIntInput() {
        while (true) {
            try {
                String input = scanner.nextLine().trim();
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.print("Invalid input. Enter a valid number: ");
            }
        }
    }

    private int readPositiveIntInput() {
        while (true) {
            int val = readIntInput();
            if (val >= 0)
                return val;
            System.out.print("Cannot be negative. Enter again: ");
        }
    }

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