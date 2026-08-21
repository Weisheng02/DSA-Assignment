package boundary;

import adt.ListInterface;
import control.FrontDeskController;
import entity.Booking;
import entity.Guest;
import entity.Room;
import java.time.LocalDate;
import java.util.Scanner;

/**
 * Author: Weisheng
 * Boundary for Front-Desk operations. Entity/ADT work is delegated to the
 * controller.
 */
public class FrontDeskUI {
    private final FrontDeskController controller;
    private Scanner scanner;

    public FrontDeskUI(FrontDeskController controller) {
        if (controller == null)
            throw new IllegalArgumentException("FrontDeskController is required.");
        this.controller = controller;
    }

    public void displayMenu(Scanner input) {
        if (input != null)
            scanner = input;
        int choice;
        do {
            System.out.println("\n--------------------------------------------------");
            System.out.println("            FRONT DESK MANAGEMENT SYSTEM          ");
            System.out.println("--------------------------------------------------");
            System.out.println("1. Search Guest Information");
            System.out.println("2. Manage Guest Records");
            System.out.println("3. Search Room Availability");
            System.out.println("4. Display All Room Statuses");
            System.out.println("5. Process Guest Check-In");
            System.out.println("6. Process Room Transfer (Change Room)");
            System.out.println("7. Preview Billing & Receipt");
            System.out.println("8. Complete Guest Check-Out");
            System.out.println("9. Extend Active Stay");
            System.out.println("10. Management Reports");
            System.out.println("11. View Tree Structure Info");
            System.out.println("0. Back to Main Menu");
            System.out.println("--------------------------------------------------");
            System.out.print("Enter choice (0-11): ");
            choice = readMenuChoice(0, 11);
            switch (choice) {
                case 1:
                    guestSearch();
                    break;
                case 2:
                    guestManagement();
                    break;
                case 3:
                    roomAvailability();
                    break;
                case 4:
                    printAllRooms();
                    break;
                case 5:
                    checkIn();
                    break;
                case 6:
                    roomTransfer();
                    break;
                case 7:
                    billing();
                    break;
                case 8:
                    checkOut();
                    break;
                case 9:
                    extendStay();
                    break;
                case 10:
                    reports();
                    break;
                case 11:
                    diagnostics();
                    break;
                case 0:
                    System.out.println("Returning to Main Menu...");
                    break;
            }
            if (choice >= 3 && choice <= 9)
                pauseForEnter();
        } while (choice != 0);
    }

    private void guestSearch() {
        int mode;
        do {
            System.out.print(
                    "\n--- Guest Search ---\n1. Search by Confirmation Number\n2. Search by Guest Name\n3. Search by Confirmation Number Range\n4. Search by IC / Passport Number\n5. Search Stay by Booking ID\n6. Display All Guest Stay Records\n0. Return\n--------------------------------------------------\n"
                            + //
                            "Enter choice (0-6): ");
            mode = readMenuChoice(0, 6);
            if (mode == 1) {
                String confirmation = readConfirmation("Enter 8-digit Confirmation Number: ");
                if (confirmation != null)
                    printGuestDetails(controller.searchGuestByConfirmationNumber(confirmation));
            } else if (mode == 2) {
                String name = readRequiredTextOrBack("Enter guest name: ");
                if (name != null)
                    printGuestNameResults(name, controller.searchGuestsByNameArray(name));
            } else if (mode == 3) {
                String start = readConfirmation("Enter START Confirmation Number: ");
                if (start == null)
                    continue;
                String end = readConfirmation("Enter END Confirmation Number: ");
                if (end != null)
                    printGuestRangeResults(start, end, controller.searchGuestsByConfirmationRangeArray(start, end));
            } else if (mode == 4) {
                String ic = readRequiredTextOrBack("Enter IC / Passport Number: ");
                if (ic != null)
                    printGuestIcResults(ic, controller.searchGuestsByICArray(ic));
            } else if (mode == 5) {
                String bookingId = readRequiredTextOrBack("Enter Booking ID (e.g. BK0001): ");
                if (bookingId != null)
                    printBookingResult(controller.searchBookingById(bookingId));
            } else if (mode == 6) {
                printAllGuestRecords(controller.getAllGuestArray());
            }
            if (mode != 0)
                pauseForEnter();
        } while (mode != 0);
    }

    private void guestManagement() {
        int choice;
        do {
            System.out.print(
                    "\n--- Manage Guest Records ---\n1. Register New Guest\n2. Update Guest Profile\n3. Remove Unbooked Guest Record\n0. Return\n--------------------------------------------------\n"
                            + //
                            "Enter choice (0-3): ");
            choice = readMenuChoice(0, 3);
            if (choice == 1) {
                String name = readRequiredTextOrBack("Enter Guest Name: ");
                if (name == null)
                    continue;
                String ic = readRequiredTextOrBack("Enter IC / Passport Number: ");
                if (ic == null)
                    continue;
                System.out.print("Enter Phone Number (or press Enter to skip): ");
                String phone = scanner.nextLine().trim();
                printRegistrationResult(controller.registerGuestWithGeneratedConfirmation(name, ic, phone), ic);
            } else if (choice == 2)
                updateProfile();
            else if (choice == 3)
                removeGuest();
            if (choice != 0)
                pauseForEnter();
        } while (choice != 0);
    }

    private void updateProfile() {
        String confirmation = readConfirmation("Enter Confirmation Number to update: ");
        if (confirmation == null)
            return;
        String[] current = controller.getGuestProfileFields(confirmation);
        if (current == null) {
            System.out.println("Guest record not found.");
            return;
        }
        printGuestDetails(controller.searchGuestByConfirmationNumber(confirmation));
        System.out.println("Press Enter to keep the current value.");
        String name = optional("Guest Name", current[0]);
        String ic = optional("IC / Passport", current[1]);
        String phone = optional("Phone", current[2]);
        String gender = optional("Gender", current[3]);
        String nationality = optional("Nationality", current[4]);
        String email = optional("Email", current[5]);
        String request = optional("Special Request", current[6]);
        int result = controller.updateGuestProfile(confirmation, name, ic, phone, gender, nationality, email, request);
        if (result == 1) {
            System.out.println("Guest profile updated successfully. Confirmation Number remains unchanged.");
            printGuestDetails(controller.searchGuestByConfirmationNumber(confirmation));
        } else if (result == -3)
            System.out.println("Update failed: IC / Passport is already used by another guest.");
        else if (result == -4)
            System.out.println("Update failed: Email format is invalid.");
        else if (result == -5)
            System.out.println("Update failed: Phone must contain 7-15 digits and valid phone symbols only.");
        else if (result == -6)
            System.out.println("Update failed: Loyalty identity and transaction history could not be migrated safely.");
        else
            System.out.println("Update failed: Guest name and IC / Passport are required.");
    }

    private void removeGuest() {
        String ic = readRequiredTextOrBack("Enter IC / Passport Number to find guest records: ");
        if (ic == null)
            return;
        Guest[] matches = controller.searchGuestsByICArray(ic);
        if (matches.length == 0) {
            System.out.println("No guest record found for IC / Passport " + ic + ".");
            return;
        }
        Guest guest = matches[0];
        if (matches.length > 1) {
            System.out.println("Multiple stay records found. Select the exact record to remove:");
            printGuestResultTable(matches);
            String confirmation = readConfirmation("Enter Confirmation Number to remove: ");
            if (confirmation == null)
                return;
            guest = findGuestInResults(matches, confirmation);
            if (guest == null) {
                System.out.println("That Confirmation Number does not belong to the entered IC / Passport.");
                return;
            }
        }
        String confirmation = guest.getConfirmationNumber();
        if (!controller.canRemoveGuest(confirmation)) {
            System.out.println("Cannot remove this record: only an unbooked guest with Registered status can be "
                    + "removed. Reservations and stay history must be handled through cancellation or check-out.");
            return;
        }
        printGuestDetails(guest);
        String confirmationChoice = readYesNoOrBack("Permanently remove this guest record? (Y/N): ");
        if ("Y".equals(confirmationChoice)) {
            Guest removed = controller.removeGuest(confirmation);
            if (removed == null) {
                System.out.println("Guest removal failed.");
            } else {
                String room = removed.getAssignedRoomNumber();
                printOperationSummary("GUEST RECORD REMOVED",
                        new String[] { "Guest", "Confirmation No", "Released Room", "Booking Action" },
                        new String[] { removed.getGuestName(), confirmation, valueOrNa(room),
                                room == null || room.isEmpty() ? "No assigned room" : "Booking cancelled" });
            }
        } else if (confirmationChoice != null)
            System.out.println("Removal cancelled. No data was changed.");
    }

    private Guest findGuestInResults(Guest[] guests, String confirmation) {
        for (Guest guest : guests)
            if (guest.getConfirmationNumber().equalsIgnoreCase(confirmation))
                return guest;
        return null;
    }

    private void roomAvailability() {
        System.out.println("\n--- Room Availability Search ---");
        String date;
        int nights;
        while (true) {
            System.out.print("Check-In Date (YYYY-MM-DD, 0 to return): ");
            date = scanner.nextLine().trim();
            if ("0".equals(date))
                return;
            System.out.print("Number of Nights (1-30, 0 to return): ");
            nights = readInt();
            if (nights == 0)
                return;
            int validation = controller.validateStayPeriod(date, nights);
            if (validation == 1)
                break;
            System.out.println("Wrong input");
            printStayValidation(validation);
        }
        String type = readNumberedChoiceOrBack("Select Room Type:",
                "Standard Room", "Deluxe Suite", "Presidential Suite", "ALL");
        if (type == null)
            return;
        double max = readDouble("Maximum Price per Night (0 for no limit): ");
        System.out.print("Sort Price (1=Ascending, 2=Descending, 0=Return): ");
        int sort = readMenuChoice(0, 2);
        if (sort == 0)
            return;
        printAvailableRooms(date, nights,
                controller.searchAvailableRoomArray(date, nights, type, max, sort == 1));
    }

    private void checkIn() {
        System.out.println("\n--- Guest Check-In ---");
        printTodaysArrivals(controller.getTodaysReservedGuestArray());
        String confirmation = readConfirmation("Enter 8-digit Confirmation Number: ");
        if (confirmation == null)
            return;
        Guest guest = controller.searchGuestByConfirmationNumber(confirmation);
        if (guest == null) {
            System.out.println("Check-in failed: Confirmation number does not exist.");
            return;
        }
        printCheckInGuestInfo(guest);
        System.out.print("Room Number (press Enter for assigned room): ");
        String room = scanner.nextLine().trim();
        String selectedRoom = room.isEmpty() ? guest.getAssignedRoomNumber() : room;
        Room upgrade = controller.suggestRoomUpgrade(selectedRoom, confirmation);
        if (upgrade != null) {
            printOperationSummary("COMPLIMENTARY ROOM UPGRADE AVAILABLE",
                    new String[] { "Upgrade Room", "Room Type", "Published Rate", "Guest Charged Rate" },
                    new String[] { upgrade.getRoomNumber(), upgrade.getRoomType(),
                            String.format("RM %.2f/night", upgrade.getPrice()),
                            String.format("RM %.2f/night (original rate)",
                                    controller.getCheckInRate(confirmation, selectedRoom)) });
            String upgradeChoice = readYesNoOrBack("Accept complementary upgrade? (Y/N, 0 to return): ");
            if (upgradeChoice == null)
                return;
            if ("Y".equals(upgradeChoice))
                selectedRoom = upgrade.getRoomNumber();
        }
        System.out.print("Any Special Request? (Enter request or press Enter to skip): ");
        String request = scanner.nextLine().trim();
        int result = controller.processCheckIn(confirmation, selectedRoom, request);
        if (result != 1) {
            System.out.println(checkInError(result, confirmation, selectedRoom));
            return;
        }
        Guest updated = controller.searchGuestByConfirmationNumber(confirmation);
        Room occupiedRoom = controller.searchRoomByNumber(selectedRoom);
        printOperationSummary("CHECK-IN SUCCESSFUL",
                new String[] { "Guest", "Confirmation No", "Room / Type", "Room Status", "Actual Check-In",
                        "Booked Duration" },
                new String[] { updated.getGuestName(), updated.getConfirmationNumber(),
                        selectedRoom + " / " + (occupiedRoom == null ? "Unknown" : occupiedRoom.getRoomType()),
                        occupiedRoom == null ? "Occupied" : occupiedRoom.getRoomStatus(), updated.getCheckInDate(),
                        updated.getNumberOfNights() + " night(s)" });
    }

    private void roomTransfer() {
        System.out.println("\n--- Process Room Transfer (Change Room Mid-Stay) ---");
        String confirmation = readConfirmation("Enter 8-digit Confirmation Number: ");
        if (confirmation == null)
            return;
        Guest guest = controller.searchGuestByConfirmationNumber(confirmation);
        if (guest == null) {
            System.out.println("Room Transfer failed: Confirmation number does not exist.");
            return;
        }
        if (!guest.isCheckedIn()) {
            System.out.println("Room Transfer unavailable: " + guest.getGuestName()
                    + " is not currently checked in (Status: " + guest.getBookingStatus() + ").");
            return;
        }
        Room currentRoom = controller.searchRoomByNumber(guest.getAssignedRoomNumber());
        printOperationSummary("CURRENT STAY DETAILS",
                new String[] { "Guest", "Confirmation No", "Membership", "Current Room", "Room Type",
                        "Room Status" },
                new String[] { guest.getGuestName(), guest.getConfirmationNumber(), guest.getLoyaltyTier(),
                        valueOrNa(guest.getAssignedRoomNumber()), currentRoom == null ? "Unknown" : currentRoom.getRoomType(),
                        currentRoom == null ? "Unknown" : currentRoom.getRoomStatus() });
        Room[] availableRooms = controller.getAvailableTransferRoomArray(confirmation);
        System.out.println("Available Rooms for Transfer:");
        printTransferRooms(availableRooms);
        if (availableRooms.length == 0) {
            System.out.println("No rooms are ready and conflict-free for the remaining stay.");
            return;
        }
        String room = readRequiredTextOrBack("Enter NEW Room Number to transfer to: ");
        if (room == null)
            return;
        String oldRoom = guest.getAssignedRoomNumber();
        int result = controller.processRoomTransfer(confirmation, room);
        if (result != 1) {
            System.out.println(roomTransferError(result, room, confirmation));
            return;
        }
        Room newRoom = controller.searchRoomByNumber(room);
        printOperationSummary("ROOM TRANSFER SUCCESSFUL",
                new String[] { "Guest", "Confirmation No", "Old Room", "Old Room Status", "New Room / Type",
                        "New Room Status", "Charged Rate" },
                new String[] { guest.getGuestName(), guest.getConfirmationNumber(), oldRoom, "Dirty",
                        room + " / " + (newRoom == null ? "Unknown" : newRoom.getRoomType()),
                        newRoom == null ? "Occupied" : newRoom.getRoomStatus(),
                        String.format("RM %.2f/night (original rate preserved)", guest.getRoomRate()) });
    }

    private void extendStay() {
        System.out.println("\n--- Extend Active Stay ---");
        String confirmation = readConfirmation("Enter 8-digit Confirmation Number: ");
        if (confirmation == null)
            return;
        Guest guest = controller.searchGuestByConfirmationNumber(confirmation);
        if (guest == null || !guest.isCheckedIn()) {
            System.out.println("Only a currently checked-in guest can extend a stay.");
            return;
        }
        int currentNights = guest.getNumberOfNights();
        printOperationSummary("CURRENT STAY DETAILS",
                new String[] { "Guest", "Confirmation No", "Room", "Current Duration" },
                new String[] { guest.getGuestName(), guest.getConfirmationNumber(),
                        valueOrNa(guest.getAssignedRoomNumber()), currentNights + " night(s)" });
        System.out.print("Additional nights requested (0 to return): ");
        int nights = readPositiveInt();
        if (nights == 0)
            return;
        int result = controller.extendStay(confirmation, nights);
        if (result == 1) {
            printOperationSummary("STAY EXTENDED SUCCESSFULLY",
                    new String[] { "Guest", "Room", "Previous Duration", "Additional Nights", "New Duration" },
                    new String[] { guest.getGuestName(), valueOrNa(guest.getAssignedRoomNumber()),
                            currentNights + " night(s)", nights + " night(s)",
                            guest.getNumberOfNights() + " night(s)" });
        } else if (result == -4)
            System.out
                    .println("Extension rejected: the room is already reserved for another guest during those dates.");
        else if (result == -5)
            System.out.println("Extension rejected: total stay must not exceed 30 nights.");
        else if (result == -3)
            System.out.println("Extension rejected: the current stay date is invalid.");
        else
            System.out.println("Extension could not be completed.");
    }

    private void billing() {
        System.out.println("\n--- Billing & Receipt Preview ---");
        String confirmation = readConfirmation("Enter 8-digit Confirmation Number: ");
        if (confirmation == null)
            return;
        FrontDeskController.BillingDetails bill = controller.calculateBill(confirmation);
        if (bill == null) {
            System.out.println("Billing unavailable. Guest must be checked in with a valid assigned room.");
            return;
        }
        printBillingReceipt(bill, false, 0);
    }

    private void checkOut() {
        System.out.println("\n--- Complete Guest Check-Out ---");
        String confirmation = readConfirmation("Enter 8-digit Confirmation Number: ");
        if (confirmation == null)
            return;
        FrontDeskController.BillingDetails bill = controller.calculateBill(confirmation);
        if (bill == null) {
            System.out.println("Billing unavailable. Guest must be checked in with a valid assigned room.");
            return;
        }
        printBillingReceipt(bill, false, 0);
        String paymentChoice = readYesNoOrBack("Confirm payment and complete check-out? (Y/N, 0 to return): ");
        if ("Y".equals(paymentChoice)) {
            FrontDeskController.CheckoutResult result = controller.completeCheckOutAndReward(confirmation);
            printCheckoutResult(result);
        } else if (paymentChoice != null)
            System.out.println("Check-out cancelled. No data was changed.");
    }

    private void reports() {
        int choice;
        do {
            System.out.println(
                    "\n--- MANAGEMENT REPORTS ---\n1. Room Status & Occupancy Summary\n2. Filtered & Sorted Room Pricing Report\n3. Loyalty Member Segment & Stay Status Report\n0. Return");
            System.out.print("Enter choice (0-3): ");
            choice = readMenuChoice(0, 3);
            if (choice == 1)
                printRoomStatusReport();
            else if (choice == 2)
                reportRooms();
            else if (choice == 3)
                reportGuests();
            if (choice != 0)
                pauseForEnter();
        } while (choice != 0);
    }

    private void reportRooms() {
        String status = readNumberedChoiceOrBack("Select Room Status:",
                "Ready for Check-In", "Occupied", "Dirty", "Cleaning In Progress", "Inspected",
                "Reserved", "Maintenance", "ALL");
        if (status == null)
            return;
        String type = readNumberedChoiceOrBack("Select Room Type:",
                "Standard Room", "Deluxe Suite", "Presidential Suite", "ALL");
        if (type == null)
            return;
        double min = readDouble("Enter Minimum Price per night: ");
        double max = readDouble("Enter Maximum Price per night (0 for no limit): ");
        System.out.print("Sort by price? (1=Ascending, 2=Descending, 0=Return): ");
        int sort = readMenuChoice(0, 2);
        if (sort != 0) {
            Room[] rooms = controller.getFilteredAndSortedRoomArray(status, type, min, max, sort == 1);
            printRoomPricingReport(rooms);
        }
    }

    private void reportGuests() {
        String tier = readNumberedChoiceOrBack("Select Membership Tier:",
                "Standard", "Silver", "Gold", "Platinum", "ALL");
        if (tier == null)
            return;
        String status = readNumberedChoiceOrBack("Select Stay Status:",
                "Registered", "Waiting", "Reserved", "CheckedIn", "CheckedOut", "Cancelled", "NoShow", "ALL");
        if (status == null)
            return;
        System.out.print("Enter Minimum Loyalty Points Threshold: ");
        int points = readInt();
        while (points < 0) {
            System.out.println("Wrong input");
            System.out.print("Enter Minimum Loyalty Points Threshold (0 or above): ");
            points = readInt();
        }
        System.out.print("Sort (1=Points Descending, 2=Guest Name Ascending, 0=Return): ");
        int sort = readMenuChoice(0, 2);
        if (sort != 0) {
            Guest[] guests = controller.getFilteredAndSortedGuestArray(tier, status, points, sort == 1);
            printGuestSegmentReport(guests);
        }
    }

    private void diagnostics() {
        int choice;
        do {
            int[] guestStats = controller.getGuestTreeStatistics();
            int[] roomStats = controller.getRoomTreeStatistics();
            Guest smallest = controller.getSmallestGuest();
            Guest largest = controller.getLargestGuest();
            System.out.printf("%nGuest BST: Nodes=%d | Height=%d | Leaves=%d | %s | Smallest=%s | Largest=%s%n",
                    guestStats[0], guestStats[1], guestStats[2],
                    balanceLabel(controller.isGuestTreeBalanced()),
                    guestIdentity(smallest), guestIdentity(largest));
            System.out.printf("Room BST:  Nodes=%d | Height=%d | Leaves=%d | %s%n",
                    roomStats[0], roomStats[1], roomStats[2],
                    balanceLabel(controller.isRoomTreeBalanced()));
            System.out.println(
                    "1. Render Guest BST\n2. Render Room BST\n3. Compare Traversals\n4. Rebalance Trees\n0. Return");
            System.out.print("Enter choice (0-4): ");
            choice = readMenuChoice(0, 4);
            if (choice == 1) {
                System.out.println("\n=== Guest BST ASCII Visualizer ===\n" + controller.getGuestTreeStructure());
                printGuestTreeNodeDetails(controller.getGuestTraversalArray(1));
            } else if (choice == 2) {
                System.out.println("\n=== Room BST ASCII Visualizer ===\n" + controller.getRoomTreeStructure());
                printRoomTreeNodeDetails(controller.getAllRooms());
            } else if (choice == 3) {
                System.out.println("\n=== Guest BST Traversal Modes ===");
                System.out.println("1. In-Order   (Left -> Root -> Right) - sorted by confirmation number");
                System.out.println("2. Pre-Order  (Root -> Left -> Right) - root is visited first");
                System.out.println("3. Post-Order (Left -> Right -> Root) - root is visited last");
                System.out.println("0. Return");
                System.out.print("Select traversal mode (0-3): ");
                int mode = readMenuChoice(0, 3);
                if (mode != 0)
                    printTraversal(mode, controller.getGuestTraversalArray(mode));
            } else if (choice == 4) {
                int guestHeightBefore = guestStats[1];
                int roomHeightBefore = roomStats[1];
                boolean guestBalancedBefore = controller.isGuestTreeBalanced();
                boolean roomBalancedBefore = controller.isRoomTreeBalanced();
                controller.rebalanceTrees();
                int[] guestStatsAfter = controller.getGuestTreeStatistics();
                int[] roomStatsAfter = controller.getRoomTreeStatistics();
                System.out.println("\n=== Rebalance Result ===");
                printRebalanceResult("Guest BST", guestHeightBefore, guestStatsAfter[1],
                        guestBalancedBefore, controller.isGuestTreeBalanced());
                printRebalanceResult("Room BST ", roomHeightBefore, roomStatsAfter[1],
                        roomBalancedBefore, controller.isRoomTreeBalanced());
                System.out.println("Render either tree now to view its new structure.");
            }
            if (choice != 0)
                pauseForEnter();
        } while (choice != 0);
    }

    private String balanceLabel(boolean balanced) {
        return balanced ? "Balanced" : "Unbalanced";
    }

    private void printRebalanceResult(String treeName, int heightBefore, int heightAfter,
            boolean balancedBefore, boolean balancedAfter) {
        System.out.printf("%s: Height %d -> %d | %s -> %s%n",
                treeName, heightBefore, heightAfter,
                balanceLabel(balancedBefore), balanceLabel(balancedAfter));
    }

    private void printGuestDetails(Guest guest) {
        if (guest == null) {
            System.out.println("No guest record found.");
            return;
        }
        System.out.println("\nGUEST DETAILS");
        int[] widths = { 20, 32 };
        ConsoleTable.printHeader(new String[] { "Field", "Details" }, widths);
        ConsoleTable.printRow(new String[] { "Confirmation No", guest.getConfirmationNumber() }, widths);
        ConsoleTable.printRow(new String[] { "Guest Name", guest.getGuestName() }, widths);
        ConsoleTable.printRow(new String[] { "IC / Passport", guest.getIcNo() }, widths);
        ConsoleTable.printRow(new String[] { "Phone Number", guest.getPhoneNumber() }, widths);
        ConsoleTable.printRow(new String[] { "Booking Status", guest.getBookingStatus() }, widths);
        ConsoleTable.printRow(new String[] { "Loyalty Tier", guest.getLoyaltyTier() }, widths);
        ConsoleTable.printRow(new String[] { "Reward Points", guest.getLoyaltyPoints() + " pts" }, widths);
        ConsoleTable.printRow(new String[] { "Assigned Room", valueOrNa(guest.getAssignedRoomNumber()) }, widths);
        ConsoleTable.printFooter(widths);
    }

    private void printGuestResultTable(Guest[] guests) {
        int[] widths = { 4, 12, 18, 18, 13, 10, 8, 8 };
        ConsoleTable.printHeader(
                new String[] { "No.", "Confirmation", "Guest Name", "IC / Passport", "Status", "Tier", "Points",
                        "Room" },
                widths);
        for (int i = 0; i < guests.length; i++) {
            Guest guest = guests[i];
            ConsoleTable.printRow(new String[] { String.valueOf(i + 1), guest.getConfirmationNumber(),
                    guest.getGuestName(), guest.getIcNo(), guest.getBookingStatus(), guest.getLoyaltyTier(),
                    String.valueOf(guest.getLoyaltyPoints()), valueOrNa(guest.getAssignedRoomNumber()) }, widths);
        }
        ConsoleTable.printFooter(widths);
    }

    private void printBookingDetails(Booking booking) {
        System.out.println("\nBOOKING DETAILS");
        int[] widths = { 20, 42 };
        ConsoleTable.printHeader(new String[] { "Field", "Details" }, widths);
        ConsoleTable.printRow(new String[] { "Booking ID", booking.getBookingId() }, widths);
        ConsoleTable.printRow(new String[] { "Guest", booking.getGuestName() }, widths);
        ConsoleTable.printRow(new String[] { "Confirmation No", booking.getGuestConfirmationNumber() }, widths);
        ConsoleTable.printRow(new String[] { "Room / Type", booking.getRoomNumber() + " / " + booking.getRoomType() },
                widths);
        ConsoleTable.printRow(new String[] { "Scheduled Stay",
                booking.getCheckInDate() + " to " + booking.getCheckOutDate() + " (" + booking.getNumberOfNights()
                        + " night(s))" },
                widths);
        ConsoleTable.printRow(new String[] { "Booking Status", booking.getBookingStatus() }, widths);
        ConsoleTable.printFooter(widths);
    }

    private void printOperationSummary(String title, String[] fields, String[] details) {
        System.out.println("\n" + title);
        int[] widths = { 22, 44 };
        ConsoleTable.printHeader(new String[] { "Field", "Details" }, widths);
        int rowCount = Math.min(fields.length, details.length);
        for (int i = 0; i < rowCount; i++)
            ConsoleTable.printRow(new String[] { fields[i], details[i] }, widths);
        ConsoleTable.printFooter(widths);
    }

    private void printGuestNameResults(String name, Guest[] guests) {
        if (guests.length == 0) {
            System.out.println("No guests found matching: \"" + name + "\"");
            return;
        }
        System.out.println("Found " + guests.length + " guest(s):");
        printGuestResultTable(guests);
    }

    private void printGuestIcResults(String ic, Guest[] guests) {
        if (guests.length == 0) {
            System.out.println("No guest record found for IC / Passport " + ic + ".");
        } else if (guests.length == 1) {
            printGuestDetails(guests[0]);
        } else {
            System.out.println("Found " + guests.length + " stay records for IC / Passport " + ic + ":");
            printGuestResultTable(guests);
        }
    }

    private void printAllGuestRecords(Guest[] guests) {
        System.out.println("\nALL GUEST STAY RECORDS (BST IN-ORDER BY CONFIRMATION NUMBER)");
        System.out.println("Total Records: " + guests.length);
        if (guests.length == 0) {
            System.out.println("The Master Guest Registry is empty.");
            return;
        }
        printGuestResultTable(guests);
    }

    private void printGuestRangeResults(String start, String end, Guest[] guests) {
        System.out.println("RANGE SEARCH RESULTS: " + guests.length + " guest(s) found [" + start + " - " + end + "]");
        if (guests.length == 0) {
            System.out.println("No guests found within the specified range.");
            return;
        }
        printGuestResultTable(guests);
    }

    private void printBookingResult(Booking booking) {
        if (booking == null) {
            System.out.println("Booking record not found.");
            return;
        }
        printBookingDetails(booking);
        Guest guest = controller.searchGuestByConfirmationNumber(booking.getGuestConfirmationNumber());
        if (guest != null)
            printGuestDetails(guest);
    }

    private void printRegistrationResult(FrontDeskController.GuestRegistrationResult result, String ic) {
        if (result.getStatus() == 1) {
            System.out.println("Guest registered successfully. Confirmation Number was generated automatically.");
            printGuestDetails(result.getGuest());
        } else if (result.getStatus() == -3) {
            Guest identity = controller.searchGuestByIC(ic);
            System.out.println("Registration failed: This IC / Passport is already linked to Confirmation "
                    + (identity == null ? "another guest" : identity.getConfirmationNumber()) + ".");
        } else if (result.getStatus() == -4) {
            System.out.println("Registration failed: No 8-digit Confirmation Number remains available.");
        } else {
            System.out.println(
                    "Failed to register guest. Check required fields, phone format, and duplicate IC.");
        }
    }

    private void printAllRooms() {
        int[] widths = { 10, 22, 22, 12 };
        ConsoleTable.printHeader(new String[] { "Room No", "Room Type", "Status", "Price/Night" }, widths);
        ListInterface<Room> rooms = controller.getAllRooms();
        for (int i = 0; i < rooms.getNumberOfEntries(); i++) {
            Room room = rooms.get(i);
            ConsoleTable.printRow(new String[] { room.getRoomNumber(), room.getRoomType(), room.getRoomStatus(),
                    String.format("RM %.2f", room.getPrice()) }, widths);
        }
        ConsoleTable.printFooter(widths);
    }

    private void printAvailableRooms(String date, int nights, Room[] rooms) {
        System.out.println("AVAILABLE ROOMS: " + rooms.length + " result(s) for " + date + ", " + nights + " night(s)");
        if (rooms.length == 0) {
            System.out.println("No room matches this date range, type and budget. Try broader filters.");
            return;
        }
        int[] widths = { 10, 22, 12, 12, 12 };
        ConsoleTable.printHeader(new String[] { "Room No", "Room Type", "Check-In", "Check-Out", "Price/Night" },
                widths);
        for (Room room : rooms)
            ConsoleTable.printRow(new String[] { room.getRoomNumber(), room.getRoomType(), date,
                    LocalDate.parse(date).plusDays(nights).toString(), String.format("RM %.2f", room.getPrice()) },
                    widths);
        ConsoleTable.printFooter(widths);
    }

    private void printTodaysArrivals(Guest[] arrivals) {
        System.out.println("Today's Reserved Arrivals (" + LocalDate.now() + "):");
        if (arrivals.length == 0) {
            System.out.println("No confirmed reservations are scheduled to arrive today.");
            return;
        }
        int[] widths = { 12, 18, 10, 20 };
        ConsoleTable.printHeader(new String[] { "Confirmation", "Guest Name", "Room No", "Room Type" }, widths);
        for (Guest guest : arrivals)
            ConsoleTable.printRow(new String[] { guest.getConfirmationNumber(), guest.getGuestName(),
                    guest.getAssignedRoomNumber(), guest.getRoomType() }, widths);
        ConsoleTable.printFooter(widths);
    }

    private void printCheckInGuestInfo(Guest guest) {
        String roomNumber = guest.getAssignedRoomNumber();
        Room room = roomNumber == null ? null : controller.searchRoomByNumber(roomNumber);
        printOperationSummary("CHECK-IN RESERVATION DETAILS",
                new String[] { "Guest", "Confirmation No", "Membership", "Reserved Room", "Room Type",
                        "Room Status", "Scheduled Arrival", "Booked Duration" },
                new String[] { guest.getGuestName(), guest.getConfirmationNumber(), guest.getLoyaltyTier(),
                        valueOrNa(roomNumber), room == null ? "N/A" : room.getRoomType(),
                        room == null ? "N/A" : room.getRoomStatus(), valueOrNa(guest.getCheckInDate()),
                        guest.getNumberOfNights() + " night(s)" });
    }

    private String checkInError(int result, String confirmation, String room) {
        switch (result) {
            case -2:
                return "Check-in failed: Room " + room + " does not exist.";
            case -3:
                return "Check-in failed: Room " + room + " is not ready.";
            case -4:
                return "Check-in failed: Guest (" + confirmation + ") has ALREADY checked in.";
            case -5:
                return "Check-in failed: Room " + room + " is reserved for another guest.";
            case -6:
                return "Check-in failed: Guest has ALREADY CHECKED OUT.";
            case -7:
                return "Check-in failed: Booking was CANCELLED.";
            case -8:
                return "Check-in failed: This reservation is marked as NO-SHOW.";
            case -9:
                return "Check-in failed: Arrival is earlier than scheduled.";
            case -10:
                return "Check-in failed: The scheduled arrival date has passed.";
            case -11:
                return "Check-in failed: Stored reservation date is invalid.";
            case -12:
                return "Check-in failed: Guest does not have Reserved status.";
            case -13:
                return "Check-in failed: No confirmed booking is linked.";
            default:
                return "Check-in failed.";
        }
    }

    private String roomTransferError(int result, String room, String confirmation) {
        switch (result) {
            case -3:
                return "Room Transfer failed: Room " + room + " does not exist.";
            case -4:
                return "Room Transfer failed: Room " + room + " is not ready.";
            case -5:
                return "Room Transfer failed: Guest (" + confirmation + ") is already in that room.";
            case -6:
                return "Room Transfer failed: Room has another overlapping reservation.";
            case -7:
                return "Room Transfer failed: Active stay dates are invalid.";
            default:
                return "Room Transfer failed.";
        }
    }

    private void printBillingReceipt(FrontDeskController.BillingDetails bill, boolean finalized,
            int earnedPoints) {
        Guest guest = bill.getGuest();
        Room room = bill.getRoom();
        Booking booking = bill.getBooking();
        System.out.println(finalized ? "\nFINAL CHECK-OUT RECEIPT" : "\nBILLING RECEIPT PREVIEW");
        int[] widths = { 24, 40 };
        ConsoleTable.printHeader(new String[] { "Receipt Field", "Details" }, widths);
        ConsoleTable.printRow(new String[] { "Payment Status", finalized ? "PAID / CHECKED OUT" : "PREVIEW ONLY" },
                widths);
        ConsoleTable.printRow(new String[] { "Booking ID", booking == null ? "Walk-In / N/A" : booking.getBookingId() },
                widths);
        ConsoleTable.printRow(new String[] { "Confirmation No", guest.getConfirmationNumber() }, widths);
        ConsoleTable.printRow(new String[] { "Guest", guest.getGuestName() + " (" + guest.getIcNo() + ")" }, widths);
        ConsoleTable.printRow(new String[] { "Membership", guest.getLoyaltyTier() + String.format(" (%.0f%% discount)",
                bill.getDiscountRate() * 100) }, widths);
        ConsoleTable.printRow(new String[] { "Room", room.getRoomNumber() + " - " + room.getRoomType() }, widths);
        if (booking != null)
            ConsoleTable.printRow(new String[] { "Stay Period", booking.getCheckInDate() + " to "
                    + booking.getCheckOutDate() }, widths);
        ConsoleTable.printRow(new String[] { "Billable Nights", bill.getNights() + " night(s)" }, widths);
        ConsoleTable.printRow(new String[] { "Rate per Night", String.format("RM %.2f", bill.getChargedRate()) }, widths);
        ConsoleTable.printRow(new String[] { "Room Subtotal", String.format("RM %.2f", bill.getSubtotal()) }, widths);
        ConsoleTable.printRow(new String[] { "Tier Discount", String.format("- RM %.2f", bill.getDiscountAmount()) },
                widths);
        ConsoleTable.printRow(new String[] { "TOTAL PAYABLE", String.format("RM %.2f", bill.getTotal()) }, widths);
        ConsoleTable.printRow(new String[] { finalized ? "Points Awarded" : "Projected Points",
                "+" + (finalized ? earnedPoints : bill.getProjectedPoints()) + " pts" }, widths);
        if (finalized) {
            ConsoleTable.printRow(new String[] { "Updated Point Balance", guest.getLoyaltyPoints() + " pts" }, widths);
            ConsoleTable.printRow(new String[] { "Room Follow-Up", "Room " + room.getRoomNumber()
                    + " marked Dirty for Housekeeping" }, widths);
        }
        ConsoleTable.printFooter(widths);
        if (!finalized)
            System.out.println("Preview only. Complete check-out to finalize payment and award points.");
    }

    private void printCheckoutResult(FrontDeskController.CheckoutResult result) {
        if (result.getStatus() != 1) {
            System.out.println(result.getStatus() == -8
                    ? "Check-out stopped: loyalty reward validation failed or was already processed."
                    : "Check-out failed. No loyalty points were awarded.");
            return;
        }
        FrontDeskController.BillingDetails bill = result.getBill();
        printBillingReceipt(bill, true, result.getEarnedPoints());
    }

    private void printTransferRooms(Room[] rooms) {
        if (rooms.length == 0)
            return;
        int[] widths = { 10, 24, 22 };
        ConsoleTable.printHeader(new String[] { "Room No", "Room Type", "Status" }, widths);
        for (Room room : rooms)
            ConsoleTable.printRow(new String[] { room.getRoomNumber(), room.getRoomType(), room.getRoomStatus() },
                    widths);
        ConsoleTable.printFooter(widths);
    }

    private void printRoomStatusReport() {
        int[] summary = controller.getRoomStatusSummary();
        int[] widths = { 28, 18 };
        ConsoleTable.printHeader(new String[] { "Room Status Metric", "Value" }, widths);
        ConsoleTable.printRow(new String[] { "Total Rooms", String.valueOf(summary[0]) }, widths);
        ConsoleTable.printRow(new String[] { "Ready for Check-In", String.valueOf(summary[1]) }, widths);
        ConsoleTable.printRow(new String[] { "Occupied", String.valueOf(summary[2]) }, widths);
        ConsoleTable.printRow(new String[] { "Dirty", String.valueOf(summary[3]) }, widths);
        ConsoleTable.printRow(new String[] { "Cleaning In Progress", String.valueOf(summary[4]) }, widths);
        ConsoleTable.printRow(new String[] { "Inspected", String.valueOf(summary[5]) }, widths);
        ConsoleTable.printRow(new String[] { "Reserved", String.valueOf(summary[6]) }, widths);
        ConsoleTable.printRow(new String[] { "Maintenance", String.valueOf(summary[7]) }, widths);
        ConsoleTable.printRow(
                new String[] { "Occupancy Rate", String.format("%.1f%%", controller.calculateOccupancyRate()) },
                widths);
        ConsoleTable.printRow(new String[] { "Net Daily Room Revenue",
                String.format("RM %.2f", controller.calculateEstimatedDailyRevenue()) }, widths);
        ConsoleTable.printRow(new String[] { "Average Daily Rate (ADR)",
                String.format("RM %.2f", controller.calculateAverageDailyRate()) }, widths);
        ConsoleTable.printRow(new String[] { "Housekeeping Blocked Rooms",
                String.valueOf(summary[3] + summary[4] + summary[5] + summary[7]) }, widths);
        ConsoleTable.printFooter(widths);
    }

    private void printRoomPricingReport(Room[] rooms) {
        System.out.println("ROOM PRICING RESULTS: " + rooms.length + " room(s) found");
        int[] widths = { 10, 22, 22, 12 };
        ConsoleTable.printHeader(new String[] { "Room No", "Room Type", "Status", "Price/Night" }, widths);
        for (Room room : rooms)
            ConsoleTable.printRow(new String[] { room.getRoomNumber(), room.getRoomType(), room.getRoomStatus(),
                    String.format("RM %.2f", room.getPrice()) }, widths);
        ConsoleTable.printFooter(widths);
        if (rooms.length > 0) {
            double[] summary = controller.calculateRoomPriceSummary(rooms);
            System.out.printf("PRICE SUMMARY: Lowest RM %.2f | Average RM %.2f | Highest RM %.2f%n",
                    summary[0], summary[1], summary[2]);
        }
    }

    private void printGuestSegmentReport(Guest[] guests) {
        System.out.println("LOYALTY SEGMENT: " + guests.length + " guest(s) found");
        int[] widths = { 18, 12, 10, 12, 8 };
        ConsoleTable.printHeader(new String[] { "Guest Name", "Confirmation", "Tier", "Stay Status", "Points" },
                widths);
        for (Guest guest : guests)
            ConsoleTable.printRow(new String[] { guest.getGuestName(), guest.getConfirmationNumber(),
                    guest.getLoyaltyTier(), guest.getBookingStatus(), String.valueOf(guest.getLoyaltyPoints()) },
                    widths);
        ConsoleTable.printFooter(widths);
        if (guests.length > 0) {
            double[] summary = controller.calculateGuestPointSummary(guests);
            System.out.printf("SEGMENT SUMMARY: Total Points %,d | Average %.1f points per guest%n",
                    (int) summary[0], summary[1]);
        }
    }

    private void printTraversal(int mode, Guest[] guests) {
        String name = mode == 1 ? "In-Order" : mode == 2 ? "Pre-Order" : "Post-Order";
        System.out.println("GUEST TRAVERSAL RESULT [" + name + " Mode]: " + guests.length + " items");
        int[] widths = { 5, 12, 18, 10 };
        ConsoleTable.printHeader(new String[] { "No.", "Confirmation", "Guest Name", "Tier" }, widths);
        for (int i = 0; i < guests.length; i++)
            ConsoleTable.printRow(new String[] { String.valueOf(i + 1), guests[i].getConfirmationNumber(),
                    guests[i].getGuestName(), guests[i].getLoyaltyTier() }, widths);
        ConsoleTable.printFooter(widths);
    }

    private void printGuestTreeNodeDetails(Guest[] guests) {
        System.out.println("Guest Node Details (In-Order: smallest to largest)");
        int[] widths = { 12, 20, 13 };
        ConsoleTable.printHeader(new String[] { "Confirmation", "Guest Name", "Status" }, widths);
        for (Guest guest : guests)
            ConsoleTable.printRow(new String[] { guest.getConfirmationNumber(), guest.getGuestName(),
                    guest.getBookingStatus() }, widths);
        ConsoleTable.printFooter(widths);
    }

    private void printRoomTreeNodeDetails(ListInterface<Room> rooms) {
        System.out.println("Room Node Details");
        int[] widths = { 10, 22, 22 };
        ConsoleTable.printHeader(new String[] { "Room No", "Room Type", "Status" }, widths);
        for (int i = 0; i < rooms.getNumberOfEntries(); i++) {
            Room room = rooms.get(i);
            ConsoleTable.printRow(new String[] { room.getRoomNumber(), room.getRoomType(), room.getRoomStatus() },
                    widths);
        }
        ConsoleTable.printFooter(widths);
    }

    private String guestIdentity(Guest guest) {
        return guest == null ? "N/A" : guest.getConfirmationNumber() + " (" + guest.getGuestName() + ")";
    }

    private String valueOrNa(String value) {
        return value == null || value.trim().isEmpty() ? "N/A" : value;
    }

    private String readConfirmation(String prompt) {
        while (true) {
            System.out.print(prompt.replace(": ", " (0 to return): "));
            String value = scanner.nextLine().trim();
            if ("0".equals(value))
                return null;
            if (value.matches("\\d{8}"))
                return value;
            System.out.println("Wrong input");
        }
    }

    private String optional(String label, String current) {
        System.out.print(label + " [" + current + "]: ");
        String value = scanner.nextLine().trim();
        return value.isEmpty() ? current : value;
    }

    private int readInt() {
        while (true) {
            try {
                return Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Wrong input");
                System.out.print("Enter a valid number: ");
            }
        }
    }

    private int readPositiveInt() {
        int value;
        do {
            value = readInt();
            if (value < 0) {
                System.out.println("Wrong input");
                System.out.print("Enter a positive number or 0 to return: ");
            }
        } while (value < 0);
        return value;
    }

    private double readDouble(String prompt) {
        System.out.print(prompt);
        while (true) {
            try {
                double value = Double.parseDouble(scanner.nextLine().trim());
                if (value >= 0)
                    return value;
            } catch (NumberFormatException ignored) {
            }
            System.out.println("Wrong input");
            System.out.print("Please enter a non-negative number: ");
        }
    }

    private int readMenuChoice(int min, int max) {
        while (true) {
            int value = readInt();
            if (value >= min && value <= max)
                return value;
            System.out.println("Wrong input");
            System.out.print("Enter a choice from " + min + " to " + max + ": ");
        }
    }

    private String readRequiredTextOrBack(String prompt) {
        while (true) {
            System.out.print(prompt.replace(": ", " (0 to return): "));
            String value = scanner.nextLine().trim();
            if ("0".equals(value))
                return null;
            if (!value.isEmpty())
                return value;
            System.out.println("Wrong input");
        }
    }

    private String readYesNoOrBack(String prompt) {
        while (true) {
            System.out.print(prompt);
            String value = scanner.nextLine().trim().toUpperCase();
            if ("0".equals(value))
                return null;
            if ("Y".equals(value) || "N".equals(value))
                return value;
            System.out.println("Wrong input");
        }
    }

    private String readNumberedChoiceOrBack(String title, String... choices) {
        System.out.println(title);
        for (int i = 0; i < choices.length; i++)
            System.out.println((i + 1) + ". " + choices[i]);
        System.out.print("Enter choice (1-" + choices.length + ", or 0 to return): ");
        int choice = readMenuChoice(0, choices.length);
        return choice == 0 ? null : choices[choice - 1];
    }

    private void pauseForEnter() {
        System.out.print("Press Enter to return to the previous menu...");
        scanner.nextLine();
    }

    private void printStayValidation(int result) {
        if (result == -1)
            System.out.println("Invalid date. Use YYYY-MM-DD.");
        else if (result == -2)
            System.out.println("Stay duration must be between 1 and 30 nights.");
        else if (result == -3)
            System.out.println("Check-in date cannot be in the past.");
        else
            System.out.println("Check-in date cannot be more than 365 days in advance.");
    }

}
