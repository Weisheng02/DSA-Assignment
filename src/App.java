import adt.BSTInterface;
import adt.BinarySearchTree;
import adt.ListInterface;
import adt.MyArrayList;
import boundary.BookingUI;
import boundary.FrontDeskUI;
import boundary.HousekeepingUI;
import boundary.LoyaltyUI;
import control.BookingController;
import control.FrontDeskController;
import control.HousekeepingController;
import control.LoyaltyController;
import data.ResortDataSeeder;
import entity.Guest;
import entity.Booking;
import entity.Room;
import java.util.Scanner;

/**
 * Author: Yeap Wei Sheng
 * Main Application Entrance for TARUMT Resorts Management System
 * Driven by ONE shared Master Guest Registry and Shared Master Room List in
 * memory.
 */
public class App {
        public static void main(String[] args) {
                Scanner scanner = new Scanner(System.in);
                int choice = -1;

                // 1. Initialize In-Memory Shared Master Collections (No External DB Needed)
                BSTInterface<Guest> masterGuestRegistry = new BinarySearchTree<>();
                ListInterface<Room> sharedRoomList = new MyArrayList<>();
                ListInterface<Booking> sharedBookingList = new MyArrayList<>();

                // 2. Seed Initial Master Data ONCE
                ResortDataSeeder.seed(masterGuestRegistry, sharedRoomList, sharedBookingList);

                // 3. Instantiate UI Subsystems passing shared memory references
                BookingController bookingController = new BookingController(
                                sharedRoomList, masterGuestRegistry, sharedBookingList);
                LoyaltyController loyaltyController = new LoyaltyController(masterGuestRegistry);
                HousekeepingController housekeepingController = new HousekeepingController(sharedRoomList);
                FrontDeskController frontDeskController = new FrontDeskController(
                                masterGuestRegistry, sharedRoomList, sharedBookingList, loyaltyController);

                BookingUI bookingUI = new BookingUI(bookingController);
                HousekeepingUI housekeepingUI = new HousekeepingUI(housekeepingController);
                FrontDeskUI frontDeskUI = new FrontDeskUI(frontDeskController);
                LoyaltyUI loyaltyUI = new LoyaltyUI(loyaltyController);

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

                        String input = scanner.nextLine().trim();
                        try {
                                choice = Integer.parseInt(input);
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
                                        System.out.println(
                                                        "Thank you for using TARUMT Resort Management System. Goodbye!");
                                        break;
                                default:
                                        System.out.println("Wrong input");
                                        System.out.println("Enter again, or enter 0 to exit.");
                        }
                } while (choice != 0);
        }

}
