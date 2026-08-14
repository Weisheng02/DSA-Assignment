package boundary;

import adt.BSTInterface;
import adt.ListInterface;
import control.BookingController;
import entity.Booking;
import entity.Guest;
import entity.Room;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

/**
 * Author: Zhi Xuan
 * Boundary Class for Walk-In Registrations & Booking Module
 */
public class BookingUI {
    private BookingController controller;
    private Scanner scanner;

    public BookingUI() {
        this(new BookingController());
    }

    public BookingUI(BookingController controller) {
        this.controller = (controller != null) ? controller : new BookingController();
    }

    public BookingUI(ListInterface<Room> sharedRoomList, BSTInterface<Guest> masterGuestRegistry) {
        this(new BookingController(sharedRoomList, masterGuestRegistry));
    }

    public BookingUI(ListInterface<Room> sharedRoomList, ListInterface<Booking> sharedBookingList,
            BSTInterface<Guest> masterGuestRegistry) {
        this(new BookingController(sharedRoomList, masterGuestRegistry, sharedBookingList));
    }

    public void displayMenu() {
        displayMenu(new Scanner(System.in));
    }

    public void displayMenu(Scanner scanner) {
        this.scanner = (scanner != null) ? scanner : new Scanner(System.in);
        int choice = -1;

        do {
            System.out.println("\n--------------------------------------------------");
            System.out.println("   WALK-IN REGISTRATIONS & STANDARD BOOKING       ");
            System.out.println("--------------------------------------------------");
            System.out.println("1. Register New Walk-In Guest (Enqueue)");
            System.out.println("2. View Waiting Queue");
            System.out.println("3. Process Next Guest in Queue (Dequeue -> Assign Room)");
            System.out.println("4. View All Bookings");
            System.out.println("5. Search Booking (Booking ID / Confirmation No.)");
            System.out.println("6. Modify Confirmed Booking");
            System.out.println("7. Cancel a Booking");
            System.out.println("8. Cancel Waiting Registration");
            System.out.println("9. View Available Rooms for a Stay");
            System.out.println("10. Report: Booking Summary (Multi-Criteria Filter & Sort)");
            System.out.println("11. Report: Guest Registration & Tier Analysis");
            System.out.println("0. Back to Main Menu");
            System.out.println("--------------------------------------------------");
            System.out.print("Enter your choice (0-11): ");

            choice = readIntInput();
            System.out.println();

            switch (choice) {
                case 1:
                    handleRegisterWalkIn();
                    break;
                case 2:
                    handleViewWaitingQueue();
                    break;
                case 3:
                    handleProcessNextGuest();
                    break;
                case 4:
                    handleViewAllBookings();
                    break;
                case 5:
                    handleSearchBooking();
                    break;
                case 6:
                    handleModifyBooking();
                    break;
                case 7:
                    handleCancelBooking();
                    break;
                case 8:
                    handleCancelWaitingRegistration();
                    break;
                case 9:
                    handleViewAvailableRooms();
                    break;
                case 10:
                    displayReport1();
                    break;
                case 11:
                    displayReport2();
                    break;
                case 0:
                    System.out.println("Returning to main menu...");
                    break;
                default:
                    System.out.println("Invalid selection. Please enter a number between 0 and 11.");
            }
        } while (choice != 0);
    }

    private void handleRegisterWalkIn() {
        System.out.println("\n--- Register Walk-In Guest / Reservation ---");
        System.out.print("Enter Guest IC / Passport Number (e.g. 980101-14-5566): ");
        String ic = scanner.nextLine().trim();
        if (ic.isEmpty()) {
            System.out.println("Error: IC / Passport Number cannot be empty.");
            return;
        }

        Guest existingGuest = controller.findGuestByIC(ic);
        String name;
        String tier;
        int points = 0;

        if (existingGuest != null) {
            name = existingGuest.getGuestName();
            tier = existingGuest.getLoyaltyTier();
            points = existingGuest.getLoyaltyPoints();
            System.out.println("\n✨ Existing Member Recognized!");
            System.out.printf("  Guest Name    : %s\n", name);
            System.out.printf("  Loyalty Tier  : %s (%d pts)\n", tier, points);
            System.out.println("  ℹ️  A NEW stay/reservation will be created for this member (new Confirmation No).");
        } else {
            System.out.print("New Guest Detected. Enter Guest Name: ");
            name = scanner.nextLine().trim();
            if (name.isEmpty()) {
                System.out.println("Error: Guest name cannot be empty.");
                return;
            }

            // Default new guest to Standard tier with 0 initial points
            tier = "Standard";
            points = 0;
            System.out.println("Assigned Default Loyalty Tier: [Standard] (0 pts)");
        }

        Guest newGuest = controller.registerWalkInGuest(name, ic, tier, points);

        System.out.println("\n==================================================");
        System.out.println("         WALK-IN REGISTRATION SUCCESSFUL          ");
        System.out.println("==================================================");
        System.out.printf(" Guest Name        : %s\n", newGuest.getGuestName());
        System.out.printf(" IC / Passport     : %s\n", newGuest.getIcNo());
        System.out.printf(" Confirmation No   : %s (Unique For This Stay)\n", newGuest.getConfirmationNumber());
        System.out.printf(" Loyalty Tier      : %s (%d pts)\n", newGuest.getLoyaltyTier(), newGuest.getLoyaltyPoints());
        System.out.printf(" Queue Position    : #%d\n", controller.getWaitingCount());
        System.out.println("==================================================");
        System.out.println("Guest has been added to the waiting queue.");
    }

    private void handleViewWaitingQueue() {
        ListInterface<Guest> queueList = controller.getWaitingQueueList();

        System.out.println("\n==================================================");
        System.out.println("         CURRENT WAITING QUEUE (FIFO ORDER)       ");
        System.out.println("==================================================");

        if (queueList.isEmpty()) {
            System.out.println(" The waiting queue is empty. No guests waiting.");
        } else {
            System.out.printf(" Total Guests Waiting: %d\n\n", queueList.getNumberOfEntries());
            System.out.printf("%-6s | %-15s | %-12s | %-10s\n",
                    "Pos", "Guest Name", "Confirm No", "Tier");
            System.out.println("-------+------------------+--------------+-----------");
            for (int i = 0; i < queueList.getNumberOfEntries(); i++) {
                Guest g = queueList.get(i);
                String posLabel = (i == 0) ? "#1 [NEXT]" : "#" + (i + 1);
                System.out.printf("%-9s | %-15s | %-12s | %-10s\n",
                        posLabel, g.getGuestName(), g.getConfirmationNumber(), g.getLoyaltyTier());
            }
        }
        System.out.println("==================================================");
    }

    private void handleProcessNextGuest() {
        System.out.println("\n--- Process Next Guest in Queue ---");

        Guest nextGuest = controller.peekNextGuest();
        if (nextGuest == null) {
            System.out.println("The waiting queue is empty. No guest to process.");
            return;
        }

        System.out.println("Next guest in queue:");
        System.out.printf("  Name: %s | Confirm No: %s | Tier: %s\n",
                nextGuest.getGuestName(), nextGuest.getConfirmationNumber(),
                nextGuest.getLoyaltyTier());

        System.out.print("Enter Check-In Date (YYYY-MM-DD): ");
        String checkInDate = scanner.nextLine().trim();
        if (!isValidDateInput(checkInDate)) {
            System.out.println("Error: Check-in date must be a valid YYYY-MM-DD date.");
            return;
        }
        System.out.print("Enter Number of Nights: ");
        int nights = readPositiveIntInput();
        int stayValidation = controller.validateStayPeriod(checkInDate, nights);
        if (stayValidation != 1) {
            printStayValidationError(stayValidation);
            return;
        }

        // Show date-aware availability, including a room with a non-overlapping future stay.
        ListInterface<Room> availableRooms = controller.getAvailableRooms(checkInDate, nights);
        if (availableRooms.isEmpty()) {
            System.out.println("\nNo rooms are available for the requested stay period.");
            System.out.println("Guest remains in the queue.");
            return;
        }

        printRooms(availableRooms, "AVAILABLE ROOMS FOR REQUESTED STAY");

        System.out.print("Enter Room Number to assign: ");
        String roomNo = scanner.nextLine().trim();
        System.out.print("Special Request (leave blank for None): ");
        String specialRequest = scanner.nextLine().trim();

        int result = controller.processNextGuest(roomNo, checkInDate, nights, specialRequest);

        switch (result) {
            case 1:
                Booking lastBooking = controller.getLastBooking();
                System.out.println("\n==================================================");
                System.out.println("       BOOKING CONFIRMED SUCCESSFULLY!            ");
                System.out.println("==================================================");
                System.out.printf(" Booking ID        : %s\n", lastBooking.getBookingId());
                System.out.printf(" Guest Name        : %s\n", lastBooking.getGuestName());
                System.out.printf(" Confirmation No   : %s\n", lastBooking.getGuestConfirmationNumber());
                System.out.printf(" Room Assigned     : Room %s (%s)\n",
                        lastBooking.getRoomNumber(), lastBooking.getRoomType());
                System.out.printf(" Check-In Date     : %s\n", lastBooking.getCheckInDate());
                System.out.printf(" Expected Check-Out: %s\n", lastBooking.getCheckOutDate());
                System.out.printf(" Duration          : %d Night(s)\n", lastBooking.getNumberOfNights());
                System.out.printf(" Rate / Night      : RM %.2f\n", lastBooking.getRoomPrice());
                System.out.printf(" Total Amount      : RM %.2f\n", lastBooking.getTotalPrice());
                System.out.printf(" Special Request   : %s\n", lastBooking.getSpecialRequest());
                System.out.println("==================================================");
                System.out.printf(" Remaining in queue: %d guest(s)\n", controller.getWaitingCount());
                break;
            case -1:
                System.out.println("Error: Queue is empty.");
                break;
            case -2:
                System.out.println("Error: Room number not found.");
                break;
            case -3:
                System.out.println("Error: Room " + roomNo + " is unavailable for the requested dates.");
                break;
            case -4:
                System.out.println("Error: Check-in date must use YYYY-MM-DD format and be a valid date.");
                break;
            case -5:
                System.out.println("Error: Stay must be between 1 and 30 nights.");
                break;
            case -6:
                System.out.println("Error: Check-in date cannot be in the past.");
                break;
            case -7:
                System.out.println("Error: Reservations can only be made up to 365 days in advance.");
                break;
        }
    }

    private void handleViewAllBookings() {
        ListInterface<Booking> bookings = controller.getAllBookings();

        System.out.println(
                "\n=========================================================================================================================");
        System.out.println(
                "                            ALL BOOKING RECORDS                                              ");
        System.out.println(
                "=========================================================================================================================");

        if (bookings.isEmpty()) {
            System.out.println(" No bookings found.");
        } else {
            System.out.printf("%-8s | %-12s | %-12s | %-8s | %-12s | %-12s | %-6s | %-10s | %-10s\n",
                    "ID", "Guest", "Confirm No", "Room", "Check-In", "Check-Out", "Nights", "Total(RM)", "Status");
            System.out.println(
                    "-------------------------------------------------------------------------------------------------------------------------");
            for (int i = 0; i < bookings.getNumberOfEntries(); i++) {
                Booking b = bookings.get(i);
                System.out.printf("%-8s | %-12s | %-12s | %-8s | %-12s | %-12s | %-6d | %10.2f | %-10s\n",
                        b.getBookingId(),
                        truncate(b.getGuestName(), 12),
                        b.getGuestConfirmationNumber(),
                        b.getRoomNumber(),
                        b.getCheckInDate(),
                        b.getCheckOutDate(),
                        b.getNumberOfNights(),
                        b.getTotalPrice(),
                        b.getBookingStatus());
            }
        }
        System.out.println(
                "=========================================================================================================================");
    }

    private void handleCancelBooking() {
        System.out.println("\n--- Cancel a Booking ---");
        handleViewAllBookings();

        System.out.print("Enter Booking ID to cancel (e.g. BK0001): ");
        String bookingId = scanner.nextLine().trim();
        System.out.print("Cancellation reason: ");
        String reason = scanner.nextLine().trim();
        System.out.print("Processed by (staff name): ");
        String staffName = scanner.nextLine().trim();

        int result = controller.cancelBooking(bookingId, reason, staffName);
        switch (result) {
            case 1:
                System.out.println("\nBooking " + bookingId + " has been cancelled successfully.");
                System.out.println("The reservation dates were released; the room's physical housekeeping status was preserved.");
                break;
            case -1:
                System.out.println("Error: Booking ID '" + bookingId + "' not found.");
                break;
            case -2:
                System.out.println("Error: Booking '" + bookingId + "' is already cancelled.");
                break;
            case -3:
                System.out.println("Error: Cannot cancel booking '" + bookingId
                        + "'. Guest is currently CHECKED IN at Front Desk.");
                break;
            case -4:
                System.out.println("Error: Cannot cancel booking '" + bookingId + "'. Guest has ALREADY CHECKED OUT.");
                break;
            case -5:
                System.out.println("Error: A NoShow reservation can no longer be cancelled.");
                break;
        }
    }

    private void handleSearchBooking() {
        System.out.println("\n--- Search Booking ---");
        System.out.print("Search by (1) Booking ID or (2) Confirmation No.: ");
        int choice = readIntInput();
        System.out.print("Enter search value: ");
        String value = scanner.nextLine().trim();
        Booking booking = (choice == 2) ? controller.findBookingByConfirmation(value)
                : controller.findBookingById(value);
        if (booking == null) {
            System.out.println("No booking record was found.");
            return;
        }
        printBookingDetails(booking);
    }

    private void handleModifyBooking() {
        System.out.println("\n--- Modify Confirmed Booking ---");
        System.out.print("Enter Booking ID: ");
        String bookingId = scanner.nextLine().trim();
        Booking existing = controller.findBookingById(bookingId);
        if (existing == null) {
            System.out.println("Error: Booking ID not found.");
            return;
        }
        if (!"Confirmed".equalsIgnoreCase(existing.getBookingStatus())) {
            System.out.println("Only a confirmed, not-yet-checked-in booking can be modified.");
            return;
        }
        printBookingDetails(existing);

        System.out.print("New Check-In Date (YYYY-MM-DD): ");
        String checkInDate = scanner.nextLine().trim();
        if (!isValidDateInput(checkInDate)) {
            System.out.println("Error: Check-in date must be a valid YYYY-MM-DD date.");
            return;
        }
        System.out.print("New Number of Nights: ");
        int nights = readPositiveIntInput();
        int stayValidation = controller.validateStayPeriod(checkInDate, nights);
        if (stayValidation != 1) {
            printStayValidationError(stayValidation);
            return;
        }
        ListInterface<Room> rooms = controller.getAvailableRoomsForUpdate(bookingId, checkInDate, nights);
        printRooms(rooms, "ROOMS AVAILABLE FOR NEW STAY PERIOD");
        System.out.print("New Room Number: ");
        String roomNumber = scanner.nextLine().trim();
        System.out.print("Special Request (leave blank for None): ");
        String specialRequest = scanner.nextLine().trim();

        int result = controller.updateBooking(bookingId, roomNumber, checkInDate, nights, specialRequest);
        if (result == 1) {
            System.out.println("Booking updated successfully and synchronized with the Front Desk guest record.");
            printBookingDetails(controller.findBookingById(bookingId));
        } else if (result == -3) {
            System.out.println("Error: Room number not found.");
        } else if (result == -4 || result == -6 || result == -7) {
            printStayValidationError(result);
        } else if (result == -5) {
            System.out.println("Error: Selected room is unavailable for the requested dates.");
        } else {
            System.out.println("Error: This booking cannot be modified.");
        }
    }

    private void handleCancelWaitingRegistration() {
        System.out.println("\n--- Cancel Waiting Registration ---");
        handleViewWaitingQueue();
        System.out.print("Enter Confirmation No. to remove from queue: ");
        String confirmationNumber = scanner.nextLine().trim();
        if (controller.cancelWaitingRegistration(confirmationNumber) == 1) {
            System.out.println("Waiting registration cancelled. All remaining guests keep their original FIFO order.");
        } else {
            System.out.println("Error: No waiting guest has that confirmation number.");
        }
    }

    private void handleViewAvailableRooms() {
        System.out.println("\n--- View Available Rooms for a Stay ---");
        System.out.print("Enter Check-In Date (YYYY-MM-DD): ");
        String checkInDate = scanner.nextLine().trim();
        if (!isValidDateInput(checkInDate)) {
            System.out.println("Error: Check-in date must be a valid YYYY-MM-DD date.");
            return;
        }
        System.out.print("Enter Number of Nights: ");
        int nights = readPositiveIntInput();
        int stayValidation = controller.validateStayPeriod(checkInDate, nights);
        if (stayValidation != 1) {
            printStayValidationError(stayValidation);
            return;
        }
        printRooms(controller.getAvailableRooms(checkInDate, nights), "AVAILABLE ROOMS FOR REQUESTED STAY");
    }

    private void printBookingDetails(Booking booking) {
        System.out.println("\n==================================================");
        System.out.println("                 BOOKING DETAILS                  ");
        System.out.println("==================================================");
        System.out.printf(" Booking ID       : %s%n", booking.getBookingId());
        System.out.printf(" Guest            : %s (%s)%n", booking.getGuestName(), booking.getGuestConfirmationNumber());
        System.out.printf(" Room             : %s (%s)%n", booking.getRoomNumber(), booking.getRoomType());
        System.out.printf(" Stay             : %s to %s (%d night(s))%n", booking.getCheckInDate(),
                booking.getCheckOutDate(), booking.getNumberOfNights());
        System.out.printf(" Rate / Total     : RM %.2f / RM %.2f%n", booking.getRoomPrice(), booking.getTotalPrice());
        System.out.printf(" Status           : %s%n", booking.getBookingStatus());
        System.out.printf(" Special Request  : %s%n", booking.getSpecialRequest());
        System.out.printf(" Created On       : %s%n", booking.getBookingCreatedDate());
        if (!"N/A".equals(booking.getActualCheckInDate())) {
            System.out.printf(" Actual Check-In  : %s%n", booking.getActualCheckInDate());
        }
        if (!"N/A".equals(booking.getActualCheckOutDate())) {
            System.out.printf(" Actual Check-Out : %s (%d actual night(s))%n", booking.getActualCheckOutDate(),
                    booking.getActualNightsStayed());
        }
        if ("NoShow".equalsIgnoreCase(booking.getBookingStatus())) {
            System.out.printf(" No-Show Recorded : %s%n", booking.getNoShowDate());
        }
        if ("Cancelled".equalsIgnoreCase(booking.getBookingStatus())) {
            System.out.printf(" Cancelled On     : %s%n", booking.getCancellationDate());
            System.out.printf(" Cancelled By     : %s%n", booking.getCancelledBy());
            System.out.printf(" Cancellation Note: %s%n", booking.getCancellationReason());
        }
        System.out.println("==================================================");
    }

    private void printRooms(ListInterface<Room> rooms, String title) {
        System.out.println("\n==========================================================================");
        System.out.printf(" %-72s%n", title);
        System.out.println("==========================================================================");
        if (rooms.isEmpty()) {
            System.out.println(" No rooms match the requested availability.");
        } else {
            System.out.printf("%-10s | %-26s | %-16s | %-10s%n", "Room No", "Room Type", "Selected Dates", "Price/Night");
            System.out.println("==========================================================================");
            for (int i = 0; i < rooms.getNumberOfEntries(); i++) {
                Room room = rooms.get(i);
                System.out.printf("%-10s | %-26s | %-16s | RM %7.2f%n", room.getRoomNumber(),
                        room.getRoomType(), "Available", room.getPrice());
            }
        }
        System.out.println("==========================================================================");
    }

    private void displayReport1() {
        System.out.println("\n--- REPORT 1: BOOKING SUMMARY (MULTI-CRITERIA FILTER & SORT) ---");

        System.out.print("Enter Room Type filter (Deluxe Suite / Presidential Suite / Standard Room / ALL): ");
        String typeFilter = readChoice("Deluxe Suite", "Presidential Suite", "Standard Room", "ALL");

        System.out.print("Enter Booking Status filter (Confirmed / CheckedIn / CheckedOut / Cancelled / NoShow / ALL): ");
        String statusFilter = readChoice("Confirmed", "CheckedIn", "CheckedOut", "Cancelled", "NoShow", "ALL");

        System.out.print("Start Check-In Date (YYYY-MM-DD, or ALL): ");
        String startDate = readDateOrAll();
        System.out.print("End Check-In Date (YYYY-MM-DD, or ALL): ");
        String endDate = readDateOrAll();
        if (!"ALL".equals(startDate) && !"ALL".equals(endDate) && startDate.compareTo(endDate) > 0) {
            System.out.println("Error: Start date cannot be after end date.");
            return;
        }

        System.out.print("Enter Minimum Number of Nights (Enter 0 for no filter): ");
        int minNights = readNonNegativeIntInput();

        System.out.print("Sort by total price? (1 for Ascending, 2 for Descending): ");
        int sortChoice = readChoiceNumber(1, 2);
        boolean ascending = sortChoice == 1;

        ListInterface<Booking> filtered = controller.getFilteredAndSortedBookings(
                typeFilter, statusFilter, startDate, endDate, minNights, ascending);

        System.out.println(
                "\n=============================================================================================");
        System.out.printf(" REPORT RESULTS: %d booking(s) match criteria\n", filtered.getNumberOfEntries());
        System.out.printf(" Filters: Type=%s | Status=%s | Check-In=%s to %s | Min Nights=%d | Total=%s%n",
                typeFilter, statusFilter, startDate, endDate, minNights, ascending ? "Low to High" : "High to Low");
        System.out.println(
                "=============================================================================================");

        if (filtered.isEmpty()) {
            System.out.println(" No bookings match the specified criteria.");
        } else {
            System.out.printf("%-8s | %-12s | %-8s | %-16s | %-12s | %-6s | %-10s | %-10s\n",
                    "ID", "Guest", "Room", "Type", "Check-In", "Nights", "Total(RM)", "Status");
            System.out.println(
                    "---------------------------------------------------------------------------------------------");

            double grandTotal = 0;
            for (int i = 0; i < filtered.getNumberOfEntries(); i++) {
                Booking b = filtered.get(i);
                System.out.printf("%-8s | %-12s | %-8s | %-16s | %-12s | %-6d | %10.2f | %-10s\n",
                        b.getBookingId(),
                        truncate(b.getGuestName(), 12),
                        b.getRoomNumber(),
                        truncate(b.getRoomType(), 16),
                        b.getCheckInDate(),
                        b.getNumberOfNights(),
                        b.getTotalPrice(),
                        b.getBookingStatus());
                if (!"Cancelled".equalsIgnoreCase(b.getBookingStatus())
                        && !"NoShow".equalsIgnoreCase(b.getBookingStatus())) {
                    grandTotal += b.getTotalPrice();
                }
            }
            System.out.println(
                    "---------------------------------------------------------------------------------------------");
            double[] metrics = controller.getBookingMetrics(filtered);
            System.out.printf(" Total Value (excluding cancelled): RM %.2f%n", grandTotal);
            System.out.printf(" Active / Cancelled / NoShow: %.0f / %.0f / %.0f | Average Stay: %.2f nights%n",
                    metrics[0], metrics[1], metrics[5], metrics[4]);
        }
        System.out.println(
                "=============================================================================================");
    }

    private void displayReport2() {
        System.out.println("\n--- REPORT 2: GUEST REGISTRATION & TIER ANALYSIS ---");

        System.out.print("Enter Loyalty Tier filter (Platinum / Gold / Silver / Standard / ALL): ");
        String tierFilter = readChoice("Platinum", "Gold", "Silver", "Standard", "ALL");

        System.out.print("Enter Status filter (Waiting / Confirmed / CheckedIn / CheckedOut / Cancelled / NoShow / ALL): ");
        String statusFilter = readChoice("Waiting", "Confirmed", "CheckedIn", "CheckedOut", "Cancelled", "NoShow", "ALL");

        System.out.print("Sort by Confirmation No? (1 for Ascending, 2 for Descending): ");
        int sortChoice = readChoiceNumber(1, 2);
        boolean ascending = sortChoice == 1;

        ListInterface<Guest> filtered = controller.getFilteredAndSortedGuests(
                tierFilter, statusFilter, ascending);

        // Summary statistics
        int[] summary = controller.getRegistrationSummary();

        System.out.println("\n==================================================");
        System.out.println("   GUEST REGISTRATION & TIER ANALYSIS REPORT      ");
        System.out.println("==================================================");
        System.out.printf(" Total Registered   : %d\n", summary[0]);
        System.out.printf(" Currently Waiting  : %d\n", summary[1]);
        System.out.printf(" Confirmed          : %d\n", summary[2]);
        System.out.printf(" Checked In         : %d\n", summary[3]);
        System.out.printf(" Checked Out        : %d\n", summary[4]);
        System.out.printf(" Cancelled          : %d\n", summary[5]);
        System.out.printf(" No-Show            : %d\n", summary[6]);
        System.out.println("--------------------------------------------------");
        System.out.printf(" Platinum Members   : %d\n", summary[7]);
        System.out.printf(" Gold Members       : %d\n", summary[8]);
        System.out.printf(" Silver Members     : %d\n", summary[9]);
        System.out.printf(" Standard Members   : %d\n", summary[10]);
        System.out.println("==================================================");

        System.out.printf("\n Filtered Results: %d guest(s) match criteria\n\n", filtered.getNumberOfEntries());

        if (filtered.isEmpty()) {
            System.out.println(" No guests match the specified criteria.");
        } else {
            System.out.printf("%-5s | %-15s | %-12s | %-10s | %-10s\n",
                    "Rank", "Guest Name", "Confirm No", "Tier", "Status");
            System.out.println("------+------------------+--------------+------------+-----------");

            for (int i = 0; i < filtered.getNumberOfEntries(); i++) {
                Guest g = filtered.get(i);
                String status = controller.getGuestOperationalStatus(g);
                System.out.printf("#%-4d | %-15s | %-12s | %-10s | %-10s\n",
                        (i + 1), g.getGuestName(), g.getConfirmationNumber(),
                        g.getLoyaltyTier(), status);
            }
        }
        System.out.println("==================================================");
    }

    private String truncate(String text, int maxLen) {
        if (text == null)
            return "";
        return text.length() <= maxLen ? text : text.substring(0, maxLen - 2) + "..";
    }

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

    private int readPositiveIntInput() {
        while (true) {
            int val = readIntInput();
            if (val > 0)
                return val;
            System.out.print("Value must be greater than 0. Please enter again: ");
        }
    }

    private int readNonNegativeIntInput() {
        while (true) {
            int value = readIntInput();
            if (value >= 0) return value;
            System.out.print("Value cannot be negative. Please enter again: ");
        }
    }

    private int readChoiceNumber(int first, int second) {
        while (true) {
            int value = readIntInput();
            if (value == first || value == second) return value;
            System.out.print("Please enter " + first + " or " + second + ": ");
        }
    }

    private String readChoice(String... choices) {
        while (true) {
            String input = scanner.nextLine().trim();
            for (String choice : choices) {
                if (choice.equalsIgnoreCase(input)) return choice;
            }
            System.out.print("Invalid option. Please enter one of the listed values: ");
        }
    }

    private String readDateOrAll() {
        while (true) {
            String input = scanner.nextLine().trim();
            if ("ALL".equalsIgnoreCase(input)) return "ALL";
            if (isValidDateInput(input)) return input;
            System.out.print("Invalid date. Use YYYY-MM-DD or ALL: ");
        }
    }

    private boolean isValidDateInput(String date) {
        try {
            LocalDate.parse(date);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    private void printStayValidationError(int result) {
        if (result == -4) {
            System.out.println("Error: Date must be a real calendar date in YYYY-MM-DD format.");
        } else if (result == -5) {
            System.out.println("Error: Stay duration must be between 1 and 30 nights.");
        } else if (result == -6) {
            System.out.println("Error: Check-in date cannot be earlier than today (" + LocalDate.now() + ").");
        } else if (result == -7) {
            System.out.println("Error: Check-in cannot be more than 365 days from today.");
        }
    }

    public static void main(String[] args) {
        BookingUI ui = new BookingUI();
        ui.displayMenu();
    }
}
