package control;

import adt.BSTInterface;
import adt.BinarySearchTree;
import adt.ListInterface;
import adt.MyArrayList;
import entity.Guest;
import entity.Booking;
import entity.Room;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Comparator;

/**
 * Author: Weisheng
 * Controller for Front-Desk Operations. Uses BST for guest and room management.
 */
public class FrontDeskController {
    private BSTInterface<Guest> guestTree;
    private BSTInterface<Room> roomTree;

    private ListInterface<Room> sharedRoomList;
    private ListInterface<Booking> sharedBookingList;

    /** Read-only billing projection returned to the boundary for display. */
    public static class BillingDetails {
        private final Guest guest;
        private final Room room;
        private final Booking booking;
        private final int nights;
        private final double chargedRate;
        private final double discountRate;
        private final double subtotal;
        private final double discountAmount;
        private final double total;

        private BillingDetails(Guest guest, Room room, Booking booking, int nights,
                double chargedRate, double discountRate) {
            this.guest = guest;
            this.room = room;
            this.booking = booking;
            this.nights = nights;
            this.chargedRate = chargedRate;
            this.discountRate = discountRate;
            this.subtotal = chargedRate * nights;
            this.discountAmount = subtotal * discountRate;
            this.total = subtotal - discountAmount;
        }

        public Guest getGuest() { return guest; }
        public Room getRoom() { return room; }
        public Booking getBooking() { return booking; }
        public int getNights() { return nights; }
        public double getChargedRate() { return chargedRate; }
        public double getDiscountRate() { return discountRate; }
        public double getSubtotal() { return subtotal; }
        public double getDiscountAmount() { return discountAmount; }
        public double getTotal() { return total; }
        public int getProjectedPoints() { return (int) (subtotal / 10.0); }
    }

    /** Atomic checkout outcome, including the already-calculated receipt values. */
    public static class CheckoutResult {
        private final int status;
        private final BillingDetails bill;
        private final int earnedPoints;

        private CheckoutResult(int status, BillingDetails bill, int earnedPoints) {
            this.status = status;
            this.bill = bill;
            this.earnedPoints = earnedPoints;
        }

        public int getStatus() { return status; }
        public BillingDetails getBill() { return bill; }
        public int getEarnedPoints() { return earnedPoints; }
    }

    public FrontDeskController() {
        this(null, null, null);
    }

    public FrontDeskController(BSTInterface<Guest> masterGuestTree, ListInterface<Room> sharedRoomList) {
        this(masterGuestTree, sharedRoomList, null);
    }

    public FrontDeskController(BSTInterface<Guest> masterGuestTree, ListInterface<Room> sharedRoomList,
            ListInterface<Booking> sharedBookingList) {
        this.guestTree = (masterGuestTree != null) ? masterGuestTree : new BinarySearchTree<>();
        this.roomTree = new BinarySearchTree<>();
        this.sharedRoomList = sharedRoomList;
        this.sharedBookingList = (sharedBookingList != null) ? sharedBookingList : new MyArrayList<>();
        if (masterGuestTree == null && sharedRoomList == null) {
            seedInitialData();
        } else {
            syncRoomTree();
        }
    }

    private void syncRoomTree() {
        if (sharedRoomList != null) {
            roomTree.clear();
            for (int i = 0; i < sharedRoomList.getNumberOfEntries(); i++) {
                roomTree.add(sharedRoomList.get(i));
            }
        }
    }

    private void seedInitialData() {
        Guest alice = new Guest("Alice Tan", "980101-14-5566", "10000001", "Platinum", 1200);
        alice.setCheckedIn(true);
        alice.setAssignedRoomNumber("104");
        alice.setEffectiveRoomRate(350.00);
        alice.setRoomType("Deluxe Suite");
        alice.setCheckInDate("2026-08-12");
        alice.setNumberOfNights(3);
        guestTree.add(alice);

        guestTree.add(new Guest("Bob Lee", "990202-08-1234", "10000002", "Gold", 500));
        guestTree.add(new Guest("Charlie Lim", "000303-10-9988", "10000003", "Silver", 200));
        guestTree.add(new Guest("David Wong", "950404-01-3322", "10000004", "Standard", 50));
        guestTree.add(new Guest("Eva Green", "920505-07-7711", "10000005", "Platinum", 1800));
        guestTree.add(new Guest("Frank Wright", "960606-05-4433", "10000006", "Gold", 850));

        roomTree.add(new Room("101", "Deluxe Suite", "Ready for Check-In", 350.00));
        roomTree.add(new Room("102", "Presidential Suite", "Dirty", 800.00));
        roomTree.add(new Room("103", "Standard Room", "Ready for Check-In", 180.00));
        roomTree.add(new Room("104", "Deluxe Suite", "Occupied", 350.00));
        roomTree.add(new Room("105", "Standard Room", "Cleaning In Progress", 180.00));
        roomTree.add(new Room("201", "Presidential Suite", "Ready for Check-In", 950.00));
        roomTree.add(new Room("202", "Deluxe Suite", "Ready for Check-In", 400.00));
    }

    // Search guest stay record by IC Number - Non-Key Linear Traversal O(n)
    public Guest searchGuestByIC(String icNo) {
        if (icNo == null || icNo.trim().isEmpty())
            return null;
        String queryClean = icNo.trim().replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
        ListInterface<Guest> allGuests = guestTree.inOrderTraversal();
        for (int i = 0; i < allGuests.getNumberOfEntries(); i++) {
            Guest g = allGuests.get(i);
            if (g.getIcNo() != null) {
                String cleanIc = g.getIcNo().replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
                if (cleanIc.equalsIgnoreCase(queryClean)) {
                    return g;
                }
            }
        }
        return null;
    }

    // Search guest stay record by Confirmation Number (BST Primary Key) - O(log n) BST Search
    public Guest searchGuestByConfirmationNumber(String confirmNo) {
        if (confirmNo == null || confirmNo.trim().isEmpty())
            return null;
        Guest targetDummy = new Guest("", confirmNo.trim(), "", 0);
        return guestTree.search(targetDummy);
    }

    // Search guest stay records within a range of Confirmation Numbers - O(log n + k) BST Range Search
    public ListInterface<Guest> searchGuestsByConfirmationRange(String startNo, String endNo) {
        if (startNo == null || endNo == null)
            return new MyArrayList<>();
        String s = startNo.trim();
        String e = endNo.trim();
        if (s.compareToIgnoreCase(e) > 0) {
            String temp = s;
            s = e;
            e = temp;
        }
        Guest minDummy = new Guest("", s, "", 0);
        Guest maxDummy = new Guest("", e, "", 0);
        return guestTree.rangeSearch(minDummy, maxDummy);
    }

    // Add a new guest into the BST
    public boolean registerGuest(Guest guest) {
        if (guest == null || guest.getConfirmationNumber() == null
                || !guest.getConfirmationNumber().matches("\\d{8}")
                || isMissingRequired(guest.getGuestName()) || isMissingRequired(guest.getIcNo())
                || !isValidOptionalPhone(guest.getPhoneNumber()))
            return false;
        if (guestTree.contains(guest))
            return false;
        Guest duplicateIdentity = searchGuestByIC(guest.getIcNo());
        if (duplicateIdentity != null)
            return false;
        return guestTree.add(guest);
    }

    /**
     * Updates editable guest-profile fields while preserving the immutable BST key.
     * Return: 1 success, -1 guest missing, -2 invalid required data,
     * -3 duplicate IC/passport, -4 invalid email, -5 invalid phone.
     */
    public int updateGuestProfile(String confirmationNumber, String name, String icNo,
            String phone, String gender, String nationality, String email, String specialRequest) {
        Guest guest = searchGuestByConfirmationNumber(confirmationNumber);
        if (guest == null) return -1;
        if (isMissingRequired(name) || isMissingRequired(icNo)) return -2;

        Guest sameIdentity = searchGuestByIC(icNo);
        if (sameIdentity != null && sameIdentity != guest) return -3;
        if (!isBlank(email) && !"N/A".equalsIgnoreCase(email.trim())
                && !email.trim().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) return -4;
        if (!isValidOptionalPhone(phone)) return -5;

        guest.setGuestName(name.trim());
        guest.setIcNo(icNo.trim());
        guest.setPhoneNumber(normalizeOptional(phone));
        guest.setGender(normalizeOptional(gender));
        guest.setNationality(isBlank(nationality) ? "N/A" : nationality.trim());
        guest.setEmail(normalizeOptional(email));
        guest.setSpecialRequest(isBlank(specialRequest) || "N/A".equalsIgnoreCase(specialRequest.trim())
                ? null : specialRequest.trim());

        // Booking stores display snapshots, so keep them aligned with the master guest.
        for (int i = 0; i < sharedBookingList.getNumberOfEntries(); i++) {
            Booking booking = sharedBookingList.get(i);
            if (booking != null && confirmationNumber.equalsIgnoreCase(booking.getGuestConfirmationNumber())) {
                booking.setGuestName(guest.getGuestName());
                booking.setSpecialRequest(guest.getSpecialRequest());
            }
        }
        return 1;
    }

    public boolean updateSpecialRequest(String confirmationNumber, String specialRequest) {
        Guest guest = searchGuestByConfirmationNumber(confirmationNumber);
        if (guest == null) return false;
        String normalized = isBlank(specialRequest) ? null : specialRequest.trim();
        guest.setSpecialRequest(normalized);
        Booking booking = findBookingByConfirmation(confirmationNumber);
        if (booking != null) booking.setSpecialRequest(normalized);
        return true;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private boolean isMissingRequired(String value) {
        return isBlank(value) || "N/A".equalsIgnoreCase(value.trim());
    }

    private boolean isValidOptionalPhone(String phone) {
        if (isBlank(phone) || "N/A".equalsIgnoreCase(phone.trim())) return true;
        String digits = phone.replaceAll("\\D", "");
        return digits.length() >= 7 && digits.length() <= 15
                && phone.matches("^[+()0-9 .-]+$");
    }

    private String normalizeOptional(String value) {
        return isBlank(value) ? "N/A" : value.trim();
    }

    // Remove a guest from the BST
    public Guest removeGuest(String confirmNo) {
        if (confirmNo == null || confirmNo.trim().isEmpty())
            return null;
        Guest dummy = new Guest("", confirmNo.trim(), "", 0);
        Guest removed = guestTree.remove(dummy);
        if (removed != null) {
            String assignedRoomNumber = removed.getAssignedRoomNumber();
            if (assignedRoomNumber != null) {
                Room room = searchRoomByNumber(assignedRoomNumber);
                if (room != null && "Reserved".equalsIgnoreCase(room.getRoomStatus())) {
                    room.setRoomStatus("Ready for Check-In");
                }
            }
            Booking booking = findBookingByConfirmation(confirmNo);
            if (booking != null && !"CheckedOut".equalsIgnoreCase(booking.getBookingStatus())) {
                booking.recordCancellation("Guest record removed at Front Desk", "Front Desk Staff");
            }
        }
        return removed;
    }

    private Booking findBookingByConfirmation(String confirmationNumber) {
        if (confirmationNumber == null)
            return null;
        for (int i = sharedBookingList.getNumberOfEntries() - 1; i >= 0; i--) {
            Booking booking = sharedBookingList.get(i);
            if (booking != null && confirmationNumber.trim()
                    .equalsIgnoreCase(booking.getGuestConfirmationNumber())) {
                return booking;
            }
        }
        return null;
    }

    public Booking getBookingByConfirmation(String confirmationNumber) {
        return findBookingByConfirmation(confirmationNumber);
    }

    public Booking searchBookingById(String bookingId) {
        if (isBlank(bookingId)) return null;
        for (int i = 0; i < sharedBookingList.getNumberOfEntries(); i++) {
            Booking booking = sharedBookingList.get(i);
            if (booking != null && bookingId.trim().equalsIgnoreCase(booking.getBookingId())) return booking;
        }
        return null;
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
            if (g.getGuestName() != null && g.getGuestName().toLowerCase().contains(queryLower)) {
                results.add(g);
            }
        }
        return results;
    }

    // Search room by room number using BST
    public Room searchRoomByNumber(String roomNumber) {
        if (roomNumber == null || roomNumber.trim().isEmpty())
            return null;
        syncRoomTree();
        Room targetDummy = new Room(roomNumber.trim(), "", "", 0.0);
        return roomTree.search(targetDummy);
    }

    public ListInterface<Room> getAllRooms() {
        syncRoomTree();
        return roomTree.inOrderTraversal();
    }

    /** Validates Front Desk room-availability search input. */
    public int validateStayPeriod(String checkInDate, int numberOfNights) {
        if (isBlank(checkInDate)) return -1;
        try {
            LocalDate checkIn = LocalDate.parse(checkInDate.trim());
            if (numberOfNights < 1 || numberOfNights > 30) return -2;
            if (checkIn.isBefore(LocalDate.now())) return -3;
            if (checkIn.isAfter(LocalDate.now().plusDays(365))) return -4;
            return 1;
        } catch (DateTimeParseException e) {
            return -1;
        }
    }

    /**
     * Searches rooms by date range, room type and budget, then sorts by price.
     * This uses the shared booking schedule rather than only today's physical status.
     */
    public ListInterface<Room> searchAvailableRooms(String checkInDate, int numberOfNights,
            String roomTypeFilter, double maxPrice, boolean sortAscending) {
        ListInterface<Room> result = new MyArrayList<>();
        if (validateStayPeriod(checkInDate, numberOfNights) != 1 || maxPrice < 0) return result;
        syncRoomTree();
        LocalDate start = LocalDate.parse(checkInDate.trim());
        LocalDate end = start.plusDays(numberOfNights);
        ListInterface<Room> rooms = roomTree.inOrderTraversal();
        for (int i = 0; i < rooms.getNumberOfEntries(); i++) {
            Room room = rooms.get(i);
            boolean typeMatches = isBlank(roomTypeFilter) || "ALL".equalsIgnoreCase(roomTypeFilter.trim())
                    || room.getRoomType().equalsIgnoreCase(roomTypeFilter.trim());
            boolean budgetMatches = maxPrice == 0 || room.getPrice() <= maxPrice;
            if (!typeMatches || !budgetMatches || !isRoomSellableForPeriod(room, start, end)) continue;
            result.add(room);
        }
        result.sort((left, right) -> sortAscending
                ? Double.compare(left.getPrice(), right.getPrice())
                : Double.compare(right.getPrice(), left.getPrice()));
        return result;
    }

    private boolean isRoomSellableForPeriod(Room room, LocalDate start, LocalDate end) {
        String status = room.getRoomStatus();
        if ("Maintenance".equalsIgnoreCase(status) || "Out of Service".equalsIgnoreCase(status)) return false;
        if (start.equals(LocalDate.now()) && !"Ready for Check-In".equalsIgnoreCase(status)) return false;
        return !hasBookingConflict(room.getRoomNumber(), start, end, "")
                && !hasActiveGuestConflict(room.getRoomNumber(), start, end);
    }

    private boolean hasActiveGuestConflict(String roomNumber, LocalDate start, LocalDate end) {
        ListInterface<Guest> guests = guestTree.inOrderTraversal();
        for (int i = 0; i < guests.getNumberOfEntries(); i++) {
            Guest guest = guests.get(i);
            if (!guest.isCheckedIn() || guest.getAssignedRoomNumber() == null
                    || !roomNumber.equalsIgnoreCase(guest.getAssignedRoomNumber())) continue;
            try {
                LocalDate occupiedStart = LocalDate.parse(guest.getCheckInDate());
                LocalDate occupiedEnd = occupiedStart.plusDays(Math.max(1, guest.getNumberOfNights()));
                if (start.isBefore(occupiedEnd) && occupiedStart.isBefore(end)) return true;
            } catch (Exception ignored) {
                return true; // Invalid active-stay data must fail closed to prevent double selling.
            }
        }
        return false;
    }

    /** Stores the intended duration of an unbooked walk-in before check-in. */
    public int setWalkInStayLength(String confirmationNumber, int numberOfNights) {
        Guest guest = searchGuestByConfirmationNumber(confirmationNumber);
        if (guest == null) return -1;
        if (numberOfNights < 1 || numberOfNights > 30) return -2;
        if (findBookingByConfirmation(confirmationNumber) != null) return -3;
        if (guest.isCheckedIn() || guest.isCheckedOut() || guest.isCancelled()) return -4;
        guest.setCheckInDate(LocalDate.now().toString());
        guest.setNumberOfNights(numberOfNights);
        return 1;
    }

    private ListInterface<String> activeCheckedInConfirmations = new MyArrayList<>();

    public boolean isGuestCheckedIn(String confirmationNumber) {
        if (confirmationNumber == null || confirmationNumber.trim().isEmpty())
            return false;
        Guest guest = searchGuestByConfirmationNumber(confirmationNumber);
        if (guest != null && guest.isCheckedIn())
            return true;
        for (int i = 0; i < activeCheckedInConfirmations.getNumberOfEntries(); i++) {
            if (activeCheckedInConfirmations.get(i).equalsIgnoreCase(confirmationNumber.trim())) {
                return true;
            }
        }
        return false;
    }

    // Process check-in: returns 1=success, -1=guest not found, -2=room not found,
    // -3=room not ready, -4=guest already checked-in
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

        if ("CheckedOut".equalsIgnoreCase(guest.getBookingStatus())) {
            return -6; // Guest already checked out for this stay
        }

        if ("Cancelled".equalsIgnoreCase(guest.getBookingStatus())) {
            return -7; // Booking was cancelled
        }

        if ("NoShow".equalsIgnoreCase(guest.getBookingStatus())) return -8;

        LocalDate today = LocalDate.now();
        Booking booking = findBookingByConfirmation(confirmationNumber);
        if (booking != null) {
            if ("NoShow".equalsIgnoreCase(booking.getBookingStatus())) return -8;
            LocalDate scheduledArrival = LocalDate.parse(booking.getCheckInDate());
            if (today.isBefore(scheduledArrival)) return -9; // Too early
            if (today.isAfter(scheduledArrival)) {
                booking.recordNoShow();
                guest.setBookingStatus("NoShow");
                return -10; // Scheduled arrival date has passed
            }
        } else if (guest.getCheckInDate() != null) {
            try {
                LocalDate scheduledArrival = LocalDate.parse(guest.getCheckInDate());
                if (today.isBefore(scheduledArrival)) return -9;
                if (today.isAfter(scheduledArrival)) return -10;
            } catch (DateTimeParseException ignored) {
                return -11;
            }
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

        LocalDate stayEnd = booking != null ? LocalDate.parse(booking.getCheckOutDate())
                : today.plusDays(Math.max(1, guest.getNumberOfNights()));
        if (hasBookingConflict(roomNumber.trim(), today, stayEnd, confirmationNumber)) return -5;

        // Validate that if a room is Reserved, it must be reserved for THIS guest
        if ("Reserved".equalsIgnoreCase(currentStatus)) {
            if (guest.getAssignedRoomNumber() == null
                    || !guest.getAssignedRoomNumber().equalsIgnoreCase(roomNumber.trim())) {
                return -5; // Room is reserved for another guest
            }
        }

        // If an upgrade or different room was selected, release the guest's original
        // reservation only after every validation above has succeeded.
        String originallyReservedRoomNo = guest.getAssignedRoomNumber();
        if (originallyReservedRoomNo != null
                && !originallyReservedRoomNo.equalsIgnoreCase(roomNumber.trim())) {
            Room originallyReservedRoom = searchRoomByNumber(originallyReservedRoomNo);
            if (originallyReservedRoom != null
                    && "Reserved".equalsIgnoreCase(originallyReservedRoom.getRoomStatus())) {
                if (!hasAnotherActiveBookingForRoom(originallyReservedRoomNo, confirmationNumber)) {
                    originallyReservedRoom.setRoomStatus("Ready for Check-In");
                }
            }
        }

        room.setRoomStatus("Occupied");
        guest.setCheckedIn(true);
        guest.setCheckInDate(today.toString());
        guest.setCheckOutDate(null);
        guest.setAssignedRoomNumber(roomNumber.trim());
        guest.setEffectiveRoomRate(baseRoomPrice > 0 ? baseRoomPrice : room.getPrice());
        guest.setRoomType(room.getRoomType());
        if (booking != null) {
            booking.recordCheckIn();
            booking.setRoomNumber(room.getRoomNumber());
            booking.setRoomType(room.getRoomType());
        }
        activeCheckedInConfirmations.add(confirmationNumber.trim());
        return 1;
    }

    private boolean hasBookingConflict(String roomNumber, LocalDate start, LocalDate end,
            String excludedConfirmationNumber) {
        for (int i = 0; i < sharedBookingList.getNumberOfEntries(); i++) {
            Booking other = sharedBookingList.get(i);
            if (other == null || !roomNumber.equalsIgnoreCase(other.getRoomNumber())
                    || excludedConfirmationNumber.equalsIgnoreCase(other.getGuestConfirmationNumber())
                    || !("Confirmed".equalsIgnoreCase(other.getBookingStatus())
                            || "CheckedIn".equalsIgnoreCase(other.getBookingStatus()))) continue;
            LocalDate otherStart = LocalDate.parse(other.getCheckInDate());
            LocalDate otherEnd = LocalDate.parse(other.getCheckOutDate());
            if (start.isBefore(otherEnd) && otherStart.isBefore(end)) return true;
        }
        return false;
    }

    /**
     * Shared Booking records can contain consecutive, non-overlapping stays for
     * the same room. Do not release a Reserved room while another active stay
     * still refers to it.
     */
    private boolean hasAnotherActiveBookingForRoom(String roomNumber, String excludedConfirmationNumber) {
        if (roomNumber == null) return false;
        for (int i = 0; i < sharedBookingList.getNumberOfEntries(); i++) {
            Booking booking = sharedBookingList.get(i);
            if (booking != null && roomNumber.equalsIgnoreCase(booking.getRoomNumber())
                    && !excludedConfirmationNumber.equalsIgnoreCase(booking.getGuestConfirmationNumber())
                    && ("Confirmed".equalsIgnoreCase(booking.getBookingStatus())
                            || "CheckedIn".equalsIgnoreCase(booking.getBookingStatus()))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Process Guest Check-Out:
     * Sets room status to "Dirty" for Housekeeping, resets guest checked-in state,
     * and removes guest from active checked-in list.
     * 
     * @return 1: success, -1: guest not found, -2: guest not checked-in
     */
    public int processCheckOut(String confirmationNumber) {
        if (confirmationNumber == null || confirmationNumber.trim().isEmpty())
            return -1;
        Guest guest = searchGuestByConfirmationNumber(confirmationNumber);
        if (guest == null)
            return -1;
        if (!guest.isCheckedIn())
            return -2;

        String roomNo = guest.getAssignedRoomNumber();
        if (roomNo != null) {
            Room room = searchRoomByNumber(roomNo);
            if (room != null) {
                room.setRoomStatus("Dirty");
            }
        }

        guest.setBookingStatus("CheckedOut");
        guest.setCheckOutDate(LocalDate.now().toString());
        Booking booking = findBookingByConfirmation(confirmationNumber);
        if (booking != null) {
            booking.recordCheckOut();
        }
        // Note: We intentionally keep assignedRoomNumber, roomType, and roomRate
        // so the guest's stay history is preserved for reports and Loyalty module.
        // The Room object itself is already set to "Dirty" above.

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

    /** Calculates a consistent receipt without mutating guest or room state. */
    public BillingDetails calculateBill(String confirmationNumber) {
        Guest guest = searchGuestByConfirmationNumber(confirmationNumber);
        if (guest == null || !guest.isCheckedIn()) return null;
        Room room = searchRoomByNumber(guest.getAssignedRoomNumber());
        if (room == null) return null;
        Booking booking = findBookingByConfirmation(confirmationNumber);
        int nights = booking != null
                ? booking.getBillableNightsAsOf(LocalDate.now())
                : getWalkInBillableNights(guest);
        double rate = guest.getEffectiveRoomRate() > 0 ? guest.getEffectiveRoomRate() : room.getPrice();
        return new BillingDetails(guest, room, booking, nights, rate,
                getDiscountPercentage(guest.getLoyaltyTier()));
    }

    private int getWalkInBillableNights(Guest guest) {
        int reservedNights = Math.max(1, guest.getNumberOfNights());
        try {
            long elapsed = java.time.temporal.ChronoUnit.DAYS.between(
                    LocalDate.parse(guest.getCheckInDate()), LocalDate.now());
            return Math.max(reservedNights, (int) Math.max(1, elapsed));
        } catch (Exception e) {
            return reservedNights;
        }
    }

    /** Performs checkout first, then awards points and promotes tier exactly once. */
    public CheckoutResult completeCheckOutAndReward(String confirmationNumber) {
        BillingDetails bill = calculateBill(confirmationNumber);
        if (bill == null) return new CheckoutResult(-1, null, 0);
        int status = processCheckOut(confirmationNumber);
        if (status != 1) return new CheckoutResult(status, bill, 0);

        Guest guest = bill.getGuest();
        int earnedPoints = bill.getProjectedPoints();
        guest.setLoyaltyPoints(guest.getLoyaltyPoints() + earnedPoints);
        guest.setLoyaltyTier(resolveLoyaltyTier(guest.getLoyaltyPoints()));
        return new CheckoutResult(1, bill, earnedPoints);
    }

    private String resolveLoyaltyTier(int points) {
        if (points >= 1000) return "Platinum";
        if (points >= 500) return "Gold";
        if (points >= 200) return "Silver";
        return "Standard";
    }

    /**
     * Extends an active stay after verifying the room is not promised to another
     * guest during the additional dates. Return: 1 success, -1 guest missing,
     * -2 not checked in, -3 invalid stay date, -4 date conflict, -5 invalid/max stay.
     */
    public int extendStay(String confirmationNumber, int additionalNights) {
        Guest guest = searchGuestByConfirmationNumber(confirmationNumber);
        if (guest == null) return -1;
        if (!guest.isCheckedIn()) return -2;
        Booking booking = findBookingByConfirmation(confirmationNumber);
        int currentNights = booking != null ? booking.getNumberOfNights() : guest.getNumberOfNights();
        if (additionalNights <= 0 || currentNights + additionalNights > 30) return -5;

        LocalDate oldDeparture;
        try {
            oldDeparture = booking != null ? LocalDate.parse(booking.getCheckOutDate())
                    : LocalDate.parse(guest.getCheckInDate()).plusDays(currentNights);
        } catch (Exception e) {
            return -3;
        }
        LocalDate newDeparture = oldDeparture.plusDays(additionalNights);
        String roomNumber = booking != null ? booking.getRoomNumber() : guest.getAssignedRoomNumber();
        if (hasBookingConflict(roomNumber, oldDeparture, newDeparture, confirmationNumber)) return -4;

        if (booking != null) booking.setNumberOfNights(currentNights + additionalNights);
        guest.setNumberOfNights(currentNights + additionalNights);
        return 1;
    }

    /**
     * Process Room Transfer (Change Room mid-stay):
     * Releases old room to "Dirty" for Housekeeping, sets new room to "Occupied",
     * and updates guest's assigned room and effective rate.
     * 
     * @return 1: success, -1: guest not found, -2: guest not checked-in, -3: new room not found, -4: new room not ready, -5: same room selected
     */
    public int processRoomTransfer(String confirmationNumber, String newRoomNumber) {
        if (confirmationNumber == null || confirmationNumber.trim().isEmpty() || newRoomNumber == null)
            return -1;

        Guest guest = searchGuestByConfirmationNumber(confirmationNumber);
        if (guest == null)
            return -1;
        if (!guest.isCheckedIn())
            return -2;

        String oldRoomNo = guest.getAssignedRoomNumber();
        if (oldRoomNo != null && oldRoomNo.equalsIgnoreCase(newRoomNumber.trim())) {
            return -5; // Same room
        }

        Room newRoom = searchRoomByNumber(newRoomNumber);
        if (newRoom == null)
            return -3;

        if (!"Ready for Check-In".equalsIgnoreCase(newRoom.getRoomStatus())) {
            return -4; // New room not ready
        }

        Booking booking = findBookingByConfirmation(confirmationNumber);
        LocalDate transferEnd;
        try {
            transferEnd = booking != null ? LocalDate.parse(booking.getCheckOutDate())
                    : LocalDate.parse(guest.getCheckInDate()).plusDays(Math.max(1, guest.getNumberOfNights()));
        } catch (Exception e) {
            return -7;
        }
        if (hasBookingConflict(newRoomNumber.trim(), LocalDate.now(), transferEnd, confirmationNumber)) {
            return -6; // New room has a conflicting future reservation
        }

        // Release old room to Dirty for Housekeeping
        if (oldRoomNo != null) {
            Room oldRoom = searchRoomByNumber(oldRoomNo);
            if (oldRoom != null) {
                oldRoom.setRoomStatus("Dirty");
            }
        }

        // Occupy new room and update guest details (preserve original rate for upgrade benefit)
        newRoom.setRoomStatus("Occupied");
        guest.setAssignedRoomNumber(newRoomNumber.trim());
        guest.setRoomType(newRoom.getRoomType());
        if (booking != null) {
            booking.setRoomNumber(newRoom.getRoomNumber());
            booking.setRoomType(newRoom.getRoomType());
        }
        // Do NOT overwrite effectiveRoomRate — preserve original/upgrade pricing

        return 1;
    }

    // Suggest a room upgrade - find cheapest available room that costs more than
    // current
    public Room suggestRoomUpgrade(String currentRoomNo) {
        return suggestRoomUpgrade(currentRoomNo, null);
    }

    public Room suggestRoomUpgrade(String currentRoomNo, String confirmationNumber) {
        syncRoomTree();
        Room currentRoom = searchRoomByNumber(currentRoomNo);
        if (currentRoom == null)
            return null;

        ListInterface<Room> allRooms = roomTree.inOrderTraversal();
        Room bestUpgrade = null;
        Booking booking = confirmationNumber == null ? null : findBookingByConfirmation(confirmationNumber);
        LocalDate stayEnd = LocalDate.now().plusDays(1);
        try {
            if (booking != null) {
                stayEnd = LocalDate.parse(booking.getCheckOutDate());
            } else if (confirmationNumber != null) {
                Guest guest = searchGuestByConfirmationNumber(confirmationNumber);
                if (guest != null) {
                    stayEnd = LocalDate.now().plusDays(Math.max(1, guest.getNumberOfNights()));
                }
            }
        } catch (Exception ignored) {
            return null;
        }

        for (int i = 0; i < allRooms.getNumberOfEntries(); i++) {
            Room r = allRooms.get(i);
            if ("Ready for Check-In".equalsIgnoreCase(r.getRoomStatus()) && r.getPrice() > currentRoom.getPrice()
                    && !hasBookingConflict(r.getRoomNumber(), LocalDate.now(), stayEnd,
                            confirmationNumber == null ? "" : confirmationNumber)) {
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
        if (summary[0] == 0)
            return 0.0;
        return ((double) summary[2] / summary[0]) * 100.0; // summary[2] is Occupied count
    }

    public double calculateEstimatedDailyRevenue() {
        double totalRevenue = 0.0;
        ListInterface<Guest> guests = guestTree.inOrderTraversal();
        for (int i = 0; i < guests.getNumberOfEntries(); i++) {
            Guest guest = guests.get(i);
            if (!guest.isCheckedIn()) continue;
            Room room = searchRoomByNumber(guest.getAssignedRoomNumber());
            double rate = guest.getEffectiveRoomRate() > 0 ? guest.getEffectiveRoomRate()
                    : room != null ? room.getPrice() : 0.0;
            totalRevenue += rate * (1.0 - getDiscountPercentage(guest.getLoyaltyTier()));
        }
        return totalRevenue;
    }

    public double calculateAverageDailyRate() {
        int occupied = 0;
        double revenue = 0.0;
        ListInterface<Guest> guests = guestTree.inOrderTraversal();
        for (int i = 0; i < guests.getNumberOfEntries(); i++) {
            Guest guest = guests.get(i);
            if (!guest.isCheckedIn()) continue;
            Room room = searchRoomByNumber(guest.getAssignedRoomNumber());
            double rate = guest.getEffectiveRoomRate() > 0 ? guest.getEffectiveRoomRate()
                    : room != null ? room.getPrice() : 0.0;
            revenue += rate * (1.0 - getDiscountPercentage(guest.getLoyaltyTier()));
            occupied++;
        }
        return occupied == 0 ? 0.0 : revenue / occupied;
    }

    // Report 1: Room status summary
    // Index: [0]=Total, [1]=Ready, [2]=Occupied, [3]=Dirty, [4]=Cleaning, [5]=Inspected, [6]=Reserved
    public int[] getRoomStatusSummary() {
        syncRoomTree();
        int[] summary = new int[7]; // Total, Ready, Occupied, Dirty, Cleaning, Inspected, Reserved
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
            else if ("Inspected".equalsIgnoreCase(status))
                summary[5]++;
            else if ("Reserved".equalsIgnoreCase(status))
                summary[6]++;
        }
        return summary;
    }

    // Report 2: Filter rooms by status & max price, then sort by price
    public ListInterface<Room> getFilteredAndSortedRooms(String statusFilter, double maxPrice, boolean sortAsc) {
        return getFilteredAndSortedRooms(statusFilter, "ALL", 0.0, maxPrice, sortAsc);
    }

    public ListInterface<Room> getFilteredAndSortedRooms(String statusFilter, String roomTypeFilter,
            double minPrice, double maxPrice, boolean sortAsc) {
        syncRoomTree();
        ListInterface<Room> filtered = new MyArrayList<>();
        ListInterface<Room> rooms = roomTree.inOrderTraversal();

        for (int i = 0; i < rooms.getNumberOfEntries(); i++) {
            Room r = rooms.get(i);
            boolean statusOk = "ALL".equalsIgnoreCase(statusFilter) || (r.getRoomStatus() != null && r.getRoomStatus().equalsIgnoreCase(statusFilter));
            boolean typeOk = "ALL".equalsIgnoreCase(roomTypeFilter)
                    || (r.getRoomType() != null && r.getRoomType().equalsIgnoreCase(roomTypeFilter));
            boolean priceOk = r.getPrice() >= Math.max(0.0, minPrice)
                    && ((maxPrice <= 0) || (r.getPrice() <= maxPrice));

            if (statusOk && typeOk && priceOk) {
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
        return getFilteredAndSortedGuests(tierFilter, "ALL", minPoints, true);
    }

    public ListInterface<Guest> getFilteredAndSortedGuests(String tierFilter, String stayStatusFilter,
            int minPoints, boolean sortPointsDescending) {
        ListInterface<Guest> filtered = new MyArrayList<>();
        ListInterface<Guest> guests = guestTree.inOrderTraversal();

        for (int i = 0; i < guests.getNumberOfEntries(); i++) {
            Guest g = guests.get(i);
            boolean tierOk = "ALL".equalsIgnoreCase(tierFilter) || (g.getLoyaltyTier() != null && g.getLoyaltyTier().equalsIgnoreCase(tierFilter));
            boolean statusOk = "ALL".equalsIgnoreCase(stayStatusFilter)
                    || (g.getBookingStatus() != null && g.getBookingStatus().equalsIgnoreCase(stayStatusFilter));
            boolean pointsOk = g.getLoyaltyPoints() >= minPoints;

            if (tierOk && statusOk && pointsOk) {
                filtered.add(g);
            }
        }

        filtered.sort(new Comparator<Guest>() {
            @Override
            public int compare(Guest g1, Guest g2) {
                return sortPointsDescending
                        ? Integer.compare(g2.getLoyaltyPoints(), g1.getLoyaltyPoints())
                        : g1.getGuestName().compareToIgnoreCase(g2.getGuestName());
            }
        });

        return filtered;
    }
}
