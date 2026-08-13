import adt.BSTInterface;
import adt.BinarySearchTree;
import adt.ListInterface;
import adt.MyArrayList;
import boundary.BookingUI;
import boundary.FrontDeskUI;
import boundary.HousekeepingUI;
import boundary.LoyaltyUI;
import entity.Guest;
import entity.Room;
import java.util.Scanner;

/**
 * Author: Weisheng
 * Main Application Entrance for TARUMT Resorts Management System
 * Driven by ONE shared Master Guest Registry and Shared Master Room List in
 * memory.
 */
public class App {
    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        int choice = -1;

        // 1. Initialize In-Memory Shared Master Collections (No External DB Needed)
        BSTInterface<Guest> masterGuestRegistry = new BinarySearchTree<>();
        ListInterface<Room> sharedRoomList = new MyArrayList<>();

        // 2. Seed Initial Master Data ONCE
        seedMasterData(masterGuestRegistry, sharedRoomList);

        // 3. Instantiate UI Subsystems passing shared memory references
        BookingUI bookingUI = new BookingUI(sharedRoomList, masterGuestRegistry);
        HousekeepingUI housekeepingUI = new HousekeepingUI(sharedRoomList);
        FrontDeskUI frontDeskUI = new FrontDeskUI(masterGuestRegistry, sharedRoomList);
        LoyaltyUI loyaltyUI = new LoyaltyUI(masterGuestRegistry);

        do {
            System.out.println("\n==================================================");
            System.out.println("         TARUMT RESORT MANAGEMENT SYSTEM          ");
            System.out.println("==================================================");
            System.out.println("1. Walk-In & Standard Booking (Zhi Xuan)");
            System.out.println("2. Housekeeping & Task Log (Kai Wei)");
            System.out.println("3. Front-Desk Service System (Wei Sheng)");
            System.out.println("4. Loyalty & Rewards Service (Hock Siang)");
            System.out.println("0. Exit Application");
            System.out.println("--------------------------------------------------");
            System.out.print("Enter module selection (0-4): ");

            try {
                choice = Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                choice = -1;
            }

            System.out.println();
            switch (choice) {
                case 1:
                    bookingUI.displayMenu(scanner);
                    break;
                case 2:
                    housekeepingUI.displayMenu(scanner);
                    break;
                case 3:
                    frontDeskUI.displayMenu(scanner);
                    break;
                case 4:
                    loyaltyUI.displayMenu(scanner);
                    break;
                case 0:
                    System.out.println("Thank you for using TARUMT Resort Management System. Goodbye!");
                    break;
                default:
                    System.out.println("Invalid selection. Please enter a number between 0 and 4.");
            }
        } while (choice != 0);
    }

    /**
     * Seeds the shared Master Guest Registry and Master Room List.
     * All modules share these exact objects in RAM.
     */
    private static void seedMasterData(BSTInterface<Guest> guestTree, ListInterface<Room> roomList) {
        // Seed Rooms
        roomList.add(new Room("101", "Deluxe Suite", "Ready for Check-In", 350.00));
        roomList.add(new Room("102", "Presidential Suite", "Dirty", 800.00));
        roomList.add(new Room("103", "Standard Room", "Ready for Check-In", 180.00));
        roomList.add(new Room("104", "Deluxe Suite", "Occupied", 350.00));
        roomList.add(new Room("105", "Standard Room", "Cleaning In Progress", 180.00));
        roomList.add(new Room("201", "Presidential Suite", "Ready for Check-In", 950.00));
        roomList.add(new Room("202", "Deluxe Suite", "Ready for Check-In", 400.00));

        // Seed Master Guests
        Guest alice = new Guest("Alice Tan", "980101-14-5566", "10000001", "Platinum", 1200);
        alice.setCheckedIn(true);
        alice.setAssignedRoomNumber("104");
        alice.setEffectiveRoomRate(350.00);
        guestTree.add(alice);

        guestTree.add(new Guest("Bob Lee", "990202-08-1234", "10000002", "Gold", 500));
        guestTree.add(new Guest("Charlie Lim", "000303-10-9988", "10000003", "Silver", 200));
        guestTree.add(new Guest("David Wong", "950404-01-3322", "10000004", "Standard", 50));
        guestTree.add(new Guest("Eva Green", "920505-07-7711", "10000005", "Platinum", 1800));
        guestTree.add(new Guest("Frank Wright", "960606-05-4433", "10000006", "Gold", 850));
    }
}
