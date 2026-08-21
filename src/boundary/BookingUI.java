package boundary;

import adt.ListInterface;
import control.BookingController;
import entity.Booking;
import entity.Guest;
import entity.Room;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

/**
 * Author: Yeoh Zhi Xuan
 * Boundary class for walk-in registrations and standard booking.
 */
public class BookingUI {
    private final BookingController controller;
    private Scanner scanner;

    public BookingUI(BookingController controller) {
        if (controller == null)
            throw new IllegalArgumentException("BookingController is required.");
        this.controller = controller;
    }

    public void displayMenu(Scanner scanner) {
        this.scanner = (scanner != null) ? scanner : new Scanner(System.in);
        int choice;
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
            choice = readMenuChoice(0, 11);
            System.out.println();
            switch (choice) {
                case 1:
                    handleRegisterWalkIn();
                    break;
                case 2:
                    displayWaitingQueue(controller.getWaitingQueueList());
                    break;
                case 3:
                    handleProcessNextGuest();
                    break;
                case 4:
                    displayAllBookings(controller.getAllBookings());
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
            }
            if (choice != 0)
                pauseForEnter();
        } while (choice != 0);
    }

    private void handleRegisterWalkIn() {
        System.out.println("\n--- Register Walk-In Guest / Reservation ---");
        String ic = readRequiredTextOrBack("Enter Guest IC / Passport Number (e.g. 980101-14-5566): ");
        if (ic == null)
            return;
        String name;
        Guest existingMember = controller.findGuestByIC(ic);
        if (existingMember != null) {
            System.out.println("\n✨ Existing Member Recognized!");
            displayGuestSummary(existingMember);
            System.out.println("  ℹ️  A NEW stay/reservation will be created for this member (new Confirmation No).");
            name = existingMember.getGuestName();
        } else {
            name = readRequiredTextOrBack("New Guest Detected. Enter Guest Name: ");
            if (name == null)
                return;
            System.out.println("Assigned Default Loyalty Tier: [Standard] (0 pts)");
        }
        Guest registered = controller.registerWalkInGuest(name, ic, "Standard", 0);
        displayRegistrationSuccess(registered, controller.getWaitingCount());
    }

    private void handleProcessNextGuest() {
        System.out.println("\n--- Process Next Guest in Queue ---");
        Guest nextGuest = controller.peekNextGuest();
        if (nextGuest == null) {
            System.out.println("The waiting queue is empty. No guest to process.");
            return;
        }
        System.out.println("Next guest in queue:");
        displayNextGuest(nextGuest);
        String[] stay = readStayPeriodOrBack("Enter Check-In Date (YYYY-MM-DD): ");
        if (stay == null)
            return;
        String date = stay[0];
        int nights = Integer.parseInt(stay[1]);
        ListInterface<Room> availableRooms = controller.getAvailableRooms(date, nights);
        displayRooms(availableRooms, "AVAILABLE ROOMS FOR REQUESTED STAY");
        if (availableRooms.isEmpty()) {
            System.out.println("Guest remains in the queue.");
            return;
        }
        String room = readRequiredTextOrBack("Enter Room Number to assign: ");
        if (room == null)
            return;
        System.out.print("Special Request (leave blank for None): ");
        String request = scanner.nextLine().trim();
        int result = controller.processNextGuest(room, date, nights, request);
        displayProcessNextGuestResult(result, room);
    }

    private void handleSearchBooking() {
        System.out.println("\n--- Search Booking ---");
        displayAllBookings(controller.getAllBookings());
        System.out.print("Search by (1) Booking ID or (2) Confirmation No. (0 to return): ");
        int choice = readChoiceNumber(1, 2);
        if (choice == 0)
            return;
        String value = readRequiredTextOrBack("Enter search value: ");
        if (value == null)
            return;
        Booking booking = choice == 2
                ? controller.findBookingByConfirmation(value)
                : controller.findBookingById(value);
        if (booking == null)
            System.out.println("No booking record was found.");
        else
            displayBookingDetails(booking);
    }

    private void handleModifyBooking() {
        System.out.println("\n--- Modify Confirmed Booking ---");
        displayAllBookings(controller.getAllBookings());
        String id = readRequiredTextOrBack("Enter Booking ID to modify (e.g. BK0002, 0 to return): ");
        if (id == null)
            return;
        Booking existing = controller.findBookingById(id);
        if (existing == null) {
            System.out.println("Error: Booking ID '" + id + "' was not found. Please choose an ID from the list above.");
            return;
        }
        if (!controller.isBookingEditable(id)) {
            displayBookingNotEditableMessage(existing);
            return;
        }
        displayBookingDetails(existing);
        String[] stay = readStayPeriodOrBack("New Check-In Date (YYYY-MM-DD): ");
        if (stay == null)
            return;
        String date = stay[0];
        int nights = Integer.parseInt(stay[1]);
        ListInterface<Room> availableRooms = controller.getAvailableRoomsForUpdate(id, date, nights);
        displayRooms(availableRooms, "ROOMS AVAILABLE FOR NEW STAY PERIOD");
        String room = readRequiredTextOrBack("New Room Number: ");
        if (room == null)
            return;
        System.out.print("Special Request (leave blank for None): ");
        String request = scanner.nextLine().trim();
        int result = controller.updateBooking(id, room, date, nights, request);
        displayUpdateBookingResult(result, id);
    }

    private void displayBookingNotEditableMessage(Booking booking) {
        String id = booking.getBookingId();
        String status = booking.getBookingStatus();
        System.out.println("Booking '" + id + "' cannot be modified because its current status is [" + status + "].");
        if ("CheckedIn".equalsIgnoreCase(status))
            System.out.println("The guest has already checked in. Use the Front Desk module for active-stay changes.");
        else if ("CheckedOut".equalsIgnoreCase(status))
            System.out.println("This stay has already been completed, so its reservation details are historical records.");
        else if ("Cancelled".equalsIgnoreCase(status))
            System.out.println("A cancelled booking is retained for audit history and cannot be reactivated here.");
        else if ("NoShow".equalsIgnoreCase(status))
            System.out.println("The scheduled arrival has already been recorded as a no-show.");
        else
            System.out.println("Only a booking with [Confirmed] status can be modified.");
    }

    private void handleCancelBooking() {
        System.out.println("\n--- Cancel a Booking ---");
        displayAllBookings(controller.getAllBookings());
        String id = readRequiredTextOrBack("Enter Booking ID to cancel (e.g. BK0001): ");
        if (id == null)
            return;
        String reason = readRequiredTextOrBack("Cancellation reason: ");
        if (reason == null)
            return;
        String staff = readRequiredTextOrBack("Processed by (staff name): ");
        if (staff == null)
            return;
        int result = controller.cancelBooking(id, reason, staff);
        displayCancelBookingResult(result, id);
    }

    private void handleCancelWaitingRegistration() {
        System.out.println("\n--- Cancel Waiting Registration ---");
        displayWaitingQueue(controller.getWaitingQueueList());
        String confirmation = readRequiredTextOrBack("Enter Confirmation No. to remove from queue: ");
        if (confirmation != null) {
            int result = controller.cancelWaitingRegistration(confirmation);
            displayCancelWaitingResult(result);
        }
    }

    private void handleViewAvailableRooms() {
        System.out.println("\n--- View Available Rooms for a Stay ---");
        String[] stay = readStayPeriodOrBack("Enter Check-In Date (YYYY-MM-DD): ");
        if (stay == null)
            return;
        String date = stay[0];
        int nights = Integer.parseInt(stay[1]);
        displayRooms(controller.getAvailableRooms(date, nights), "AVAILABLE ROOMS FOR REQUESTED STAY");
    }

    private void displayReport1() {
        System.out.println("\n--- REPORT 1: BOOKING SUMMARY (MULTI-CRITERIA FILTER & SORT) ---");
        String type = readMenuSelection("Select Room Type filter:",
                new String[] { "Standard Room", "Deluxe Suite", "Presidential Suite", "All" },
                new String[] { "Standard Room", "Deluxe Suite", "Presidential Suite", "ALL" });
        if (type == null)
            return;
        String status = readMenuSelection("Select Booking Status filter:",
                new String[] { "Confirmed", "Checked In", "Checked Out", "Cancelled", "No-Show", "All" },
                new String[] { "Confirmed", "CheckedIn", "CheckedOut", "Cancelled", "NoShow", "ALL" });
        if (status == null)
            return;
        String start = readOptionalDateFilter("Select Start Check-In Date filter:");
        if (start == null)
            return;
        String end = readOptionalDateFilter("Select End Check-In Date filter:");
        if (end == null)
            return;
        if (!"ALL".equals(start) && !"ALL".equals(end) && start.compareTo(end) > 0) {
            System.out.println("Error: Start date cannot be after end date.");
            return;
        }
        System.out.print("Enter Minimum Number of Nights (Enter 0 for no filter): ");
        int min = readNonNegativeIntInput();
        System.out.print("Sort by total price? (1 for Ascending, 2 for Descending): ");
        int sortChoice = readChoiceNumber(1, 2);
        if (sortChoice == 0)
            return;
        boolean asc = sortChoice == 1;
        ListInterface<Booking> filtered = controller.getFilteredAndSortedBookings(
                type, status, start, end, min, asc);
        displayBookingReport(filtered, controller.getBookingMetrics(filtered),
                type, status, start, end, min, asc);
    }

    private void displayReport2() {
        System.out.println("\n--- REPORT 2: GUEST REGISTRATION & TIER ANALYSIS ---");
        String tier = readMenuSelection("Select Loyalty Tier filter:",
                new String[] { "Standard", "Silver", "Gold", "Platinum", "All" },
                new String[] { "Standard", "Silver", "Gold", "Platinum", "ALL" });
        if (tier == null)
            return;
        String status = readMenuSelection("Select Guest Status filter:",
                new String[] { "Waiting", "Confirmed", "Checked In", "Checked Out", "Cancelled", "No-Show", "All" },
                new String[] { "Waiting", "Confirmed", "CheckedIn", "CheckedOut", "Cancelled", "NoShow", "ALL" });
        if (status == null)
            return;
        System.out.print("Sort by Confirmation No? (1 for Ascending, 2 for Descending): ");
        int sortChoice = readChoiceNumber(1, 2);
        if (sortChoice == 0)
            return;
        boolean asc = sortChoice == 1;
        ListInterface<Guest> filtered = controller.getFilteredAndSortedGuests(tier, status, asc);
        displayGuestReport(filtered, controller.getRegistrationSummary());
    }

    private void displayGuestSummary(Guest guest) {
        System.out.printf("  Guest Name    : %s%n  Loyalty Tier  : %s (%d pts)%n",
                guest.getGuestName(), guest.getLoyaltyTier(), guest.getLoyaltyPoints());
    }

    private void displayRegistrationSuccess(Guest guest, int queuePosition) {
        System.out.printf("%n==================================================%n"
                + "         WALK-IN REGISTRATION SUCCESSFUL          %n"
                + "==================================================%n"
                + " Guest Name        : %s%n"
                + " IC / Passport     : %s%n"
                + " Confirmation No   : %s (Unique For This Stay)%n"
                + " Loyalty Tier      : %s (%d pts)%n"
                + " Queue Position    : #%d%n"
                + "==================================================%n"
                + "Guest has been added to the waiting queue.%n",
                guest.getGuestName(), guest.getIcNo(), guest.getConfirmationNumber(),
                guest.getLoyaltyTier(), guest.getLoyaltyPoints(), queuePosition);
    }

    private void displayWaitingQueue(ListInterface<Guest> queueList) {
        System.out.println("\nCURRENT WAITING QUEUE (FIFO ORDER)");
        if (queueList.isEmpty()) {
            System.out.println("The waiting queue is empty. No guests waiting.");
        } else {
            System.out.println("Total Guests Waiting: " + queueList.getNumberOfEntries());
            int[] widths = { 10, 18, 12, 10 };
            ConsoleTable.printHeader(new String[] { "Position", "Guest Name", "Confirm No", "Tier" }, widths);
            for (int i = 0; i < queueList.getNumberOfEntries(); i++) {
                Guest guest = queueList.get(i);
                String position = (i == 0) ? "#1 [NEXT]" : "#" + (i + 1);
                ConsoleTable.printRow(new String[] { position, guest.getGuestName(),
                        guest.getConfirmationNumber(), guest.getLoyaltyTier() }, widths);
            }
            ConsoleTable.printFooter(widths);
        }
    }

    private void displayNextGuest(Guest guest) {
        int[] widths = { 18, 12, 10 };
        ConsoleTable.printHeader(new String[] { "Guest Name", "Confirm No", "Tier" }, widths);
        ConsoleTable.printRow(new String[] { guest.getGuestName(), guest.getConfirmationNumber(),
                guest.getLoyaltyTier() }, widths);
        ConsoleTable.printFooter(widths);
    }

    private void displayProcessNextGuestResult(int result, String roomNumber) {
        if (result == 1) {
            Booking booking = controller.getLastBooking();
            if (booking == null) {
                System.out.println("Error: Unable to display the newly created booking.");
                return;
            }
            System.out.printf("%n==================================================%n"
                    + "       BOOKING CONFIRMED SUCCESSFULLY!            %n"
                    + "==================================================%n"
                    + " Booking ID        : %s%n Guest Name        : %s%n Confirmation No   : %s%n"
                    + " Room Assigned     : Room %s (%s)%n Check-In Date     : %s%n"
                    + " Expected Check-Out: %s%n Duration          : %d Night(s)%n"
                    + " Rate / Night      : RM %.2f%n Total Amount      : RM %.2f%n"
                    + " Special Request   : %s%n"
                    + "==================================================%n"
                    + " Remaining in queue: %d guest(s)%n",
                    booking.getBookingId(), booking.getGuestName(), booking.getGuestConfirmationNumber(),
                    booking.getRoomNumber(), booking.getRoomType(), booking.getCheckInDate(),
                    booking.getCheckOutDate(), booking.getNumberOfNights(), booking.getRoomPrice(),
                    booking.getTotalPrice(), booking.getSpecialRequest(), controller.getWaitingCount());
        } else if (result == -1) {
            System.out.println("Error: Queue is empty.");
        } else if (result == -2) {
            System.out.println("Error: Room number not found.");
        } else if (result == -3) {
            System.out.println("Error: Room " + roomNumber + " is unavailable for the requested dates.");
        } else if (result == -4) {
            System.out.println("Error: Check-in date must use YYYY-MM-DD format and be a valid date.");
        } else if (result == -5) {
            System.out.println("Error: Stay must be between 1 and 30 nights.");
        } else if (result == -6) {
            System.out.println("Error: Check-in date cannot be in the past.");
        } else if (result == -7) {
            System.out.println("Error: Reservations can only be made up to 365 days in advance.");
        } else {
            System.out.println("Error: Unable to process the waiting guest.");
        }
    }

    private void displayRooms(ListInterface<Room> rooms, String title) {
        System.out.println("\n" + title);
        if (rooms.isEmpty()) {
            System.out.println("No rooms match the requested availability.");
        } else {
            int[] widths = { 10, 24, 16, 12 };
            ConsoleTable.printHeader(new String[] { "Room No", "Room Type", "Availability", "Price/Night" }, widths);
            for (int i = 0; i < rooms.getNumberOfEntries(); i++) {
                Room room = rooms.get(i);
                ConsoleTable.printRow(new String[] { room.getRoomNumber(), room.getRoomType(), "Available",
                        String.format("RM %.2f", room.getPrice()) }, widths);
            }
            ConsoleTable.printFooter(widths);
        }
    }

    private void displayAllBookings(ListInterface<Booking> bookings) {
        System.out.println("\nALL BOOKING RECORDS");
        if (bookings.isEmpty()) {
            System.out.println("No bookings found.");
        } else {
            int[] widths = { 8, 14, 12, 8, 12, 12, 6, 11, 10 };
            ConsoleTable.printHeader(new String[] { "ID", "Guest", "Confirm No", "Room", "Check-In", "Check-Out",
                    "Nights", "Total (RM)", "Status" }, widths);
            for (int i = 0; i < bookings.getNumberOfEntries(); i++) {
                Booking booking = bookings.get(i);
                ConsoleTable.printRow(new String[] { booking.getBookingId(), booking.getGuestName(),
                        booking.getGuestConfirmationNumber(), booking.getRoomNumber(), booking.getCheckInDate(),
                        booking.getCheckOutDate(), String.valueOf(booking.getNumberOfNights()),
                        String.format("%.2f", booking.getTotalPrice()), booking.getBookingStatus() }, widths);
            }
            ConsoleTable.printFooter(widths);
        }
    }

    private void displayBookingDetails(Booking booking) {
        System.out.println("\n==================================================");
        System.out.println("                 BOOKING DETAILS                  ");
        System.out.println("==================================================");
        System.out.printf(" Booking ID       : %s%n Guest            : %s (%s)%n Room             : %s (%s)%n"
                + " Stay             : %s to %s (%d night(s))%n Rate / Total     : RM %.2f / RM %.2f%n"
                + " Status           : %s%n Special Request  : %s%n Created On       : %s%n",
                booking.getBookingId(), booking.getGuestName(), booking.getGuestConfirmationNumber(),
                booking.getRoomNumber(), booking.getRoomType(), booking.getCheckInDate(), booking.getCheckOutDate(),
                booking.getNumberOfNights(), booking.getRoomPrice(), booking.getTotalPrice(),
                booking.getBookingStatus(), booking.getSpecialRequest(), booking.getBookingCreatedDate());
        if (!"N/A".equals(booking.getActualCheckInDate()))
            System.out.printf(" Actual Check-In  : %s%n", booking.getActualCheckInDate());
        if (!"N/A".equals(booking.getActualCheckOutDate()))
            System.out.printf(" Actual Check-Out : %s (%d actual night(s))%n", booking.getActualCheckOutDate(),
                    booking.getActualNightsStayed());
        if ("NoShow".equalsIgnoreCase(booking.getBookingStatus()))
            System.out.printf(" No-Show Recorded : %s%n", booking.getNoShowDate());
        if ("Cancelled".equalsIgnoreCase(booking.getBookingStatus()))
            System.out.printf(" Cancelled On     : %s%n Cancelled By     : %s%n Cancellation Note: %s%n",
                    booking.getCancellationDate(), booking.getCancelledBy(), booking.getCancellationReason());
        System.out.println("==================================================");
    }

    private void displayCancelBookingResult(int result, String bookingId) {
        if (result == 1)
            System.out.println("Booking " + bookingId
                    + " has been cancelled successfully.\nThe reservation dates were released; the room's physical housekeeping status was preserved.");
        else if (result == -1)
            System.out.println("Error: Booking ID '" + bookingId + "' not found.");
        else if (result == -2)
            System.out.println("Error: Booking '" + bookingId + "' is already cancelled.");
        else if (result == -3)
            System.out.println("Error: Cannot cancel booking '" + bookingId
                    + "'. Guest is currently CHECKED IN at Front Desk.");
        else if (result == -4)
            System.out.println("Error: Cannot cancel booking '" + bookingId + "'. Guest has ALREADY CHECKED OUT.");
        else if (result == -5)
            System.out.println("Error: A NoShow reservation can no longer be cancelled.");
        else
            System.out.println("Error: Booking could not be cancelled.");
    }

    private void displayUpdateBookingResult(int result, String bookingId) {
        if (result == 1) {
            System.out.println("Booking updated successfully and synchronized with the Front Desk guest record.");
            Booking updated = controller.findBookingById(bookingId);
            if (updated != null)
                displayBookingDetails(updated);
        } else if (result == -3) {
            System.out.println("Error: Room number not found.");
        } else if (result == -4) {
            System.out.println("Error: Date must be a real calendar date in YYYY-MM-DD format.");
        } else if (result == -5) {
            System.out.println("Error: Stay duration must be between 1 and 30 nights.");
        } else if (result == -6) {
            System.out.println("Error: Check-in date cannot be earlier than today (" + LocalDate.now() + ").");
        } else if (result == -7) {
            System.out.println("Error: Check-in cannot be more than 365 days from today.");
        } else if (result == -8) {
            System.out.println("Error: Selected room is unavailable for the requested dates.");
        } else {
            System.out.println("Error: This booking cannot be modified.");
        }
    }

    private void displayCancelWaitingResult(int result) {
        if (result == 1)
            System.out.println("Waiting registration cancelled. All remaining guests keep their original FIFO order.");
        else
            System.out.println("Error: No waiting guest has that confirmation number.");
    }

    private void displayBookingReport(ListInterface<Booking> filtered, double[] metrics,
            String typeFilter, String statusFilter, String startDate, String endDate,
            int minNights, boolean ascending) {
        System.out.println("\nBOOKING REPORT");
        System.out.printf(
                "Results: %d | Filters: Type=%s, Status=%s, Check-In=%s to %s, Min Nights=%d, Total=%s%n",
                filtered.getNumberOfEntries(), typeFilter, statusFilter, startDate, endDate, minNights,
                ascending ? "Low to High" : "High to Low");
        if (filtered.isEmpty()) {
            System.out.println("No bookings match the specified criteria.");
            return;
        }
        int[] widths = { 8, 14, 8, 18, 12, 6, 11, 10 };
        ConsoleTable.printHeader(new String[] { "ID", "Guest", "Room", "Room Type", "Check-In", "Nights",
                "Total (RM)", "Status" }, widths);
        for (int i = 0; i < filtered.getNumberOfEntries(); i++) {
            Booking booking = filtered.get(i);
            ConsoleTable.printRow(new String[] { booking.getBookingId(), booking.getGuestName(),
                    booking.getRoomNumber(), booking.getRoomType(), booking.getCheckInDate(),
                    String.valueOf(booking.getNumberOfNights()), String.format("%.2f", booking.getTotalPrice()),
                    booking.getBookingStatus() }, widths);
        }
        ConsoleTable.printFooter(widths);
        System.out.printf(
                "Total Value (excluding cancelled/no-show): RM %.2f%nActive / Cancelled / NoShow: %.0f / %.0f / %.0f | Average Stay: %.2f nights%n",
                metrics[3], metrics[0], metrics[1], metrics[5], metrics[4]);
    }

    private void displayGuestReport(ListInterface<Guest> filtered, int[] summary) {
        System.out.println("\nGUEST REGISTRATION & TIER ANALYSIS REPORT");
        int[] summaryWidths = { 22, 8 };
        ConsoleTable.printHeader(new String[] { "Summary", "Count" }, summaryWidths);
        String[] labels = { "Total Registered", "Currently Waiting", "Confirmed", "Checked In", "Checked Out",
                "Cancelled", "No-Show", "Platinum Members", "Gold Members", "Silver Members", "Standard Members" };
        for (int i = 0; i < labels.length; i++)
            ConsoleTable.printRow(new String[] { labels[i], String.valueOf(summary[i]) }, summaryWidths);
        ConsoleTable.printFooter(summaryWidths);
        System.out.println("Filtered Results: " + filtered.getNumberOfEntries() + " guest(s) match criteria");
        if (filtered.isEmpty()) {
            System.out.println("No guests match the specified criteria.");
            return;
        }
        int[] widths = { 6, 18, 13, 10, 12 };
        ConsoleTable.printHeader(new String[] { "Rank", "Guest Name", "Confirm No", "Tier", "Status" }, widths);
        for (int i = 0; i < filtered.getNumberOfEntries(); i++) {
            Guest guest = filtered.get(i);
            ConsoleTable.printRow(new String[] { "#" + (i + 1), guest.getGuestName(),
                    guest.getConfirmationNumber(), guest.getLoyaltyTier(),
                    controller.getGuestOperationalStatus(guest) }, widths);
        }
        ConsoleTable.printFooter(widths);
    }

    private int readIntInput() {
        while (true) {
            try {
                return Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Wrong input");
                System.out.print("Enter again, or enter 0 to return: ");
            }
        }
    }

    private int readPositiveIntInput() {
        int value;
        do {
            value = readIntInput();
            if (value < 0) {
                System.out.println("Wrong input");
                System.out.print("Enter again, or enter 0 to return: ");
            }
        } while (value < 0);
        return value;
    }

    private int readNonNegativeIntInput() {
        int value;
        do {
            value = readIntInput();
            if (value < 0) {
                System.out.println("Wrong input");
                System.out.print("Enter a non-negative value: ");
            }
        } while (value < 0);
        return value;
    }

    private int readChoiceNumber(int first, int second) {
        int value;
        do {
            value = readIntInput();
            if (value == 0)
                return 0;
            if (value != first && value != second) {
                System.out.println("Wrong input");
                System.out.print("Enter " + first + " or " + second + ", or 0 to return: ");
            }
        } while (value != first && value != second);
        return value;
    }

    private String readMenuSelection(String title, String[] labels, String[] values) {
        if (labels == null || values == null || labels.length == 0 || labels.length != values.length)
            throw new IllegalArgumentException("Menu labels and values must have the same non-zero length.");
        System.out.println(title);
        for (int i = 0; i < labels.length; i++)
            System.out.println((i + 1) + ". " + labels[i]);
        System.out.println("0. Return");
        System.out.print("Enter option number (0-" + labels.length + "): ");
        int choice = readMenuChoice(0, labels.length);
        return choice == 0 ? null : values[choice - 1];
    }

    private String readOptionalDateFilter(String title) {
        System.out.println(title);
        System.out.println("1. All dates");
        System.out.println("2. Enter a specific date");
        System.out.println("0. Return");
        System.out.print("Enter option number (0-2): ");
        int choice = readMenuChoice(0, 2);
        if (choice == 0)
            return null;
        if (choice == 1)
            return "ALL";
        return readValidDateOrBack("Enter date (YYYY-MM-DD, or 0 to return): ");
    }

    private int readMenuChoice(int min, int max) {
        while (true) {
            int value = readIntInput();
            if (value >= min && value <= max)
                return value;
            System.out.println("Wrong input");
            System.out.print("Enter a value from " + min + " to " + max + ", or 0 to return: ");
        }
    }

    private String readRequiredTextOrBack(String prompt) {
        while (true) {
            System.out.print(prompt);
            String value = scanner.nextLine().trim();
            if ("0".equals(value))
                return null;
            if (!value.isEmpty())
                return value;
            System.out.println("Wrong input");
            System.out.println("Enter again, or enter 0 to return.");
        }
    }

    private String readValidDateOrBack(String prompt) {
        while (true) {
            System.out.print(prompt);
            String value = scanner.nextLine().trim();
            if ("0".equals(value))
                return null;
            if (isValidDateInput(value))
                return value;
            System.out.println("Wrong input");
            System.out.println("Use YYYY-MM-DD, or enter 0 to return.");
        }
    }

    private String[] readStayPeriodOrBack(String datePrompt) {
        while (true) {
            String date = readValidDateOrBack(datePrompt);
            if (date == null)
                return null;
            System.out.print("Enter Number of Nights (1-30, 0 to return): ");
            int nights = readPositiveIntInput();
            if (nights == 0)
                return null;
            int validation = controller.validateStayPeriod(date, nights);
            if (validation == 1)
                return new String[] { date, String.valueOf(nights) };
            System.out.println("Wrong input");
            printStayValidationError(validation);
        }
    }

    private void pauseForEnter() {
        System.out.print("\nPress Enter to return to the previous menu...");
        scanner.nextLine();
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
        if (result == -4)
            System.out.println("Error: Date must be a real calendar date in YYYY-MM-DD format.");
        else if (result == -5)
            System.out.println("Error: Stay duration must be between 1 and 30 nights.");
        else if (result == -6)
            System.out.println("Error: Check-in date cannot be earlier than today (" + LocalDate.now() + ").");
        else if (result == -7)
            System.out.println("Error: Check-in cannot be more than 365 days from today.");
    }

}
