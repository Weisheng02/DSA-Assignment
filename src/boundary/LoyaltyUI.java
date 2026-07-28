package boundary;

import control.LoyaltyController;

/**
 * Placeholder Boundary Class for Member 4 (Loyalty and Rewards Service Module)
 */
public class LoyaltyUI {
    private LoyaltyController controller;

    public LoyaltyUI() {
        controller = new LoyaltyController();
    }

    public void displayMenu() {
        System.out.println("\n--------------------------------------------------");
        System.out.println("       LOYALTY & REWARDS SERVICE MODULE           ");
        System.out.println("--------------------------------------------------");
        System.out.println("[Notice] Teammate 4 module integration placeholder.");
        System.out.println("--------------------------------------------------");
    }

    public static void main(String[] args) {
        LoyaltyUI ui = new LoyaltyUI();
        ui.displayMenu();
    }
}
