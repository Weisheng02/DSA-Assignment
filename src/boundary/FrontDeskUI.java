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
        this(new FrontDeskController());
    }

    public FrontDeskUI(FrontDeskController controller) {
        this.controller = (controller != null) ? controller : new FrontDeskController();
        this.scanner = new Scanner(System.in);
    }

    public FrontDeskUI(adt.BSTInterface<entity.Guest> masterGuestTree, adt.ListInterface<entity.Room> sharedRoomList) {
        this(new FrontDeskController(masterGuestTree, sharedRoomList));
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
            System.out.println("5. Process Room Transfer (Change Room)");
            System.out.println("6. Generate Billing & Receipt");
            System.out.println("7. Management Reports");
            System.out.println("8. View Tree Structure Info");
            System.out.println("0. Back to Main Menu");
            System.out.println("--------------------------------------------------");
            System.out.print("Enter choice (0-8): ");

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
                    handleRoomTransfer();
                    break;
                case 6:
                    handleBillingReceipt();
                    break;
                case 7:
                    displayReportsSubmenu();
                    break;
                case 8:
                    displayTreeDiagnostics();
                    break;
                case 0:
                    System.out.println("Returning to Main Menu...");
                    break;
                default:
                    System.out.println("Invalid option! Please enter a number between 0 and 8.");
            }
        } while (choice != 0);
    }

    private void handleGuestSearch() {
        System.out.println("\n--- Guest Search ---");
        System.out.println("1. Search by Confirmation Number");
        System.out.println("2. Search by Guest Name");
        System.out.println("3. Search by Confirmation Number Range");
        System.out.println("4. Search by IC / Passport Number");
        System.out.println("0. Return to Front Desk Menu");
        System.out.print("Enter choice (0-4): ");
        int mode = readIntInput();

        if (mode == 0) {
            System.out.println("Returning to Front Desk Menu...");
            return;
        }

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
        } else if (mode == 4) {
            System.out.print("Enter IC / Passport Number: ");
            String ic = scanner.nextLine().trim();
            Guest g = controller.searchGuestByIC(ic);
            if (g != null) {
                printGuestCard(g);
            } else {
                System.out.println("\nNo record found for IC / Passport No: " + ic);
            }
        } else {
            System.out.println("Invalid search mode.");
        }
    }

    private void handleGuestManagement() {
        System.out.println("\n--- Manage Guest Records ---");
        System.out.println("1. Register New Guest");
        System.out.println("2. Remove Guest Record");
        System.out.println("0. Return to Front Desk Menu");
        System.out.print("Enter choice (0-2): ");
        int choice = readIntInput();

        if (choice == 0) {
            System.out.println("Returning to Front Desk Menu...");
            return;
        }

        if (choice == 1) {
            String confirmNo = readValidConfirmationNumber("Enter new 8-digit Confirmation Number: ");
            if (controller.searchGuestByConfirmationNumber(confirmNo) != null) {
                System.out.println("Error: Confirmation Number " + confirmNo + " already exists.");
                return;
            }
            System.out.print("Enter Guest Name: ");
            String name = scanner.nextLine().trim();

            System.out.print("Enter IC / Passport Number: ");
            String ic = scanner.nextLine().trim();

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

            Guest newGuest = new Guest(name, ic, confirmNo, tier, points);
            if (controller.registerGuest(newGuest)) {
                System.out.println("\nGuest " + name + " (" + confirmNo + ") registered successfully!");
            } else {
                System.out.println("\nFailed to register guest.");
            }
        } else if (choice == 2) {
            String confirmNo = readValidConfirmationNumber("Enter 8-digit Confirmation Number to remove: ");
            Guest g = controller.searchGuestByConfirmationNumber(confirmNo);
            if (g == null) {
                System.out.println("\nConfirmation Number " + confirmNo + " not found.");
                return;
            }
            if (g.isCheckedIn()) {
                System.out.println("\nCannot remove guest: Guest (" + confirmNo + " - " + g.getGuestName() + ") is currently CHECKED IN to Room " + g.getAssignedRoomNumber() + ".");
                System.out.println("Note: Please process Guest Check-Out before removing guest record.");
                return;
            }
            Guest removed = controller.removeGuest(confirmNo);
            if (removed != null) {
                System.out.println("\nRemoved guest: " + removed.getGuestName() + " (" + confirmNo + ")");
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

        if (g.isCheckedIn() || controller.isGuestCheckedIn(confirmNo)) {
            String roomInfo = (g.getAssignedRoomNumber() != null) ? " (Room " + g.getAssignedRoomNumber() + ")" : "";
            System.out.println("\nCheck-in failed: Guest (" + confirmNo + ") has ALREADY checked in to a room" + roomInfo + ".");
            System.out.println("Note: A guest cannot check in multiple times simultaneously.");
            return;
        }

        String roomNo = g.getAssignedRoomNumber();

        if (roomNo != null && !roomNo.isEmpty()) {
            Room preBooked = controller.searchRoomByNumber(roomNo);
            if (preBooked != null) {
                System.out.println("Auto-Detected Reserved Room: Room " + roomNo + " (" + preBooked.getRoomType() + " - Status: " + preBooked.getRoomStatus() + ")");
            } else {
                System.out.println("Assigned Room: Room " + roomNo);
            }
        } else {
            displayRoomList();
            System.out.print("Enter Room Number to assign: ");
            roomNo = scanner.nextLine().trim();
        }

        Room initialRoom = controller.searchRoomByNumber(roomNo);
        double chargedRate = (g.getEffectiveRoomRate() > 0) ? g.getEffectiveRoomRate() : ((initialRoom != null) ? initialRoom.getPrice() : 0.0);

        // Suggest upgrade for Platinum/Gold members
        if ("Platinum".equalsIgnoreCase(g.getLoyaltyTier()) || "Gold".equalsIgnoreCase(g.getLoyaltyTier())) {
            Room upgrade = controller.suggestRoomUpgrade(roomNo);
            if (upgrade != null) {
                System.out.println("\n*** Complementary Room Upgrade Available ***");
                System.out.printf(" As a %s member, you get a FREE upgrade to Room %s (%s, Value: RM %.2f/night)!\n",
                        g.getLoyaltyTier(), upgrade.getRoomNumber(), upgrade.getRoomType(), upgrade.getPrice());
                System.out.printf(" You will only be charged your original rate of RM %.2f/night.\n", chargedRate);
                System.out.print(" Accept complementary upgrade? (Y/N): ");
                String ans = scanner.nextLine().trim();
                if (ans.equalsIgnoreCase("Y")) {
                    roomNo = upgrade.getRoomNumber();
                }
            }
        }

        int result = controller.processCheckIn(confirmNo, roomNo, chargedRate);
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
            case -4:
                System.out.println("\nCheck-in failed: Guest (" + confirmNo + ") has ALREADY checked in to a room.");
                System.out.println("Note: A guest cannot check in multiple times simultaneously.");
                break;
            case -5:
                System.out.println("\nCheck-in failed: Room " + roomNo + " is reserved for another guest.");
                System.out.println("Note: You can only check in to rooms that are 'Ready for Check-In' or reserved specifically for this guest.");
                break;
        }
    }

    private void handleRoomTransfer() {
        System.out.println("\n--- Process Room Transfer (Change Room Mid-Stay) ---");
        String confirmNo = readValidConfirmationNumber("Enter 8-digit Confirmation Number (e.g. 10000001): ");

        Guest g = controller.searchGuestByConfirmationNumber(confirmNo);
        if (g == null) {
            System.out.println("Room Transfer failed: Confirmation number does not exist.");
            return;
        }

        if (!g.isCheckedIn()) {
            System.out.println("\nRoom Transfer failed: Guest (" + confirmNo + " - " + g.getGuestName() + ") is NOT currently checked in to any room.");
            System.out.println("Note: Room Transfer is only available for guests currently checked in.");
            return;
        }

        String oldRoomNo = g.getAssignedRoomNumber();
        Room currentRoom = (oldRoomNo != null) ? controller.searchRoomByNumber(oldRoomNo) : null;
        String roomTypeStr = (currentRoom != null) ? currentRoom.getRoomType() : "Unknown";

        System.out.println("\nGuest Found: " + g.getGuestName() + " (" + g.getLoyaltyTier() + " Member)");
        System.out.println("Current Occupied Room: Room " + oldRoomNo + " (" + roomTypeStr + ")");

        System.out.println("\nAvailable Rooms for Transfer:");
        displayRoomList();

        System.out.print("Enter NEW Room Number to transfer to: ");
        String newRoomNo = scanner.nextLine().trim();

        int result = controller.processRoomTransfer(confirmNo, newRoomNo);
        switch (result) {
            case 1:
                Room newRoom = controller.searchRoomByNumber(newRoomNo);
                System.out.println("\n==================================================");
                System.out.println("          ROOM TRANSFER SUCCESSFUL!               ");
                System.out.println("==================================================");
                System.out.printf(" Guest Name        : %s\n", g.getGuestName());
                System.out.printf(" Old Room Released : Room %s (Status set to [Dirty] for Housekeeping)\n", oldRoomNo);
                System.out.printf(" New Room Assigned : Room %s (%s - Status: [Occupied])\n", newRoomNo, (newRoom != null ? newRoom.getRoomType() : ""));
                System.out.printf(" New Room Rate     : RM %.2f/night\n", g.getEffectiveRoomRate());
                System.out.println("==================================================");
                break;
            case -3:
                System.out.println("\nRoom Transfer failed: Room " + newRoomNo + " does not exist.");
                break;
            case -4:
                Room r = controller.searchRoomByNumber(newRoomNo);
                String st = (r != null) ? r.getRoomStatus() : "Unavailable";
                System.out.println("\nRoom Transfer failed: Room " + newRoomNo + " is currently [" + st + "].");
                System.out.println("Note: Guest can only transfer to rooms with status [Ready for Check-In].");
                break;
            case -5:
                System.out.println("\nRoom Transfer failed: Guest (" + confirmNo + ") is ALREADY occupied in Room " + newRoomNo + ".");
                break;
            default:
                System.out.println("\nRoom Transfer failed.");
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

        if (!guest.isCheckedIn()) {
            System.out.println("Billing Error: Guest (" + confirmNo + " - " + guest.getGuestName() + ") is NOT currently checked in to any room.");
            System.out.println("Note: Please process Guest Check-In (Option 4) before generating billing.");
            return;
        }

        String roomNo = guest.getAssignedRoomNumber();
        Room room = (roomNo != null) ? controller.searchRoomByNumber(roomNo) : null;

        if (room == null) {
            System.out.println("Error: No valid room record assigned for this guest.");
            return;
        }

        System.out.println("Auto-retrieved Stay Information: Room " + roomNo + " (" + room.getRoomType() + ")");

        System.out.print("Enter number of nights stayed: ");
        int nights = readPositiveIntInput();

        double nightRate = (guest.getEffectiveRoomRate() > 0) ? guest.getEffectiveRoomRate() : room.getPrice();
        double discountRate = controller.getDiscountPercentage(guest.getLoyaltyTier());
        double subtotal = nightRate * nights;
        double discountAmt = subtotal * discountRate;
        double total = subtotal - discountAmt;

        System.out.println("\n==================================================");
        System.out.println("                 RESORT INVOICE                   ");
        System.out.println("==================================================");
        System.out.printf(" Confirmation No : %s\n", guest.getConfirmationNumber());
        System.out.printf(" Guest Name      : %s\n", guest.getGuestName());
        System.out.printf(" IC / Passport   : %s\n", guest.getIcNo());
        System.out.printf(" Membership Tier : %s (%.0f%% Discount)\n", guest.getLoyaltyTier(), discountRate * 100);
        System.out.printf(" Room Assigned   : Room %s (%s)\n", room.getRoomNumber(), room.getRoomType());
        if (nightRate < room.getPrice()) {
            System.out.printf(" Upgrade Benefit : Free Upgrade (Saved RM %.2f/night!)\n", (room.getPrice() - nightRate));
        }
        System.out.printf(" Stay Duration   : %d Night(s)\n", nights);
        System.out.println("--------------------------------------------------");
        System.out.printf(" Charged Rate/Nt : RM %8.2f\n", nightRate);
        System.out.printf(" Subtotal        : RM %8.2f\n", subtotal);
        System.out.printf(" Tier Discount   :-RM %8.2f\n", discountAmt);
        System.out.println("--------------------------------------------------");
        System.out.printf(" Total Payable   : RM %8.2f\n", total);
        System.out.println("==================================================");

        System.out.print("\nComplete Check-Out for this guest now? (Y/N): ");
        String checkOutAns = scanner.nextLine().trim();
        if (checkOutAns.equalsIgnoreCase("Y")) {
            // Earn loyalty points (RM 10 = 1 point)
            int earnedPoints = (int) (total / 10.0);
            int updatedPoints = guest.getLoyaltyPoints() + earnedPoints;
            guest.setLoyaltyPoints(updatedPoints);

            // Auto-promote loyalty tier
            if (updatedPoints >= 1000) {
                guest.setLoyaltyTier("Platinum");
            } else if (updatedPoints >= 500) {
                guest.setLoyaltyTier("Gold");
            } else if (updatedPoints >= 200) {
                guest.setLoyaltyTier("Silver");
            } else {
                guest.setLoyaltyTier("Standard");
            }

            int coResult = controller.processCheckOut(confirmNo);
            if (coResult == 1) {
                System.out.println("\nCheck-Out Successful!");
                System.out.println("Guest (" + confirmNo + ") is checked out. Room " + roomNo + " status updated to [Dirty] for Housekeeping.");
                System.out.println("--------------------------------------------------");
                System.out.printf("🎉 LOYALTY REWARD: Earned +%d Points for this stay!\n", earnedPoints);
                System.out.printf("   Total Points: %d pts | Current Tier: %s\n", guest.getLoyaltyPoints(), guest.getLoyaltyTier());
                System.out.println("--------------------------------------------------");
            } else {
                System.out.println("Check-Out Failed.");
            }
        }
    }

    private void displayReportsSubmenu() {
        int reportChoice = -1;
        do {
            System.out.println("\n--------------------------------------------------");
            System.out.println("            MANAGEMENT REPORTS                    ");
            System.out.println("--------------------------------------------------");
            System.out.println("1. Room Status & Occupancy Summary");
            System.out.println("2. Filtered & Sorted Room Pricing Report");
            System.out.println("3. Registered Guest Directory & Demographic Report");
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
        int choice = -1;
        do {
            String[] stats = controller.getGuestTreeDiagnostics();
            System.out.println("\n==================================================");
            System.out.println("       BST DIAGNOSTICS & VISUALIZER SUBMENU       ");
            System.out.println("==================================================");
            System.out.printf(" Total Nodes      : %s\n", stats[0]);
            System.out.printf(" Tree Height      : %s\n", stats[1]);
            System.out.printf(" Leaf Node Count  : %s\n", stats[2]);
            System.out.printf(" Balance Status   : %s\n", stats[3]);
            System.out.printf(" Smallest Key     : %s\n", stats[4]);
            System.out.printf(" Largest Key      : %s\n", stats[5]);
            System.out.println("--------------------------------------------------");
            System.out.println("1. Render Guest BST ASCII Tree Visualizer");
            System.out.println("2. Render Room BST ASCII Tree Visualizer");
            System.out.println("3. Compare BST Traversals (In-Order / Pre-Order / Post-Order)");
            System.out.println("4. Rebalance Binary Search Trees");
            System.out.println("0. Back to Main Menu");
            System.out.println("--------------------------------------------------");
            System.out.print("Enter choice (0-4): ");

            choice = readIntInput();
            System.out.println();

            switch (choice) {
                case 1:
                    controller.printGuestTreeStructure();
                    break;
                case 2:
                    controller.printRoomTreeStructure();
                    break;
                case 3:
                    handleTraversalComparison();
                    break;
                case 4:
                    System.out.println("Rebalancing Binary Search Trees...");
                    controller.rebalanceTrees();
                    System.out.println("Rebalance complete! Tree height-balanced successfully.");
                    break;
                case 0:
                    System.out.println("Returning to Main Menu...");
                    break;
                default:
                    System.out.println("Invalid option. Enter a number between 0 and 4.");
            }
        } while (choice != 0);
    }

    private void handleTraversalComparison() {
        System.out.println("\n--- BST Traversal Comparison ---");
        System.out.println("1. In-Order Traversal  (Left -> Root -> Right) [Sorted Order]");
        System.out.println("2. Pre-Order Traversal (Root -> Left -> Right) [Copy / Structure]");
        System.out.println("3. Post-Order Traversal (Left -> Right -> Root) [Bottom-Up / Deletion]");
        System.out.print("Select traversal mode (1-3): ");
        int mode = readIntInput();

        ListInterface<Guest> result = controller.getGuestTraversal(mode);
        String modeName = (mode == 1) ? "In-Order" : (mode == 2) ? "Pre-Order" : "Post-Order";

        System.out.println("\n==================================================");
        System.out.printf(" GUEST TRAVERSAL RESULT [%s Mode]: %d items\n", modeName, result.getNumberOfEntries());
        System.out.println("==================================================");
        for (int i = 0; i < result.getNumberOfEntries(); i++) {
            Guest g = result.get(i);
            System.out.printf(" [%2d] Confirmation: %-10s | Name: %-15s | Tier: %-8s\n",
                    (i + 1), g.getConfirmationNumber(), g.getGuestName(), g.getLoyaltyTier());
        }
        System.out.println("==================================================");
    }

    private void displayReport1() {
        int[] summary = controller.getRoomStatusSummary();
        double occupancyRate = controller.calculateOccupancyRate();
        double estRevenue = controller.calculateEstimatedDailyRevenue();

        System.out.println("\n==================================================");
        System.out.println("     REPORT 1: ROOM OCCUPANCY & FINANCIAL SUMMARY ");
        System.out.println("==================================================");
        System.out.printf(" Total Rooms                : %d\n", summary[0]);
        System.out.printf(" Ready for Check-In         : %d\n", summary[1]);
        System.out.printf(" Occupied                   : %d\n", summary[2]);
        System.out.printf(" Dirty                      : %d\n", summary[3]);
        System.out.printf(" Cleaning In Progress       : %d\n", summary[4]);
        System.out.printf(" Reserved                   : %d\n", summary[5]);
        System.out.println("--------------------------------------------------");
        System.out.printf(" Occupancy Rate             : %.1f%%\n", occupancyRate);
        System.out.printf(" Estimated Daily Revenue    : RM %.2f\n", estRevenue);
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
        System.out.println("\n--- REPORT 3: REGISTERED GUEST DIRECTORY ---");
        System.out.print("Enter Membership Tier filter (Platinum / Gold / Silver / Standard / ALL): ");
        String tierFilter = scanner.nextLine().trim();

        System.out.print("Enter Minimum Loyalty Points Threshold: ");
        int minPoints = readIntInput();

        ListInterface<Guest> filteredGuests = controller.getFilteredAndSortedGuests(tierFilter, minPoints);

        System.out.println("\n==========================================================================");
        System.out.printf(" REGISTERED GUEST DIRECTORY: %d guest(s) found\n", filteredGuests.getNumberOfEntries());
        System.out.println("==========================================================================");
        System.out.printf("%-5s | %-18s | %-12s | %-12s | %-10s\n", "No", "Guest Name", "Confirm No", "Tier", "Points");
        System.out.println("--------------------------------------------------------------------------");
        for (int i = 0; i < filteredGuests.getNumberOfEntries(); i++) {
            Guest g = filteredGuests.get(i);
            System.out.printf("%-5d | %-18s | %-12s | %-12s | %-10d\n",
                    (i + 1), g.getGuestName(), g.getConfirmationNumber(), g.getLoyaltyTier(), g.getLoyaltyPoints());
        }
        System.out.println("==========================================================================");
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
            if (val > 0)
                return val;
            System.out.print("Value must be greater than 0. Enter again: ");
        }
    }

    private void printGuestCard(Guest g) {
        System.out.println("--------------------------------------------------");
        System.out.printf(" Confirmation No : %s\n", g.getConfirmationNumber());
        System.out.printf(" Guest Name      : %s\n", g.getGuestName());
        System.out.printf(" IC / Passport   : %s\n", g.getIcNo());
        System.out.printf(" Loyalty Tier    : %s\n", g.getLoyaltyTier());
        System.out.printf(" Reward Points   : %d pts\n", g.getLoyaltyPoints());
        System.out.println("--------------------------------------------------");
    }

    public static void main(String[] args) {
        FrontDeskUI ui = new FrontDeskUI();
        ui.displayMenu();
    }
}