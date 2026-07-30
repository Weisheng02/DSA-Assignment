package boundary;

import adt.ListInterface;
import control.BookingController;
import entity.Booking;
import entity.Guest;
import entity.Room;

import java.util.Scanner;

/**
 * Author: Weisheng
 * Boundary Class for Walk-In Registrations & Booking Module
 */
public class BookingUI {
    private BookingController controller;
    private Scanner scanner;

    public BookingUI() {
        controller = new BookingController();
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
            System.out.println("5. Cancel a Booking");
            System.out.println("6. Report: Booking Summary (Multi-Criteria Filter & Sort)");
            System.out.println("7. Report: Guest Registration & Tier Analysis");
            System.out.println("0. Back to Main Menu");
            System.out.println("--------------------------------------------------");
            System.out.print("Enter your choice (0-7): ");

            choice = readIntInput();
            System.out.println();

            switch (choice) {
                case 1: handleRegisterWalkIn(); break;
                case 2: handleViewWaitingQueue(); break;
                case 3: handleProcessNextGuest(); break;
                case 4: handleViewAllBookings(); break;
                case 5: handleCancelBooking(); break;
                case 6: displayReport1(); break;
                case 7: displayReport2(); break;
                case 0: System.out.println("Returning to main menu..."); break;
                default: System.out.println("Invalid selection. Please enter a number between 0 and 7.");
            }
        } while (choice != 0);
    }

    private void handleRegisterWalkIn() {
        System.out.println("\n--- Register New Walk-In Guest ---");
        System.out.print("Enter Guest Name: ");
        String name = scanner.nextLine().trim();
        if (name.isEmpty()) {
            System.out.println("Error: Guest name cannot be empty.");
            return;
        }

        System.out.println("Select Loyalty Tier:");
        System.out.println("  1. Platinum");
        System.out.println("  2. Gold");
        System.out.println("  3. Silver");
        System.out.println("  4. Standard");
        System.out.print("Enter choice (1-4): ");
        int tierChoice = readIntInput();

        String tier;
        switch (tierChoice) {
            case 1: tier = "Platinum"; break;
            case 2: tier = "Gold"; break;
            case 3: tier = "Silver"; break;
            default: tier = "Standard"; break;
        }

        Guest newGuest = controller.registerWalkInGuest(name, tier);

        System.out.println("\n==================================================");
        System.out.println("         WALK-IN REGISTRATION SUCCESSFUL          ");
        System.out.println("==================================================");
        System.out.printf(" Guest Name        : %s\n", newGuest.getGuestName());
        System.out.printf(" Confirmation No   : %s\n", newGuest.getConfirmationNumber());
        System.out.printf(" Loyalty Tier      : %s\n", newGuest.getLoyaltyTier());
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

        // Show available rooms
        ListInterface<Room> availableRooms = controller.getAvailableRooms();
        if (availableRooms.isEmpty()) {
            System.out.println("\nNo rooms are currently available (Ready for Check-In).");
            System.out.println("Guest remains in the queue.");
            return;
        }

        System.out.println("\nAvailable Rooms:");
        System.out.println("==========================================================================");
        System.out.printf("%-10s | %-20s | %-22s | %-10s\n",
                "Room No", "Room Type", "Status", "Price/Night");
        System.out.println("==========================================================================");
        for (int i = 0; i < availableRooms.getNumberOfEntries(); i++) {
            Room r = availableRooms.get(i);
            System.out.printf("%-10s | %-20s | %-22s | RM %7.2f\n",
                    r.getRoomNumber(), r.getRoomType(), r.getRoomStatus(), r.getPrice());
        }
        System.out.println("==========================================================================");

        System.out.print("Enter Room Number to assign: ");
        String roomNo = scanner.nextLine().trim();

        System.out.print("Enter Check-In Date (e.g. 2026-07-29): ");
        String checkInDate = scanner.nextLine().trim();

        System.out.print("Enter Number of Nights: ");
        int nights = readPositiveIntInput();

        int result = controller.processNextGuest(roomNo, checkInDate, nights);

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
                System.out.printf(" Duration          : %d Night(s)\n", lastBooking.getNumberOfNights());
                System.out.printf(" Rate / Night      : RM %.2f\n", lastBooking.getRoomPrice());
                System.out.printf(" Total Amount      : RM %.2f\n", lastBooking.getTotalPrice());
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
                Room r = controller.findRoomByNumber(roomNo);
                System.out.println("Error: Room " + roomNo + " is currently [" +
                        r.getRoomStatus() + "]. Only 'Ready for Check-In' rooms can be assigned.");
                break;
        }
    }

    private void handleViewAllBookings() {
        ListInterface<Booking> bookings = controller.getAllBookings();

        System.out.println("\n=============================================================================================");
        System.out.println("                            ALL BOOKING RECORDS                                              ");
        System.out.println("=============================================================================================");

        if (bookings.isEmpty()) {
            System.out.println(" No bookings found.");
        } else {
            System.out.printf("%-8s | %-12s | %-12s | %-8s | %-16s | %-6s | %-10s | %-10s\n",
                    "ID", "Guest", "Confirm No", "Room", "Type", "Nights", "Total(RM)", "Status");
            System.out.println("---------------------------------------------------------------------------------------------");
            for (int i = 0; i < bookings.getNumberOfEntries(); i++) {
                Booking b = bookings.get(i);
                System.out.printf("%-8s | %-12s | %-12s | %-8s | %-16s | %-6d | %10.2f | %-10s\n",
                        b.getBookingId(),
                        truncate(b.getGuestName(), 12),
                        b.getGuestConfirmationNumber(),
                        b.getRoomNumber(),
                        truncate(b.getRoomType(), 16),
                        b.getNumberOfNights(),
                        b.getTotalPrice(),
                        b.getBookingStatus());
            }
        }
        System.out.println("=============================================================================================");
    }

    private void handleCancelBooking() {
        System.out.println("\n--- Cancel a Booking ---");
        handleViewAllBookings();

        System.out.print("Enter Booking ID to cancel (e.g. BK0001): ");
        String bookingId = scanner.nextLine().trim();

        int result = controller.cancelBooking(bookingId);
        switch (result) {
            case 1:
                System.out.println("\nBooking " + bookingId + " has been cancelled successfully.");
                System.out.println("The room has been released back to [Ready for Check-In].");
                break;
            case -1:
                System.out.println("Error: Booking ID '" + bookingId + "' not found.");
                break;
            case -2:
                System.out.println("Error: Booking '" + bookingId + "' is already cancelled.");
                break;
        }
    }

    private void displayReport1() {
        System.out.println("\n--- REPORT 1: BOOKING SUMMARY (MULTI-CRITERIA FILTER & SORT) ---");

        System.out.print("Enter Room Type filter (Deluxe Suite / Presidential Suite / Standard Room / ALL): ");
        String typeFilter = scanner.nextLine().trim();

        System.out.print("Enter Minimum Number of Nights (Enter 0 for no filter): ");
        int minNights = readIntInput();

        System.out.print("Sort by total price? (1 for Ascending, 2 for Descending): ");
        int sortChoice = readIntInput();
        boolean ascending = (sortChoice != 2);

        ListInterface<Booking> filtered = controller.getFilteredAndSortedBookings(
                typeFilter, minNights, ascending);

        System.out.println("\n=============================================================================================");
        System.out.printf(" REPORT RESULTS: %d booking(s) match criteria\n", filtered.getNumberOfEntries());
        System.out.println("=============================================================================================");

        if (filtered.isEmpty()) {
            System.out.println(" No bookings match the specified criteria.");
        } else {
            System.out.printf("%-8s | %-12s | %-8s | %-16s | %-12s | %-6s | %-10s | %-10s\n",
                    "ID", "Guest", "Room", "Type", "Check-In", "Nights", "Total(RM)", "Status");
            System.out.println("---------------------------------------------------------------------------------------------");

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
                if ("Confirmed".equalsIgnoreCase(b.getBookingStatus())) {
                    grandTotal += b.getTotalPrice();
                }
            }
            System.out.println("---------------------------------------------------------------------------------------------");
            System.out.printf(" Grand Total (Confirmed only): RM %.2f\n", grandTotal);
        }
        System.out.println("=============================================================================================");
    }

    private void displayReport2() {
        System.out.println("\n--- REPORT 2: GUEST REGISTRATION & TIER ANALYSIS ---");

        System.out.print("Enter Loyalty Tier filter (Platinum / Gold / Silver / Standard / ALL): ");
        String tierFilter = scanner.nextLine().trim();

        System.out.print("Enter Status filter (Waiting / Checked-In / ALL): ");
        String statusFilter = scanner.nextLine().trim();

        System.out.print("Sort by Confirmation No? (1 for Ascending, 2 for Descending): ");
        int sortChoice = readIntInput();
        boolean ascending = (sortChoice != 2);

        ListInterface<Guest> filtered = controller.getFilteredAndSortedGuests(
                tierFilter, statusFilter, ascending);

        // Summary statistics
        int[] summary = controller.getRegistrationSummary();

        System.out.println("\n==================================================");
        System.out.println("   GUEST REGISTRATION & TIER ANALYSIS REPORT      ");
        System.out.println("==================================================");
        System.out.printf(" Total Registered   : %d\n", summary[0]);
        System.out.printf(" Currently Waiting  : %d\n", summary[1]);
        System.out.printf(" Already Checked-In : %d\n", summary[2]);
        System.out.println("--------------------------------------------------");
        System.out.printf(" Platinum Members   : %d\n", summary[3]);
        System.out.printf(" Gold Members       : %d\n", summary[4]);
        System.out.printf(" Silver Members     : %d\n", summary[5]);
        System.out.printf(" Standard Members   : %d\n", summary[6]);
        System.out.println("==================================================");

        System.out.printf("\n Filtered Results: %d guest(s) match criteria\n\n", filtered.getNumberOfEntries());

        if (filtered.isEmpty()) {
            System.out.println(" No guests match the specified criteria.");
        } else {
            System.out.printf("%-5s | %-15s | %-12s | %-10s | %-10s\n",
                    "Rank", "Guest Name", "Confirm No", "Tier", "Status");
            System.out.println("------+------------------+--------------+------------+-----------");

            ListInterface<Guest> queueSnapshot = controller.getWaitingQueueList();
            for (int i = 0; i < filtered.getNumberOfEntries(); i++) {
                Guest g = filtered.get(i);
                boolean isWaiting = isInList(g.getConfirmationNumber(), queueSnapshot);
                String status = isWaiting ? "Waiting" : "Checked-In";
                System.out.printf("#%-4d | %-15s | %-12s | %-10s | %-10s\n",
                        (i + 1), g.getGuestName(), g.getConfirmationNumber(),
                        g.getLoyaltyTier(), status);
            }
        }
        System.out.println("==================================================");
    }


    private boolean isInList(String confirmNo, ListInterface<Guest> list) {
        for (int i = 0; i < list.getNumberOfEntries(); i++) {
            if (list.get(i).getConfirmationNumber().equals(confirmNo)) {
                return true;
            }
        }
        return false;
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
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
            if (val > 0) return val;
            System.out.print("Value must be greater than 0. Please enter again: ");
        }
    }

    public static void main(String[] args) {
        BookingUI ui = new BookingUI();
        ui.displayMenu();
    }
}
