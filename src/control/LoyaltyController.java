package control;

import adt.BSTInterface;
import entity.Guest;

/**
 * Author: Hock Siang
 * Controller Placeholder for Loyalty & Rewards Service Module.
 * Pending final code integration from team member Hock Siang.
 */
public class LoyaltyController {

    private BSTInterface<Guest> masterGuestRegistry;

    public LoyaltyController() {
        this(null);
    }

    public LoyaltyController(BSTInterface<Guest> masterGuestRegistry) {
        this.masterGuestRegistry = masterGuestRegistry;
    }

    public BSTInterface<Guest> getMasterGuestRegistry() {
        return masterGuestRegistry;
    }
}
