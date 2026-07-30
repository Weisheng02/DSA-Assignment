package control;

import adt.ArrayQueue;
import adt.ListInterface;
import adt.MyArrayList;
import adt.QueueInterface;
import entity.Booking;
import entity.Guest;
import entity.Room;
import java.util.Comparator;

/**
 * Author: Weisheng
 * Controller for Walk-In Registrations & Standard Booking.
 * Uses Queue (FIFO) to manage incoming guests.
 */
public class BookingController {

    private QueueInterface<Guest> waitingQueue;
    private ListInterface<Booking> bookingList;
    private ListInterface<Guest> registeredGuests;
    private ListInterface<Room> roomList;
    private int nextConfirmationNumber;
    private int nextBookingId;

    public BookingController() {
        waitingQueue = new ArrayQueue<>();
        bookingList = new MyArrayList<>();
        registeredGuests = new MyArrayList<>();
        roomList = new MyArrayList<>();
        nextConfirmationNumber = 20000001; // Start from 20000001 to avoid clash with FrontDesk data
        nextBookingId = 1;
        seedInitialData();
    }

    // Load initial sample data
    private void seedInitialData() {
        // Seed rooms (same set as other modules for consistency)
        roomList.add(new Room("101", "Deluxe Suite", "Ready for Check-In", 350.00));
        roomList.add(new Room("102", "Presidential Suite", "Dirty", 800.00));
        roomList.add(new Room("103", "Standard Room", "Ready for Check-In", 180.00));
        roomList.add(new Room("104", "Deluxe Suite", "Occupied", 350.00));
        roomList.add(new Room("105", "Standard Room", "Cleaning In Progress", 180.00));
        roomList.add(new Room("201", "Presidential Suite", "Ready for Check-In", 950.00));
        roomList.add(new Room("202", "Deluxe Suite", "Ready for Check-In", 400.00));

        // Seed some walk-in guests already in the waiting queue
        Guest g1 = new Guest("Sarah Chen", "20000001", "Silver", 150);
        Guest g2 = new Guest("James Ong", "20000002", "Standard", 30);
        Guest g3 = new Guest("Linda Tan", "20000003", "Gold", 620);
        waitingQueue.enqueue(g1);
        waitingQueue.enqueue(g2);
        waitingQueue.enqueue(g3);
        registeredGuests.add(g1);
        registeredGuests.add(g2);
        registeredGuests.add(g3);
        nextConfirmationNumber = 20000004;

        // Seed one existing booking (Room 104 is already Occupied)
        bookingList.add(new Booking("BK0001", "10000001", "Alice Tan",
                "104", "Deluxe Suite", 350.00, "2026-07-28", 3));
        nextBookingId = 2;
    }



    /**
     * Register a new walk-in guest and add to the waiting queue.
     * Auto-generates an 8-digit confirmation number.
     * @return The newly registered Guest object.
     */
    public Guest registerWalkInGuest(String guestName, String loyaltyTier) {
        String confirmNo = String.valueOf(nextConfirmationNumber++);
        Guest newGuest = new Guest(guestName, confirmNo, loyaltyTier, 0);
        waitingQueue.enqueue(newGuest);
        registeredGuests.add(newGuest);
        return newGuest;
    }

    /**
     * Returns a list snapshot of all guests currently waiting in the queue,
     * in FIFO order (front to back), WITHOUT modifying the queue.
     */
    public ListInterface<Guest> getWaitingQueueList() {
        return waitingQueue.toList();
    }

    /**
     * Peeks at the next guest to be served (front of queue).
     */
    public Guest peekNextGuest() {
        return waitingQueue.getFront();
    }

    /**
     * Returns the number of guests currently waiting.
     */
    public int getWaitingCount() {
        return waitingQueue.getNumberOfEntries();
    }



    /**
     * Process the next guest in the waiting queue:
     * 1. Dequeue the front guest
     * 2. Validate room is available
     * 3. Create a Booking record
     * 4. Set room status to Occupied
     *
     * @return 1: success, -1: queue empty, -2: room not found, -3: room not ready
     */
    public int processNextGuest(String roomNumber, String checkInDate, int numberOfNights) {
        if (waitingQueue.isEmpty()) return -1;

        Room room = findRoomByNumber(roomNumber);
        if (room == null) return -2;
        if (!"Ready for Check-In".equalsIgnoreCase(room.getRoomStatus())) return -3;

        // Dequeue the front guest
        Guest guest = waitingQueue.dequeue();

        // Create booking record
        String bookingId = String.format("BK%04d", nextBookingId++);
        Booking booking = new Booking(bookingId, guest.getConfirmationNumber(),
                guest.getGuestName(), room.getRoomNumber(), room.getRoomType(),
                room.getPrice(), checkInDate, numberOfNights);
        bookingList.add(booking);

        // Update room status
        room.setRoomStatus("Occupied");

        return 1;
    }

    /**
     * Returns the most recently created booking (for display after processing).
     */
    public Booking getLastBooking() {
        if (bookingList.isEmpty()) return null;
        return bookingList.get(bookingList.getNumberOfEntries() - 1);
    }

    /**
     * Cancel a booking by booking ID. Sets status to "Cancelled" and
     * releases the room back to "Ready for Check-In".
     * @return 1: success, -1: booking not found, -2: already cancelled
     */
    public int cancelBooking(String bookingId) {
        for (int i = 0; i < bookingList.getNumberOfEntries(); i++) {
            Booking b = bookingList.get(i);
            if (b.getBookingId().equalsIgnoreCase(bookingId.trim())) {
                if ("Cancelled".equalsIgnoreCase(b.getBookingStatus())) {
                    return -2;
                }
                b.setBookingStatus("Cancelled");
                // Release the room
                Room room = findRoomByNumber(b.getRoomNumber());
                if (room != null) {
                    room.setRoomStatus("Ready for Check-In");
                }
                return 1;
            }
        }
        return -1;
    }

    /**
     * Returns all booking records.
     */
    public ListInterface<Booking> getAllBookings() {
        return bookingList;
    }



    /**
     * Find a room by room number (linear search).
     */
    public Room findRoomByNumber(String roomNumber) {
        if (roomNumber == null) return null;
        for (int i = 0; i < roomList.getNumberOfEntries(); i++) {
            Room r = roomList.get(i);
            if (r.getRoomNumber().equalsIgnoreCase(roomNumber.trim())) {
                return r;
            }
        }
        return null;
    }

    /**
     * Returns all rooms that are "Ready for Check-In".
     */
    public ListInterface<Room> getAvailableRooms() {
        ListInterface<Room> available = new MyArrayList<>();
        for (int i = 0; i < roomList.getNumberOfEntries(); i++) {
            Room r = roomList.get(i);
            if ("Ready for Check-In".equalsIgnoreCase(r.getRoomStatus())) {
                available.add(r);
            }
        }
        return available;
    }

    /**
     * Returns all rooms.
     */
    public ListInterface<Room> getAllRooms() {
        return roomList;
    }



    /**
     * Report 1: Multi-Criteria Filtered & Sorted Booking Summary Report.
     * Filters by room type and minimum nights, then sorts by total price.
     *
     * @param roomTypeFilter Room type filter ("ALL" for no filter)
     * @param minNights Minimum number of nights (0 for no filter)
     * @param sortByPriceAscending true for ascending, false for descending
     * @return Filtered and sorted list of bookings
     */
    public ListInterface<Booking> getFilteredAndSortedBookings(
            String roomTypeFilter, int minNights, boolean sortByPriceAscending) {

        ListInterface<Booking> filtered = new MyArrayList<>();

        for (int i = 0; i < bookingList.getNumberOfEntries(); i++) {
            Booking b = bookingList.get(i);

            // Search/filter by room type
            boolean typeMatch = "ALL".equalsIgnoreCase(roomTypeFilter)
                    || b.getRoomType().equalsIgnoreCase(roomTypeFilter);

            // Search/filter by minimum nights
            boolean nightsMatch = (minNights <= 0) || (b.getNumberOfNights() >= minNights);

            if (typeMatch && nightsMatch) {
                filtered.add(b);
            }
        }

        // Apply Selection Sort on the ADT (using MyArrayList.sort)
        filtered.sort(new Comparator<Booking>() {
            @Override
            public int compare(Booking b1, Booking b2) {
                if (sortByPriceAscending) {
                    return Double.compare(b1.getTotalPrice(), b2.getTotalPrice());
                } else {
                    return Double.compare(b2.getTotalPrice(), b1.getTotalPrice());
                }
            }
        });

        return filtered;
    }

    /**
     * Report 2: Guest Registration & Tier Analysis Report.
     * Filters all registered guests by loyalty tier and registration status
     * (whether they are still waiting in queue or already checked in).
     * Sorts by confirmation number.
     *
     * @param tierFilter Loyalty tier filter ("ALL" for no filter)
     * @param statusFilter "Waiting", "Checked-In", or "ALL"
     * @param sortAscending true for ascending confirmation number, false for descending
     * @return Filtered and sorted list of guests
     */
    public ListInterface<Guest> getFilteredAndSortedGuests(
            String tierFilter, String statusFilter, boolean sortAscending) {

        ListInterface<Guest> filtered = new MyArrayList<>();

        // Build a list of confirmation numbers currently in the queue
        ListInterface<Guest> queueSnapshot = waitingQueue.toList();

        for (int i = 0; i < registeredGuests.getNumberOfEntries(); i++) {
            Guest g = registeredGuests.get(i);

            // Search/filter by tier
            boolean tierMatch = "ALL".equalsIgnoreCase(tierFilter)
                    || g.getLoyaltyTier().equalsIgnoreCase(tierFilter);

            // Determine if guest is still waiting in queue
            boolean isWaiting = isGuestInQueue(g.getConfirmationNumber(), queueSnapshot);

            // Search/filter by status
            boolean statusMatch;
            if ("ALL".equalsIgnoreCase(statusFilter)) {
                statusMatch = true;
            } else if ("Waiting".equalsIgnoreCase(statusFilter)) {
                statusMatch = isWaiting;
            } else { // "Checked-In"
                statusMatch = !isWaiting;
            }

            if (tierMatch && statusMatch) {
                filtered.add(g);
            }
        }

        // Apply Selection Sort on the ADT
        filtered.sort(new Comparator<Guest>() {
            @Override
            public int compare(Guest g1, Guest g2) {
                if (sortAscending) {
                    return g1.getConfirmationNumber().compareTo(g2.getConfirmationNumber());
                } else {
                    return g2.getConfirmationNumber().compareTo(g1.getConfirmationNumber());
                }
            }
        });

        return filtered;
    }

    /**
     * Checks if a guest's confirmation number exists in the queue snapshot.
     */
    private boolean isGuestInQueue(String confirmNo, ListInterface<Guest> queueSnapshot) {
        for (int i = 0; i < queueSnapshot.getNumberOfEntries(); i++) {
            if (queueSnapshot.get(i).getConfirmationNumber().equals(confirmNo)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns summary statistics for the registration & tier report.
     * Index: [0]=Total registered, [1]=Waiting, [2]=Checked-In,
     *        [3]=Platinum, [4]=Gold, [5]=Silver, [6]=Standard
     */
    public int[] getRegistrationSummary() {
        int[] summary = new int[7];
        summary[0] = registeredGuests.getNumberOfEntries();
        summary[1] = waitingQueue.getNumberOfEntries();
        summary[2] = summary[0] - summary[1];

        ListInterface<Guest> queueSnapshot = waitingQueue.toList();
        for (int i = 0; i < registeredGuests.getNumberOfEntries(); i++) {
            Guest g = registeredGuests.get(i);
            String tier = g.getLoyaltyTier();
            if ("Platinum".equalsIgnoreCase(tier)) summary[3]++;
            else if ("Gold".equalsIgnoreCase(tier)) summary[4]++;
            else if ("Silver".equalsIgnoreCase(tier)) summary[5]++;
            else summary[6]++;
        }
        return summary;
    }
}
