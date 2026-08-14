package boundary;

import control.FrontDeskController;
import entity.Guest;
import entity.Booking;
import entity.Room;
import adt.ListInterface;
import java.time.LocalDate;
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

    public FrontDeskUI(adt.BSTInterface<Guest> masterGuestTree, ListInterface<Room> sharedRoomList,
            ListInterface<Booking> sharedBookingList) {
        this(new FrontDeskController(masterGuestTree, sharedRoomList, sharedBookingList));
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
                    handleRoomAvailabilitySearch();
                    break;
                case 4:
                    displayRoomList();
                    break;
                case 5:
                    handleCheckIn();
                    break;
                case 6:
                    handleRoomTransfer();
                    break;
                case 7:
                    handleBillingReceipt();
                    break;
                case 8:
                    handleCheckOut();
                    break;
                case 9:
                    handleExtendStay();
                    break;
                case 10:
                    displayReportsSubmenu();
                    break;
                case 11:
                    displayTreeDiagnostics();
                    break;
                case 0:
                    System.out.println("Returning to Main Menu...");
                    break;
                default:
                    System.out.println("Invalid option! Please enter a number between 0 and 11.");
            }
        } while (choice != 0);
    }

    private void handleGuestSearch() {
        System.out.println("\n--- Guest Search ---");
        System.out.println("1. Search by Confirmation Number");
        System.out.println("2. Search by Guest Name");
        System.out.println("3. Search by Confirmation Number Range");
        System.out.println("4. Search by IC / Passport Number");
        System.out.println("5. Search Stay by Booking ID");
        System.out.println("0. Return to Front Desk Menu");
        System.out.print("Enter choice (0-5): ");
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
        } else if (mode == 5) {
            System.out.print("Enter Booking ID (e.g. BK0001): ");
            Booking booking = controller.searchBookingById(scanner.nextLine().trim());
            if (booking == null) {
                System.out.println("\nBooking record not found.");
            } else {
                printBookingCard(booking);
                Guest guest = controller.searchGuestByConfirmationNumber(booking.getGuestConfirmationNumber());
                if (guest != null) printGuestCard(guest);
            }
        } else {
            System.out.println("Invalid search mode.");
        }
    }

    private void handleGuestManagement() {
        System.out.println("\n--- Manage Guest Records ---");
        System.out.println("1. Register New Guest");
        System.out.println("2. Update Guest Profile");
        System.out.println("3. Remove Guest Record");
        System.out.println("0. Return to Front Desk Menu");
        System.out.print("Enter choice (0-3): ");
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

            System.out.print("Enter Phone Number (or press Enter to skip): ");
            String phone = scanner.nextLine().trim();

            if (name.isEmpty() || ic.isEmpty()) {
                System.out.println("Registration failed: Guest name and IC / Passport are required.");
                return;
            }
            Guest existingIdentity = controller.searchGuestByIC(ic);
            if (existingIdentity != null) {
                System.out.println("Registration failed: This IC / Passport is already linked to Confirmation "
                        + existingIdentity.getConfirmationNumber() + ".");
                return;
            }

            // New guest registration defaults to Standard tier with 0 initial points
            String tier = "Standard";
            int points = 0;

            Guest newGuest = new Guest(name, ic, phone, confirmNo, tier, points);
            if (controller.registerGuest(newGuest)) {
                System.out.println("\nGuest " + name + " (" + confirmNo + ") registered successfully!");
                System.out.println("Default Loyalty Tier assigned: [Standard] (0 pts)");
            } else {
                System.out.println("\nFailed to register guest. Check required fields, phone format, confirmation number, and duplicate IC.");
            }
        } else if (choice == 2) {
            handleGuestProfileUpdate();
        } else if (choice == 3) {
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
            printGuestCard(g);
            System.out.print("Permanently remove this guest record? (Y/N): ");
            if (!"Y".equalsIgnoreCase(scanner.nextLine().trim())) {
                System.out.println("Removal cancelled. No data was changed.");
                return;
            }
            String reservedRoomBeforeRemoval = g.isReserved() ? g.getAssignedRoomNumber() : null;
            Guest removed = controller.removeGuest(confirmNo);
            if (removed != null) {
                if (reservedRoomBeforeRemoval != null && !reservedRoomBeforeRemoval.isEmpty()) {
                    System.out.println("Room " + reservedRoomBeforeRemoval
                            + " released back to [Ready for Check-In], and its booking was cancelled.");
                }
                System.out.println("\nRemoved guest: " + removed.getGuestName() + " (" + confirmNo + ")");
            }
        } else {
            System.out.println("Invalid choice.");
        }
    }

    private void handleGuestProfileUpdate() {
        String confirmNo = readValidConfirmationNumber("Enter Confirmation Number to update: ");
        Guest guest = controller.searchGuestByConfirmationNumber(confirmNo);
        if (guest == null) {
            System.out.println("Guest record not found.");
            return;
        }
        printGuestCard(guest);
        System.out.println("Press Enter to keep the current value.");
        String name = readOptionalReplacement("Guest Name", guest.getGuestName());
        String ic = readOptionalReplacement("IC / Passport", guest.getIcNo());
        String phone = readOptionalReplacement("Phone", guest.getPhoneNumber());
        String gender = readOptionalReplacement("Gender", guest.getGender());
        String nationality = readOptionalReplacement("Nationality", guest.getNationality());
        String email = readOptionalReplacement("Email", guest.getEmail());
        String request = readOptionalReplacement("Special Request", guest.getSpecialRequest());

        int result = controller.updateGuestProfile(confirmNo, name, ic, phone, gender,
                nationality, email, request);
        if (result == 1) {
            System.out.println("Guest profile updated successfully. Confirmation Number remains unchanged.");
            printGuestCard(guest);
        } else if (result == -3) {
            System.out.println("Update failed: IC / Passport is already used by another guest.");
        } else if (result == -4) {
            System.out.println("Update failed: Email format is invalid.");
        } else if (result == -5) {
            System.out.println("Update failed: Phone must contain 7-15 digits and valid phone symbols only.");
        } else {
            System.out.println("Update failed: Guest name and IC / Passport are required.");
        }
    }

    private String readOptionalReplacement(String label, String currentValue) {
        String displayed = currentValue == null ? "N/A" : currentValue;
        System.out.print(label + " [" + displayed + "]: ");
        String input = scanner.nextLine().trim();
        return input.isEmpty() ? displayed : input;
    }

    private void displayRoomList() {
        ListInterface<Room> rooms = controller.getAllRooms();
        printRoomTable(rooms);
    }

    private void handleRoomAvailabilitySearch() {
        System.out.println("\n--- Room Availability Search ---");
        System.out.print("Check-In Date (YYYY-MM-DD): ");
        String checkInDate = scanner.nextLine().trim();
        System.out.print("Number of Nights (1-30): ");
        int nights = readIntInput();
        int validation = controller.validateStayPeriod(checkInDate, nights);
        if (validation != 1) {
            if (validation == -1) System.out.println("Invalid date. Use YYYY-MM-DD.");
            else if (validation == -2) System.out.println("Stay duration must be between 1 and 30 nights.");
            else if (validation == -3) System.out.println("Check-in date cannot be in the past.");
            else System.out.println("Check-in date cannot be more than 365 days in advance.");
            return;
        }
        System.out.print("Room Type (Standard Room / Deluxe Suite / Presidential Suite / ALL): ");
        String roomType = scanner.nextLine().trim();
        if (!isValidRoomTypeFilter(roomType)) {
            System.out.println("Invalid room type filter.");
            return;
        }
        double maxPrice = readNonNegativeDouble("Maximum Price per Night (0 for no limit): ");
        System.out.print("Sort Price (1=Ascending, 2=Descending): ");
        int sortChoice = readIntInput();
        if (sortChoice != 1 && sortChoice != 2) {
            System.out.println("Invalid sort choice.");
            return;
        }

        ListInterface<Room> rooms = controller.searchAvailableRooms(checkInDate, nights,
                roomType, maxPrice, sortChoice == 1);
        System.out.printf("%nAVAILABLE ROOMS: %d result(s) for %s, %d night(s)%n",
                rooms.getNumberOfEntries(), checkInDate, nights);
        printAvailableRoomTable(rooms, checkInDate, nights);
        if (rooms.isEmpty()) {
            System.out.println("No room matches this date range, type and budget. Try broader filters.");
        }
    }

    private void handleCheckIn() {
        System.out.println("\n--- Guest Check-In ---");

        ListInterface<Guest> todayArrivals = controller.getTodaysReservedGuests();
        System.out.println("Today's Reserved Arrivals (" + LocalDate.now() + "):");
        if (todayArrivals.isEmpty()) {
            System.out.println("No confirmed reservations are scheduled to arrive today.");
            return;
        }
        System.out.printf("%-12s | %-18s | %-8s | %-18s%n",
                "Confirm No", "Guest Name", "Room", "Room Type");
        System.out.println("-------------+--------------------+----------+-------------------");
        for (int i = 0; i < todayArrivals.getNumberOfEntries(); i++) {
            Guest arrival = todayArrivals.get(i);
            System.out.printf("%-12s | %-18s | %-8s | %-18s%n",
                    arrival.getConfirmationNumber(), arrival.getGuestName(),
                    arrival.getAssignedRoomNumber(), arrival.getRoomType());
        }

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

        if ("CheckedOut".equalsIgnoreCase(g.getBookingStatus())) {
            System.out.println("\nCheck-in failed: Guest (" + confirmNo + ") has ALREADY CHECKED OUT.");
            System.out.println("Note: This confirmation number is completed and cannot be checked in again.");
            return;
        }

        if ("Cancelled".equalsIgnoreCase(g.getBookingStatus())) {
            System.out.println("\nCheck-in failed: Booking (" + confirmNo + ") was CANCELLED.");
            System.out.println("Note: Cancelled bookings cannot be checked in.");
            return;
        }

        Booking linkedBooking = controller.getBookingByConfirmation(confirmNo);
        if (!g.isReserved()) {
            System.out.println("\nCheck-in failed: Guest status is [" + g.getBookingStatus() + "].");
            System.out.println("Only a confirmed reservation scheduled for today can be checked in.");
            return;
        }
        if (linkedBooking == null || !"Confirmed".equalsIgnoreCase(linkedBooking.getBookingStatus())) {
            System.out.println("\nCheck-in failed: No complete confirmed booking is linked to this guest.");
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
            Room upgrade = controller.suggestRoomUpgrade(roomNo, confirmNo);
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
                System.out.println("Actual Check-In Date: " + g.getCheckInDate() + " (recorded automatically)");
                System.out.println("Booked Duration: " + g.getNumberOfNights() + " night(s)");

                System.out.print("Any Special Request? (Enter request or press Enter to skip): ");
                String specialReq = scanner.nextLine().trim();
                if (!specialReq.isEmpty()) {
                    controller.updateSpecialRequest(confirmNo, specialReq);
                }
                break;
            case -2:
                System.out.println("\nCheck-in failed: Room " + roomNo + " does not exist.");
                break;
            case -3:
                Room r = controller.searchRoomByNumber(roomNo);
                String roomStatus = (r != null) ? r.getRoomStatus() : "Unknown";
                System.out.println("\nCheck-in failed: Room " + roomNo + " is currently [" + roomStatus + "].");
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
            case -6:
                System.out.println("\nCheck-in failed: Guest (" + confirmNo + ") has ALREADY CHECKED OUT.");
                System.out.println("Note: This confirmation number is completed and cannot be checked in again.");
                break;
            case -7:
                System.out.println("\nCheck-in failed: Booking (" + confirmNo + ") was CANCELLED.");
                System.out.println("Note: Cancelled bookings cannot be checked in.");
                break;
            case -8:
                System.out.println("\nCheck-in failed: This reservation is marked as NO-SHOW.");
                break;
            case -9:
                System.out.println("\nCheck-in failed: Arrival is earlier than the scheduled check-in date.");
                System.out.println("Scheduled Check-In: " + g.getCheckInDate());
                break;
            case -10:
                System.out.println("\nCheck-in failed: The scheduled arrival date has passed.");
                System.out.println("The reservation has been marked as NoShow; a new reservation is required.");
                break;
            case -11:
                System.out.println("\nCheck-in failed: The stored reservation date is invalid. Please correct the booking first.");
                break;
            case -12:
                System.out.println("\nCheck-in failed: This guest does not have Reserved status.");
                System.out.println("Create and process the reservation in the Booking module first.");
                break;
            case -13:
                System.out.println("\nCheck-in failed: No complete confirmed booking is linked to this confirmation number.");
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
                System.out.printf(" Charged Rate      : RM %.2f/night (original rate preserved)\n", g.getRoomRate());
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
            case -6:
                System.out.println("\nRoom Transfer failed: Room " + newRoomNo
                        + " has another reservation that overlaps the remaining stay.");
                break;
            case -7:
                System.out.println("\nRoom Transfer failed: The active stay dates are invalid.");
                break;
            default:
                System.out.println("\nRoom Transfer failed.");
                break;
        }
    }

    private void handleExtendStay() {
        System.out.println("\n--- Extend Active Stay ---");
        String confirmNo = readValidConfirmationNumber("Enter 8-digit Confirmation Number: ");
        Guest guest = controller.searchGuestByConfirmationNumber(confirmNo);
        if (guest == null || !guest.isCheckedIn()) {
            System.out.println("Only a currently checked-in guest can extend a stay.");
            return;
        }
        System.out.printf("Current stay: %d night(s), Room %s%n", guest.getNumberOfNights(),
                guest.getAssignedRoomNumber());
        System.out.print("Additional nights requested: ");
        int additionalNights = readPositiveIntInput();
        int result = controller.extendStay(confirmNo, additionalNights);
        if (result == 1) {
            System.out.printf("Stay extended successfully. New duration: %d night(s).%n", guest.getNumberOfNights());
        } else if (result == -4) {
            System.out.println("Extension rejected: the room is already reserved for another guest during those dates.");
        } else if (result == -5) {
            System.out.println("Extension rejected: total stay must not exceed 30 nights.");
        } else if (result == -3) {
            System.out.println("Extension rejected: the current stay date is invalid.");
        } else {
            System.out.println("Extension could not be completed.");
        }
    }

    private void handleCheckOut() {
        System.out.println("\n--- Complete Guest Check-Out ---");
        String confirmNo = readValidConfirmationNumber("Enter 8-digit Confirmation Number: ");
        FrontDeskController.BillingDetails preview = controller.calculateBill(confirmNo);
        if (preview == null) {
            System.out.println("Check-out failed. Ensure the guest exists, is checked in, and has a valid room.");
            return;
        }
        printBillingDetails(preview);
        System.out.print("Confirm payment and complete check-out? (Y/N): ");
        if (!"Y".equalsIgnoreCase(scanner.nextLine().trim())) {
            System.out.println("Check-out cancelled. No data was changed.");
            return;
        }
        FrontDeskController.CheckoutResult result = controller.completeCheckOutAndReward(confirmNo);
        if (result.getStatus() != 1) {
            System.out.println("Check-out failed. No loyalty points were awarded.");
            return;
        }
        FrontDeskController.BillingDetails bill = result.getBill();
        System.out.printf("Check-out complete. Total paid: RM %.2f | Points earned: +%d%n",
                bill.getTotal(), result.getEarnedPoints());
        System.out.println("Room " + bill.getRoom().getRoomNumber() + " is now [Dirty] for Housekeeping.");
        System.out.printf("Updated loyalty balance: %d points | Tier: %s%n",
                bill.getGuest().getLoyaltyPoints(), bill.getGuest().getLoyaltyTier());
    }

    private void handleBillingReceipt() {
        System.out.println("\n--- Billing & Receipt Preview ---");
        String confirmNo = readValidConfirmationNumber("Enter 8-digit Confirmation Number (e.g. 10000001): ");
        FrontDeskController.BillingDetails bill = controller.calculateBill(confirmNo);
        if (bill == null) {
            System.out.println("Billing unavailable. Guest must be checked in with a valid assigned room.");
            return;
        }
        printBillingDetails(bill);
        System.out.println("Preview only. Use 'Complete Guest Check-Out' to finalize payment and award points.");
    }

    private void printBillingDetails(FrontDeskController.BillingDetails bill) {
        Guest guest = bill.getGuest();
        Room room = bill.getRoom();
        Booking stayBooking = bill.getBooking();
        System.out.println("\n==================================================");
        System.out.println("                 RESORT INVOICE                   ");
        System.out.println("==================================================");
        System.out.printf(" Confirmation No : %s\n", guest.getConfirmationNumber());
        System.out.printf(" Guest Name      : %s\n", guest.getGuestName());
        System.out.printf(" IC / Passport   : %s\n", guest.getIcNo());
        System.out.printf(" Membership Tier : %s (%.0f%% Discount)\n", guest.getLoyaltyTier(), bill.getDiscountRate() * 100);
        System.out.printf(" Room Assigned   : Room %s (%s)\n", room.getRoomNumber(), room.getRoomType());
        if (stayBooking != null) {
            System.out.printf(" Scheduled Stay  : %s to %s\n", stayBooking.getCheckInDate(), stayBooking.getCheckOutDate());
            System.out.printf(" Actual Check-In : %s\n", stayBooking.getActualCheckInDate());
        }
        if (bill.getChargedRate() < room.getPrice()) {
            System.out.printf(" Upgrade Benefit : Free Upgrade (Saved RM %.2f/night!)\n",
                    room.getPrice() - bill.getChargedRate());
        }
        System.out.printf(" Stay Duration   : %d Night(s)\n", bill.getNights());
        System.out.println("--------------------------------------------------");
        System.out.printf(" Charged Rate/Nt : RM %8.2f\n", bill.getChargedRate());
        System.out.printf(" Subtotal        : RM %8.2f\n", bill.getSubtotal());
        System.out.printf(" Tier Discount   :-RM %8.2f\n", bill.getDiscountAmount());
        System.out.println("--------------------------------------------------");
        System.out.printf(" Total Payable   : RM %8.2f\n", bill.getTotal());
        System.out.printf(" Points on Checkout: +%d%n", bill.getProjectedPoints());
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
            System.out.println("3. Loyalty Member Segment & Stay Status Report");
            System.out.println("0. Back to Front Desk Menu");
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
                    System.out.println("Returning to Front Desk Menu...");
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
            System.out.println("0. Back to Front Desk Menu");
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
                    System.out.println("Returning to Front Desk Menu...");
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
        double averageDailyRate = controller.calculateAverageDailyRate();

        System.out.println("\n==================================================");
        System.out.println("     REPORT 1: ROOM OCCUPANCY & FINANCIAL SUMMARY ");
        System.out.println("==================================================");
        System.out.printf(" Total Rooms                : %d\n", summary[0]);
        System.out.printf(" Ready for Check-In         : %d\n", summary[1]);
        System.out.printf(" Occupied                   : %d\n", summary[2]);
        System.out.printf(" Dirty                      : %d\n", summary[3]);
        System.out.printf(" Cleaning In Progress       : %d\n", summary[4]);
        System.out.printf(" Inspected                  : %d\n", summary[5]);
        System.out.printf(" Reserved                   : %d\n", summary[6]);
        System.out.println("--------------------------------------------------");
        System.out.printf(" Occupancy Rate             : %.1f%%\n", occupancyRate);
        System.out.printf(" Net Daily Room Revenue     : RM %.2f\n", estRevenue);
        System.out.printf(" Average Daily Rate (ADR)   : RM %.2f\n", averageDailyRate);
        System.out.printf(" Housekeeping Blocked Rooms : %d (Dirty / Cleaning / Inspected)\n",
                summary[3] + summary[4] + summary[5]);
        System.out.println("==================================================");
    }

    private void displayReport2() {
        System.out.println("\n--- REPORT 2: FILTERED & SORTED ROOM PRICING ---");
        System.out.print(
                "Enter Room Status filter (Ready for Check-In / Dirty / Occupied / Cleaning In Progress / Inspected / Reserved / ALL): ");
        String statusFilter = scanner.nextLine().trim();
        if (!isValidRoomStatusFilter(statusFilter)) {
            System.out.println("Invalid room status filter.");
            return;
        }

        System.out.print("Enter Room Type filter (Standard Room / Deluxe Suite / Presidential Suite / ALL): ");
        String roomTypeFilter = scanner.nextLine().trim();
        if (!isValidRoomTypeFilter(roomTypeFilter)) {
            System.out.println("Invalid room type filter.");
            return;
        }

        double minPrice = readNonNegativeDouble("Enter Minimum Price per night: ");
        double maxPrice = readNonNegativeDouble("Enter Maximum Price per night (0 for no limit): ");
        if (maxPrice > 0 && maxPrice < minPrice) {
            System.out.println("Invalid price range: maximum price must be zero or at least the minimum price.");
            return;
        }

        System.out.print("Sort by price? (1=Ascending, 2=Descending): ");
        int sortChoice = readIntInput();
        if (sortChoice != 1 && sortChoice != 2) {
            System.out.println("Invalid sort choice.");
            return;
        }
        boolean asc = sortChoice == 1;

        ListInterface<Room> filteredRooms = controller.getFilteredAndSortedRooms(statusFilter,
                roomTypeFilter, minPrice, maxPrice, asc);

        System.out.println("\n==========================================================================");
        System.out.printf(" RESULTS: %d room(s) found\n", filteredRooms.getNumberOfEntries());
        System.out.printf(" FILTERS: Status=%s | Type=%s | Price=RM %.2f to %s | Sort=%s%n",
                statusFilter, roomTypeFilter, minPrice, maxPrice == 0 ? "No Limit" : String.format("RM %.2f", maxPrice),
                asc ? "Ascending" : "Descending");
        System.out.println("==========================================================================");
        printRoomTable(filteredRooms);
        if (!filteredRooms.isEmpty()) {
            double totalPrice = 0.0;
            double lowest = Double.MAX_VALUE;
            double highest = 0.0;
            for (int i = 0; i < filteredRooms.getNumberOfEntries(); i++) {
                double price = filteredRooms.get(i).getPrice();
                totalPrice += price;
                lowest = Math.min(lowest, price);
                highest = Math.max(highest, price);
            }
            System.out.printf("PRICE SUMMARY: Lowest RM %.2f | Average RM %.2f | Highest RM %.2f%n",
                    lowest, totalPrice / filteredRooms.getNumberOfEntries(), highest);
        }
    }

    private void displayReport3() {
        System.out.println("\n--- REPORT 3: LOYALTY MEMBER SEGMENT & STAY STATUS ---");
        System.out.print("Enter Membership Tier filter (Platinum / Gold / Silver / Standard / ALL): ");
        String tierFilter = scanner.nextLine().trim();
        if (!isValidTierFilter(tierFilter)) {
            System.out.println("Invalid membership tier filter.");
            return;
        }

        System.out.print("Enter Stay Status filter (Registered / Reserved / CheckedIn / CheckedOut / Cancelled / NoShow / ALL): ");
        String statusFilter = scanner.nextLine().trim();
        if (!isValidGuestStatusFilter(statusFilter)) {
            System.out.println("Invalid stay status filter.");
            return;
        }

        System.out.print("Enter Minimum Loyalty Points Threshold (0 or more): ");
        int minPoints = readIntInput();
        if (minPoints < 0) {
            System.out.println("Minimum points cannot be negative.");
            return;
        }
        System.out.print("Sort (1=Points Descending, 2=Guest Name Ascending): ");
        int sortChoice = readIntInput();
        if (sortChoice != 1 && sortChoice != 2) {
            System.out.println("Invalid sort choice.");
            return;
        }

        ListInterface<Guest> filteredGuests = controller.getFilteredAndSortedGuests(tierFilter,
                statusFilter, minPoints, sortChoice == 1);

        System.out.println("\n==========================================================================");
        System.out.printf(" LOYALTY SEGMENT: %d guest(s) found | Tier=%s | Status=%s | Minimum Points=%d%n",
                filteredGuests.getNumberOfEntries(), tierFilter, statusFilter, minPoints);
        System.out.println("==========================================================================");
        System.out.printf("%-5s | %-18s | %-12s | %-12s | %-12s | %-8s%n",
                "No", "Guest Name", "Confirm No", "Tier", "Stay Status", "Points");
        System.out.println("-----------------------------------------------------------------------------------------");
        int totalPoints = 0;
        for (int i = 0; i < filteredGuests.getNumberOfEntries(); i++) {
            Guest g = filteredGuests.get(i);
            totalPoints += g.getLoyaltyPoints();
            System.out.printf("%-5d | %-18s | %-12s | %-12s | %-12s | %-8d%n",
                    (i + 1), g.getGuestName(), g.getConfirmationNumber(), g.getLoyaltyTier(),
                    g.getBookingStatus(), g.getLoyaltyPoints());
        }
        System.out.println("==========================================================================");
        if (!filteredGuests.isEmpty()) {
            System.out.printf("SEGMENT SUMMARY: Total Points %,d | Average %.1f points per guest%n",
                    totalPoints, (double) totalPoints / filteredGuests.getNumberOfEntries());
        }
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

    /** Displays schedule availability without presenting today's room status as a future guarantee. */
    private void printAvailableRoomTable(ListInterface<Room> rooms, String checkInDate, int nights) {
        String checkOutDate = LocalDate.parse(checkInDate).plusDays(nights).toString();
        System.out.println("================================================================================");
        System.out.printf("%-10s | %-24s | %-23s | %-10s%n", "Room No", "Room Type", "Selected Stay", "Price/Night");
        System.out.println("--------------------------------------------------------------------------------");
        for (int i = 0; i < rooms.getNumberOfEntries(); i++) {
            Room room = rooms.get(i);
            System.out.printf("%-10s | %-24s | %-10s to %-10s | RM %7.2f%n", room.getRoomNumber(),
                    room.getRoomType(), checkInDate, checkOutDate, room.getPrice());
        }
        System.out.println("================================================================================");
    }

    private boolean isValidRoomTypeFilter(String roomType) {
        return "ALL".equalsIgnoreCase(roomType)
                || "Standard Room".equalsIgnoreCase(roomType)
                || "Deluxe Suite".equalsIgnoreCase(roomType)
                || "Presidential Suite".equalsIgnoreCase(roomType);
    }

    private boolean isValidRoomStatusFilter(String status) {
        return "ALL".equalsIgnoreCase(status)
                || "Ready for Check-In".equalsIgnoreCase(status)
                || "Dirty".equalsIgnoreCase(status)
                || "Occupied".equalsIgnoreCase(status)
                || "Cleaning In Progress".equalsIgnoreCase(status)
                || "Inspected".equalsIgnoreCase(status)
                || "Reserved".equalsIgnoreCase(status);
    }

    private boolean isValidTierFilter(String tier) {
        return "ALL".equalsIgnoreCase(tier)
                || "Platinum".equalsIgnoreCase(tier)
                || "Gold".equalsIgnoreCase(tier)
                || "Silver".equalsIgnoreCase(tier)
                || "Standard".equalsIgnoreCase(tier);
    }

    private boolean isValidGuestStatusFilter(String status) {
        return "ALL".equalsIgnoreCase(status)
                || "Registered".equalsIgnoreCase(status)
                || "Reserved".equalsIgnoreCase(status)
                || "CheckedIn".equalsIgnoreCase(status)
                || "CheckedOut".equalsIgnoreCase(status)
                || "Cancelled".equalsIgnoreCase(status)
                || "NoShow".equalsIgnoreCase(status);
    }

    private double readNonNegativeDouble(String prompt) {
        System.out.print(prompt);
        return readNonNegativeDoubleInput();
    }

    private void printBookingCard(Booking booking) {
        System.out.println("--------------------------------------------------");
        System.out.printf(" Booking ID       : %s%n", booking.getBookingId());
        System.out.printf(" Guest            : %s (%s)%n", booking.getGuestName(), booking.getGuestConfirmationNumber());
        System.out.printf(" Room / Type      : %s / %s%n", booking.getRoomNumber(), booking.getRoomType());
        System.out.printf(" Scheduled Stay   : %s to %s (%d night(s))%n", booking.getCheckInDate(),
                booking.getCheckOutDate(), booking.getNumberOfNights());
        System.out.printf(" Booking Status   : %s%n", booking.getBookingStatus());
        System.out.println("--------------------------------------------------");
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

    private double readNonNegativeDoubleInput() {
        while (true) {
            try {
                double value = Double.parseDouble(scanner.nextLine().trim());
                if (value >= 0) return value;
            } catch (NumberFormatException ignored) {
            }
            System.out.print("Please enter a non-negative number: ");
        }
    }

    private void printGuestCard(Guest g) {
        System.out.println("--------------------------------------------------");
        System.out.printf(" Confirmation No : %s\n", g.getConfirmationNumber());
        System.out.printf(" Guest Name      : %s\n", g.getGuestName());
        System.out.printf(" IC / Passport   : %s\n", g.getIcNo());
        System.out.printf(" Phone Number    : %s\n", g.getPhoneNumber());
        if (!"N/A".equalsIgnoreCase(g.getGender())) {
            System.out.printf(" Gender          : %s\n", g.getGender());
        }
        if (!"N/A".equalsIgnoreCase(g.getNationality())) {
            System.out.printf(" Nationality     : %s\n", g.getNationality());
        }
        if (!"N/A".equalsIgnoreCase(g.getEmail())) {
            System.out.printf(" Email           : %s\n", g.getEmail());
        }
        System.out.printf(" Booking Status  : %s\n", g.getBookingStatus());
        System.out.printf(" Loyalty Tier    : %s\n", g.getLoyaltyTier());
        System.out.printf(" Reward Points   : %d pts\n", g.getLoyaltyPoints());
        if (g.getAssignedRoomNumber() != null) {
            System.out.printf(" Assigned Room   : %s (%s)\n", g.getAssignedRoomNumber(),
                    g.getRoomType() != null ? g.getRoomType() : "N/A");
            System.out.printf(" Room Rate       : RM %.2f/night\n", g.getRoomRate());
        }
        if (g.getCheckInDate() != null) {
            System.out.printf(" Check-In Date   : %s\n", g.getCheckInDate());
            System.out.printf(" No. of Nights   : %d\n", g.getNumberOfNights());
        }
        if (g.getCheckOutDate() != null) {
            System.out.printf(" Check-Out Date  : %s\n", g.getCheckOutDate());
        }
        if (g.getSpecialRequest() != null) {
            System.out.printf(" Special Request : %s\n", g.getSpecialRequest());
        }
        System.out.println("--------------------------------------------------");
    }

    public static void main(String[] args) {
        FrontDeskUI ui = new FrontDeskUI();
        ui.displayMenu();
    }
}
