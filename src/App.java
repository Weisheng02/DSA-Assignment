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
import entity.Guest;
import entity.Booking;
import entity.Room;
import java.time.LocalDate;
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
                ListInterface<Booking> sharedBookingList = new MyArrayList<>();

                // 2. Seed Initial Master Data ONCE
                seedMasterData(masterGuestRegistry, sharedRoomList, sharedBookingList);

                // 3. Instantiate UI Subsystems passing shared memory references
                LoyaltyController loyaltyController = new LoyaltyController(masterGuestRegistry);
                BookingController bookingController = new BookingController(
                                sharedRoomList, masterGuestRegistry, sharedBookingList);
                HousekeepingController housekeepingController = new HousekeepingController(sharedRoomList);
                FrontDeskController frontDeskController = new FrontDeskController(
                                masterGuestRegistry, sharedRoomList, sharedBookingList, loyaltyController);

                BookingUI bookingUI = new BookingUI(bookingController);
                HousekeepingUI housekeepingUI = new HousekeepingUI(housekeepingController);
                FrontDeskUI frontDeskUI = new FrontDeskUI(frontDeskController);
                LoyaltyUI loyaltyUI = new LoyaltyUI(masterGuestRegistry, loyaltyController);

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
                                        System.out.println(
                                                        "Thank you for using TARUMT Resort Management System. Goodbye!");
                                        break;
                                default:
                                        System.out.println("Invalid selection. Please enter a number between 0 and 4.");
                        }
                } while (choice != 0);
        }

        /**
         * Seeds a coherent set of rooms, guests, and bookings for demonstrations.
         * All modules share these exact objects in RAM.
         */
        private static void seedMasterData(BSTInterface<Guest> guestTree, ListInterface<Room> roomList,
                        ListInterface<Booking> bookingList) {
                LocalDate today = LocalDate.now();

                // Seed Rooms
                roomList.add(new Room("101", "Deluxe Suite", "Ready for Check-In", 350.00));
                roomList.add(new Room("102", "Presidential Suite", "Dirty", 800.00));
                roomList.add(new Room("103", "Standard Room", "Reserved", 180.00));
                roomList.add(new Room("104", "Deluxe Suite", "Occupied", 350.00));
                roomList.add(new Room("105", "Standard Room", "Cleaning In Progress", 180.00));
                roomList.add(new Room("201", "Presidential Suite", "Inspected", 950.00));
                roomList.add(new Room("202", "Deluxe Suite", "Ready for Check-In", 400.00));
                roomList.add(new Room("203", "Standard Room", "Ready for Check-In", 200.00));

                // Seed guests with complete contact details and different lifecycle states.
                Guest alice = new Guest("Alice Tan", "980101-14-5566", "+60 12-345 6789", "Female",
                                "Malaysian", "alice.tan@example.com", "10000001", "Platinum", 1200);
                alice.setCheckedIn(true);
                alice.setAssignedRoomNumber("104");
                alice.setEffectiveRoomRate(350.00);
                alice.setRoomType("Deluxe Suite");
                alice.setCheckInDate(today.minusDays(1).toString());
                alice.setNumberOfNights(3);
                alice.setSpecialRequest("High floor and extra pillows");

                Guest bob = new Guest("Bob Lee", "990202-08-1234", "+60 17-222 3344", "Male",
                                "Malaysian", "bob.lee@example.com", "10000002", "Gold", 500);
                bob.setBookingStatus("Reserved");
                bob.setAssignedRoomNumber("103");
                bob.setRoomType("Standard Room");
                bob.setEffectiveRoomRate(180.00);
                bob.setCheckInDate(today.toString());
                bob.setNumberOfNights(2);
                bob.setSpecialRequest("Late arrival after 8 PM");

                Guest charlie = new Guest("Charlie Lim", "A12345678", "+60 16-333 4455", "Male",
                                "Singaporean", "charlie.lim@example.com", "10000003", "Silver", 200);
                charlie.setSpecialRequest("Quiet room if available");

                Guest david = new Guest("David Wong", "950404-01-3322", "+60 19-444 5566", "Male",
                                "Malaysian", "david.wong@example.com", "10000004", "Standard", 50);
                david.setBookingStatus("CheckedOut");
                david.setAssignedRoomNumber("101");
                david.setRoomType("Deluxe Suite");
                david.setEffectiveRoomRate(350.00);
                david.setCheckInDate(today.minusDays(2).toString());
                david.setCheckOutDate(today.toString());
                david.setNumberOfNights(2);
                david.setSpecialRequest("None");

                Guest eva = new Guest("Eva Green", "P-GB-927711", "+44 20 7946 0958", "Female",
                                "British", "eva.green@example.com", "10000005", "Platinum", 1800);
                eva.setBookingStatus("Cancelled");
                eva.setSpecialRequest("Airport transfer");

                Guest frank = new Guest("Frank Wright", "P-AU-964433", "+61 2 5550 7788", "Male",
                                "Australian", "frank.wright@example.com", "10000006", "Gold", 850);
                frank.setBookingStatus("NoShow");
                frank.setAssignedRoomNumber("202");
                frank.setRoomType("Deluxe Suite");
                frank.setEffectiveRoomRate(400.00);
                frank.setCheckInDate(today.minusDays(2).toString());
                frank.setNumberOfNights(1);
                frank.setSpecialRequest("Non-smoking room");

                // Balanced insertion order gives the initial BST a useful demonstration shape.
                guestTree.add(david);
                guestTree.add(bob);
                guestTree.add(frank);
                guestTree.add(alice);
                guestTree.add(charlie);
                guestTree.add(eva);

                // Seed matching bookings so Guest, Room, and Booking states agree.
                Booking aliceBooking = new Booking("BK0001", alice.getConfirmationNumber(), alice.getGuestName(),
                                "104", "Deluxe Suite", 350.00, alice.getCheckInDate(), alice.getNumberOfNights());
                aliceBooking.setSpecialRequest(alice.getSpecialRequest());
                aliceBooking.recordCheckIn(today.minusDays(1));
                bookingList.add(aliceBooking);

                Booking bobBooking = new Booking("BK0002", bob.getConfirmationNumber(), bob.getGuestName(),
                                "103", "Standard Room", 180.00, bob.getCheckInDate(), bob.getNumberOfNights());
                bobBooking.setSpecialRequest(bob.getSpecialRequest());
                bookingList.add(bobBooking);

                Booking davidBooking = new Booking("BK0003", david.getConfirmationNumber(), david.getGuestName(),
                                "101", "Deluxe Suite", 350.00, david.getCheckInDate(), david.getNumberOfNights());
                davidBooking.setSpecialRequest(david.getSpecialRequest());
                davidBooking.recordCheckIn(today.minusDays(2));
                davidBooking.recordCheckOut(today);
                bookingList.add(davidBooking);

                Booking evaBooking = new Booking("BK0004", eva.getConfirmationNumber(), eva.getGuestName(),
                                "201", "Presidential Suite", 950.00, today.plusDays(10).toString(), 4);
                evaBooking.setSpecialRequest(eva.getSpecialRequest());
                evaBooking.recordCancellation("Travel plans changed", "Reservation Desk");
                bookingList.add(evaBooking);

                Booking frankBooking = new Booking("BK0005", frank.getConfirmationNumber(), frank.getGuestName(),
                                "202", "Deluxe Suite", 400.00, frank.getCheckInDate(), frank.getNumberOfNights());
                frankBooking.setSpecialRequest(frank.getSpecialRequest());
                frankBooking.recordNoShow(today.minusDays(1));
                bookingList.add(frankBooking);
        }
}
