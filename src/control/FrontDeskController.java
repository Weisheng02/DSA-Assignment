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
 * Author: Yeap Wei Sheng
 * Controller for Front-Desk Operations. Uses BST for guest and room management.
 */
public class FrontDeskController {
    private static final int MAX_STAY_NIGHTS = 30;
    private static final int MAX_ADVANCE_BOOKING_DAYS = 365;

    private BSTInterface<Guest> guestTree;
    private BSTInterface<Room> roomTree;

    private ListInterface<Room> sharedRoomList;
    private ListInterface<Booking> sharedBookingList;
    private LoyaltyController loyaltyController;
    private int syncedRoomCount = -1;
    private int nextGuestConfirmationNumber;

    /** Result of a Front Desk registration with a system-generated key. */
    public static class GuestRegistrationResult {
        private final int status;
        private final Guest guest;

        private GuestRegistrationResult(int status, Guest guest) {
            this.status = status;
            this.guest = guest;
        }

        public int getStatus() {
            return status;
        }

        public Guest getGuest() {
            return guest;
        }
    }

    /**
     * Billing calculation result returned to the boundary for display and checkout.
     */
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

        public Guest getGuest() {
            return guest;
        }

        public Room getRoom() {
            return room;
        }

        public Booking getBooking() {
            return booking;
        }

        public int getNights() {
            return nights;
        }

        public double getChargedRate() {
            return chargedRate;
        }

        public double getDiscountRate() {
            return discountRate;
        }

        public double getSubtotal() {
            return subtotal;
        }

        public double getDiscountAmount() {
            return discountAmount;
        }

        public double getTotal() {
            return total;
        }

        public int getProjectedPoints() {
            return (int) (subtotal / 10.0);
        }
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

        public int getStatus() {
            return status;
        }

        public BillingDetails getBill() {
            return bill;
        }

        public int getEarnedPoints() {
            return earnedPoints;
        }
    }

    public FrontDeskController(BSTInterface<Guest> masterGuestTree, ListInterface<Room> sharedRoomList,
            ListInterface<Booking> sharedBookingList, LoyaltyController loyaltyController) {
        this.guestTree = (masterGuestTree != null) ? masterGuestTree : new BinarySearchTree<>();
        this.roomTree = new BinarySearchTree<>();
        this.sharedRoomList = sharedRoomList;
        this.sharedBookingList = (sharedBookingList != null) ? sharedBookingList : new MyArrayList<>();
        initializeNextGuestConfirmationNumber();
        syncRoomTree();
        this.loyaltyController = (loyaltyController != null)
                ? loyaltyController
                : new LoyaltyController(this.guestTree);
    }

    private void initializeNextGuestConfirmationNumber() {
        int highest = 10000000;
        ListInterface<Guest> guests = guestTree.inOrderTraversal();
        for (int i = 0; i < guests.getNumberOfEntries(); i++) {
            String confirmation = guests.get(i).getConfirmationNumber();
            if (confirmation != null && confirmation.matches("\\d{8}"))
                highest = Math.max(highest, Integer.parseInt(confirmation));
        }
        nextGuestConfirmationNumber = highest + 1;
    }

    private void syncRoomTree() {
        if (sharedRoomList == null || syncedRoomCount == sharedRoomList.getNumberOfEntries())
            return;

        // Room objects are shared references, so status/price/type changes are
        // already visible inside the tree. Rebuild only when the list structure
        // changes. Keep the insertion-order shape so the diagnostic menu can
        // demonstrate the visible effect of an explicit BST rebalance.
        roomTree.clear();
        for (int i = 0; i < sharedRoomList.getNumberOfEntries(); i++)
            roomTree.add(sharedRoomList.get(i));
        syncedRoomCount = sharedRoomList.getNumberOfEntries();
    }

    // Search guest stay record by IC Number - Non-Key Linear Traversal O(n)
    public Guest searchGuestByIC(String icNo) {
        return ControllerDataSupport.findGuestByIc(guestTree.inOrderTraversal(), icNo);
    }

    /** Returns every stay record belonging to the same IC/passport identity. */
    public Guest[] searchGuestsByICArray(String icNo) {
        ListInterface<Guest> matches = new MyArrayList<>();
        if (icNo == null || icNo.trim().isEmpty())
            return new Guest[0];
        ListInterface<Guest> guests = guestTree.inOrderTraversal();
        for (int i = 0; i < guests.getNumberOfEntries(); i++) {
            Guest guest = guests.get(i);
            if (guest.getIcNo() != null && guest.getIcNo().equalsIgnoreCase(icNo.trim()))
                matches.add(guest);
        }
        Guest[] result = new Guest[matches.getNumberOfEntries()];
        for (int i = 0; i < matches.getNumberOfEntries(); i++)
            result[i] = matches.get(i);
        return result;
    }

    // Search guest stay record by Confirmation Number (BST Primary Key) - O(log n)
    // BST Search
    public Guest searchGuestByConfirmationNumber(String confirmNo) {
        return ControllerDataSupport.findGuestByConfirmation(guestTree, confirmNo);
    }

    // Search guest stay records within a range of Confirmation Numbers - O(log n +
    // k) BST Range Search
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

    public Guest[] searchGuestsByConfirmationRangeArray(String startNo, String endNo) {
        ListInterface<Guest> guests = searchGuestsByConfirmationRange(startNo, endNo);
        Guest[] result = new Guest[guests.getNumberOfEntries()];
        for (int i = 0; i < guests.getNumberOfEntries(); i++)
            result[i] = guests.get(i);
        return result;
    }

    /** Registers a guest from boundary primitives without exposing entity types. */
    public boolean registerGuest(String name, String icNo, String phoneNumber, String confirmationNumber) {
        return registerGuestWithStatus(name, icNo, phoneNumber, confirmationNumber) == 1;
    }

    /**
     * Registers a guest and returns a reason code for the boundary to present.
     * Return: 1 success, -1 invalid data, -2 duplicate confirmation,
     * -3 duplicate IC/passport.
     */
    public int registerGuestWithStatus(String name, String icNo, String phoneNumber, String confirmationNumber) {
        Guest guest = new Guest(name, icNo, phoneNumber, confirmationNumber, "Standard", 0);
        if (guest.getConfirmationNumber() == null || !guest.getConfirmationNumber().matches("\\d{8}")
                || isMissingRequired(guest.getGuestName()) || isMissingRequired(guest.getIcNo())
                || !isValidOptionalPhone(guest.getPhoneNumber()))
            return -1;
        if (guestTree.contains(guest))
            return -2;
        if (searchGuestByIC(guest.getIcNo()) != null)
            return -3;
        return addValidatedGuest(guest) ? 1 : -1;
    }

    /**
     * Registers a guest while keeping confirmation-number generation inside the
     * controller. Return status: 1 success, -1 invalid data, -3 duplicate
     * IC/passport, -4 no 8-digit confirmation number remains available.
     */
    public GuestRegistrationResult registerGuestWithGeneratedConfirmation(
            String name, String icNo, String phoneNumber) {
        if (isMissingRequired(name) || isMissingRequired(icNo) || !isValidOptionalPhone(phoneNumber))
            return new GuestRegistrationResult(-1, null);
        if (searchGuestByIC(icNo) != null)
            return new GuestRegistrationResult(-3, null);

        while (nextGuestConfirmationNumber <= 99999999
                && searchGuestByConfirmationNumber(String.format("%08d", nextGuestConfirmationNumber)) != null)
            nextGuestConfirmationNumber++;
        if (nextGuestConfirmationNumber > 99999999)
            return new GuestRegistrationResult(-4, null);

        String confirmation = String.format("%08d", nextGuestConfirmationNumber++);
        Guest guest = new Guest(name, icNo, phoneNumber, confirmation, "Standard", 0);
        return addValidatedGuest(guest)
                ? new GuestRegistrationResult(1, guest)
                : new GuestRegistrationResult(-1, null);
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
        return addValidatedGuest(guest);
    }

    private boolean addValidatedGuest(Guest guest) {
        return guestTree.add(guest);
    }

    /**
     * Updates editable guest-profile fields while preserving the immutable BST key.
     * Return: 1 success, -1 guest missing, -2 invalid required data,
     * -3 duplicate IC/passport, -4 invalid email, -5 invalid phone,
     * -6 loyalty identity migration failed.
     */
    public int updateGuestProfile(String confirmationNumber, String name, String icNo,
            String phone, String gender, String nationality, String email, String specialRequest) {
        Guest guest = searchGuestByConfirmationNumber(confirmationNumber);
        if (guest == null)
            return -1;
        if (isMissingRequired(name) || isMissingRequired(icNo))
            return -2;

        boolean identityUnchanged = ControllerDataSupport.normalizeIdentity(guest.getIcNo())
                .equals(ControllerDataSupport.normalizeIdentity(icNo));
        Guest sameIdentity = searchGuestByIC(icNo);
        if (sameIdentity != null && sameIdentity != guest && !identityUnchanged)
            return -3;
        if (!isBlank(email) && !"N/A".equalsIgnoreCase(email.trim())
                && !email.trim().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))
            return -4;
        if (!isValidOptionalPhone(phone))
            return -5;

        if (!identityUnchanged && !loyaltyController.migrateMemberIdentity(guest, icNo.trim()))
            return -6;

        guest.setGuestName(name.trim());
        guest.setIcNo(icNo.trim());
        guest.setPhoneNumber(normalizeOptional(phone));
        guest.setGender(normalizeOptional(gender));
        guest.setNationality(isBlank(nationality) ? "N/A" : nationality.trim());
        guest.setEmail(normalizeOptional(email));
        guest.setSpecialRequest(isBlank(specialRequest) || "N/A".equalsIgnoreCase(specialRequest.trim())
                ? null
                : specialRequest.trim());

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
        if (guest == null)
            return false;
        String normalized = isBlank(specialRequest) ? null : specialRequest.trim();
        guest.setSpecialRequest(normalized);
        Booking booking = findBookingByConfirmation(confirmationNumber);
        if (booking != null)
            booking.setSpecialRequest(normalized);
        return true;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private boolean isMissingRequired(String value) {
        return isBlank(value) || "N/A".equalsIgnoreCase(value.trim());
    }

    private boolean isValidOptionalPhone(String phone) {
        if (isBlank(phone) || "N/A".equalsIgnoreCase(phone.trim()))
            return true;
        String digits = phone.replaceAll("\\D", "");
        return digits.length() >= 7 && digits.length() <= 15
                && phone.matches("^[+()0-9 .-]+$");
    }

    private String normalizeOptional(String value) {
        return isBlank(value) ? "N/A" : value.trim();
    }

    // Remove a guest from the BST
    public Guest removeGuest(String confirmNo) {
        if (confirmNo == null || confirmNo.trim().isEmpty() || !canRemoveGuest(confirmNo))
            return null;
        Guest dummy = new Guest("", confirmNo.trim(), "", 0);
        return guestTree.remove(dummy);
    }

    /** Only a newly registered profile with no booking/history may be removed. */
    public boolean canRemoveGuest(String confirmationNumber) {
        Guest guest = searchGuestByConfirmationNumber(confirmationNumber);
        return guest != null
                && "Registered".equalsIgnoreCase(guest.getBookingStatus())
                && findBookingByConfirmation(confirmationNumber) == null;
    }

    private Booking findBookingByConfirmation(String confirmationNumber) {
        return ControllerDataSupport.findLatestBookingByConfirmation(sharedBookingList, confirmationNumber);
    }

    public Booking getBookingByConfirmation(String confirmationNumber) {
        return findBookingByConfirmation(confirmationNumber);
    }

    /** Returns only complete, confirmed reservations scheduled to arrive today. */
    public ListInterface<Guest> getTodaysReservedGuests() {
        ListInterface<Guest> arrivals = new MyArrayList<>();
        LocalDate today = LocalDate.now();
        ListInterface<Guest> guests = guestTree.inOrderTraversal();

        for (int i = 0; i < guests.getNumberOfEntries(); i++) {
            Guest guest = guests.get(i);
            if (guest == null || !guest.isReserved())
                continue;

            Booking booking = findBookingByConfirmation(guest.getConfirmationNumber());
            if (booking == null || !"Confirmed".equalsIgnoreCase(booking.getBookingStatus()))
                continue;

            try {
                if (today.equals(LocalDate.parse(booking.getCheckInDate())))
                    arrivals.add(guest);
            } catch (DateTimeParseException ignored) {
                // Invalid booking dates are excluded and rejected by processCheckIn.
            }
        }
        return arrivals;
    }

    public Guest[] getTodaysReservedGuestArray() {
        ListInterface<Guest> guests = getTodaysReservedGuests();
        Guest[] result = new Guest[guests.getNumberOfEntries()];
        for (int i = 0; i < guests.getNumberOfEntries(); i++)
            result[i] = guests.get(i);
        return result;
    }

    public Booking searchBookingById(String bookingId) {
        return ControllerDataSupport.findBookingById(sharedBookingList, bookingId);
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

    public Guest[] searchGuestsByNameArray(String nameQuery) {
        ListInterface<Guest> guests = searchGuestsByName(nameQuery);
        Guest[] result = new Guest[guests.getNumberOfEntries()];
        for (int i = 0; i < guests.getNumberOfEntries(); i++)
            result[i] = guests.get(i);
        return result;
    }

    /** Returns the complete Master Guest Registry in BST key order. */
    public Guest[] getAllGuestArray() {
        ListInterface<Guest> guests = guestTree.inOrderTraversal();
        Guest[] result = new Guest[guests.getNumberOfEntries()];
        for (int i = 0; i < guests.getNumberOfEntries(); i++)
            result[i] = guests.get(i);
        return result;
    }

    // Search room by room number using BST
    public Room searchRoomByNumber(String roomNumber) {
        syncRoomTree();
        return ControllerDataSupport.findRoomByNumber(roomTree, roomNumber);
    }

    public ListInterface<Room> getAllRooms() {
        syncRoomTree();
        return roomTree.inOrderTraversal();
    }

    /** Validates Front Desk room-availability search input. */
    public int validateStayPeriod(String checkInDate, int numberOfNights) {
        ControllerDataSupport.StayValidation result = ControllerDataSupport.validateStayPeriod(
                checkInDate, numberOfNights, MAX_STAY_NIGHTS, MAX_ADVANCE_BOOKING_DAYS);
        switch (result) {
            case INVALID_DATE:
                return -1;
            case INVALID_NIGHTS:
                return -2;
            case PAST_DATE:
                return -3;
            case TOO_FAR_IN_ADVANCE:
                return -4;
            default:
                return 1;
        }
    }

    /**
     * Searches rooms by date range, room type and budget, then sorts by price.
     * This uses the shared booking schedule rather than only today's physical
     * status.
     */
    public ListInterface<Room> searchAvailableRooms(String checkInDate, int numberOfNights,
            String roomTypeFilter, double maxPrice, boolean sortAscending) {
        ListInterface<Room> result = new MyArrayList<>();
        if (validateStayPeriod(checkInDate, numberOfNights) != 1 || maxPrice < 0)
            return result;
        syncRoomTree();
        LocalDate start = LocalDate.parse(checkInDate.trim());
        LocalDate end = start.plusDays(numberOfNights);
        ListInterface<Room> rooms = roomTree.inOrderTraversal();
        for (int i = 0; i < rooms.getNumberOfEntries(); i++) {
            Room room = rooms.get(i);
            boolean typeMatches = isBlank(roomTypeFilter) || "ALL".equalsIgnoreCase(roomTypeFilter.trim())
                    || room.getRoomType().equalsIgnoreCase(roomTypeFilter.trim());
            boolean budgetMatches = maxPrice == 0 || room.getPrice() <= maxPrice;
            if (!typeMatches || !budgetMatches || !isRoomSellableForPeriod(room, start, end))
                continue;
            result.add(room);
        }
        result.sort((left, right) -> sortAscending
                ? Double.compare(left.getPrice(), right.getPrice())
                : Double.compare(right.getPrice(), left.getPrice()));
        return result;
    }

    public Room[] searchAvailableRoomArray(String checkInDate, int numberOfNights,
            String roomTypeFilter, double maxPrice, boolean sortAscending) {
        ListInterface<Room> rooms = searchAvailableRooms(checkInDate, numberOfNights,
                roomTypeFilter, maxPrice, sortAscending);
        Room[] result = new Room[rooms.getNumberOfEntries()];
        for (int i = 0; i < rooms.getNumberOfEntries(); i++)
            result[i] = rooms.get(i);
        return result;
    }

    private boolean isRoomSellableForPeriod(Room room, LocalDate start, LocalDate end) {
        return ControllerDataSupport.isRoomAvailableForStay(room, start, end,
                sharedBookingList, guestTree.inOrderTraversal(), null);
    }

    /** Stores the intended duration of an unbooked walk-in before check-in. */
    public int setWalkInStayLength(String confirmationNumber, int numberOfNights) {
        Guest guest = searchGuestByConfirmationNumber(confirmationNumber);
        if (guest == null)
            return -1;
        if (numberOfNights < 1 || numberOfNights > 30)
            return -2;
        if (findBookingByConfirmation(confirmationNumber) != null)
            return -3;
        if (guest.isCheckedIn() || guest.isCheckedOut() || guest.isCancelled())
            return -4;
        guest.setCheckInDate(LocalDate.now().toString());
        guest.setNumberOfNights(numberOfNights);
        return 1;
    }

    public boolean isGuestCheckedIn(String confirmationNumber) {
        if (confirmationNumber == null || confirmationNumber.trim().isEmpty())
            return false;
        Guest guest = searchGuestByConfirmationNumber(confirmationNumber);
        return guest != null && guest.isCheckedIn();
    }

    // Process check-in: only a complete Reserved booking scheduled for today is
    // eligible.
    public int processCheckIn(String confirmationNumber, String roomNumber) {
        Guest guest = searchGuestByConfirmationNumber(confirmationNumber);
        String selectedRoom = isBlank(roomNumber) && guest != null
                ? guest.getAssignedRoomNumber() : roomNumber;
        Room r = searchRoomByNumber(selectedRoom);
        double price = guest != null && guest.getEffectiveRoomRate() > 0
                ? guest.getEffectiveRoomRate()
                : (r != null ? r.getPrice() : 0.0);
        return processCheckIn(confirmationNumber, selectedRoom, price);
    }

    /** Completes the check-in and stores its request as one controller workflow. */
    public int processCheckIn(String confirmationNumber, String roomNumber, String specialRequest) {
        int result = processCheckIn(confirmationNumber, roomNumber);
        if (result == 1 && !isBlank(specialRequest))
            updateSpecialRequest(confirmationNumber, specialRequest);
        return result;
    }

    /** Returns the rate that must be preserved when this guest checks in. */
    public double getCheckInRate(String confirmationNumber, String roomNumber) {
        Guest guest = searchGuestByConfirmationNumber(confirmationNumber);
        if (guest != null && guest.getEffectiveRoomRate() > 0)
            return guest.getEffectiveRoomRate();
        String selectedRoom = isBlank(roomNumber) && guest != null
                ? guest.getAssignedRoomNumber() : roomNumber;
        Room room = searchRoomByNumber(selectedRoom);
        return room == null ? 0.0 : room.getPrice();
    }

    public int processCheckIn(String confirmationNumber, String roomNumber, double baseRoomPrice) {
        syncRoomTree();
        Guest guest = searchGuestByConfirmationNumber(confirmationNumber);
        if (guest == null)
            return -1;

        // Guest.bookingStatus is the single source of truth for check-in state.
        if (guest.isCheckedIn()) {
            return -4;
        }

        if ("CheckedOut".equalsIgnoreCase(guest.getBookingStatus())) {
            return -6; // Guest already checked out for this stay
        }

        if ("Cancelled".equalsIgnoreCase(guest.getBookingStatus())) {
            return -7; // Booking was cancelled
        }

        if ("NoShow".equalsIgnoreCase(guest.getBookingStatus()))
            return -8;

        if (!guest.isReserved())
            return -12;

        LocalDate today = LocalDate.now();
        Booking booking = findBookingByConfirmation(confirmationNumber);
        if (booking == null)
            return -13;
        if ("NoShow".equalsIgnoreCase(booking.getBookingStatus()))
            return -8;
        if (!"Confirmed".equalsIgnoreCase(booking.getBookingStatus()))
            return -13;
        // Validate the complete scheduled stay before making any state change. In
        // particular, a malformed checkout date must be reported as the same
        // controlled date error as a malformed check-in date.
        LocalDate scheduledArrival;
        LocalDate bookingStayEnd;
        try {
            scheduledArrival = LocalDate.parse(booking.getCheckInDate());
            bookingStayEnd = LocalDate.parse(booking.getCheckOutDate());
        } catch (DateTimeParseException | NullPointerException ignored) {
            return -11;
        }
        if (today.isBefore(scheduledArrival))
            return -9; // Too early
        if (today.isAfter(scheduledArrival)) {
            booking.recordNoShow();
            guest.setBookingStatus("NoShow");
            return -10; // Scheduled arrival date has passed
        }

        Room room = searchRoomByNumber(roomNumber);
        if (room == null)
            return -2;

        String currentStatus = room.getRoomStatus();
        if (!"Ready for Check-In".equalsIgnoreCase(currentStatus) && !"Reserved".equalsIgnoreCase(currentStatus)) {
            return -3;
        }

        LocalDate stayEnd = booking != null ? bookingStayEnd
                : today.plusDays(Math.max(1, guest.getNumberOfNights()));
        if (hasBookingConflict(roomNumber.trim(), today, stayEnd, confirmationNumber))
            return -5;

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
        return 1;
    }

    private boolean hasBookingConflict(String roomNumber, LocalDate start, LocalDate end,
            String excludedConfirmationNumber) {
        return ControllerDataSupport.hasBookingConflict(sharedBookingList, roomNumber,
                start, end, excludedConfirmationNumber);
    }

    /**
     * Shared Booking records can contain consecutive, non-overlapping stays for
     * the same room. Do not release a Reserved room while another active stay
     * still refers to it.
     */
    private boolean hasAnotherActiveBookingForRoom(String roomNumber, String excludedConfirmationNumber) {
        if (roomNumber == null)
            return false;
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
     * Sets room status to "Dirty" for Housekeeping and updates the shared Guest
     * and Booking lifecycle state.
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

        return 1;
    }

    /** Calculates a consistent receipt without mutating guest or room state. */
    public BillingDetails calculateBill(String confirmationNumber) {
        Guest guest = searchGuestByConfirmationNumber(confirmationNumber);
        if (guest == null || !guest.isCheckedIn())
            return null;
        Room room = searchRoomByNumber(guest.getAssignedRoomNumber());
        if (room == null)
            return null;
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

    /**
     * Performs checkout first, then delegates the single reward mutation to
     * Loyalty.
     */
    public CheckoutResult completeCheckOutAndReward(String confirmationNumber) {
        BillingDetails bill = calculateBill(confirmationNumber);
        if (bill == null)
            return new CheckoutResult(-1, null, 0);
        Guest guest = bill.getGuest();
        int earnedPoints = bill.getProjectedPoints();
        String sourceReference = bill.getBooking() != null
                ? bill.getBooking().getBookingId()
                : "CHECKOUT-" + guest.getConfirmationNumber();
        LoyaltyController.AwardResult validation = loyaltyController.validateCheckoutAward(
                guest.getConfirmationNumber(), sourceReference, earnedPoints);
        if (!validation.isSuccess())
            return new CheckoutResult(-8, bill, 0);

        int status = processCheckOut(confirmationNumber);
        if (status != 1)
            return new CheckoutResult(status, bill, 0);

        LoyaltyController.AwardResult award = loyaltyController.awardCheckoutPoints(
                guest.getConfirmationNumber(), sourceReference, earnedPoints);
        if (!award.isSuccess())
            return new CheckoutResult(-9, bill, 0);
        return new CheckoutResult(1, bill, award.getPointsAwarded());
    }

    /**
     * Extends an active stay after verifying the room is not promised to another
     * guest during the additional dates. Return: 1 success, -1 guest missing,
     * -2 not checked in, -3 invalid stay date, -4 date conflict, -5 invalid/max
     * stay.
     */
    public int extendStay(String confirmationNumber, int additionalNights) {
        Guest guest = searchGuestByConfirmationNumber(confirmationNumber);
        if (guest == null)
            return -1;
        if (!guest.isCheckedIn())
            return -2;
        Booking booking = findBookingByConfirmation(confirmationNumber);
        int currentNights = booking != null ? booking.getNumberOfNights() : guest.getNumberOfNights();
        if (additionalNights <= 0 || currentNights + additionalNights > 30)
            return -5;

        LocalDate oldDeparture;
        try {
            oldDeparture = booking != null ? LocalDate.parse(booking.getCheckOutDate())
                    : LocalDate.parse(guest.getCheckInDate()).plusDays(currentNights);
        } catch (Exception e) {
            return -3;
        }
        LocalDate newDeparture = oldDeparture.plusDays(additionalNights);
        String roomNumber = booking != null ? booking.getRoomNumber() : guest.getAssignedRoomNumber();
        if (roomNumber == null || roomNumber.trim().isEmpty())
            return -3;
        if (hasBookingConflict(roomNumber, oldDeparture, newDeparture, confirmationNumber))
            return -4;

        if (booking != null)
            booking.setNumberOfNights(currentNights + additionalNights);
        guest.setNumberOfNights(currentNights + additionalNights);
        return 1;
    }

    /**
     * Process Room Transfer (Change Room mid-stay):
     * Releases old room to "Dirty" for Housekeeping, sets new room to "Occupied",
     * and updates guest's assigned room and effective rate.
     * 
     * @return 1: success, -1: guest not found, -2: guest not checked-in, -3: new
     *         room not found, -4: new room not ready, -5: same room selected
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

        // Occupy new room and update guest details (preserve original rate for upgrade
        // benefit)
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

    /**
     * Returns only rooms that can accept this guest for the remainder of the
     * active stay. Historical room references on checked-out guests are ignored.
     */
    public Room[] getAvailableTransferRoomArray(String confirmationNumber) {
        Guest guest = searchGuestByConfirmationNumber(confirmationNumber);
        if (guest == null || !guest.isCheckedIn())
            return new Room[0];
        Booking booking = findBookingByConfirmation(confirmationNumber);
        LocalDate transferEnd;
        try {
            transferEnd = booking != null ? LocalDate.parse(booking.getCheckOutDate())
                    : LocalDate.parse(guest.getCheckInDate()).plusDays(Math.max(1, guest.getNumberOfNights()));
        } catch (Exception e) {
            return new Room[0];
        }

        syncRoomTree();
        ListInterface<Room> available = new MyArrayList<>();
        ListInterface<Room> rooms = roomTree.inOrderTraversal();
        for (int i = 0; i < rooms.getNumberOfEntries(); i++) {
            Room room = rooms.get(i);
            if (!room.getRoomNumber().equalsIgnoreCase(guest.getAssignedRoomNumber())
                    && "Ready for Check-In".equalsIgnoreCase(room.getRoomStatus())
                    && !hasBookingConflict(room.getRoomNumber(), LocalDate.now(), transferEnd,
                            confirmationNumber)) {
                available.add(room);
            }
        }
        Room[] result = new Room[available.getNumberOfEntries()];
        for (int i = 0; i < result.length; i++)
            result[i] = available.get(i);
        return result;
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

    public boolean isRoomTreeBalanced() {
        syncRoomTree();
        return roomTree.isBalanced();
    }

    /** Raw ADT structure; the boundary supplies the console title. */
    public String getGuestTreeStructure() {
        return guestTree.getTopDownTreeDisplayText(guest -> "[" + guest.getConfirmationNumber() + "]");
    }

    /** Raw ADT structure; the boundary supplies the console title. */
    public String getRoomTreeStructure() {
        syncRoomTree();
        return roomTree.getTopDownTreeDisplayText(room -> "[" + room.getRoomNumber() + "]");
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

    public Guest[] getGuestTraversalArray(int mode) {
        ListInterface<Guest> guests = getGuestTraversal(mode);
        Guest[] result = new Guest[guests.getNumberOfEntries()];
        for (int i = 0; i < guests.getNumberOfEntries(); i++)
            result[i] = guests.get(i);
        return result;
    }

    /** Number of nodes, height and leaf count for boundary diagnostics. */
    public int[] getGuestTreeStatistics() {
        return new int[] { guestTree.getNumberOfEntries(), guestTree.getHeight(), guestTree.getLeafCount() };
    }

    /** Number of nodes, height and leaf count for the room-tree diagnostics. */
    public int[] getRoomTreeStatistics() {
        syncRoomTree();
        return new int[] { roomTree.getNumberOfEntries(), roomTree.getHeight(), roomTree.getLeafCount() };
    }

    public Guest getSmallestGuest() {
        return guestTree.getMin();
    }

    public Guest getLargestGuest() {
        return guestTree.getMax();
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
            if (!guest.isCheckedIn())
                continue;
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
            if (!guest.isCheckedIn())
                continue;
            Room room = searchRoomByNumber(guest.getAssignedRoomNumber());
            double rate = guest.getEffectiveRoomRate() > 0 ? guest.getEffectiveRoomRate()
                    : room != null ? room.getPrice() : 0.0;
            revenue += rate * (1.0 - getDiscountPercentage(guest.getLoyaltyTier()));
            occupied++;
        }
        return occupied == 0 ? 0.0 : revenue / occupied;
    }

    // Report 1: Room status summary
    // Index: [0]=Total, [1]=Ready, [2]=Occupied, [3]=Dirty, [4]=Cleaning,
    // [5]=Inspected, [6]=Reserved, [7]=Maintenance
    public int[] getRoomStatusSummary() {
        syncRoomTree();
        int[] summary = new int[8];
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
            else if ("Maintenance".equalsIgnoreCase(status))
                summary[7]++;
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
            boolean statusOk = "ALL".equalsIgnoreCase(statusFilter)
                    || (r.getRoomStatus() != null && r.getRoomStatus().equalsIgnoreCase(statusFilter));
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

    public Room[] getFilteredAndSortedRoomArray(String statusFilter, String roomTypeFilter,
            double minPrice, double maxPrice, boolean sortAsc) {
        ListInterface<Room> rooms = getFilteredAndSortedRooms(statusFilter, roomTypeFilter,
                minPrice, maxPrice, sortAsc);
        Room[] result = new Room[rooms.getNumberOfEntries()];
        for (int i = 0; i < rooms.getNumberOfEntries(); i++)
            result[i] = rooms.get(i);
        return result;
    }

    /** Returns lowest, average and highest price for an already-filtered result. */
    public double[] calculateRoomPriceSummary(Room[] rooms) {
        if (rooms == null || rooms.length == 0)
            return new double[] { 0.0, 0.0, 0.0 };
        double total = 0.0;
        double lowest = rooms[0].getPrice();
        double highest = rooms[0].getPrice();
        for (Room room : rooms) {
            total += room.getPrice();
            lowest = Math.min(lowest, room.getPrice());
            highest = Math.max(highest, room.getPrice());
        }
        return new double[] { lowest, total / rooms.length, highest };
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
            boolean tierOk = "ALL".equalsIgnoreCase(tierFilter)
                    || (g.getLoyaltyTier() != null && g.getLoyaltyTier().equalsIgnoreCase(tierFilter));
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

    public Guest[] getFilteredAndSortedGuestArray(String tierFilter, String stayStatusFilter,
            int minPoints, boolean sortPointsDescending) {
        ListInterface<Guest> guests = getFilteredAndSortedGuests(tierFilter, stayStatusFilter,
                minPoints, sortPointsDescending);
        Guest[] result = new Guest[guests.getNumberOfEntries()];
        for (int i = 0; i < guests.getNumberOfEntries(); i++)
            result[i] = guests.get(i);
        return result;
    }

    /** Returns total and average points for an already-filtered result. */
    public double[] calculateGuestPointSummary(Guest[] guests) {
        if (guests == null || guests.length == 0)
            return new double[] { 0.0, 0.0 };
        int total = 0;
        for (Guest guest : guests)
            total += guest.getLoyaltyPoints();
        return new double[] { total, (double) total / guests.length };
    }

    /**
     * Current editable values used by the boundary to implement "press Enter to
     * keep".
     */
    public String[] getGuestProfileFields(String confirmationNumber) {
        Guest guest = searchGuestByConfirmationNumber(confirmationNumber);
        if (guest == null)
            return null;
        return new String[] { valueOrEmpty(guest.getGuestName()), valueOrEmpty(guest.getIcNo()),
                valueOrEmpty(guest.getPhoneNumber()), valueOrEmpty(guest.getGender()),
                valueOrEmpty(guest.getNationality()), valueOrEmpty(guest.getEmail()),
                valueOrEmpty(guest.getSpecialRequest()) };
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }
}
