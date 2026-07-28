package boundary;

import control.BookingController;

/**
 * Placeholder Boundary Class for Member 1 (Walk-In Registrations & Booking Module)
 */
public class BookingUI {
    private BookingController controller;

    public BookingUI() {
        controller = new BookingController();
    }

    public void displayMenu() {
        System.out.println("\n--------------------------------------------------");
        System.out.println("     WALK-IN REGISTRATIONS & BOOKING MODULE       ");
        System.out.println("--------------------------------------------------");
        System.out.println("[Notice] Teammate 1 module integration placeholder.");
        System.out.println("--------------------------------------------------");
    }

    public static void main(String[] args) {
        BookingUI ui = new BookingUI();
        ui.displayMenu();
    }
}
