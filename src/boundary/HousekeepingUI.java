package boundary;

import control.HousekeepingController;

/**
 * Placeholder Boundary Class for Member 2 (Housekeeping and Task Log Module)
 */
public class HousekeepingUI {
    private HousekeepingController controller;

    public HousekeepingUI() {
        controller = new HousekeepingController();
    }

    public void displayMenu() {
        System.out.println("\n--------------------------------------------------");
        System.out.println("        HOUSEKEEPING & TASK LOG MODULE            ");
        System.out.println("--------------------------------------------------");
        System.out.println("[Notice] Teammate 2 module integration placeholder.");
        System.out.println("--------------------------------------------------");
    }

    public static void main(String[] args) {
        HousekeepingUI ui = new HousekeepingUI();
        ui.displayMenu();
    }
}
