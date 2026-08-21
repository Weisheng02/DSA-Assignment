package control;

import adt.BSTInterface;
import adt.ListInterface;
import entity.Booking;
import entity.Guest;
import entity.Room;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Author: Weisheng
 * Shared, stateless data-query and reservation rules used by the controllers.
 * Public controller methods remain the module boundary; this class prevents the
 * same cross-module rule from drifting into several different implementations.
 */
public final class ControllerDataSupport {

    enum StayValidation {
        VALID,
        INVALID_DATE,
        INVALID_NIGHTS,
        PAST_DATE,
        TOO_FAR_IN_ADVANCE
    }

    private ControllerDataSupport() {
    }

    static StayValidation validateStayPeriod(String checkInDate, int numberOfNights,
            int maxStayNights, int maxAdvanceDays) {
        if (checkInDate == null || checkInDate.trim().isEmpty())
            return StayValidation.INVALID_DATE;

        final LocalDate checkIn;
        try {
            checkIn = LocalDate.parse(checkInDate.trim());
        } catch (DateTimeParseException e) {
            return StayValidation.INVALID_DATE;
        }

        if (numberOfNights < 1 || numberOfNights > maxStayNights) {
            return StayValidation.INVALID_NIGHTS;
        }

        LocalDate today = LocalDate.now();
        if (checkIn.isBefore(today))
            return StayValidation.PAST_DATE;
        if (checkIn.isAfter(today.plusDays(maxAdvanceDays)))
            return StayValidation.TOO_FAR_IN_ADVANCE;
        return StayValidation.VALID;
    }

    static Guest findGuestByIc(ListInterface<Guest> guests, String icNo) {
        if (guests == null || isBlank(icNo))
            return null;
        String query = normalizeIdentity(icNo);
        for (int i = 0; i < guests.getNumberOfEntries(); i++) {
            Guest guest = guests.get(i);
            if (guest != null && query.equals(normalizeIdentity(guest.getIcNo())))
                return guest;
        }
        return null;
    }

    static Guest findGuestByConfirmation(BSTInterface<Guest> guests, String confirmationNumber) {
        if (guests == null || isBlank(confirmationNumber))
            return null;
        return guests.search(new Guest("", confirmationNumber.trim(), "", 0));
    }

    static Guest findGuestByConfirmation(ListInterface<Guest> guests, String confirmationNumber) {
        if (guests == null || isBlank(confirmationNumber))
            return null;
        String query = confirmationNumber.trim();
        for (int i = 0; i < guests.getNumberOfEntries(); i++) {
            Guest guest = guests.get(i);
            if (guest != null && query.equalsIgnoreCase(guest.getConfirmationNumber()))
                return guest;
        }
        return null;
    }

    static Room findRoomByNumber(ListInterface<Room> rooms, String roomNumber) {
        if (rooms == null || isBlank(roomNumber))
            return null;
        String query = roomNumber.trim();
        for (int i = 0; i < rooms.getNumberOfEntries(); i++) {
            Room room = rooms.get(i);
            if (room != null && query.equalsIgnoreCase(room.getRoomNumber()))
                return room;
        }
        return null;
    }

    static Room findRoomByNumber(BSTInterface<Room> rooms, String roomNumber) {
        if (rooms == null || isBlank(roomNumber))
            return null;
        return rooms.search(new Room(roomNumber.trim(), "", "", 0.0));
    }

    static Booking findBookingById(ListInterface<Booking> bookings, String bookingId) {
        if (bookings == null || isBlank(bookingId))
            return null;
        String query = bookingId.trim();
        for (int i = 0; i < bookings.getNumberOfEntries(); i++) {
            Booking booking = bookings.get(i);
            if (booking != null && query.equalsIgnoreCase(booking.getBookingId()))
                return booking;
        }
        return null;
    }

    static Booking findLatestBookingByConfirmation(ListInterface<Booking> bookings, String confirmationNumber) {
        if (bookings == null || isBlank(confirmationNumber))
            return null;
        String query = confirmationNumber.trim();
        for (int i = bookings.getNumberOfEntries() - 1; i >= 0; i--) {
            Booking booking = bookings.get(i);
            if (booking != null && query.equalsIgnoreCase(booking.getGuestConfirmationNumber()))
                return booking;
        }
        return null;
    }

    static boolean isActiveReservation(Booking booking) {
        if (booking == null)
            return false;
        return "Confirmed".equalsIgnoreCase(booking.getBookingStatus())
                || "CheckedIn".equalsIgnoreCase(booking.getBookingStatus());
    }

    static boolean hasBookingConflict(ListInterface<Booking> bookings, String roomNumber,
            LocalDate start, LocalDate end, String excludedConfirmationNumber) {
        if (bookings == null || isBlank(roomNumber) || start == null || end == null || !start.isBefore(end)) {
            return true;
        }

        String excludedConfirmation = excludedConfirmationNumber == null
                ? ""
                : excludedConfirmationNumber.trim();
        for (int i = 0; i < bookings.getNumberOfEntries(); i++) {
            Booking booking = bookings.get(i);
            if (booking == null || !isActiveReservation(booking)
                    || booking.getRoomNumber() == null
                    || !roomNumber.trim().equalsIgnoreCase(booking.getRoomNumber())
                    || (!excludedConfirmation.isEmpty()
                            && excludedConfirmation.equalsIgnoreCase(booking.getGuestConfirmationNumber()))) {
                continue;
            }

            try {
                LocalDate existingStart = LocalDate.parse(booking.getCheckInDate());
                LocalDate existingEnd = LocalDate.parse(booking.getCheckOutDate());
                if (periodsOverlap(start, end, existingStart, existingEnd))
                    return true;
            } catch (DateTimeParseException | NullPointerException e) {
                return true; // Corrupt active data must fail closed to prevent double booking.
            }
        }
        return false;
    }

    static boolean hasActiveGuestConflict(ListInterface<Guest> guests, String roomNumber,
            LocalDate start, LocalDate end, String excludedConfirmationNumber) {
        if (guests == null || isBlank(roomNumber) || start == null || end == null || !start.isBefore(end)) {
            return true;
        }

        String excludedConfirmation = excludedConfirmationNumber == null
                ? ""
                : excludedConfirmationNumber.trim();
        for (int i = 0; i < guests.getNumberOfEntries(); i++) {
            Guest guest = guests.get(i);
            if (guest == null || !guest.isCheckedIn() || guest.getAssignedRoomNumber() == null
                    || !roomNumber.trim().equalsIgnoreCase(guest.getAssignedRoomNumber())
                    || (!excludedConfirmation.isEmpty()
                            && excludedConfirmation.equalsIgnoreCase(guest.getConfirmationNumber()))) {
                continue;
            }

            try {
                LocalDate occupiedStart = LocalDate.parse(guest.getCheckInDate());
                LocalDate occupiedEnd = occupiedStart.plusDays(Math.max(1, guest.getNumberOfNights()));
                if (periodsOverlap(start, end, occupiedStart, occupiedEnd))
                    return true;
            } catch (DateTimeParseException | NullPointerException e) {
                return true;
            }
        }
        return false;
    }

    static boolean isRoomAvailableForStay(Room room, LocalDate start, LocalDate end,
            ListInterface<Booking> bookings, ListInterface<Guest> guests, Booking excludedBooking) {
        if (room == null || start == null || end == null || !start.isBefore(end))
            return false;

        String status = room.getRoomStatus();
        if ("Maintenance".equalsIgnoreCase(status) || "Out of Service".equalsIgnoreCase(status))
            return false;

        if (start.equals(LocalDate.now())) {
            boolean ready = "Ready for Check-In".equalsIgnoreCase(status);
            boolean ownReservation = excludedBooking != null
                    && "Reserved".equalsIgnoreCase(status)
                    && room.getRoomNumber().equalsIgnoreCase(excludedBooking.getRoomNumber());
            if (!ready && !ownReservation)
                return false;
        }

        String excludedConfirmation = excludedBooking == null
                ? ""
                : excludedBooking.getGuestConfirmationNumber();
        return !hasBookingConflict(bookings, room.getRoomNumber(), start, end, excludedConfirmation)
                && !hasActiveGuestConflict(guests, room.getRoomNumber(), start, end,
                        excludedConfirmation);
    }

    private static boolean periodsOverlap(LocalDate firstStart, LocalDate firstEnd,
            LocalDate secondStart, LocalDate secondEnd) {
        return firstStart.isBefore(secondEnd) && secondStart.isBefore(firstEnd);
    }

    static String normalizeIdentity(String value) {
        return value == null ? "" : value.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
