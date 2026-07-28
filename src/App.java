import boundary.BookingUI;
import boundary.HousekeepingUI;
import boundary.FrontDeskUI;
import boundary.LoyaltyUI;
import java.util.Scanner;

/**
 * Author: Weisheng
 * Main Application Entrance for TARUMT Resorts Management System
 */
public class App {
    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        int choice = -1;

        do {
            System.out.println("\n==================================================");
            System.out.println("         TARUMT RESORT MANAGEMENT SYSTEM          ");
            System.out.println("==================================================");
            System.out.println("1. Walk-In & Standard Booking (Member 1)");
            System.out.println("2. Housekeeping & Task Log (Member 2)");
            System.out.println("3. Front-Desk Service System (Member 3 - Weisheng)");
            System.out.println("4. Loyalty & Rewards Service (Member 4)");
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
                    new BookingUI().displayMenu();
                    break;
                case 2:
                    new HousekeepingUI().displayMenu();
                    break;
                case 3:
                    new FrontDeskUI().displayMenu(scanner);
                    break;
                case 4:
                    new LoyaltyUI().displayMenu();
                    break;
                case 0:
                    System.out.println("Thank you for using TARUMT Resort Management System. Goodbye!");
                    break;
                default:
                    System.out.println("Invalid selection. Please enter a number between 0 and 4.");
            }
        } while (choice != 0);
    }
}
