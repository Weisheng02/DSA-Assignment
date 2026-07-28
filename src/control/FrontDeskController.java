package control;

import adt.MyArrayList;
import adt.ListInterface;
import entity.Guest;
import entity.Room;
import java.util.Comparator;

/**
 * Author: Weisheng
 * Controller Class for Front-Desk Operations
 */
public class FrontDeskController {
    private ListInterface<Guest> guestList;
    private ListInterface<Room> roomList;

    public FrontDeskController() {
        guestList = new MyArrayList<>();
        roomList = new MyArrayList<>();
        seedInitialData();
    }

    // Load initial sample data
    private void seedInitialData() {
        guestList.add(new Guest("Alice Tan", "10000001", "Platinum", 1200));
        guestList.add(new Guest("Bob Lee", "10000002", "Gold", 500));
        guestList.add(new Guest("Charlie Lim", "10000003", "Silver", 200));
        guestList.add(new Guest("David Wong", "10000004", "Standard", 50));
        guestList.add(new Guest("Eva Green", "10000005", "Platinum", 1800));
        guestList.add(new Guest("Frank Wright", "10000006", "Gold", 850));

        roomList.add(new Room("101", "Deluxe Suite", "Ready for Check-In", 350.00));
        roomList.add(new Room("102", "Presidential Suite", "Dirty", 800.00));
        roomList.add(new Room("103", "Standard Room", "Ready for Check-In", 180.00));
        roomList.add(new Room("104", "Deluxe Suite", "Occupied", 350.00));
        roomList.add(new Room("105", "Standard Room", "Cleaning In Progress", 180.00));
        roomList.add(new Room("201", "Presidential Suite", "Ready for Check-In", 950.00));
        roomList.add(new Room("202", "Deluxe Suite", "Ready for Check-In", 400.00));
    }

    // Find guest by 8-digit confirmation number
    public Guest searchGuestByConfirmationNumber(String confirmNo) {
        if (confirmNo == null || confirmNo.trim().isEmpty())
            return null;
        for (int i = 0; i < guestList.getNumberOfEntries(); i++) {
            Guest g = guestList.get(i);
            if (g.getConfirmationNumber().trim().equalsIgnoreCase(confirmNo.trim())) {
                return g;
            }
        }
        return null;
    }

    // Find guests by name (case-insensitive fuzzy search)
    public ListInterface<Guest> searchGuestsByName(String nameQuery) {
        ListInterface<Guest> results = new MyArrayList<>();
        if (nameQuery == null || nameQuery.trim().isEmpty())
            return results;

        String queryLower = nameQuery.trim().toLowerCase();
        for (int i = 0; i < guestList.getNumberOfEntries(); i++) {
            Guest g = guestList.get(i);
            if (g.getGuestName().toLowerCase().contains(queryLower)) {
                results.add(g);
            }
        }
        return results;
    }

    // Find room by room number
    public Room searchRoomByNumber(String roomNumber) {
        if (roomNumber == null || roomNumber.trim().isEmpty())
            return null;
        for (int i = 0; i < roomList.getNumberOfEntries(); i++) {
            Room r = roomList.get(i);
            if (r.getRoomNumber().equalsIgnoreCase(roomNumber.trim())) {
                return r;
            }
        }
        return null;
    }

    // Return list of all rooms
    public ListInterface<Room> getAllRooms() {
        return roomList;
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
        summary[0] = roomList.getNumberOfEntries();

        for (int i = 0; i < roomList.getNumberOfEntries(); i++) {
            String status = roomList.get(i).getRoomStatus();
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

        for (int i = 0; i < roomList.getNumberOfEntries(); i++) {
            Room r = roomList.get(i);
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

        for (int i = 0; i < guestList.getNumberOfEntries(); i++) {
            Guest g = guestList.get(i);
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