package control;

import adt.BSTInterface;
import adt.BinarySearchTree;
import adt.ListInterface;
import adt.MyArrayList;
import entity.Guest;
import entity.Room;
import java.util.Comparator;

/**
 * Author: Weisheng
 * Controller for Front-Desk Operations. Uses BST for guest and room management.
 */
public class FrontDeskController {
    private BSTInterface<Guest> guestTree;
    private BSTInterface<Room> roomTree;

    private ListInterface<Room> sharedRoomList;

    public FrontDeskController() {
        this(null, null);
    }

    public FrontDeskController(BSTInterface<Guest> masterGuestTree, ListInterface<Room> sharedRoomList) {
        this.guestTree = (masterGuestTree != null) ? masterGuestTree : new BinarySearchTree<>();
        this.roomTree = new BinarySearchTree<>();
        this.sharedRoomList = sharedRoomList;
        if (masterGuestTree == null && sharedRoomList == null) {
            seedInitialData();
        } else {
            syncRoomTree();
        }
    }

    public void syncRoomTree() {
        if (sharedRoomList != null) {
            roomTree.clear();
            for (int i = 0; i < sharedRoomList.getNumberOfEntries(); i++) {
                roomTree.add(sharedRoomList.get(i));
            }
        }
    }

    private void seedInitialData() {
        guestTree.add(new Guest("Alice Tan", "10000001", "Platinum", 1200));
        guestTree.add(new Guest("Bob Lee", "10000002", "Gold", 500));
        guestTree.add(new Guest("Charlie Lim", "10000003", "Silver", 200));
        guestTree.add(new Guest("David Wong", "10000004", "Standard", 50));
        guestTree.add(new Guest("Eva Green", "10000005", "Platinum", 1800));
        guestTree.add(new Guest("Frank Wright", "10000006", "Gold", 850));

        roomTree.add(new Room("101", "Deluxe Suite", "Ready for Check-In", 350.00));
        roomTree.add(new Room("102", "Presidential Suite", "Dirty", 800.00));
        roomTree.add(new Room("103", "Standard Room", "Ready for Check-In", 180.00));
        roomTree.add(new Room("104", "Deluxe Suite", "Occupied", 350.00));
        roomTree.add(new Room("105", "Standard Room", "Cleaning In Progress", 180.00));
        roomTree.add(new Room("201", "Presidential Suite", "Ready for Check-In", 950.00));
        roomTree.add(new Room("202", "Deluxe Suite", "Ready for Check-In", 400.00));
    }

    // Search guest by confirmation number using BST search
    public Guest searchGuestByConfirmationNumber(String confirmNo) {
        if (confirmNo == null || confirmNo.trim().isEmpty())
            return null;
        Guest targetDummy = new Guest("", confirmNo.trim(), "", 0);
        return guestTree.search(targetDummy);
    }

    // Search guests within a range of confirmation numbers
    public ListInterface<Guest> searchGuestsByConfirmationRange(String startNo, String endNo) {
        if (startNo == null || endNo == null) return new MyArrayList<>();
        Guest minDummy = new Guest("", startNo.trim(), "", 0);
        Guest maxDummy = new Guest("", endNo.trim(), "", 0);
        return guestTree.rangeSearch(minDummy, maxDummy);
    }

    // Add a new guest into the BST
    public boolean registerGuest(Guest guest) {
        if (guest == null || guest.getConfirmationNumber() == null) return false;
        if (guestTree.contains(guest)) return false;
        return guestTree.add(guest);
    }

    // Remove a guest from the BST
    public Guest removeGuest(String confirmNo) {
        if (confirmNo == null || confirmNo.trim().isEmpty()) return null;
        Guest dummy = new Guest("", confirmNo.trim(), "", 0);
        return guestTree.remove(dummy);
    }

    // Find guests by name (traverses all nodes then filters)
    public ListInterface<Guest> searchGuestsByName(String nameQuery) {
        ListInterface<Guest> results = new MyArrayList<>();
        if (nameQuery == null || nameQuery.trim().isEmpty())
            return results;

        String queryLower = nameQuery.trim().toLowerCase();
        ListInterface<Guest> allGuests = guestTree.inOrderTraversal();
        for (int i = 0; i < allGuests.getNumberOfEntries(); i++) {
            Guest g = allGuests.get(i);
            if (g.getGuestName().toLowerCase().contains(queryLower)) {
                results.add(g);
            }
        }
        return results;
    }

    // Search room by room number using BST
    public Room searchRoomByNumber(String roomNumber) {
        if (roomNumber == null || roomNumber.trim().isEmpty())
            return null;
        Room targetDummy = new Room(roomNumber.trim(), "", "", 0.0);
        return roomTree.search(targetDummy);
    }

    public ListInterface<Room> getAllRooms() {
        syncRoomTree();
        return roomTree.inOrderTraversal();
    }

    private ListInterface<String> activeCheckedInConfirmations = new MyArrayList<>();

    // Process check-in: returns 1=success, -1=guest not found, -2=room not found, -3=room not ready, -4=guest already checked-in
    public int processCheckIn(String confirmationNumber, String roomNumber) {
        Room r = searchRoomByNumber(roomNumber);
        double price = (r != null) ? r.getPrice() : 0.0;
        return processCheckIn(confirmationNumber, roomNumber, price);
    }

    public int processCheckIn(String confirmationNumber, String roomNumber, double baseRoomPrice) {
        syncRoomTree();
        Guest guest = searchGuestByConfirmationNumber(confirmationNumber);
        if (guest == null)
            return -1;

        // Double Lock: Check both active list AND Guest entity's checkedIn flag
        if (guest.isCheckedIn()) {
            return -4;
        }

        for (int i = 0; i < activeCheckedInConfirmations.getNumberOfEntries(); i++) {
            if (activeCheckedInConfirmations.get(i).equalsIgnoreCase(confirmationNumber.trim())) {
                return -4;
            }
        }

        Room room = searchRoomByNumber(roomNumber);
        if (room == null)
            return -2;

        String currentStatus = room.getRoomStatus();
        if (!"Ready for Check-In".equalsIgnoreCase(currentStatus) && !"Reserved".equalsIgnoreCase(currentStatus)) {
            return -3;
        }

        // Validate that if a room is Reserved, it must be reserved for THIS guest
        if ("Reserved".equalsIgnoreCase(currentStatus)) {
            if (guest.getAssignedRoomNumber() == null || !guest.getAssignedRoomNumber().equalsIgnoreCase(roomNumber.trim())) {
                return -5; // Room is reserved for another guest
            }
        }

        room.setRoomStatus("Occupied");
        guest.setCheckedIn(true);
        guest.setAssignedRoomNumber(roomNumber);
        guest.setEffectiveRoomRate(baseRoomPrice > 0 ? baseRoomPrice : room.getPrice());
        activeCheckedInConfirmations.add(confirmationNumber.trim());
        return 1;
    }

    /**
     * Process Guest Check-Out:
     * Sets room status to "Dirty" for Housekeeping, resets guest checked-in state,
     * and removes guest from active checked-in list.
     * @return 1: success, -1: guest not found, -2: guest not checked-in
     */
    public int processCheckOut(String confirmationNumber) {
        if (confirmationNumber == null || confirmationNumber.trim().isEmpty()) return -1;
        Guest guest = searchGuestByConfirmationNumber(confirmationNumber);
        if (guest == null) return -1;
        if (!guest.isCheckedIn()) return -2;

        String roomNo = guest.getAssignedRoomNumber();
        if (roomNo != null) {
            Room room = searchRoomByNumber(roomNo);
            if (room != null) {
                room.setRoomStatus("Dirty");
            }
        }

        guest.setCheckedIn(false);
        guest.setAssignedRoomNumber(null);
        guest.setEffectiveRoomRate(0.0);

        // Remove confirmation from active checked in list
        ListInterface<String> updatedActive = new MyArrayList<>();
        for (int i = 0; i < activeCheckedInConfirmations.getNumberOfEntries(); i++) {
            String c = activeCheckedInConfirmations.get(i);
            if (!c.equalsIgnoreCase(confirmationNumber.trim())) {
                updatedActive.add(c);
            }
        }
        activeCheckedInConfirmations = updatedActive;
        return 1;
    }

    // Suggest a room upgrade - find cheapest available room that costs more than current
    public Room suggestRoomUpgrade(String currentRoomNo) {
        Room currentRoom = searchRoomByNumber(currentRoomNo);
        if (currentRoom == null) return null;

        ListInterface<Room> allRooms = roomTree.inOrderTraversal();
        Room bestUpgrade = null;

        for (int i = 0; i < allRooms.getNumberOfEntries(); i++) {
            Room r = allRooms.get(i);
            if ("Ready for Check-In".equalsIgnoreCase(r.getRoomStatus()) && r.getPrice() > currentRoom.getPrice()) {
                if (bestUpgrade == null || r.getPrice() < bestUpgrade.getPrice()) {
                    bestUpgrade = r;
                }
            }
        }
        return bestUpgrade;
    }

    // Get discount percentage based on loyalty tier
    public double getDiscountPercentage(String loyaltyTier) {
        if (loyaltyTier == null)
            return 0.0;
        switch (loyaltyTier.toUpperCase()) {
            case "PLATINUM":
                return 0.20;
            case "GOLD":
                return 0.10;
            case "SILVER":
                return 0.05;
            default:
                return 0.00;
        }
    }

    // Advanced BST Diagnostic & Rebalance Methods
    public void rebalanceTrees() {
        guestTree.rebalance();
        roomTree.rebalance();
    }

    public boolean isGuestTreeBalanced() {
        return guestTree.isBalanced();
    }

    public void printGuestTreeStructure() {
        System.out.println("\n=== Guest BST ASCII Visualizer ===");
        guestTree.printTree();
    }

    public void printRoomTreeStructure() {
        System.out.println("\n=== Room BST ASCII Visualizer ===");
        roomTree.printTree();
    }

    public ListInterface<Guest> getGuestTraversal(int mode) {
        switch (mode) {
            case 1:
                return guestTree.inOrderTraversal();
            case 2:
                return guestTree.preOrderTraversal();
            case 3:
                return guestTree.postOrderTraversal();
            default:
                return guestTree.inOrderTraversal();
        }
    }

    // Comprehensive diagnostics report array
    public String[] getGuestTreeDiagnostics() {
        String[] stats = new String[6];
        stats[0] = String.valueOf(guestTree.getNumberOfEntries());
        stats[1] = String.valueOf(guestTree.getHeight());
        stats[2] = String.valueOf(guestTree.getLeafCount());
        stats[3] = guestTree.isBalanced() ? "Balanced (Balanced Height)" : "Unbalanced";
        Guest minGuest = guestTree.getMin();
        Guest maxGuest = guestTree.getMax();
        stats[4] = (minGuest != null) ? minGuest.getConfirmationNumber() + " (" + minGuest.getGuestName() + ")" : "N/A";
        stats[5] = (maxGuest != null) ? maxGuest.getConfirmationNumber() + " (" + maxGuest.getGuestName() + ")" : "N/A";
        return stats;
    }

    // Revenue and Occupancy Analytics
    public double calculateOccupancyRate() {
        int[] summary = getRoomStatusSummary();
        if (summary[0] == 0) return 0.0;
        return ((double) summary[2] / summary[0]) * 100.0; // summary[2] is Occupied count
    }

    public double calculateEstimatedDailyRevenue() {
        ListInterface<Room> rooms = roomTree.inOrderTraversal();
        double totalRevenue = 0.0;
        for (int i = 0; i < rooms.getNumberOfEntries(); i++) {
            Room r = rooms.get(i);
            if ("Occupied".equalsIgnoreCase(r.getRoomStatus())) {
                totalRevenue += r.getPrice();
            }
        }
        return totalRevenue;
    }

    // Report 1: Room status summary
    public int[] getRoomStatusSummary() {
        int[] summary = new int[6]; // Total, Ready, Occupied, Dirty, Cleaning, Reserved
        ListInterface<Room> rooms = roomTree.inOrderTraversal();
        summary[0] = rooms.getNumberOfEntries();

        for (int i = 0; i < rooms.getNumberOfEntries(); i++) {
            String status = rooms.get(i).getRoomStatus();
            if ("Ready for Check-In".equalsIgnoreCase(status))
                summary[1]++;
            else if ("Occupied".equalsIgnoreCase(status))
                summary[2]++;
            else if ("Dirty".equalsIgnoreCase(status))
                summary[3]++;
            else if ("Cleaning In Progress".equalsIgnoreCase(status))
                summary[4]++;
            else if ("Reserved".equalsIgnoreCase(status))
                summary[5]++;
        }
        return summary;
    }

    // Report 2: Filter rooms by status & max price, then sort by price
    public ListInterface<Room> getFilteredAndSortedRooms(String statusFilter, double maxPrice, boolean sortAsc) {
        ListInterface<Room> filtered = new MyArrayList<>();
        ListInterface<Room> rooms = roomTree.inOrderTraversal();

        for (int i = 0; i < rooms.getNumberOfEntries(); i++) {
            Room r = rooms.get(i);
            boolean statusOk = "ALL".equalsIgnoreCase(statusFilter) || r.getRoomStatus().equalsIgnoreCase(statusFilter);
            boolean priceOk = (maxPrice <= 0) || (r.getPrice() <= maxPrice);

            if (statusOk && priceOk) {
                filtered.add(r);
            }
        }

        filtered.sort(new Comparator<Room>() {
            @Override
            public int compare(Room r1, Room r2) {
                if (sortAsc) {
                    return Double.compare(r1.getPrice(), r2.getPrice());
                } else {
                    return Double.compare(r2.getPrice(), r1.getPrice());
                }
            }
        });

        return filtered;
    }

    // Report 3: Filter guests by tier & min points, sort by points descending
    public ListInterface<Guest> getFilteredAndSortedGuests(String tierFilter, int minPoints) {
        ListInterface<Guest> filtered = new MyArrayList<>();
        ListInterface<Guest> guests = guestTree.inOrderTraversal();

        for (int i = 0; i < guests.getNumberOfEntries(); i++) {
            Guest g = guests.get(i);
            boolean tierOk = "ALL".equalsIgnoreCase(tierFilter) || g.getLoyaltyTier().equalsIgnoreCase(tierFilter);
            boolean pointsOk = g.getLoyaltyPoints() >= minPoints;

            if (tierOk && pointsOk) {
                filtered.add(g);
            }
        }

        filtered.sort(new Comparator<Guest>() {
            @Override
            public int compare(Guest g1, Guest g2) {
                return Integer.compare(g2.getLoyaltyPoints(), g1.getLoyaltyPoints());
            }
        });

        return filtered;
    }
}