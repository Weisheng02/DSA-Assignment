package data;

import adt.BSTInterface;
import adt.ListInterface;
import entity.Booking;
import entity.Guest;
import entity.Room;
import java.time.LocalDate;

/** Creates the shared in-memory demonstration data used by App. */
public final class ResortDataSeeder {

    private ResortDataSeeder() {
    }

    /** Seeds one coherent set of rooms, guest stays, and matching bookings. */
    public static void seed(BSTInterface<Guest> guestTree, ListInterface<Room> roomList,
            ListInterface<Booking> bookingList) {
        if (guestTree == null || roomList == null || bookingList == null)
            throw new IllegalArgumentException("Shared master collections are required.");
        if (!guestTree.isEmpty() || !roomList.isEmpty() || !bookingList.isEmpty())
            return;

        LocalDate today = LocalDate.now();
        seedDefaultRooms(roomList);

        Guest alice = new Guest("Alice Tan", "980101-14-5566", "+60 12-345 6789", "Female",
                "Malaysian", "alice.tan@example.com", "10000001", "Platinum", 1200);
        alice.setCheckedIn(true);
        alice.setAssignedRoomNumber("104");
        alice.setEffectiveRoomRate(350.00);
        alice.setRoomType("Deluxe Suite");
        alice.setCheckInDate(today.minusDays(1).toString());
        alice.setNumberOfNights(3);
        alice.setSpecialRequest("High floor and extra pillows");

        Guest bob = new Guest("Bob Lee", "990202-08-1234", "+60 17-222 3344", "Male",
                "Malaysian", "bob.lee@example.com", "10000002", "Gold", 500);
        bob.setBookingStatus("Reserved");
        bob.setAssignedRoomNumber("103");
        bob.setRoomType("Standard Room");
        bob.setEffectiveRoomRate(180.00);
        bob.setCheckInDate(today.toString());
        bob.setNumberOfNights(2);
        bob.setSpecialRequest("Late arrival after 8 PM");

        Guest charlie = new Guest("Charlie Lim", "A12345678", "+60 16-333 4455", "Male",
                "Singaporean", "charlie.lim@example.com", "10000003", "Silver", 200);
        charlie.setSpecialRequest("Quiet room if available");

        Guest david = new Guest("David Wong", "950404-01-3322", "+60 19-444 5566", "Male",
                "Malaysian", "david.wong@example.com", "10000004", "Standard", 50);
        david.setBookingStatus("CheckedOut");
        david.setAssignedRoomNumber("101");
        david.setRoomType("Deluxe Suite");
        david.setEffectiveRoomRate(350.00);
        david.setCheckInDate(today.minusDays(2).toString());
        david.setCheckOutDate(today.toString());
        david.setNumberOfNights(2);
        david.setSpecialRequest("None");

        Guest eva = new Guest("Eva Green", "P-GB-927711", "+44 20 7946 0958", "Female",
                "British", "eva.green@example.com", "10000005", "Platinum", 1800);
        eva.setBookingStatus("Cancelled");
        eva.setSpecialRequest("Airport transfer");

        Guest frank = new Guest("Frank Wright", "P-AU-964433", "+61 2 5550 7788", "Male",
                "Australian", "frank.wright@example.com", "10000006", "Gold", 850);
        frank.setBookingStatus("NoShow");
        frank.setAssignedRoomNumber("202");
        frank.setRoomType("Deluxe Suite");
        frank.setEffectiveRoomRate(400.00);
        frank.setCheckInDate(today.minusDays(2).toString());
        frank.setNumberOfNights(1);
        frank.setSpecialRequest("Non-smoking room");

        Guest grace = new Guest("Grace Kim", "P-KR-552810", "+82 10-8821 5520", "Female",
                "Korean", "grace.kim@example.com", "10000007", "Standard", 80);
        grace.setSpecialRequest("Near the lift");

        Guest hassan = new Guest("Hassan Rahman", "930818-10-4412", "+60 13-818 4412", "Male",
                "Malaysian", "hassan.rahman@example.com", "10000008", "Silver", 320);
        hassan.setBookingStatus("Reserved");
        hassan.setAssignedRoomNumber("205");
        hassan.setRoomType("Standard Room");
        hassan.setEffectiveRoomRate(210.00);
        hassan.setCheckInDate(today.plusDays(7).toString());
        hassan.setNumberOfNights(3);
        hassan.setSpecialRequest("Prayer mat");

        Guest isabella = new Guest("Isabella Rossi", "P-IT-735920", "+39 06 7359 2000", "Female",
                "Italian", "isabella.rossi@example.com", "10000009", "Gold", 760);
        isabella.setCheckedIn(true);
        isabella.setAssignedRoomNumber("204");
        isabella.setRoomType("Deluxe Suite");
        isabella.setEffectiveRoomRate(420.00);
        isabella.setCheckInDate(today.minusDays(1).toString());
        isabella.setNumberOfNights(5);
        isabella.setSpecialRequest("Gluten-free breakfast");

        Guest jason = new Guest("Jason Ng", "910922-07-2188", "+60 18-922 2188", "Male",
                "Malaysian", "jason.ng@example.com", "10000010", "Silver", 280);
        jason.setBookingStatus("CheckedOut");
        jason.setAssignedRoomNumber("302");
        jason.setRoomType("Deluxe Suite");
        jason.setEffectiveRoomRate(450.00);
        jason.setCheckInDate(today.minusDays(4).toString());
        jason.setCheckOutDate(today.minusDays(2).toString());
        jason.setNumberOfNights(2);
        jason.setSpecialRequest("Early breakfast");

        Guest karen = new Guest("Karen Ho", "970611-14-6621", "+60 12-611 6621", "Female",
                "Malaysian", "karen.ho@example.com", "10000011", "Platinum", 1350);
        karen.setBookingStatus("Reserved");
        karen.setAssignedRoomNumber("301");
        karen.setRoomType("Standard Room");
        karen.setEffectiveRoomRate(220.00);
        karen.setCheckInDate(today.toString());
        karen.setNumberOfNights(1);
        karen.setSpecialRequest("Late check-out if available");

        // Same person, but a separate future stay and confirmation number.
        Guest aliceReturnStay = new Guest("Alice Tan", "980101-14-5566", "+60 12-345 6789", "Female",
                "Malaysian", "alice.tan@example.com", "10000012", "Platinum", 1200);
        aliceReturnStay.setBookingStatus("Reserved");
        aliceReturnStay.setAssignedRoomNumber("101");
        aliceReturnStay.setRoomType("Deluxe Suite");
        aliceReturnStay.setEffectiveRoomRate(350.00);
        aliceReturnStay.setCheckInDate(today.plusDays(14).toString());
        aliceReturnStay.setNumberOfNights(2);
        aliceReturnStay.setSpecialRequest("Returning guest - quiet room");

        guestTree.add(david);
        guestTree.add(bob);
        guestTree.add(frank);
        guestTree.add(alice);
        guestTree.add(charlie);
        guestTree.add(eva);
        guestTree.add(grace);
        guestTree.add(hassan);
        guestTree.add(isabella);
        guestTree.add(jason);
        guestTree.add(karen);
        guestTree.add(aliceReturnStay);

        addBooking(bookingList, new Booking("BK0001", alice.getConfirmationNumber(), alice.getGuestName(),
                "104", "Deluxe Suite", 350.00, alice.getCheckInDate(), alice.getNumberOfNights()),
                alice.getSpecialRequest(), today.minusDays(1), null, null);
        addBooking(bookingList, new Booking("BK0002", bob.getConfirmationNumber(), bob.getGuestName(),
                "103", "Standard Room", 180.00, bob.getCheckInDate(), bob.getNumberOfNights()),
                bob.getSpecialRequest(), null, null, null);
        addBooking(bookingList, new Booking("BK0003", david.getConfirmationNumber(), david.getGuestName(),
                "101", "Deluxe Suite", 350.00, david.getCheckInDate(), david.getNumberOfNights()),
                david.getSpecialRequest(), today.minusDays(2), today, null);

        Booking evaBooking = new Booking("BK0004", eva.getConfirmationNumber(), eva.getGuestName(),
                "201", "Presidential Suite", 950.00, today.plusDays(10).toString(), 4);
        evaBooking.setSpecialRequest(eva.getSpecialRequest());
        evaBooking.recordCancellation("Travel plans changed", "Reservation Desk");
        bookingList.add(evaBooking);

        addBooking(bookingList, new Booking("BK0005", frank.getConfirmationNumber(), frank.getGuestName(),
                "202", "Deluxe Suite", 400.00, frank.getCheckInDate(), frank.getNumberOfNights()),
                frank.getSpecialRequest(), null, null, today.minusDays(1));
        addBooking(bookingList, new Booking("BK0006", hassan.getConfirmationNumber(), hassan.getGuestName(),
                "205", "Standard Room", 210.00, hassan.getCheckInDate(), hassan.getNumberOfNights()),
                hassan.getSpecialRequest(), null, null, null);
        addBooking(bookingList, new Booking("BK0007", isabella.getConfirmationNumber(), isabella.getGuestName(),
                "204", "Deluxe Suite", 420.00, isabella.getCheckInDate(), isabella.getNumberOfNights()),
                isabella.getSpecialRequest(), today.minusDays(1), null, null);
        addBooking(bookingList, new Booking("BK0008", jason.getConfirmationNumber(), jason.getGuestName(),
                "302", "Deluxe Suite", 450.00, jason.getCheckInDate(), jason.getNumberOfNights()),
                jason.getSpecialRequest(), today.minusDays(4), today.minusDays(2), null);
        addBooking(bookingList, new Booking("BK0009", karen.getConfirmationNumber(), karen.getGuestName(),
                "301", "Standard Room", 220.00, karen.getCheckInDate(), karen.getNumberOfNights()),
                karen.getSpecialRequest(), null, null, null);
        addBooking(bookingList, new Booking("BK0010", aliceReturnStay.getConfirmationNumber(),
                aliceReturnStay.getGuestName(), "101", "Deluxe Suite", 350.00,
                aliceReturnStay.getCheckInDate(), aliceReturnStay.getNumberOfNights()),
                aliceReturnStay.getSpecialRequest(), null, null, null);
    }

    /** Adds the standard room master data when the supplied list is empty. */
    public static void seedDefaultRooms(ListInterface<Room> rooms) {
        if (rooms == null || !rooms.isEmpty())
            return;
        rooms.add(new Room("101", "Deluxe Suite", "Ready for Check-In", 350.00));
        rooms.add(new Room("102", "Presidential Suite", "Dirty", 800.00));
        rooms.add(new Room("103", "Standard Room", "Reserved", 180.00));
        rooms.add(new Room("104", "Deluxe Suite", "Occupied", 350.00));
        rooms.add(new Room("105", "Standard Room", "Cleaning In Progress", 180.00));
        rooms.add(new Room("201", "Presidential Suite", "Inspected", 950.00));
        rooms.add(new Room("202", "Deluxe Suite", "Ready for Check-In", 400.00));
        rooms.add(new Room("203", "Standard Room", "Ready for Check-In", 200.00));
        rooms.add(new Room("204", "Deluxe Suite", "Occupied", 420.00));
        rooms.add(new Room("205", "Standard Room", "Ready for Check-In", 210.00));
        rooms.add(new Room("301", "Standard Room", "Reserved", 220.00));
        rooms.add(new Room("302", "Deluxe Suite", "Dirty", 450.00));
        rooms.add(new Room("303", "Presidential Suite", "Cleaning In Progress", 1000.00));
        rooms.add(new Room("304", "Deluxe Suite", "Inspected", 480.00));
        rooms.add(new Room("305", "Standard Room", "Maintenance", 230.00));
        rooms.add(new Room("401", "Presidential Suite", "Ready for Check-In", 1200.00));
    }

    private static void addBooking(ListInterface<Booking> bookings, Booking booking, String request,
            LocalDate checkIn, LocalDate checkOut, LocalDate noShow) {
        booking.setSpecialRequest(request);
        if (checkIn != null)
            booking.recordCheckIn(checkIn);
        if (checkOut != null)
            booking.recordCheckOut(checkOut);
        if (noShow != null)
            booking.recordNoShow(noShow);
        bookings.add(booking);
    }
}
