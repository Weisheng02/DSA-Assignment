package boundary;

import adt.ListInterface;
import control.HousekeepingController;
import entity.HousekeepingLog;
import entity.Room;

import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * Author: Kai Wei
 * Boundary Class for the Housekeeping & Task Log Module (Linear ADT: Stack).
 */
public class HousekeepingUI {

    private HousekeepingController controller;
    private Scanner scanner;

    public HousekeepingUI(HousekeepingController controller) {
        if (controller == null)
            throw new IllegalArgumentException("HousekeepingController is required.");
        this.controller = controller;
    }

    public void displayMenu(Scanner scanner) {
        this.scanner = (scanner != null) ? scanner : new Scanner(System.in);
        int choice = -1;

        do {
            System.out.println("\n--------------------------------------------------");
            System.out.println("       HOUSEKEEPING & TASK LOG MODULE              ");
            System.out.println("--------------------------------------------------");
            System.out.println("1. View All Rooms & Status");
            System.out.println("2. Advance Room to Next Cleaning Stage");
            System.out.println("3. Manually Set / Correct Room Status");
            System.out.println("4. Rollback Last Status Change");
            System.out.println("5. Report: Room Status Summary");
            System.out.println("6. Report: Filtered Task Log");
            System.out.println("7. Report: Rooms Needing Attention");
            System.out.println("0. Back to Main Menu");
            System.out.println("--------------------------------------------------");
            System.out.print("Enter your choice (0-7): ");

            String choiceInput = readLineOrNull();
            if (choiceInput == null) {
                System.out.println("Input ended. Returning to main menu...");
                break;
            }
            choice = parseMenuChoice(choiceInput, 0, 7);
            while (choice == -1) {
                System.out.println("Wrong input");
                System.out.print("Enter again, or enter 0 to return: ");
                choiceInput = readLineOrNull();
                if (choiceInput == null) {
                    choice = 0;
                    break;
                }
                choice = parseMenuChoice(choiceInput, 0, 7);
            }

            switch (choice) {
                case 1:
                    viewAllRooms();
                    break;
                case 2:
                    advanceRoomStatus();
                    break;
                case 3:
                    manualSetStatus();
                    break;
                case 4:
                    rollbackChange();
                    break;
                case 5:
                    roomStatusSummaryReport();
                    break;
                case 6:
                    filteredTaskLogReport();
                    break;
                case 7:
                    roomsNeedingAttentionReport();
                    break;
                case 0:
                    System.out.println("Returning to main menu...");
                    break;
            }
            if (choice != 0)
                pauseForEnter();
        } while (choice != 0);
    }

    private void viewAllRooms() {
        System.out.println("\nALL ROOMS");
        int[] widths = { 10, 22, 22 };
        ConsoleTable.printHeader(new String[] { "Room No", "Room Type", "Status" }, widths);
        ListInterface<Room> rooms = controller.getAllRooms();
        for (int i = 0; i < rooms.getNumberOfEntries(); i++) {
            Room room = rooms.get(i);
            ConsoleTable.printRow(new String[] { room.getRoomNumber(), room.getRoomType(), room.getRoomStatus() },
                    widths);
        }
        ConsoleTable.printFooter(widths);
    }

    private void advanceRoomStatus() {
        String roomNumber;
        while (true) {
            roomNumber = readRequiredOrBack("Enter Room Number (0 to return): ");
            if (roomNumber == null)
                return;
            if (controller.getRoomStatus(roomNumber) != null)
                break;
            System.out.println("Wrong input");
            System.out.println("Room " + roomNumber + " not found.");
        }
        String staffName = readRequiredOrBack("Enter Staff Name (0 to return): ");
        if (staffName == null)
            return;

        int result = controller.advanceRoomStatus(roomNumber, staffName);
        switch (result) {
            case 1:
                System.out.println("Room " + roomNumber + " advanced to: "
                        + controller.getRoomStatus(roomNumber));
                break;
            case -1:
                System.out.println("Room not found.");
                break;
            case -2:
                System.out.println("Room is already Ready for Check-In.");
                break;
            case -3:
                System.out.println("Room is not currently in the housekeeping cycle (e.g. Occupied).");
                break;
            case -4:
                System.out.println("Staff name cannot be blank.");
                break;
        }
    }

    private void manualSetStatus() {
        String roomNumber;
        while (true) {
            roomNumber = readRequiredOrBack("Enter Room Number (0 to return): ");
            if (roomNumber == null)
                return;
            if (controller.getRoomStatus(roomNumber) != null)
                break;
            System.out.println("Wrong input");
            System.out.println("Room " + roomNumber + " not found.");
        }

        String[] statuses = controller.getStatusSequence();
        System.out.println("Select new status:");
        for (int i = 0; i < statuses.length; i++)
            System.out.println("  " + (i + 1) + ". " + statuses[i]);
        int statusChoice = -1;
        while (statusChoice == -1) {
            System.out.print("Enter status number (1-4, or 0 to return): ");
            String newStatusInput = readLineOrNull();
            if (newStatusInput == null)
                return;
            statusChoice = parseMenuChoice(newStatusInput, 0, statuses.length);
            if (statusChoice == -1)
                System.out.println("Wrong input");
        }
        if (statusChoice == 0)
            return;
        String newStatus = statuses[statusChoice - 1];
        String staffName = readRequiredOrBack("Enter Staff Name (0 to return): ");
        if (staffName == null)
            return;

        int result = controller.setRoomStatus(roomNumber, newStatus, staffName);
        switch (result) {
            case 1:
                System.out.println("Status updated successfully.");
                break;
            case -1:
                System.out.println("Room not found.");
                break;
            case -2:
                System.out.println("Invalid status entered.");
                break;
            case 0:
                System.out.println("Room is already at that status; no task was logged.");
                break;
            case -4:
                System.out.println("Staff name cannot be blank.");
                break;
        }
    }

    private void rollbackChange() {
        HousekeepingLog last = controller.peekLastChange();
        if (last == null) {
            System.out.println("No changes to roll back.");
            return;
        }
        System.out.println("About to roll back: " + formatTaskLog(last));
        System.out.print("Confirm rollback? (Y/N): ");
        String confirmInput = readLineOrNull();
        if (confirmInput == null)
            return;
        String confirm = confirmInput.trim();
        while (!"Y".equalsIgnoreCase(confirm) && !"N".equalsIgnoreCase(confirm)
                && !"0".equals(confirm)) {
            System.out.println("Wrong input");
            System.out.print("Enter Y/N, or 0 to return: ");
            confirmInput = readLineOrNull();
            if (confirmInput == null)
                return;
            confirm = confirmInput.trim();
        }
        if ("0".equals(confirm))
            return;
        if (confirm.equalsIgnoreCase("Y")) {
            int result = controller.rollbackLastChange();
            if (result == 1) {
                System.out.println("Rolled back successfully.");
            } else if (result == -2) {
                System.out.println("Rollback blocked: the room was changed externally; no status was overwritten.");
            } else if (result == -1) {
                System.out.println("Rollback blocked: the room no longer exists.");
            } else {
                System.out.println("No changes to roll back.");
            }
        } else {
            System.out.println("Rollback cancelled.");
        }
    }

    private void roomStatusSummaryReport() {
        int[] summary = controller.getRoomStatusSummary();
        String[] sequence = controller.getStatusSequence();

        System.out.println("\nROOM STATUS SUMMARY REPORT");
        int[] widths = { 24, 8 };
        ConsoleTable.printHeader(new String[] { "Status", "Count" }, widths);
        ConsoleTable.printRow(new String[] { "Total Rooms", String.valueOf(summary[0]) }, widths);
        for (int i = 0; i < sequence.length; i++)
            ConsoleTable.printRow(new String[] { sequence[i], String.valueOf(summary[i + 1]) }, widths);
        ConsoleTable.printFooter(widths);
    }

    private void filteredTaskLogReport() {
        System.out.println("Filter by Room Number:");
        System.out.println("  1. All rooms");
        System.out.println("  2. Enter a specific room number");
        System.out.println("  0. Return");
        int roomChoice = readNumberedChoiceOrBack(2);
        if (roomChoice == 0)
            return;
        String roomFilter = "ALL";
        if (roomChoice == 2) {
            roomFilter = readRequiredOrBack("Enter Room Number (0 to return): ");
            if (roomFilter == null)
                return;
        }
        String statusFilter = readHousekeepingStatusFilter();
        if (statusFilter == null)
            return;
        String sort = readYesNoOrBack("Sort newest first? (Y/N, 0 to return): ");
        if (sort == null)
            return;
        boolean newestFirst = "Y".equals(sort);

        ListInterface<HousekeepingLog> logs = controller.getFilteredTaskLog(
                roomFilter,
                statusFilter,
                newestFirst);

        System.out.println("\nTASK LOG REPORT");
        if (logs.isEmpty()) {
            System.out.println("No matching records.");
            return;
        }
        int[] widths = { 8, 8, 22, 22, 16, 19 };
        ConsoleTable.printHeader(new String[] { "Task ID", "Room", "Previous Status", "New Status", "Staff",
                "Timestamp" }, widths);
        for (int i = 0; i < logs.getNumberOfEntries(); i++) {
            HousekeepingLog log = logs.get(i);
            ConsoleTable.printRow(new String[] { String.valueOf(log.getTaskId()), log.getRoomNumber(),
                    log.getPreviousStatus(), log.getNewStatus(), log.getStaffName(), log.getTimestamp() }, widths);
        }
        ConsoleTable.printFooter(widths);
    }

    private void roomsNeedingAttentionReport() {
        ListInterface<Room> rooms = controller.getRoomsNeedingAttention();

        System.out.println("\nROOMS NEEDING ATTENTION (NOT READY)");
        if (rooms.isEmpty()) {
            System.out.println("All rooms are Ready for Check-In.");
            return;
        }
        int[] widths = { 10, 22, 22 };
        ConsoleTable.printHeader(new String[] { "Room No", "Room Type", "Current Status" }, widths);
        for (int i = 0; i < rooms.getNumberOfEntries(); i++) {
            Room room = rooms.get(i);
            ConsoleTable.printRow(new String[] { room.getRoomNumber(), room.getRoomType(), room.getRoomStatus() },
                    widths);
        }
        ConsoleTable.printFooter(widths);
    }

    /**
     * Reads one menu line without allowing an exhausted/closed input stream to
     * crash the UI.
     */
    private String readLineOrNull() {
        try {
            return scanner.nextLine();
        } catch (NoSuchElementException | IllegalStateException e) {
            return null;
        }
    }

    private int parseMenuChoice(String input, int min, int max) {
        try {
            int value = Integer.parseInt(input.trim());
            return value >= min && value <= max ? value : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private String readRequiredOrBack(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = readLineOrNull();
            if (input == null || "0".equals(input.trim()))
                return null;
            if (!input.trim().isEmpty())
                return input.trim();
            System.out.println("Wrong input");
        }
    }

    private String readYesNoOrBack(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = readLineOrNull();
            if (input == null || "0".equals(input.trim()))
                return null;
            String choice = input.trim().toUpperCase();
            if ("Y".equals(choice) || "N".equals(choice))
                return choice;
            System.out.println("Wrong input");
        }
    }

    private String readHousekeepingStatusFilter() {
        String[] statuses = controller.getStatusSequence();
        System.out.println("Filter by New Status:");
        for (int i = 0; i < statuses.length; i++)
            System.out.println("  " + (i + 1) + ". " + statuses[i]);
        System.out.println("  " + (statuses.length + 1) + ". All");
        System.out.println("  0. Return");
        int choice = readNumberedChoiceOrBack(statuses.length + 1);
        if (choice == 0)
            return null;
        return choice == statuses.length + 1 ? "ALL" : statuses[choice - 1];
    }

    private int readNumberedChoiceOrBack(int max) {
        while (true) {
            System.out.print("Enter choice (0-" + max + "): ");
            String input = readLineOrNull();
            if (input == null)
                return 0;
            int choice = parseMenuChoice(input, 0, max);
            if (choice != -1)
                return choice;
            System.out.println("Wrong input");
        }
    }

    private String formatTaskLog(HousekeepingLog log) {
        return "Task#" + log.getTaskId()
                + " | Room " + log.getRoomNumber()
                + " | " + log.getPreviousStatus() + " -> " + log.getNewStatus()
                + " | By: " + log.getStaffName()
                + " | " + log.getTimestamp();
    }

    private void pauseForEnter() {
        System.out.print("\nPress Enter to return to the previous menu...");
        readLineOrNull();
    }

}
