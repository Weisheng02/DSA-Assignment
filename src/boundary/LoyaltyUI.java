package boundary;

import adt.BSTInterface;
import control.LoyaltyController;
import entity.Guest;
import java.util.Scanner;

/**
 * Author: Hock Siang
 * Boundary UI Placeholder for Loyalty & Rewards Service Module.
 * Pending final code integration from team member Hock Siang.
 */
public class LoyaltyUI {

    private LoyaltyController controller;

    public LoyaltyUI() {
        this(new LoyaltyController());
    }

    public LoyaltyUI(LoyaltyController controller) {
        this.controller = (controller != null) ? controller : new LoyaltyController();
    }

    public LoyaltyUI(BSTInterface<Guest> masterGuestRegistry) {
        this(new LoyaltyController(masterGuestRegistry));
    }

    public void displayMenu() {
        displayMenu(new Scanner(System.in));
    }

    public void displayMenu(Scanner scanner) {
        System.out.println("\n==================================================");
        System.out.println("     LOYALTY & REWARDS SERVICE (Hock Siang)       ");
        System.out.println("==================================================");
        System.out.println(" [NOTICE] This module is currently a placeholder.");
        System.out.println(" Pending code integration from Hock Siang.");
        System.out.println("==================================================");
        System.out.println("Press Enter to return to main menu...");
        if (scanner != null && scanner.hasNextLine()) {
            scanner.nextLine();
        }
    }
}
