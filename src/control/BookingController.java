package control;

import adt.ArrayQueue;
import adt.BSTInterface;
import adt.ListInterface;
import adt.MyArrayList;
import adt.QueueInterface;
import entity.Booking;
import entity.Guest;
import entity.Room;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Comparator;

/**
 * Author: Zhi Xuan
 * Controller for Walk-In Registrations & Standard Booking.
 * Uses Queue (FIFO) to manage incoming guests.
 */
public class BookingController {

    public static final int MAX_STAY_NIGHTS = 30;
    public static final int MAX_ADVANCE_BOOKING_DAYS = 365;

    private QueueInterface<Guest> waitingQueue;
    private ListInterface<Booking> bookingList;
    private ListInterface<Guest> registeredGuests;
    private ListInterface<Room> roomList;
    private BSTInterface<Guest> masterGuestRegistry;
    private int nextConfirmationNumber;
    private int nextBookingId;

    public BookingController(ListInterface<Room> sharedRoomList, BSTInterface<Guest> masterGuestRegistry,
            ListInterface<Booking> sharedBookingList) {
        waitingQueue = new ArrayQueue<>();
        bookingList = (sharedBookingList != null) ? sharedBookingList : new MyArrayList<>();
        registeredGuests = new MyArrayList<>();
        roomList = (sharedRoomList != null) ? sharedRoomList : new MyArrayList<>();
        this.masterGuestRegistry = masterGuestRegistry;
        nextConfirmationNumber = 20000001;
        nextBookingId = 1;
        seedInitialQueueGuests();
        initializeNextIdentifiers();
    }

    private void seedInitialQueueGuests() {
        if (!waitingQueue.isEmpty())
            return;

        Guest g1 = new Guest("Sarah Chen", "010512-08-1234", "+60 12-701 1234", "Female",
                "Malaysian", "sarah.chen@example.com", "20000001", "Silver", 220);
        Guest g2 = new Guest("James Ong", "020715-14-5678", "+60 11-702 5678", "Male",
                "Malaysian", "james.ong@example.com", "20000002", "Standard", 30);
        Guest g3 = new Guest("Linda Tan", "990328-10-9012", "+60 16-703 9012", "Female",
                "Malaysian", "linda.tan@example.com", "20000003", "Gold", 620);
        g1.setBookingStatus("Waiting");
        g2.setBookingStatus("Waiting");
        g3.setBookingStatus("Waiting");
        g1.setSpecialRequest("Baby cot");
        g2.setSpecialRequest("None");
        g3.setSpecialRequest("Vegetarian breakfast");

        waitingQueue.enqueue(g1);
        waitingQueue.enqueue(g2);
        waitingQueue.enqueue(g3);

        registeredGuests.add(g1);
        registeredGuests.add(g2);
        registeredGuests.add(g3);

        if (masterGuestRegistry != null) {
            masterGuestRegistry.add(g1);
            masterGuestRegistry.add(g2);
            masterGuestRegistry.add(g3);
            masterGuestRegistry.rebalance();
        }

        // App owns the shared master guests/bookings. This method only provides
        // FIFO queue examples and does not duplicate any master booking.
    }

    /**
     * Starts generated identifiers after all records already present in shared
     * memory.
     */
    private void initializeNextIdentifiers() {
        int highestConfirmation = nextConfirmationNumber - 1;
        ListInterface<Guest> guests = getRegisteredGuestsSnapshot();
        for (int i = 0; i < guests.getNumberOfEntries(); i++) {
            String confirmation = guests.get(i).getConfirmationNumber();
            if (confirmation != null && confirmation.matches("\\d{8}")) {
                highestConfirmation = Math.max(highestConfirmation, Integer.parseInt(confirmation));
            }
        }
        nextConfirmationNumber = highestConfirmation + 1;

        int highestBookingId = 0;
        for (int i = 0; i < bookingList.getNumberOfEntries(); i++) {
            Booking booking = bookingList.get(i);
            if (booking != null && booking.getBookingId() != null
                    && booking.getBookingId().matches("(?i)BK\\d+")) {
                highestBookingId = Math.max(highestBookingId,
                        Integer.parseInt(booking.getBookingId().substring(2)));
            }
        }
        nextBookingId = highestBookingId + 1;
    }

    /**
     * Uses the shared registry as the source of truth when modules are integrated.
     */
    private ListInterface<Guest> getRegisteredGuestsSnapshot() {
        return masterGuestRegistry != null ? masterGuestRegistry.inOrderTraversal() : registeredGuests;
    }

    public Guest findGuestByIC(String icNo) {
        return ControllerDataSupport.findGuestByIc(getRegisteredGuestsSnapshot(), icNo);
    }

    public Guest registerWalkInGuest(String guestName, String loyaltyTier) {
        return registerWalkInGuest(guestName, "N/A", loyaltyTier, 0);
    }

    public Guest registerWalkInGuest(String guestName, String icNo, String loyaltyTier, int loyaltyPoints) {
        Guest existingMember = findGuestByIC(icNo);
        if (existingMember != null) {
            loyaltyTier = existingMember.getLoyaltyTier();
            loyaltyPoints = existingMember.getLoyaltyPoints();
        }
        String confirmNo = String.valueOf(nextConfirmationNumber++);
        while (isConfirmationNumberUsed(confirmNo)) {
            confirmNo = String.valueOf(nextConfirmationNumber++);
        }
        Guest newGuest = new Guest(guestName, icNo, confirmNo, loyaltyTier, loyaltyPoints);
        if (existingMember != null)
            newGuest.setLoyaltyExperience(existingMember.getLoyaltyExperience());
        newGuest.setBookingStatus("Waiting");
        waitingQueue.enqueue(newGuest);
        registeredGuests.add(newGuest);
        // Sync to Master Guest Registry so FrontDesk & Loyalty can see this guest
        if (masterGuestRegistry != null) {
            masterGuestRegistry.add(newGuest);
            if (!masterGuestRegistry.isBalanced())
                masterGuestRegistry.rebalance();
        }
        return newGuest;
    }

    private boolean isConfirmationNumberUsed(String confirmationNumber) {
        ListInterface<Guest> guests = getRegisteredGuestsSnapshot();
        for (int i = 0; i < guests.getNumberOfEntries(); i++) {
            if (confirmationNumber.equalsIgnoreCase(guests.get(i).getConfirmationNumber()))
                return true;
        }
        return false;
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
     * 4. Set room status to Reserved for Front Desk check-in
     *
     * @return 1: success, -1: queue empty, -2: room not found, -3: room not
     *         ready, -4: invalid date, -5: invalid nights, -6: past date,
     *         -7: too far in advance
     */
    public int processNextGuest(String roomNumber, String checkInDate, int numberOfNights) {
        return processNextGuest(roomNumber, checkInDate, numberOfNights, "None");
    }

    /**
     * Creates a booking for the next FIFO guest, including an optional stay
     * request.
     */
    public int processNextGuest(String roomNumber, String checkInDate, int numberOfNights, String specialRequest) {
        if (waitingQueue.isEmpty())
            return -1;

        int dateValidation = validateStayPeriod(checkInDate, numberOfNights);
        if (dateValidation != 1)
            return dateValidation;

        Room room = findRoomByNumber(roomNumber);
        if (room == null)
            return -2;
        if (!isRoomAvailableForStay(room, checkInDate, numberOfNights, null))
            return -3;

        // Dequeue the front guest
        Guest guest = waitingQueue.dequeue();

        // Create booking record
        String bookingId = String.format("BK%04d", nextBookingId++);
        while (isBookingIdUsed(bookingId)) {
            bookingId = String.format("BK%04d", nextBookingId++);
        }
        Booking booking = new Booking(bookingId, guest.getConfirmationNumber(),
                guest.getGuestName(), room.getRoomNumber(), room.getRoomType(),
                room.getPrice(), checkInDate, numberOfNights);
        booking.setSpecialRequest(specialRequest);
        bookingList.add(booking);

        // Future reservations live in the Booking schedule and must not overwrite
        // today's physical room condition. Only today's ready room becomes Reserved.
        if (LocalDate.parse(checkInDate).equals(LocalDate.now())
                && "Ready for Check-In".equalsIgnoreCase(room.getRoomStatus())) {
            room.setRoomStatus("Reserved");
        }

        // Sync with Guest record so FrontDesk System recognizes the reservation
        guest.setBookingStatus("Reserved");
        guest.setAssignedRoomNumber(room.getRoomNumber());
        guest.setRoomType(room.getRoomType());
        guest.setEffectiveRoomRate(room.getPrice());
        guest.setCheckInDate(checkInDate);
        guest.setNumberOfNights(numberOfNights);
        guest.setSpecialRequest(booking.getSpecialRequest());

        return 1;
    }

    private boolean isBookingIdUsed(String bookingId) {
        for (int i = 0; i < bookingList.getNumberOfEntries(); i++) {
            Booking booking = bookingList.get(i);
            if (booking != null && bookingId.equalsIgnoreCase(booking.getBookingId()))
                return true;
        }
        return false;
    }

    /**
     * Finds a booking by its human-readable booking ID. This is the primary
     * lookup used by search, update, and cancellation operations.
     */
    public Booking findBookingById(String bookingId) {
        refreshExpiredBookings();
        return ControllerDataSupport.findBookingById(bookingList, bookingId);
    }

    /** Finds the latest booking belonging to a stay confirmation number. */
    public Booking findBookingByConfirmation(String confirmationNumber) {
        refreshExpiredBookings();
        return ControllerDataSupport.findLatestBookingByConfirmation(bookingList, confirmationNumber);
    }

    /**
     * Updates a not-yet-checked-in booking and synchronizes the matching Guest
     * record and shared Room list. Return values: 1 success; -1 booking missing;
     * -2 booking no longer editable; -3 room missing; -4 invalid date;
     * -5 invalid nights; -6 past date; -7 too far in advance; -8 selected room
     * is not available for the requested date range.
     */
    public int updateBooking(String bookingId, String roomNumber, String checkInDate,
            int numberOfNights, String specialRequest) {
        Booking booking = findBookingById(bookingId);
        if (booking == null)
            return -1;
        if (!"Confirmed".equalsIgnoreCase(booking.getBookingStatus()))
            return -2;
        int dateValidation = validateStayPeriod(checkInDate, numberOfNights);
        if (dateValidation != 1)
            return dateValidation;

        Room newRoom = findRoomByNumber(roomNumber);
        if (newRoom == null)
            return -3;
        if (!isRoomAvailableForStay(newRoom, checkInDate, numberOfNights, booking))
            return -8;

        String oldRoomNumber = booking.getRoomNumber();
        booking.setRoomNumber(newRoom.getRoomNumber());
        booking.setRoomType(newRoom.getRoomType());
        booking.setRoomPrice(newRoom.getPrice());
        booking.setCheckInDate(checkInDate.trim());
        booking.setNumberOfNights(numberOfNights);
        booking.setSpecialRequest(specialRequest);
        if (LocalDate.parse(checkInDate).equals(LocalDate.now())
                && "Ready for Check-In".equalsIgnoreCase(newRoom.getRoomStatus())) {
            newRoom.setRoomStatus("Reserved");
        }

        Guest guest = findGuestByConfirmation(booking.getGuestConfirmationNumber());
        if (guest != null) {
            guest.setAssignedRoomNumber(newRoom.getRoomNumber());
            guest.setRoomType(newRoom.getRoomType());
            guest.setEffectiveRoomRate(newRoom.getPrice());
            guest.setCheckInDate(checkInDate.trim());
            guest.setNumberOfNights(numberOfNights);
            guest.setSpecialRequest(booking.getSpecialRequest());
        }

        releaseRoomIfUnreserved(oldRoomNumber);
        return 1;
    }

    /**
     * Cancels a guest who is still waiting, preserving all other FIFO positions.
     */
    public int cancelWaitingRegistration(String confirmationNumber) {
        if (confirmationNumber == null || confirmationNumber.trim().isEmpty())
            return -1;
        boolean removed = false;
        int entries = waitingQueue.getNumberOfEntries();
        for (int i = 0; i < entries; i++) {
            Guest guest = waitingQueue.dequeue();
            if (!removed && confirmationNumber.trim().equalsIgnoreCase(guest.getConfirmationNumber())) {
                removed = true;
                guest.setBookingStatus("Cancelled");
                guest.setSpecialRequest("Cancelled before room assignment");
            } else {
                waitingQueue.enqueue(guest);
            }
        }
        return removed ? 1 : -1;
    }

    /**
     * Validates a realistic stay period. Return: 1 valid, -4 malformed date,
     * -5 nights outside 1..30, -6 check-in is in the past, -7 more than one
     * year in advance.
     */
    public int validateStayPeriod(String checkInDate, int numberOfNights) {
        ControllerDataSupport.StayValidation result = ControllerDataSupport.validateStayPeriod(
                checkInDate, numberOfNights, MAX_STAY_NIGHTS, MAX_ADVANCE_BOOKING_DAYS);
        switch (result) {
            case INVALID_DATE:
                return -4;
            case INVALID_NIGHTS:
                return -5;
            case PAST_DATE:
                return -6;
            case TOO_FAR_IN_ADVANCE:
                return -7;
            default:
                return 1;
        }
    }

    /**
     * Returns the most recently created booking (for display after processing).
     */
    public Booking getLastBooking() {
        if (bookingList.isEmpty())
            return null;
        return bookingList.get(bookingList.getNumberOfEntries() - 1);
    }

    /**
     * Cancel a booking by booking ID. Sets status to "Cancelled" and
     * releases the room back to "Ready for Check-In".
     * 
     * @return 1: success, -1: booking not found, -2: already cancelled
     */
    public int cancelBooking(String bookingId) {
        return cancelBooking(bookingId, "No reason recorded", "Booking Staff");
    }

    /** Cancels a booking with an audit reason and staff identifier. */
    public int cancelBooking(String bookingId, String reason, String staffName) {
        Booking b = findBookingById(bookingId);
        if (b == null)
            return -1;
        if ("Cancelled".equalsIgnoreCase(b.getBookingStatus()))
            return -2;
        if ("NoShow".equalsIgnoreCase(b.getBookingStatus()))
            return -5;

        // Reset Guest state in either the shared master registry or the local
        // standalone registry. Cancellation must not leave the Guest marked
        // Reserved merely because the controller has no master registry.
        Guest guest = findGuestByConfirmation(b.getGuestConfirmationNumber());
        if (guest != null) {
            if (guest.isCheckedIn()) {
                return -3; // Cannot cancel: Guest is currently checked in
            }
            if ("CheckedOut".equalsIgnoreCase(guest.getBookingStatus())) {
                return -4; // Cannot cancel: Guest has already checked out
            }
            guest.setBookingStatus("Cancelled");
            guest.setAssignedRoomNumber(null);
            guest.setRoomType(null);
            guest.setEffectiveRoomRate(0.0);
        }

        b.recordCancellation(reason, staffName);
        releaseRoomIfUnreserved(b.getRoomNumber());
        return 1;
    }

    /**
     * Returns all booking records.
     */
    public ListInterface<Booking> getAllBookings() {
        refreshExpiredBookings();
        return bookingList;
    }

    /**
     * Find a room by room number (linear search).
     */
    public Room findRoomByNumber(String roomNumber) {
        return ControllerDataSupport.findRoomByNumber(roomList, roomNumber);
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
     * Returns rooms that are usable and have no active booking overlap for the
     * requested stay.
     */
    public ListInterface<Room> getAvailableRooms(String checkInDate, int numberOfNights) {
        ListInterface<Room> available = new MyArrayList<>();
        if (validateStayPeriod(checkInDate, numberOfNights) != 1)
            return available;
        refreshExpiredBookings();
        for (int i = 0; i < roomList.getNumberOfEntries(); i++) {
            Room room = roomList.get(i);
            if (isRoomAvailableForStay(room, checkInDate, numberOfNights, null))
                available.add(room);
        }
        return available;
    }

    /** Availability query that excludes the booking currently being edited. */
    public ListInterface<Room> getAvailableRoomsForUpdate(String bookingId, String checkInDate, int numberOfNights) {
        ListInterface<Room> available = new MyArrayList<>();
        if (validateStayPeriod(checkInDate, numberOfNights) != 1)
            return available;
        Booking excluded = findBookingById(bookingId);
        for (int i = 0; i < roomList.getNumberOfEntries(); i++) {
            Room room = roomList.get(i);
            if (isRoomAvailableForStay(room, checkInDate, numberOfNights, excluded))
                available.add(room);
        }
        return available;
    }

    private Guest findGuestByConfirmation(String confirmationNumber) {
        if (masterGuestRegistry != null) {
            Guest guest = ControllerDataSupport.findGuestByConfirmation(
                    masterGuestRegistry, confirmationNumber);
            if (guest != null)
                return guest;
        }
        return ControllerDataSupport.findGuestByConfirmation(registeredGuests, confirmationNumber);
    }

    private boolean isRoomAvailableForStay(Room room, String checkInDate, int numberOfNights, Booking excludedBooking) {
        LocalDate requestedStart = LocalDate.parse(checkInDate.trim());
        LocalDate requestedEnd = requestedStart.plusDays(numberOfNights);
        return ControllerDataSupport.isRoomAvailableForStay(room, requestedStart, requestedEnd,
                bookingList, getRegisteredGuestsSnapshot(), excludedBooking);
    }

    private boolean isActiveReservation(Booking booking) {
        return ControllerDataSupport.isActiveReservation(booking);
    }

    /**
     * Marks an unconsumed reservation NoShow on the day after scheduled arrival.
     */
    public int refreshExpiredBookings() {
        int updated = 0;
        LocalDate today = LocalDate.now();
        for (int i = 0; i < bookingList.getNumberOfEntries(); i++) {
            Booking booking = bookingList.get(i);
            if (booking == null || !"Confirmed".equalsIgnoreCase(booking.getBookingStatus()))
                continue;
            try {
                if (today.isAfter(LocalDate.parse(booking.getCheckInDate()))) {
                    booking.recordNoShow();
                    Guest guest = findGuestByConfirmation(booking.getGuestConfirmationNumber());
                    if (guest != null)
                        guest.setBookingStatus("NoShow");
                    updated++;
                }
            } catch (DateTimeParseException | NullPointerException ignored) {
                // Keep an invalid active record unchanged. Availability checks fail
                // closed, so the affected room cannot accidentally be double-sold.
            }
        }
        if (updated > 0)
            refreshReservedRoomStatuses();
        return updated;
    }

    private void refreshReservedRoomStatuses() {
        for (int i = 0; i < roomList.getNumberOfEntries(); i++) {
            Room room = roomList.get(i);
            if ("Reserved".equalsIgnoreCase(room.getRoomStatus()) && !hasArrivalToday(room.getRoomNumber())) {
                room.setRoomStatus("Ready for Check-In");
            }
        }
    }

    private boolean hasArrivalToday(String roomNumber) {
        LocalDate today = LocalDate.now();
        for (int i = 0; i < bookingList.getNumberOfEntries(); i++) {
            Booking booking = bookingList.get(i);
            if (booking == null || !roomNumber.equalsIgnoreCase(booking.getRoomNumber())
                    || !isActiveReservation(booking))
                continue;
            try {
                if (today.equals(LocalDate.parse(booking.getCheckInDate())))
                    return true;
            } catch (DateTimeParseException | NullPointerException ignored) {
                return true; // Do not release a room referenced by corrupt active data.
            }
        }
        return false;
    }

    private void releaseRoomIfUnreserved(String roomNumber) {
        Room room = findRoomByNumber(roomNumber);
        if (room == null || !"Reserved".equalsIgnoreCase(room.getRoomStatus()))
            return;
        for (int i = 0; i < bookingList.getNumberOfEntries(); i++) {
            Booking booking = bookingList.get(i);
            if (booking != null && roomNumber.equalsIgnoreCase(booking.getRoomNumber())
                    && isActiveReservation(booking)) {
                try {
                    if (LocalDate.now().equals(LocalDate.parse(booking.getCheckInDate())))
                        return;
                } catch (DateTimeParseException | NullPointerException ignored) {
                    return; // Fail closed: never release a room tied to corrupt active data.
                }
            }
        }
        room.setRoomStatus("Ready for Check-In");
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
     * @param roomTypeFilter       Room type filter ("ALL" for no filter)
     * @param minNights            Minimum number of nights (0 for no filter)
     * @param sortByPriceAscending true for ascending, false for descending
     * @return Filtered and sorted list of bookings
     */
    public ListInterface<Booking> getFilteredAndSortedBookings(
            String roomTypeFilter, int minNights, boolean sortByPriceAscending) {
        return getFilteredAndSortedBookings(roomTypeFilter, "ALL", "", "", minNights, sortByPriceAscending);
    }

    /**
     * Management report query: filters by room type, operational status,
     * check-in date range, and minimum nights before applying the ADT sort.
     */
    public ListInterface<Booking> getFilteredAndSortedBookings(String roomTypeFilter, String statusFilter,
            String startDate, String endDate, int minNights, boolean sortByPriceAscending) {

        refreshExpiredBookings();
        ListInterface<Booking> filtered = new MyArrayList<>();
        LocalDate start = parseOptionalDate(startDate);
        LocalDate end = parseOptionalDate(endDate);

        for (int i = 0; i < bookingList.getNumberOfEntries(); i++) {
            Booking b = bookingList.get(i);

            // Search/filter by room type
            boolean typeMatch = "ALL".equalsIgnoreCase(roomTypeFilter)
                    || (b.getRoomType() != null && b.getRoomType().equalsIgnoreCase(roomTypeFilter));

            // Search/filter by minimum nights
            boolean nightsMatch = (minNights <= 0) || (b.getNumberOfNights() >= minNights);
            boolean statusMatch = "ALL".equalsIgnoreCase(statusFilter)
                    || b.getBookingStatus().equalsIgnoreCase(statusFilter);
            LocalDate bookingDate = LocalDate.parse(b.getCheckInDate());
            boolean dateMatch = (start == null || !bookingDate.isBefore(start))
                    && (end == null || !bookingDate.isAfter(end));

            if (typeMatch && nightsMatch && statusMatch && dateMatch) {
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
     * Report 1 management metrics for the supplied results.
     * Index: [0]=active booking count, [1]=cancelled count, [2]=total nights,
     * [3]=active booking value, [4]=average active-stay nights, [5]=no-shows.
     */
    public double[] getBookingMetrics(ListInterface<Booking> bookings) {
        double[] metrics = new double[6];
        for (int i = 0; i < bookings.getNumberOfEntries(); i++) {
            Booking booking = bookings.get(i);
            if ("Cancelled".equalsIgnoreCase(booking.getBookingStatus())) {
                metrics[1]++;
            } else if ("NoShow".equalsIgnoreCase(booking.getBookingStatus())) {
                metrics[5]++;
            } else {
                metrics[0]++;
                metrics[2] += booking.getNumberOfNights();
                metrics[3] += booking.getTotalPrice();
            }
        }
        metrics[4] = metrics[0] == 0 ? 0 : metrics[2] / metrics[0];
        return metrics;
    }

    /**
     * Report 2: Guest Registration & Tier Analysis Report.
     * Filters all registered guests by loyalty tier and registration status
     * (whether they are still waiting in queue or already processed/assigned).
     * Sorts by confirmation number.
     *
     * @param tierFilter    Loyalty tier filter ("ALL" for no filter)
     * @param statusFilter  lifecycle status such as "Waiting", "Confirmed", or
     *                      "ALL"
     * @param sortAscending true for ascending confirmation number, false for
     *                      descending
     * @return Filtered and sorted list of guests
     */
    public ListInterface<Guest> getFilteredAndSortedGuests(
            String tierFilter, String statusFilter, boolean sortAscending) {

        // Expiry changes the operational status used by this report. Refresh
        // before applying the status filter so Confirmed cannot include a
        // booking that is already a NoShow.
        refreshExpiredBookings();
        ListInterface<Guest> filtered = new MyArrayList<>();

        // Build a list of confirmation numbers currently in the queue
        ListInterface<Guest> queueSnapshot = waitingQueue.toList();

        ListInterface<Guest> allRegisteredGuests = getRegisteredGuestsSnapshot();
        for (int i = 0; i < allRegisteredGuests.getNumberOfEntries(); i++) {
            Guest g = allRegisteredGuests.get(i);

            // Search/filter by tier
            boolean tierMatch = "ALL".equalsIgnoreCase(tierFilter)
                    || (g.getLoyaltyTier() != null && g.getLoyaltyTier().equalsIgnoreCase(tierFilter));

            String operationalStatus = getGuestOperationalStatus(g, queueSnapshot);
            boolean statusMatch = "ALL".equalsIgnoreCase(statusFilter)
                    || operationalStatus.equalsIgnoreCase(statusFilter);

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

    /** Returns the queue-aware lifecycle status used consistently in Report 2. */
    public String getGuestOperationalStatus(Guest guest) {
        return getGuestOperationalStatus(guest, waitingQueue.toList());
    }

    private String getGuestOperationalStatus(Guest guest, ListInterface<Guest> queueSnapshot) {
        if (guest != null && isGuestInQueue(guest.getConfirmationNumber(), queueSnapshot))
            return "Waiting";
        if (guest == null || guest.getBookingStatus() == null)
            return "Unknown";
        return "Reserved".equalsIgnoreCase(guest.getBookingStatus()) ? "Confirmed" : guest.getBookingStatus();
    }

    private LocalDate parseOptionalDate(String value) {
        if (value == null || value.trim().isEmpty() || "ALL".equalsIgnoreCase(value.trim()))
            return null;
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
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
     * Index: [0]=Total registered, [1]=Waiting, [2]=Confirmed, [3]=CheckedIn,
     * [4]=CheckedOut, [5]=Cancelled, [6]=NoShow, [7]=Platinum, [8]=Gold,
     * [9]=Silver, [10]=Standard.
     */
    public int[] getRegistrationSummary() {
        refreshExpiredBookings();
        int[] summary = new int[11];
        ListInterface<Guest> allRegisteredGuests = getRegisteredGuestsSnapshot();
        summary[0] = allRegisteredGuests.getNumberOfEntries();
        ListInterface<Guest> queueSnapshot = waitingQueue.toList();

        for (int i = 0; i < allRegisteredGuests.getNumberOfEntries(); i++) {
            Guest g = allRegisteredGuests.get(i);
            String status = getGuestOperationalStatus(g, queueSnapshot);
            if ("Waiting".equalsIgnoreCase(status))
                summary[1]++;
            else if ("Confirmed".equalsIgnoreCase(status) || "Reserved".equalsIgnoreCase(status))
                summary[2]++;
            else if ("CheckedIn".equalsIgnoreCase(status))
                summary[3]++;
            else if ("CheckedOut".equalsIgnoreCase(status))
                summary[4]++;
            else if ("Cancelled".equalsIgnoreCase(status))
                summary[5]++;
            else if ("NoShow".equalsIgnoreCase(status))
                summary[6]++;
            String tier = g.getLoyaltyTier();
            if ("Platinum".equalsIgnoreCase(tier))
                summary[7]++;
            else if ("Gold".equalsIgnoreCase(tier))
                summary[8]++;
            else if ("Silver".equalsIgnoreCase(tier))
                summary[9]++;
            else
                summary[10]++;
        }
        return summary;
    }

    public boolean isBookingEditable(String bookingId) {
        Booking booking = findBookingById(bookingId);
        return booking != null && "Confirmed".equalsIgnoreCase(booking.getBookingStatus());
    }
}
