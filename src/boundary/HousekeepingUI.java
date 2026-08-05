package boundary;

import adt.ListInterface;
import control.HousekeepingController;
import entity.HousekeepingLog;
import entity.Room;

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

    public HousekeepingUI(ListInterface<Room> sharedRoomList) {
        this(new HousekeepingController(sharedRoomList));
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

            try {
                choice = Integer.parseInt(scanner.nextLine().trim());
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
        ListInterface<Room> rooms = controller.getAllRooms();
        System.out.println("\n----- All Rooms -----");
        System.out.printf("%-10s%-20s%-22s%-10s%n", "Room No", "Type", "Status", "Price");
        for (int i = 0; i < rooms.getNumberOfEntries(); i++) {
            Room r = rooms.get(i);
            System.out.printf("%-10s%-20s%-22s%-10.2f%n",
                    r.getRoomNumber(), r.getRoomType(), r.getRoomStatus(), r.getPrice());
        }
    }

    private void advanceRoomStatus() {
        System.out.print("Enter Room Number: ");
        String roomNumber = scanner.nextLine().trim();
        System.out.print("Enter Staff Name: ");
        String staffName = scanner.nextLine().trim();

        int result = controller.advanceRoomStatus(roomNumber, staffName);
        switch (result) {
            case 1:
                Room r = controller.findRoomByNumber(roomNumber);
                System.out.println("Room " + roomNumber + " advanced to: " + r.getRoomStatus());
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
        }
    }

    private void manualSetStatus() {
        System.out.print("Enter Room Number: ");
        String roomNumber = scanner.nextLine().trim();

        System.out.println("Valid statuses:");
        for (String s : controller.getStatusSequence()) {
            System.out.println("  - " + s);
        }
        System.out.print("Enter New Status: ");
        String newStatus = scanner.nextLine().trim();
        System.out.print("Enter Staff Name: ");
        String staffName = scanner.nextLine().trim();

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
        }
    }

    private void rollbackChange() {
        HousekeepingLog last = controller.peekLastChange();
        if (last == null) {
            System.out.println("No changes to roll back.");
            return;
        }
        System.out.println("About to roll back: " + last);
        System.out.print("Confirm rollback? (Y/N): ");
        String confirm = scanner.nextLine().trim();
        if (confirm.equalsIgnoreCase("Y")) {
            controller.rollbackLastChange();
            System.out.println("Rolled back successfully.");
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
        String roomFilter = scanner.nextLine().trim();
        System.out.print("Filter by New Status (Enter for ALL): ");
        String statusFilter = scanner.nextLine().trim();
        System.out.print("Sort newest first? (Y/N): ");
        boolean newestFirst = scanner.nextLine().trim().equalsIgnoreCase("Y");

        ListInterface<HousekeepingLog> logs = controller.getFilteredTaskLog(
                roomFilter.isEmpty() ? "ALL" : roomFilter,
                statusFilter.isEmpty() ? "ALL" : statusFilter,
                newestFirst);

        System.out.println("\n===== Task Log Report =====");
        if (logs.isEmpty()) {
            System.out.println("No matching records.");
        }
        for (int i = 0; i < logs.getNumberOfEntries(); i++) {
            System.out.println(logs.get(i));
        }
        System.out.println("============================");
    }

    private void roomsNeedingAttentionReport() {
        ListInterface<Room> rooms = controller.getRoomsNeedingAttention();

        System.out.println("\n===== Rooms Needing Attention (Not Ready) =====");
        if (rooms.isEmpty()) {
            System.out.println("All rooms are Ready for Check-In.");
        }
        for (int i = 0; i < rooms.getNumberOfEntries(); i++) {
            Room r = rooms.get(i);
            System.out.println(r.getRoomNumber() + " - " + r.getRoomStatus());
        }
        System.out.println("================================================");
    }

    public static void main(String[] args) {
        HousekeepingUI ui = new HousekeepingUI();
        ui.displayMenu();
    }
}
