package boundary;

import control.HousekeepingController;

import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * Author: Kai Wei
 * Boundary Class for the Housekeeping & Task Log Module (Linear ADT: Stack).
 */
public class HousekeepingUI {

    private HousekeepingController controller;
    private Scanner scanner;

    public HousekeepingUI() {
        this(new HousekeepingController());
    }

    public HousekeepingUI(HousekeepingController controller) {
        this.controller = (controller != null) ? controller : new HousekeepingController();
    }

    public void displayMenu() {
        displayMenu(new Scanner(System.in));
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
            try {
                choice = Integer.parseInt(choiceInput.trim());
            } catch (NumberFormatException e) {
                choice = -1;
            }

            switch (choice) {
                case 1: viewAllRooms(); break;
                case 2: advanceRoomStatus(); break;
                case 3: manualSetStatus(); break;
                case 4: rollbackChange(); break;
                case 5: roomStatusSummaryReport(); break;
                case 6: filteredTaskLogReport(); break;
                case 7: roomsNeedingAttentionReport(); break;
                case 0: System.out.println("Returning to main menu..."); break;
                default: System.out.println("Invalid selection. Please enter a number between 0 and 7.");
            }
        } while (choice != 0);
    }

    private void viewAllRooms() {
        System.out.println("\n----- All Rooms -----");
        System.out.printf("%-10s%-20s%-22s%-10s%n", "Room No", "Type", "Status", "Price");
        for (String line : controller.getRoomDisplayLines()) {
            System.out.println(line);
        }
    }

    private void advanceRoomStatus() {
        System.out.print("Enter Room Number: ");
        String roomNumberInput = readLineOrNull();
        if (roomNumberInput == null) return;
        String roomNumber = roomNumberInput.trim();

        if (controller.getRoomStatus(roomNumber) == null) {
            System.out.println("Error: Room " + roomNumber + " not found.");
            return;
        }

        System.out.print("Enter Staff Name: ");
        String staffNameInput = readLineOrNull();
        if (staffNameInput == null) return;
        String staffName = staffNameInput.trim();

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
        System.out.print("Enter Room Number: ");
        String roomNumberInput = readLineOrNull();
        if (roomNumberInput == null) return;
        String roomNumber = roomNumberInput.trim();

        if (controller.getRoomStatus(roomNumber) == null) {
            System.out.println("Error: Room " + roomNumber + " not found.");
            return;
        }

        System.out.println("Valid statuses:");
        for (String s : controller.getStatusSequence()) {
            System.out.println("  - " + s);
        }
        System.out.print("Enter New Status: ");
        String newStatusInput = readLineOrNull();
        if (newStatusInput == null) return;
        String newStatus = newStatusInput.trim();
        System.out.print("Enter Staff Name: ");
        String staffNameInput = readLineOrNull();
        if (staffNameInput == null) return;
        String staffName = staffNameInput.trim();

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
        String last = controller.getLastChangeDisplayText();
        if (last == null) {
            System.out.println("No changes to roll back.");
            return;
        }
        System.out.println("About to roll back: " + last);
        System.out.print("Confirm rollback? (Y/N): ");
        String confirmInput = readLineOrNull();
        if (confirmInput == null) return;
        String confirm = confirmInput.trim();
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

        System.out.println("\n===== Room Status Summary Report =====");
        System.out.println("Total Rooms: " + summary[0]);
        for (int i = 0; i < sequence.length; i++) {
            System.out.println(sequence[i] + ": " + summary[i + 1]);
        }
        System.out.println("=======================================");
    }

    private void filteredTaskLogReport() {
        System.out.print("Filter by Room Number (Enter for ALL): ");
        String roomFilterInput = readLineOrNull();
        if (roomFilterInput == null) return;
        String roomFilter = roomFilterInput.trim();
        System.out.print("Filter by New Status (Enter for ALL): ");
        String statusFilterInput = readLineOrNull();
        if (statusFilterInput == null) return;
        String statusFilter = statusFilterInput.trim();
        System.out.print("Sort newest first? (Y/N): ");
        String sortInput = readLineOrNull();
        if (sortInput == null) return;
        boolean newestFirst = sortInput.trim().equalsIgnoreCase("Y");

        String[] logs = controller.getFilteredTaskLogDisplayLines(
                roomFilter.isEmpty() ? "ALL" : roomFilter,
                statusFilter.isEmpty() ? "ALL" : statusFilter,
                newestFirst);

        System.out.println("\n===== Task Log Report =====");
        if (logs.length == 0) {
            System.out.println("No matching records.");
        }
        for (String line : logs) {
            System.out.println(line);
        }
        System.out.println("============================");
    }

    private void roomsNeedingAttentionReport() {
        String[] rooms = controller.getRoomsNeedingAttentionDisplayLines();

        System.out.println("\n===== Rooms Needing Attention (Not Ready) =====");
        if (rooms.length == 0) {
            System.out.println("All rooms are Ready for Check-In.");
        }
        for (String line : rooms) {
            System.out.println(line);
        }
        System.out.println("================================================");
    }

    /** Reads one menu line without allowing an exhausted/closed input stream to crash the UI. */
    private String readLineOrNull() {
        try {
            return scanner.nextLine();
        } catch (NoSuchElementException | IllegalStateException e) {
            return null;
        }
    }

    public static void main(String[] args) {
        HousekeepingUI ui = new HousekeepingUI();
        ui.displayMenu();
    }
}
