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
 * Controller Class for Front-Desk Operations using Non-Linear ADT (Binary Search Tree)
 */
public class FrontDeskController {
    private BSTInterface<Guest> guestTree;
    private BSTInterface<Room> roomTree;

    public FrontDeskController() {
        guestTree = new BinarySearchTree<>();
        roomTree = new BinarySearchTree<>();
        seedInitialData();
    }

    // Load initial sample data into Non-Linear Tree Structures
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

    // Non-linear BST Search for Guest by 8-digit Confirmation Number O(log n)
    public Guest searchGuestByConfirmationNumber(String confirmNo) {
        if (confirmNo == null || confirmNo.trim().isEmpty())
            return null;
        Guest targetDummy = new Guest("", confirmNo.trim(), "", 0);
        return guestTree.search(targetDummy);
    }

    // Find guests by name using In-Order Tree Traversal
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

    // Non-linear BST Search for Room by Room Number O(log n)
    public Room searchRoomByNumber(String roomNumber) {
        if (roomNumber == null || roomNumber.trim().isEmpty())
            return null;
        Room targetDummy = new Room(roomNumber.trim(), "", "", 0.0);
        return roomTree.search(targetDummy);
    }

    // Return in-order list of all rooms from BST
    public ListInterface<Room> getAllRooms() {
        return roomTree.inOrderTraversal();
    }

    // Process check-in logic
    // Returns 1: success, -1: guest not found, -2: room not found, -3: room not ready
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

    // Calculate discount rate based on membership tier
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

    // Calculate summary statistics of room statuses (Report 1)
    public int[] getRoomStatusSummary() {
        int[] summary = new int[5]; // [0]: Total, [1]: Ready, [2]: Occupied, [3]: Dirty, [4]: Cleaning
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

    // Advanced Report 2: Multi-Criteria Filtered and Sorted Rooms Report
    public ListInterface<Room> getFilteredAndSortedRooms(String statusFilter, double maxPrice, boolean sortByPriceAscending) {
        ListInterface<Room> filteredList = new MyArrayList<>();
        ListInterface<Room> rooms = roomTree.inOrderTraversal();

        for (int i = 0; i < rooms.getNumberOfEntries(); i++) {
            Room r = rooms.get(i);
            boolean statusMatch = "ALL".equalsIgnoreCase(statusFilter) || r.getRoomStatus().equalsIgnoreCase(statusFilter);
            boolean priceMatch = (maxPrice <= 0) || (r.getPrice() <= maxPrice);

            if (statusMatch && priceMatch) {
                filteredList.add(r);
            }
        }

        // Apply Selection Sort on the ADT
        filteredList.sort(new Comparator<Room>() {
            @Override
            public int compare(Room r1, Room r2) {
                if (sortByPriceAscending) {
                    return Double.compare(r1.getPrice(), r2.getPrice());
                } else {
                    return Double.compare(r2.getPrice(), r1.getPrice());
                }
            }
        });

        return filteredList;
    }

    // Advanced Report 3: Multi-Criteria Filtered and Sorted VIP Guests Report
    public ListInterface<Guest> getFilteredAndSortedGuests(String tierFilter, int minPoints) {
        ListInterface<Guest> filteredList = new MyArrayList<>();
        ListInterface<Guest> guests = guestTree.inOrderTraversal();

        for (int i = 0; i < guests.getNumberOfEntries(); i++) {
            Guest g = guests.get(i);
            boolean tierMatch = "ALL".equalsIgnoreCase(tierFilter) || g.getLoyaltyTier().equalsIgnoreCase(tierFilter);
            boolean pointsMatch = g.getLoyaltyPoints() >= minPoints;

            if (tierMatch && pointsMatch) {
                filteredList.add(g);
            }
        }

        // Sort by loyalty points descending
        filteredList.sort(new Comparator<Guest>() {
            @Override
            public int compare(Guest g1, Guest g2) {
                return Integer.compare(g2.getLoyaltyPoints(), g1.getLoyaltyPoints());
            }
        });

        return filteredList;
    }
}