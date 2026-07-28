package control;

import adt.ListInterface;
import adt.MyArrayList;
import entity.Booking;

/**
 * Placeholder Control Class for Member 1 (Walk-In Registrations & Booking Module)
 */
public class BookingController {
    private ListInterface<Booking> bookingList;

    public BookingController() {
        bookingList = new MyArrayList<>();
    }

    public ListInterface<Booking> getAllBookings() {
        return bookingList;
    }
}
