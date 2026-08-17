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

    /** Immutable boundary-safe projection of a Guest record. */
    public static final class GuestView {
        private final String guestName, icNo, confirmationNumber, loyaltyTier, bookingStatus;
        private final int loyaltyPoints;

        private GuestView(Guest guest, String operationalStatus) {
            guestName = guest == null ? "" : guest.getGuestName();
            icNo = guest == null ? "" : guest.getIcNo();
            confirmationNumber = guest == null ? "" : guest.getConfirmationNumber();
            loyaltyTier = guest == null ? "" : guest.getLoyaltyTier();
            loyaltyPoints = guest == null ? 0 : guest.getLoyaltyPoints();
            bookingStatus = operationalStatus == null ? "Unknown" : operationalStatus;
        }

        public String getGuestName() { return guestName; }
        public String getIcNo() { return icNo; }
        public String getConfirmationNumber() { return confirmationNumber; }
        public String getLoyaltyTier() { return loyaltyTier; }
        public int getLoyaltyPoints() { return loyaltyPoints; }
        public String getBookingStatus() { return bookingStatus; }
    }

    /** Immutable boundary-safe projection of a Booking record. */
    public static final class BookingView {
        private final String bookingId, guestName, guestConfirmationNumber, roomNumber, roomType,
                checkInDate, checkOutDate, bookingStatus, specialRequest, bookingCreatedDate,
                actualCheckInDate, actualCheckOutDate, noShowDate, cancellationDate,
                cancelledBy, cancellationReason;
        private final int numberOfNights, actualNightsStayed;
        private final double roomPrice, totalPrice;

        private BookingView(Booking booking) {
            bookingId = booking.getBookingId();
            guestName = booking.getGuestName();
            guestConfirmationNumber = booking.getGuestConfirmationNumber();
            roomNumber = booking.getRoomNumber();
            roomType = booking.getRoomType();
            checkInDate = booking.getCheckInDate();
            checkOutDate = booking.getCheckOutDate();
            numberOfNights = booking.getNumberOfNights();
            roomPrice = booking.getRoomPrice();
            totalPrice = booking.getTotalPrice();
            bookingStatus = booking.getBookingStatus();
            specialRequest = booking.getSpecialRequest();
            bookingCreatedDate = booking.getBookingCreatedDate();
            actualCheckInDate = booking.getActualCheckInDate();
            actualCheckOutDate = booking.getActualCheckOutDate();
            actualNightsStayed = booking.getActualNightsStayed();
            noShowDate = booking.getNoShowDate();
            cancellationDate = booking.getCancellationDate();
            cancelledBy = booking.getCancelledBy();
            cancellationReason = booking.getCancellationReason();
        }

        public String getBookingId() { return bookingId; }
        public String getGuestName() { return guestName; }
        public String getGuestConfirmationNumber() { return guestConfirmationNumber; }
        public String getRoomNumber() { return roomNumber; }
        public String getRoomType() { return roomType; }
        public String getCheckInDate() { return checkInDate; }
        public String getCheckOutDate() { return checkOutDate; }
        public int getNumberOfNights() { return numberOfNights; }
        public double getRoomPrice() { return roomPrice; }
        public double getTotalPrice() { return totalPrice; }
        public String getBookingStatus() { return bookingStatus; }
        public String getSpecialRequest() { return specialRequest; }
        public String getBookingCreatedDate() { return bookingCreatedDate; }
        public String getActualCheckInDate() { return actualCheckInDate; }
        public String getActualCheckOutDate() { return actualCheckOutDate; }
        public int getActualNightsStayed() { return actualNightsStayed; }
        public String getNoShowDate() { return noShowDate; }
        public String getCancellationDate() { return cancellationDate; }
        public String getCancelledBy() { return cancelledBy; }
        public String getCancellationReason() { return cancellationReason; }
    }

    /** Immutable boundary-safe projection of a Room record. */
    public static final class RoomView {
        private final String roomNumber, roomType;
        private final double price;

        private RoomView(Room room) {
            roomNumber = room.getRoomNumber();
            roomType = room.getRoomType();
            price = room.getPrice();
        }

        public String getRoomNumber() { return roomNumber; }
        public String getRoomType() { return roomType; }
        public double getPrice() { return price; }
    }

    public BookingController() {
        this(null, null, null, null);
    }

    public BookingController(ListInterface<Room> sharedRoomList, BSTInterface<Guest> masterGuestRegistry) {
        this(sharedRoomList, null, null, masterGuestRegistry);
    }

    public BookingController(ListInterface<Room> sharedRoomList, BSTInterface<Guest> masterGuestRegistry,
            ListInterface<Booking> sharedBookingList) {
        this(sharedRoomList, sharedBookingList, null, masterGuestRegistry);
    }

    public BookingController(ListInterface<Room> sharedRoomList, ListInterface<Guest> sharedRegisteredGuests,
            BSTInterface<Guest> masterGuestRegistry) {
        this(sharedRoomList, null, sharedRegisteredGuests, masterGuestRegistry);
    }

    private BookingController(ListInterface<Room> sharedRoomList, ListInterface<Booking> sharedBookingList,
            ListInterface<Guest> sharedRegisteredGuests, BSTInterface<Guest> masterGuestRegistry) {
        waitingQueue = new ArrayQueue<>();
        bookingList = (sharedBookingList != null) ? sharedBookingList : new MyArrayList<>();
        registeredGuests = (sharedRegisteredGuests != null) ? sharedRegisteredGuests : new MyArrayList<>();
        roomList = (sharedRoomList != null) ? sharedRoomList : new MyArrayList<>();
        this.masterGuestRegistry = masterGuestRegistry;
        nextConfirmationNumber = 20000001;
        nextBookingId = 1;
        if (sharedRoomList == null) {
            seedRooms();
        }
        seedInitialQueueGuests();
        initializeNextIdentifiers();
    }

    private void seedRooms() {
        roomList.add(new Room("101", "Deluxe Suite", "Ready for Check-In", 350.00));
        roomList.add(new Room("102", "Presidential Suite", "Dirty", 800.00));
        roomList.add(new Room("103", "Standard Room", "Ready for Check-In", 180.00));
        roomList.add(new Room("104", "Deluxe Suite", "Occupied", 350.00));
        roomList.add(new Room("105", "Standard Room", "Cleaning In Progress", 180.00));
        roomList.add(new Room("201", "Presidential Suite", "Ready for Check-In", 950.00));
        roomList.add(new Room("202", "Deluxe Suite", "Ready for Check-In", 400.00));
    }

    private void seedInitialQueueGuests() {
        if (!waitingQueue.isEmpty()) return;

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
        }

        // App owns the shared master guests/bookings. This method only provides
        // FIFO queue examples and does not duplicate any master booking.
    }

    /** Starts generated identifiers after all records already present in shared memory. */
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

    /** Uses the shared registry as the source of truth when modules are integrated. */
    private ListInterface<Guest> getRegisteredGuestsSnapshot() {
        return masterGuestRegistry != null ? masterGuestRegistry.inOrderTraversal() : registeredGuests;
    }

    public Guest findGuestByIC(String icNo) {
        if (icNo == null || icNo.trim().isEmpty()) return null;
        String cleanQuery = icNo.trim().replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
        // The master registry is available in the integrated App flow.  In
        // standalone BookingUI mode, however, registeredGuests is the local
        // source of truth and must still be searchable by IC.
        ListInterface<Guest> allGuests = masterGuestRegistry != null
                ? masterGuestRegistry.inOrderTraversal() : registeredGuests;
        for (int i = 0; i < allGuests.getNumberOfEntries(); i++) {
            Guest g = allGuests.get(i);
            if (g.getIcNo() != null) {
                String cleanIc = g.getIcNo().replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
                if (cleanIc.equalsIgnoreCase(cleanQuery)) {
                    return g;
                }
            }
        }
        return null;
    }

    /** Boundary-safe lookup projection; keeps Entity types inside the control layer. */
    public GuestView findGuestByICView(String icNo) {
        Guest guest = findGuestByIC(icNo);
        return guest == null ? null : toGuestView(guest);
    }

    public Guest registerWalkInGuest(String guestName, String loyaltyTier) {
        return registerWalkInGuest(guestName, "N/A", loyaltyTier, 0);
    }

    public Guest registerWalkInGuest(String guestName, String icNo, String loyaltyTier, int loyaltyPoints) {
        String confirmNo = String.valueOf(nextConfirmationNumber++);
        while (isConfirmationNumberUsed(confirmNo)) {
            confirmNo = String.valueOf(nextConfirmationNumber++);
        }
        Guest newGuest = new Guest(guestName, icNo, confirmNo, loyaltyTier, loyaltyPoints);
        newGuest.setBookingStatus("Waiting");
        waitingQueue.enqueue(newGuest);
        registeredGuests.add(newGuest);
        // Sync to Master Guest Registry so FrontDesk & Loyalty can see this guest
        if (masterGuestRegistry != null) {
            masterGuestRegistry.add(newGuest);
        }
        return newGuest;
    }

    public GuestView registerWalkInGuestView(String guestName, String icNo, String loyaltyTier, int loyaltyPoints) {
        return toGuestView(registerWalkInGuest(guestName, icNo, loyaltyTier, loyaltyPoints));
    }

    private boolean isConfirmationNumberUsed(String confirmationNumber) {
        ListInterface<Guest> guests = getRegisteredGuestsSnapshot();
        for (int i = 0; i < guests.getNumberOfEntries(); i++) {
            if (confirmationNumber.equalsIgnoreCase(guests.get(i).getConfirmationNumber())) return true;
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

    public GuestView[] getWaitingQueueViews() {
        ListInterface<Guest> guests = waitingQueue.toList();
        GuestView[] views = new GuestView[guests.getNumberOfEntries()];
        for (int i = 0; i < views.length; i++) views[i] = toGuestView(guests.get(i));
        return views;
    }

    /**
     * Peeks at the next guest to be served (front of queue).
     */
    public Guest peekNextGuest() {
        return waitingQueue.getFront();
    }

    public GuestView peekNextGuestView() {
        Guest guest = peekNextGuest();
        return guest == null ? null : toGuestView(guest);
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

    /** Creates a booking for the next FIFO guest, including an optional stay request. */
    public int processNextGuest(String roomNumber, String checkInDate, int numberOfNights, String specialRequest) {
        if (waitingQueue.isEmpty())
            return -1;

        int dateValidation = validateStayPeriod(checkInDate, numberOfNights);
        if (dateValidation != 1) return dateValidation;

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
            if (booking != null && bookingId.equalsIgnoreCase(booking.getBookingId())) return true;
        }
        return false;
    }

    /**
     * Finds a booking by its human-readable booking ID. This is the primary
     * lookup used by view, update, and cancellation operations.
     */
    public Booking findBookingById(String bookingId) {
        refreshExpiredBookings();
        if (bookingId == null || bookingId.trim().isEmpty()) return null;
        for (int i = 0; i < bookingList.getNumberOfEntries(); i++) {
            Booking booking = bookingList.get(i);
            if (booking != null && bookingId.trim().equalsIgnoreCase(booking.getBookingId())) return booking;
        }
        return null;
    }

    /** Finds the latest booking belonging to a stay confirmation number. */
    public Booking findBookingByConfirmation(String confirmationNumber) {
        refreshExpiredBookings();
        if (confirmationNumber == null || confirmationNumber.trim().isEmpty()) return null;
        for (int i = bookingList.getNumberOfEntries() - 1; i >= 0; i--) {
            Booking booking = bookingList.get(i);
            if (booking != null && confirmationNumber.trim()
                    .equalsIgnoreCase(booking.getGuestConfirmationNumber())) return booking;
        }
        return null;
    }

    public BookingView findBookingByIdView(String bookingId) {
        Booking booking = findBookingById(bookingId);
        return booking == null ? null : new BookingView(booking);
    }

    public BookingView findBookingByConfirmationView(String confirmationNumber) {
        Booking booking = findBookingByConfirmation(confirmationNumber);
        return booking == null ? null : new BookingView(booking);
    }

    /**
     * Updates a not-yet-checked-in booking and synchronizes the matching Guest
     * record and shared Room list. Return values: 1 success; -1 booking missing;
     * -2 booking no longer editable; -3 room missing; -4 invalid date/nights;
     * -5 selected room is not available for the requested date range.
     */
    public int updateBooking(String bookingId, String roomNumber, String checkInDate,
            int numberOfNights, String specialRequest) {
        Booking booking = findBookingById(bookingId);
        if (booking == null) return -1;
        if (!"Confirmed".equalsIgnoreCase(booking.getBookingStatus())) return -2;
        int dateValidation = validateStayPeriod(checkInDate, numberOfNights);
        if (dateValidation != 1) return dateValidation;

        Room newRoom = findRoomByNumber(roomNumber);
        if (newRoom == null) return -3;
        if (!isRoomAvailableForStay(newRoom, checkInDate, numberOfNights, booking)) return -5;

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

    /** Cancels a guest who is still waiting, preserving all other FIFO positions. */
    public int cancelWaitingRegistration(String confirmationNumber) {
        if (confirmationNumber == null || confirmationNumber.trim().isEmpty()) return -1;
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

    private boolean isValidIsoDate(String date) {
        if (date == null || date.trim().isEmpty())
            return false;
        try {
            LocalDate.parse(date.trim());
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    /**
     * Validates a realistic stay period. Return: 1 valid, -4 malformed date,
     * -5 nights outside 1..30, -6 check-in is in the past, -7 more than one
     * year in advance.
     */
    public int validateStayPeriod(String checkInDate, int numberOfNights) {
        if (!isValidIsoDate(checkInDate)) return -4;
        if (numberOfNights <= 0 || numberOfNights > MAX_STAY_NIGHTS) return -5;
        LocalDate checkIn = LocalDate.parse(checkInDate.trim());
        LocalDate today = LocalDate.now();
        if (checkIn.isBefore(today)) return -6;
        if (checkIn.isAfter(today.plusDays(MAX_ADVANCE_BOOKING_DAYS))) return -7;
        return 1;
    }

    /**
     * Returns the most recently created booking (for display after processing).
     */
    public Booking getLastBooking() {
        if (bookingList.isEmpty())
            return null;
        return bookingList.get(bookingList.getNumberOfEntries() - 1);
    }

    public BookingView getLastBookingView() {
        Booking booking = getLastBooking();
        return booking == null ? null : new BookingView(booking);
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
        if (b == null) return -1;
        if ("Cancelled".equalsIgnoreCase(b.getBookingStatus())) return -2;
        if ("NoShow".equalsIgnoreCase(b.getBookingStatus())) return -5;

        // Reset Guest state in either the shared master registry or the local
        // standalone registry.  Cancellation must not leave the Guest marked
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

    public BookingView[] getAllBookingViews() {
        return toBookingViews(getAllBookings());
    }

    /**
     * Find a room by room number (linear search).
     */
    public Room findRoomByNumber(String roomNumber) {
        if (roomNumber == null)
            return null;
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

    /** Returns rooms that are usable and have no active booking overlap for the requested stay. */
    public ListInterface<Room> getAvailableRooms(String checkInDate, int numberOfNights) {
        ListInterface<Room> available = new MyArrayList<>();
        if (validateStayPeriod(checkInDate, numberOfNights) != 1) return available;
        refreshExpiredBookings();
        for (int i = 0; i < roomList.getNumberOfEntries(); i++) {
            Room room = roomList.get(i);
            if (isRoomAvailableForStay(room, checkInDate, numberOfNights, null)) available.add(room);
        }
        return available;
    }

    public RoomView[] getAvailableRoomViews(String checkInDate, int numberOfNights) {
        return toRoomViews(getAvailableRooms(checkInDate, numberOfNights));
    }

    /** Availability query that excludes the booking currently being edited. */
    public ListInterface<Room> getAvailableRoomsForUpdate(String bookingId, String checkInDate, int numberOfNights) {
        ListInterface<Room> available = new MyArrayList<>();
        if (validateStayPeriod(checkInDate, numberOfNights) != 1) return available;
        Booking excluded = findBookingById(bookingId);
        for (int i = 0; i < roomList.getNumberOfEntries(); i++) {
            Room room = roomList.get(i);
            if (isRoomAvailableForStay(room, checkInDate, numberOfNights, excluded)) available.add(room);
        }
        return available;
    }

    public RoomView[] getAvailableRoomViewsForUpdate(String bookingId, String checkInDate, int numberOfNights) {
        return toRoomViews(getAvailableRoomsForUpdate(bookingId, checkInDate, numberOfNights));
    }

    private Guest findGuestByConfirmation(String confirmationNumber) {
        if (confirmationNumber == null) return null;
        if (masterGuestRegistry != null) {
            Guest guest = masterGuestRegistry.search(new Guest("", confirmationNumber.trim(), "", 0));
            if (guest != null) return guest;
        }
        for (int i = 0; i < registeredGuests.getNumberOfEntries(); i++) {
            Guest guest = registeredGuests.get(i);
            if (confirmationNumber.trim().equalsIgnoreCase(guest.getConfirmationNumber())) return guest;
        }
        return null;
    }

    private boolean isRoomAvailableForStay(Room room, String checkInDate, int numberOfNights, Booking excludedBooking) {
        if (room == null) return false;
        LocalDate requestedStart = LocalDate.parse(checkInDate.trim());
        LocalDate requestedEnd = requestedStart.plusDays(numberOfNights);

        // Physical condition blocks arrivals today. For a future reservation,
        // housekeeping/occupancy is expected to change before arrival; only an
        // explicit maintenance/out-of-service condition blocks advance sales.
        String status = room.getRoomStatus();
        if (!requestedStart.isAfter(LocalDate.now())
                && !("Ready for Check-In".equalsIgnoreCase(status) || "Reserved".equalsIgnoreCase(status))) {
            return false;
        }
        if ("Maintenance".equalsIgnoreCase(status) || "Out of Service".equalsIgnoreCase(status)) return false;

        for (int i = 0; i < bookingList.getNumberOfEntries(); i++) {
            Booking existing = bookingList.get(i);
            if (existing == excludedBooking || existing == null
                    || !room.getRoomNumber().equalsIgnoreCase(existing.getRoomNumber())
                    || !isActiveReservation(existing)) continue;
            try {
                LocalDate existingStart = LocalDate.parse(existing.getCheckInDate());
                LocalDate existingEnd = existingStart.plusDays(existing.getNumberOfNights());
                if (requestedStart.isBefore(existingEnd) && existingStart.isBefore(requestedEnd)) return false;
            } catch (DateTimeParseException ignored) {
                return false;
            }
        }
        if (masterGuestRegistry != null) {
            ListInterface<Guest> guests = masterGuestRegistry.inOrderTraversal();
            for (int i = 0; i < guests.getNumberOfEntries(); i++) {
                Guest guest = guests.get(i);
                if (!guest.isCheckedIn() || guest.getAssignedRoomNumber() == null
                        || !room.getRoomNumber().equalsIgnoreCase(guest.getAssignedRoomNumber())) continue;
                if (excludedBooking != null && excludedBooking.getGuestConfirmationNumber()
                        .equalsIgnoreCase(guest.getConfirmationNumber())) continue;
                try {
                    LocalDate occupiedStart = LocalDate.parse(guest.getCheckInDate());
                    LocalDate occupiedEnd = occupiedStart.plusDays(Math.max(1, guest.getNumberOfNights()));
                    if (requestedStart.isBefore(occupiedEnd) && occupiedStart.isBefore(requestedEnd)) return false;
                } catch (Exception ignored) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isActiveReservation(Booking booking) {
        return "Confirmed".equalsIgnoreCase(booking.getBookingStatus())
                || "CheckedIn".equalsIgnoreCase(booking.getBookingStatus());
    }

    /** Marks an unconsumed reservation NoShow on the day after scheduled arrival. */
    public int refreshExpiredBookings() {
        int updated = 0;
        LocalDate today = LocalDate.now();
        for (int i = 0; i < bookingList.getNumberOfEntries(); i++) {
            Booking booking = bookingList.get(i);
            if ("Confirmed".equalsIgnoreCase(booking.getBookingStatus())
                    && today.isAfter(LocalDate.parse(booking.getCheckInDate()))) {
                booking.recordNoShow();
                Guest guest = findGuestByConfirmation(booking.getGuestConfirmationNumber());
                if (guest != null) guest.setBookingStatus("NoShow");
                updated++;
            }
        }
        if (updated > 0) refreshReservedRoomStatuses();
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
            if (roomNumber.equalsIgnoreCase(booking.getRoomNumber()) && isActiveReservation(booking)
                    && today.equals(LocalDate.parse(booking.getCheckInDate()))) return true;
        }
        return false;
    }

    private void releaseRoomIfUnreserved(String roomNumber) {
        Room room = findRoomByNumber(roomNumber);
        if (room == null || !"Reserved".equalsIgnoreCase(room.getRoomStatus())) return;
        for (int i = 0; i < bookingList.getNumberOfEntries(); i++) {
            Booking booking = bookingList.get(i);
            if (booking != null && roomNumber.equalsIgnoreCase(booking.getRoomNumber())
                    && isActiveReservation(booking)
                    && LocalDate.now().equals(LocalDate.parse(booking.getCheckInDate()))) return;
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

    public BookingView[] getFilteredAndSortedBookingViews(String roomTypeFilter, String statusFilter,
            String startDate, String endDate, int minNights, boolean sortByPriceAscending) {
        return toBookingViews(getFilteredAndSortedBookings(roomTypeFilter, statusFilter, startDate, endDate,
                minNights, sortByPriceAscending));
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

    public double[] getBookingViewMetrics(BookingView[] bookings) {
        double[] metrics = new double[6];
        if (bookings == null) return metrics;
        for (BookingView booking : bookings) {
            if (booking == null) continue;
            if ("Cancelled".equalsIgnoreCase(booking.getBookingStatus())) metrics[1]++;
            else if ("NoShow".equalsIgnoreCase(booking.getBookingStatus())) metrics[5]++;
            else {
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
     * @param statusFilter  lifecycle status such as "Waiting", "Confirmed", or "ALL"
     * @param sortAscending true for ascending confirmation number, false for
     *                      descending
     * @return Filtered and sorted list of guests
     */
    public ListInterface<Guest> getFilteredAndSortedGuests(
            String tierFilter, String statusFilter, boolean sortAscending) {

        // Expiry changes the operational status used by this report.  Refresh
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

    public GuestView[] getFilteredAndSortedGuestViews(String tierFilter, String statusFilter, boolean sortAscending) {
        ListInterface<Guest> guests = getFilteredAndSortedGuests(tierFilter, statusFilter, sortAscending);
        GuestView[] views = new GuestView[guests.getNumberOfEntries()];
        ListInterface<Guest> queueSnapshot = waitingQueue.toList();
        for (int i = 0; i < views.length; i++) {
            Guest guest = guests.get(i);
            views[i] = toGuestView(guest, getGuestOperationalStatus(guest, queueSnapshot));
        }
        return views;
    }

    /** Returns the queue-aware lifecycle status used consistently in Report 2. */
    public String getGuestOperationalStatus(Guest guest) {
        return getGuestOperationalStatus(guest, waitingQueue.toList());
    }

    private String getGuestOperationalStatus(Guest guest, ListInterface<Guest> queueSnapshot) {
        if (guest != null && isGuestInQueue(guest.getConfirmationNumber(), queueSnapshot)) return "Waiting";
        if (guest == null || guest.getBookingStatus() == null) return "Unknown";
        return "Reserved".equalsIgnoreCase(guest.getBookingStatus()) ? "Confirmed" : guest.getBookingStatus();
    }

    private LocalDate parseOptionalDate(String value) {
        if (value == null || value.trim().isEmpty() || "ALL".equalsIgnoreCase(value.trim())) return null;
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

    private GuestView toGuestView(Guest guest) {
        return toGuestView(guest, getGuestOperationalStatus(guest));
    }

    private GuestView toGuestView(Guest guest, String operationalStatus) {
        return new GuestView(guest, operationalStatus);
    }

    private BookingView[] toBookingViews(ListInterface<Booking> bookings) {
        BookingView[] views = new BookingView[bookings.getNumberOfEntries()];
        for (int i = 0; i < views.length; i++) views[i] = new BookingView(bookings.get(i));
        return views;
    }

    private RoomView[] toRoomViews(ListInterface<Room> rooms) {
        RoomView[] views = new RoomView[rooms.getNumberOfEntries()];
        for (int i = 0; i < views.length; i++) views[i] = new RoomView(rooms.get(i));
        return views;
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
            if ("Waiting".equalsIgnoreCase(status)) summary[1]++;
            else if ("Confirmed".equalsIgnoreCase(status) || "Reserved".equalsIgnoreCase(status)) summary[2]++;
            else if ("CheckedIn".equalsIgnoreCase(status)) summary[3]++;
            else if ("CheckedOut".equalsIgnoreCase(status)) summary[4]++;
            else if ("Cancelled".equalsIgnoreCase(status)) summary[5]++;
            else if ("NoShow".equalsIgnoreCase(status)) summary[6]++;
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
}
