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

    public FrontDeskController() {
        guestTree = new BinarySearchTree<>();
        roomTree = new BinarySearchTree<>();
        seedInitialData();
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
        return roomTree.inOrderTraversal();
    }

    // Process check-in: returns 1=success, -1=guest not found, -2=room not found, -3=room not ready
    public int processCheckIn(String confirmationNumber, String roomNumber) {
        Guest guest = searchGuestByConfirmationNumber(confirmationNumber);
        if (guest == null)
            return -1;

        Room room = searchRoomByNumber(roomNumber);
        if (room == null)
            return -2;

        if (!"Ready for Check-In".equalsIgnoreCase(room.getRoomStatus())) {
            return -3;
        }

        room.setRoomStatus("Occupied");
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

    // Get BST tree stats for the diagnostics display
    public String[] getGuestTreeDiagnostics() {
        String[] stats = new String[4];
        stats[0] = String.valueOf(guestTree.getNumberOfEntries());
        stats[1] = String.valueOf(guestTree.getHeight());
        Guest minGuest = guestTree.getMin();
        Guest maxGuest = guestTree.getMax();
        stats[2] = (minGuest != null) ? minGuest.getConfirmationNumber() + " (" + minGuest.getGuestName() + ")" : "N/A";
        stats[3] = (maxGuest != null) ? maxGuest.getConfirmationNumber() + " (" + maxGuest.getGuestName() + ")" : "N/A";
        return stats;
    }

    // Report 1: Room status summary
    public int[] getRoomStatusSummary() {
        int[] summary = new int[5]; // Total, Ready, Occupied, Dirty, Cleaning
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