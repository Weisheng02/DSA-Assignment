import adt.BSTInterface;
import adt.ArrayQueue;
import adt.ArrayStack;
import adt.BinarySearchTree;
import adt.ListInterface;
import adt.MyArrayList;
import control.BookingController;
import control.FrontDeskController;
import control.HousekeepingController;
import control.LoyaltyController;
import data.ResortDataSeeder;
import entity.Booking;
import entity.Guest;
import entity.LoyaltyTransaction;
import entity.PointBatch;
import entity.Room;
import entity.RewardItem;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Simple regression checks; run with java -ea ControllerRegressionTest. */
public class ControllerRegressionTest {

    public static void main(String[] args) {
        testValidationMappings();
        testCustomAdtBoundaryCases();
        testSharedQueriesAndRoomState();
        testSharedAvailabilityRules();
        testCorruptActiveBookingFailsClosed();
        testSharedRoomSeedCoverage();
        testResortDataSeederConsistency();
        testBstDuplicatePolicyAndBalance();
        testHousekeepingRollback();
        testHousekeepingOccupiedRollbackAndStaleLog();
        testCheckInStatusSingleSourceOfTruth();
        testRoomTransferRequiresActiveStay();
        testAtomicCheckoutRewardFlow();
        testBookingCancellationStateConsistency();
        testLoyaltyEntityOperations();
        testRepeatStayLoyaltySynchronization();
        testMissingIdentityDoesNotMergeMembers();
        testPointBatchConsumptionAndExpiry();
        testFifthRedemptionDiscountAndReports();
        System.out.println("Controller regression tests passed.");
    }

    private static void testValidationMappings() {
        Fixture f = new Fixture();
        String today = LocalDate.now().toString();

        check(f.booking.validateStayPeriod(today, 1) == 1, "Booking valid stay");
        check(f.frontDesk.validateStayPeriod(today, 1) == 1, "Front Desk valid stay");
        check(f.booking.validateStayPeriod("bad-date", 1) == -4, "Booking invalid-date code");
        check(f.frontDesk.validateStayPeriod("bad-date", 1) == -1, "Front Desk invalid-date code");
        check(f.booking.validateStayPeriod(today, 0) == -5, "Booking invalid-nights code");
        check(f.frontDesk.validateStayPeriod(today, 0) == -2, "Front Desk invalid-nights code");
        check(f.booking.validateStayPeriod(LocalDate.now().minusDays(1).toString(), 1) == -6,
                "Booking past-date code");
        check(f.frontDesk.validateStayPeriod(LocalDate.now().minusDays(1).toString(), 1) == -3,
                "Front Desk past-date code");
        check(f.booking.validateStayPeriod(LocalDate.now().plusDays(366).toString(), 1) == -7,
                "Booking advance-limit code");
        check(f.frontDesk.validateStayPeriod(LocalDate.now().plusDays(366).toString(), 1) == -4,
                "Front Desk advance-limit code");
    }

    private static void testCustomAdtBoundaryCases() {
        MyArrayList<Integer> list = new MyArrayList<>();
        for (int i = 29; i >= 0; i--)
            list.add(i);
        list.sort(Integer::compare);
        check(list.getNumberOfEntries() == 30 && list.get(0) == 0 && list.get(29) == 29,
                "Custom list should resize and selection-sort correctly");
        list.clear();
        check(list.isEmpty() && list.get(0) == null, "Cleared list should expose no stale entries");

        ArrayQueue<Integer> queue = new ArrayQueue<>();
        for (int i = 0; i < 30; i++)
            queue.enqueue(i);
        for (int i = 0; i < 10; i++)
            check(queue.dequeue() == i, "Circular queue must preserve FIFO before wrap-around");
        for (int i = 30; i < 45; i++)
            queue.enqueue(i);
        for (int expected = 10; expected < 45; expected++)
            check(queue.dequeue() == expected, "Circular queue must preserve FIFO after resize/wrap-around");
        check(queue.dequeue() == null, "Empty queue dequeue should be safe");

        ArrayStack<Integer> stack = new ArrayStack<>();
        for (int i = 0; i < 30; i++)
            stack.push(i);
        for (int expected = 29; expected >= 0; expected--)
            check(stack.pop() == expected, "Array stack must preserve LIFO after resize");
        check(stack.pop() == null, "Empty stack pop should be safe");

        BinarySearchTree<Integer> tree = new BinarySearchTree<>();
        int[] values = { 4, 2, 6, 1, 3, 5, 7 };
        for (int value : values)
            tree.add(value);
        check(tree.remove(1) == 1, "BST should remove a leaf");
        check(tree.remove(2) == 2, "BST should remove a one-child node");
        check(tree.remove(6) == 6, "BST should remove a two-child node");
        check(tree.search(6) == null && tree.getNumberOfEntries() == 4,
                "BST removal should keep search and size consistent");
        check(tree.rangeSearch(3, 7).getNumberOfEntries() == 4,
                "BST range search should include both boundaries");

        BinarySearchTree<Integer> skewed = new BinarySearchTree<>();
        for (int i = 1; i <= 31; i++)
            skewed.add(i);
        check(!skewed.isBalanced(), "Sorted insertion should demonstrate the BST worst case");
        skewed.rebalance();
        check(skewed.isBalanced() && skewed.getHeight() == 5,
                "BST rebalance should restore logarithmic height");
    }

    private static void testSharedQueriesAndRoomState() {
        Fixture f = new Fixture();

        Guest queueGuest = f.booking.peekNextGuest();
        check(queueGuest != null, "Booking queue should be seeded");
        check(f.booking.findGuestByIC(queueGuest.getIcNo()) == queueGuest, "Booking IC lookup");
        check(f.frontDesk.searchGuestByIC(queueGuest.getIcNo()) == queueGuest, "Front Desk IC lookup");

        check(f.housekeeping.advanceRoomStatus("902", "Amin") == 1, "Housekeeping status update");
        check("Cleaning In Progress".equals(f.frontDesk.searchRoomByNumber("902").getRoomStatus()),
                "Front Desk must see Housekeeping room state");
        check(f.booking.findRoomByNumber("902") == f.frontDesk.searchRoomByNumber("902"),
                "Controllers must return the same shared Room object");

        int before = f.booking.getWaitingCount();
        check(f.booking.processNextGuest("901", LocalDate.now().toString(), 2) == 1,
                "Booking should create a valid reservation");
        check(f.booking.getWaitingCount() == before - 1, "Successful booking dequeues one guest");

        Booking created = f.booking.getLastBooking();
        check(created == f.frontDesk.searchBookingById(created.getBookingId()),
                "Front Desk must see the same shared Booking object");
        check(created == f.frontDesk.getBookingByConfirmation(created.getGuestConfirmationNumber()),
                "Shared latest-confirmation lookup");
    }

    private static void testSharedAvailabilityRules() {
        Fixture f = new Fixture();
        String today = LocalDate.now().toString();

        check(f.booking.processNextGuest("901", today, 2) == 1, "Create overlap fixture");
        Booking booking = f.booking.getLastBooking();
        check(f.booking.updateBooking(booking.getBookingId(), "901", today, 2, "None") == 1,
                "Updating a booking must exclude itself from overlap checks");
        check(f.booking.updateBooking(booking.getBookingId(), "901", today, 0, "None") == -5,
                "Booking update should keep invalid nights separate from availability errors");
        check(f.booking.updateBooking(booking.getBookingId(), "902", today, 2, "None") == -8,
                "Booking update should report an unavailable room with its own result code");

        check(!containsRoom(f.frontDesk.searchAvailableRooms(today, 1, "ALL", 0, true), "901"),
                "An overlapping active booking must block the room");
        check(containsRoom(f.frontDesk.searchAvailableRooms(
                        LocalDate.now().plusDays(2).toString(), 1, "ALL", 0, true), "901"),
                "A back-to-back stay must not count as overlap");
        check(containsRoom(f.frontDesk.searchAvailableRooms(
                        LocalDate.now().plusDays(1).toString(), 1, "ALL", 0, true), "902"),
                "A future stay may be sold while the room is Dirty");

        f.rooms.add(new Room("904", "Standard Room", "Maintenance", 100));
        check(!containsRoom(f.frontDesk.searchAvailableRooms(
                        LocalDate.now().plusDays(10).toString(), 1, "ALL", 0, true), "904"),
                "Maintenance room must never be available");
    }

    private static void testSharedRoomSeedCoverage() {
        ListInterface<Room> rooms = new MyArrayList<>();
        ResortDataSeeder.seedDefaultRooms(rooms);
        check(rooms.getNumberOfEntries() == 16,
                "The shared demo seed should contain enough rooms for meaningful reports");
        Room maintenance = null;
        for (int i = 0; i < rooms.getNumberOfEntries(); i++) {
            if ("305".equals(rooms.get(i).getRoomNumber()))
                maintenance = rooms.get(i);
        }
        check(maintenance != null && "Maintenance".equals(maintenance.getRoomStatus()),
                "The expanded room seed should include a non-sellable maintenance example");
    }

    private static void testResortDataSeederConsistency() {
        BSTInterface<Guest> guests = new BinarySearchTree<>();
        ListInterface<Room> rooms = new MyArrayList<>();
        ListInterface<Booking> bookings = new MyArrayList<>();
        ResortDataSeeder.seed(guests, rooms, bookings);
        check(rooms.getNumberOfEntries() == 16, "Seeder should create the 16-room master list");
        check(guests.getNumberOfEntries() == 12, "Seeder should create 12 master guest stay profiles");
        check(bookings.getNumberOfEntries() == 10, "Seeder should create 10 matching booking records");
        Guest alice = guests.search(new Guest("", "10000001", "", 0));
        check(alice != null && alice.isCheckedIn() && "104".equals(alice.getAssignedRoomNumber()),
                "Seeded Alice stay should preserve its active Front Desk state");
        check("10000001".equals(bookings.get(0).getGuestConfirmationNumber())
                        && "CheckedIn".equals(bookings.get(0).getBookingStatus()),
                "Seeded booking should match its Guest confirmation and lifecycle state");
        ResortDataSeeder.seed(guests, rooms, bookings);
        check(rooms.getNumberOfEntries() == 16 && guests.getNumberOfEntries() == 12
                        && bookings.getNumberOfEntries() == 10,
                "Calling the seeder again must not duplicate master data");
    }

    private static void testCorruptActiveBookingFailsClosed() {
        Fixture f = new Fixture();
        Booking corrupt = new Booking("BK9999", "29999999", "Test Guest", "901",
                "Standard Room", 200, LocalDate.now().plusDays(1).toString(), 1);
        corrupt.setCheckInDate("not-a-date");
        f.bookings.add(corrupt);

        check(f.booking.findBookingById("BK9999") == corrupt,
                "A corrupt active booking must not crash normal lookup");
        check(!containsRoom(f.frontDesk.searchAvailableRooms(
                        LocalDate.now().plusDays(20).toString(), 1, "ALL", 0, true), "901"),
                "Corrupt active booking data must block sale instead of being ignored");
    }

    private static void testHousekeepingRollback() {
        Fixture f = new Fixture();

        check(f.housekeeping.advanceRoomStatus("902", "Staff") == 1,
                "Housekeeping should advance a room through its workflow");
        check("Cleaning In Progress".equals(f.housekeeping.getRoomStatus("902")),
                "Housekeeping should update the shared Room entity");
        check(f.housekeeping.rollbackLastChange() == 1,
                "Housekeeping should roll back its latest status change");
        check("Dirty".equals(f.housekeeping.getRoomStatus("902")),
                "Rollback should restore the previous Room status");
    }

    private static void testBstDuplicatePolicyAndBalance() {
        BinarySearchTree<Integer> tree = new BinarySearchTree<>();
        check(tree.add(2), "BST should accept a new key");
        check(tree.add(1), "BST should accept a lower key");
        check(tree.add(3), "BST should accept a higher key");
        check(!tree.add(2), "BST should reject an equal key");
        check(tree.getNumberOfEntries() == 3, "Rejected duplicate must not change BST size");
        check(tree.isBalanced(), "Simple three-node BST should be balanced");

        Fixture f = new Fixture();
        check(f.frontDesk.isRoomTreeBalanced(), "Shared room BST should be balanced after synchronization");
        String guestTreeDisplay = f.frontDesk.getGuestTreeStructure();
        check(guestTreeDisplay.contains("[20000002]") && !guestTreeDisplay.contains("Guest{"),
                "Guest BST visualizer should use compact labels instead of full Guest records");
        check(guestTreeDisplay.contains("/") && guestTreeDisplay.contains("\\"),
                "Guest BST visualizer should display left and right branches from a centred root");
        String roomTreeDisplay = f.frontDesk.getRoomTreeStructure();
        check(roomTreeDisplay.contains("[901]") && !roomTreeDisplay.contains("Room{"),
                "Room BST visualizer should use compact room-number labels");
        f.rooms.add(new Room("903", "Standard Room", "Ready for Check-In", 210));
        check(f.frontDesk.searchRoomByNumber("903") == f.rooms.get(2),
                "Room tree should synchronize a newly added shared room");
        check(f.frontDesk.isRoomTreeBalanced(), "Room tree should remain balanced after structural synchronization");
    }

    private static void testHousekeepingOccupiedRollbackAndStaleLog() {
        ListInterface<Room> rooms = new MyArrayList<>();
        Room occupied = new Room("801", "Standard Room", "Occupied", 180);
        Room dirty = new Room("802", "Standard Room", "Dirty", 180);
        rooms.add(occupied);
        rooms.add(dirty);
        HousekeepingController housekeeping = new HousekeepingController(rooms);

        check(housekeeping.setRoomStatus("801", "Dirty", "Staff") == 1,
                "Housekeeping correction may move Occupied to Dirty");
        check(housekeeping.rollbackLastChange() == 1,
                "Rollback should restore a valid previous Occupied status");
        check("Occupied".equals(occupied.getRoomStatus()), "Occupied status should be restored exactly");

        check(housekeeping.advanceRoomStatus("802", "Staff") == 1, "Create a rollback log");
        dirty.setRoomStatus("Inspected");
        int before = housekeeping.getLoggedTaskCount();
        check(housekeeping.rollbackLastChange() == -2,
                "Rollback must refuse to overwrite a later external status change");
        check(housekeeping.getLoggedTaskCount() == before - 1,
                "A stale top log should be discarded instead of blocking the stack forever");
        check("Inspected".equals(dirty.getRoomStatus()), "External room state must remain untouched");
    }

    private static void testCheckInStatusSingleSourceOfTruth() {
        Fixture f = new Fixture();
        check(f.booking.processNextGuest("901", LocalDate.now().toString(), 2) == 1,
                "Create today's confirmed booking");
        Guest guest = f.booking.findGuestByIC("010512-08-1234");
        check(guest != null, "Processed queue guest should remain in the shared registry");
        check(f.frontDesk.processCheckIn(guest.getConfirmationNumber(), "", "Late pillow") == 1,
                "Front Desk should use the assigned room and store the request during check-in");
        check(f.frontDesk.isGuestCheckedIn(guest.getConfirmationNumber()),
                "Check-in query should read Guest.bookingStatus");
        check("Late pillow".equals(guest.getSpecialRequest()),
                "Successful check-in should store its special request in the same controller workflow");
        check(!f.frontDesk.canRemoveGuest(guest.getConfirmationNumber()),
                "A checked-in guest must not be eligible for removal");
        check(f.frontDesk.removeGuest(guest.getConfirmationNumber()) == null
                        && f.frontDesk.searchGuestByConfirmationNumber(guest.getConfirmationNumber()) == guest,
                "Controller must reject direct removal of a checked-in guest");
        check(f.frontDesk.processCheckOut(guest.getConfirmationNumber()) == 1,
                "Front Desk should check out the active guest");
        check(!f.frontDesk.isGuestCheckedIn(guest.getConfirmationNumber()),
                "Check-out must clear active state through Guest.bookingStatus");
        check("CheckedOut".equals(guest.getBookingStatus()), "Guest lifecycle should be CheckedOut");
        check("Dirty".equals(f.rooms.get(0).getRoomStatus()), "Checkout should send the room to Housekeeping");
    }

    private static void testAtomicCheckoutRewardFlow() {
        Fixture f = new Fixture();
        check(f.booking.processNextGuest("901", LocalDate.now().toString(), 2) == 1,
                "Create booking for atomic checkout test");
        Guest guest = f.booking.findGuestByIC("010512-08-1234");
        check(f.frontDesk.processCheckIn(guest.getConfirmationNumber(), "901") == 1,
                "Check in before atomic checkout");
        int pointsBefore = guest.getLoyaltyPoints();
        FrontDeskController.CheckoutResult result = f.frontDesk.completeCheckOutAndReward(
                guest.getConfirmationNumber());
        check(result.getStatus() == 1, "Checkout and loyalty award should complete together");
        check(guest.getLoyaltyPoints() == pointsBefore + result.getEarnedPoints(),
                "Checkout should award exactly the reported points once");
        int pointsAfter = guest.getLoyaltyPoints();
        check(f.frontDesk.completeCheckOutAndReward(guest.getConfirmationNumber()).getStatus() != 1,
                "A completed stay must not be checked out twice");
        check(guest.getLoyaltyPoints() == pointsAfter, "Repeated checkout must not award duplicate points");
    }

    private static void testRoomTransferRequiresActiveStay() {
        Fixture f = new Fixture();
        f.rooms.add(new Room("903", "Standard Room", "Ready for Check-In", 210));
        check(f.booking.processNextGuest("901", LocalDate.now().toString(), 2) == 1,
                "Create today's reservation for room transfer test");
        Guest guest = f.booking.findGuestByIC("010512-08-1234");
        check(f.frontDesk.processCheckIn(guest.getConfirmationNumber(), "901") == 1,
                "Check in before requesting a transfer");
        Room[] available = f.frontDesk.getAvailableTransferRoomArray(guest.getConfirmationNumber());
        check(available.length == 1 && "903".equals(available[0].getRoomNumber()),
                "Transfer choices should include only ready, conflict-free rooms");
        check(f.frontDesk.processCheckOut(guest.getConfirmationNumber()) == 1,
                "Check out before testing historical room state");
        check(guest.getAssignedRoomNumber() != null,
                "Checkout may retain the last assigned room as stay history");
        check(f.frontDesk.getAvailableTransferRoomArray(guest.getConfirmationNumber()).length == 0,
                "Checked-out guests must not receive transfer choices");
        check(f.frontDesk.processRoomTransfer(guest.getConfirmationNumber(), "903") == -2,
                "Checked-out guests must be rejected before room transfer mutation");
    }

    private static void testBookingCancellationStateConsistency() {
        Fixture f = new Fixture();
        String tomorrow = LocalDate.now().plusDays(1).toString();
        check(f.booking.processNextGuest("901", tomorrow, 2) == 1,
                "Create a future booking for cancellation");
        Booking booking = f.booking.getLastBooking();
        Guest guest = f.booking.findGuestByIC("010512-08-1234");
        check(f.booking.cancelBooking(booking.getBookingId(), "Guest request", "Tester") == 1,
                "Confirmed booking should be cancellable");
        check("Cancelled".equals(booking.getBookingStatus()), "Booking should retain cancellation status");
        check("Cancelled".equals(guest.getBookingStatus()), "Guest status should match cancelled booking");
        check("Guest request".equals(booking.getCancellationReason()), "Cancellation reason should be audited");
        check("Tester".equals(booking.getCancelledBy()), "Cancellation staff should be audited");
        check(guest.getAssignedRoomNumber() == null, "Cancelled guest should no longer hold a room reference");
    }

    private static void testLoyaltyEntityOperations() {
        BSTInterface<Guest> guests = new BinarySearchTree<>();
        Guest member = new Guest("Loyalty Member", "A-1", "70000001", "Gold", 100);
        guests.add(member);
        LoyaltyController loyalty = new LoyaltyController(guests);

        check(loyalty.validateCheckoutAward("70000001", "BK-TEST", 10).isSuccess(),
                "Loyalty should find a Guest entity through the shared registry");
        check(loyalty.awardCheckoutPoints("70000001", "BK-TEST", 10).isSuccess(),
                "Loyalty should award points to the shared Guest entity");
        check(member.getLoyaltyPoints() == 110, "Awarded points should update the Guest entity");
        String[] checkoutHistory = loyalty.getMemberPointHistoryRecords(member.getConfirmationNumber());
        check(checkoutHistory.length >= 1 && checkoutHistory[0].contains("Checkout Reward")
                        && checkoutHistory[0].contains("|N/A|"),
                "Checkout reward points should remain active without an expiry time");
        check(loyalty.getFilteredPointReport("ACTIVE", true).getNumberOfEntries() >= 1,
                "Point report should filter active point transactions");
        String[] openingHistory = loyalty.getMemberPointHistoryRecords(member.getConfirmationNumber());
        check(openingHistory.length >= 2 && openingHistory[openingHistory.length - 1].contains("Opening Balance"),
                "Seeded loyalty points should have one traceable opening balance batch");

        RewardItem reward = loyalty.getRewardCatalog().get(0);
        int pointsBeforeRedeem = member.getLoyaltyPoints();
        check(loyalty.requestRewardRedemption(member.getConfirmationNumber(), 1)
                == LoyaltyController.RESULT_SUCCESS,
                "Loyalty should redeem a reward using the Guest entity");
        check(member.getLoyaltyPoints() < pointsBeforeRedeem,
                "Redeeming a reward should deduct points from the Guest entity");
        check(loyalty.getFilteredPointReport("DEDUCTION", true).getNumberOfEntries() >= 1,
                "Point report should filter point deductions");
        check(reward.getStockQuantity() == reward.getDefaultStockQuantity() - 1,
                "Redeeming a reward should reserve one unit of reward stock");
        check(loyalty.getPendingRewardRedemptionCount() == 1,
                "A redeemed reward should enter the pending FIFO settlement queue");
        check(loyalty.getActiveItemRedemptionCount(member, reward.getItemName()) == 0,
                "Queued redemptions should not enter inventory before settlement");
        check(loyalty.settlePendingRewardRedemptionsData().length == 1,
                "Queue settlement should commit the pending redemption");
        check(loyalty.getActiveItemRedemptionCount(member, reward.getItemName()) == 1,
                "Settled redemption count should be filtered by Guest when supplied");
        check(loyalty.getActiveItemRedemptionCount(null, reward.getItemName()) == 1,
                "Settled redemption report should count all guests when Guest is null");
        check(loyalty.getExpiredItemRedemptionCount(reward.getItemName()) == 0,
                "A newly redeemed item must not be reported as expired");
        check(reward.getTotalRedeemed() == 1,
                "Completed queue settlement should update RewardItem.totalRedeemed");
        check(loyalty.useInventoryItem(member.getConfirmationNumber(), 1)
                == LoyaltyController.RESULT_SUCCESS,
                "The active inventory row should be usable by its displayed number");
        LoyaltyTransaction[] redemptionHistory = loyalty.getMemberRedemptionHistoryArray(
                member.getConfirmationNumber());
        check(redemptionHistory.length == 1 && "USED".equals(redemptionHistory[0].getStatus()),
                "Redemption history should retain a used item and expose its terminal status");
        check(loyalty.getActiveItemRedemptionCount(member, reward.getItemName()) == 0,
                "A used reward must leave active inventory");
        check(loyalty.getExpiredItemRedemptionCount(reward.getItemName()) == 0,
                "A used reward must not be misreported as expired");
    }

    private static void testRepeatStayLoyaltySynchronization() {
        BSTInterface<Guest> guests = new BinarySearchTree<>();
        Guest original = new Guest("Repeat Member", "P-SAME-1", "70000001", "Gold", 500);
        guests.add(original);
        ListInterface<Room> rooms = new MyArrayList<>();
        rooms.add(new Room("701", "Standard Room", "Ready for Check-In", 180));
        BookingController booking = new BookingController(rooms, guests, new MyArrayList<Booking>());
        Guest repeatStay = booking.registerWalkInGuest("Repeat Member", "P-SAME-1", "Standard", 0);

        check(repeatStay != original, "A repeat reservation should keep its own stay confirmation");
        check(repeatStay.getLoyaltyPoints() == original.getLoyaltyPoints(),
                "A repeat stay should inherit the member balance");
        check(repeatStay.getLoyaltyExperience() == original.getLoyaltyExperience(),
                "A repeat stay should inherit lifetime EXP");

        LoyaltyController loyalty = new LoyaltyController(guests);
        int before = original.getLoyaltyPoints();
        check(loyalty.claimDailyCheckIn(repeatStay.getConfirmationNumber())
                == LoyaltyController.RESULT_SUCCESS,
                "Repeat stay should be able to claim today's member reward");
        check(original.getLoyaltyPoints() == before + LoyaltyController.DAILY_CHECK_IN_POINTS,
                "Loyalty mutation should synchronize the original stay profile");
        check(repeatStay.getLoyaltyPoints() == original.getLoyaltyPoints(),
                "All same-identity profiles should retain one balance");
        check(loyalty.claimDailyCheckIn(original.getConfirmationNumber())
                == LoyaltyController.RESULT_ALREADY_CLAIMED,
                "Daily reward must be limited by member identity and date, not confirmation");
        check(loyalty.getFilteredPointReport("ACTIVE", repeatStay.getConfirmationNumber(), 50, true)
                        .getNumberOfEntries() == 1,
                "Point report should apply status, confirmation and amount filters together");
        String today = LocalDate.now().toString();
        check(loyalty.getFilteredPointReport("ACTIVE", repeatStay.getConfirmationNumber(), 50,
                        today, today, true).getNumberOfEntries() == 1,
                "Point report should include a record inside its earned-date range");
        check(loyalty.getFilteredPointReport("ACTIVE", repeatStay.getConfirmationNumber(), 50,
                        LocalDate.now().minusDays(2).toString(), LocalDate.now().minusDays(1).toString(), true)
                        .isEmpty(),
                "Point report should exclude a record outside its earned-date range");
    }

    private static void testPointBatchConsumptionAndExpiry() {
        PointBatch batch = new PointBatch(50, "Test", "70000001", "member", 0);
        check(batch.consume(20) == 20, "PointBatch should consume part of its remaining balance");
        check(batch.getRemainingPoints() == 30, "PointBatch should retain the unspent balance");
        check(batch.expire(LocalDateTime.now().plusSeconds(1)) == 30,
                "Only the unspent batch balance should expire");
        check(batch.expire(LocalDateTime.now().plusSeconds(2)) == 0,
                "An expired batch must not deduct points twice");
        check("EXPIRED".equals(batch.getStatus()), "Expired batch should retain terminal state");
    }

    private static void testMissingIdentityDoesNotMergeMembers() {
        BSTInterface<Guest> guests = new BinarySearchTree<>();
        Guest first = new Guest("No IC One", "91000001", "Standard", 0);
        Guest second = new Guest("No IC Two", "91000002", "Standard", 0);
        guests.add(first);
        guests.add(second);
        LoyaltyController loyalty = new LoyaltyController(guests);

        check(loyalty.claimDailyCheckIn(first.getConfirmationNumber())
                == LoyaltyController.RESULT_SUCCESS,
                "First missing-IC profile should claim independently");
        check(loyalty.claimDailyCheckIn(second.getConfirmationNumber())
                == LoyaltyController.RESULT_SUCCESS,
                "Different missing-IC profiles must not share the daily claim key");
        check(first.getLoyaltyPoints() == LoyaltyController.DAILY_CHECK_IN_POINTS,
                "First missing-IC balance should remain independent");
        check(second.getLoyaltyPoints() == LoyaltyController.DAILY_CHECK_IN_POINTS,
                "Second missing-IC balance should remain independent");
    }

    private static void testFifthRedemptionDiscountAndReports() {
        BSTInterface<Guest> guests = new BinarySearchTree<>();
        Guest member = new Guest("Offer Member", "OFFER-1", "80000001", "Silver", 200);
        guests.add(member);
        LoyaltyController loyalty = new LoyaltyController(guests);
        RewardItem coffee = loyalty.getRewardCatalog().get(0);

        for (int i = 0; i < 5; i++)
            check(loyalty.requestRewardRedemption(member.getConfirmationNumber(), 1)
                    == LoyaltyController.RESULT_SUCCESS,
                    "Coffee redemption " + (i + 1) + " should succeed");
        check(member.getLoyaltyPoints() == 110,
                "Exactly the fifth redemption should receive the 50% discount (20*4 + 10)");
        check(loyalty.settlePendingRewardRedemptionsData().length == 5,
                "All five pending redemptions should settle in FIFO order");
        check(coffee.getTotalRedeemed() == 5, "Reward performance state should count completed redemptions");
        check(loyalty.getFilteredRewardReport(10, 5, true).getNumberOfEntries() == 1,
                "Reward report should combine stock and minimum-redemption filters");
    }

    private static boolean containsRoom(ListInterface<Room> rooms, String roomNumber) {
        for (int i = 0; i < rooms.getNumberOfEntries(); i++) {
            if (roomNumber.equalsIgnoreCase(rooms.get(i).getRoomNumber())) return true;
        }
        return false;
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static final class Fixture {
        final BSTInterface<Guest> guests = new BinarySearchTree<>();
        final ListInterface<Room> rooms = new MyArrayList<>();
        final ListInterface<Booking> bookings = new MyArrayList<>();
        final LoyaltyController loyalty = new LoyaltyController(guests);
        final BookingController booking;
        final HousekeepingController housekeeping;
        final FrontDeskController frontDesk;

        Fixture() {
            rooms.add(new Room("901", "Standard Room", "Ready for Check-In", 200));
            rooms.add(new Room("902", "Deluxe Suite", "Dirty", 350));
            booking = new BookingController(rooms, guests, bookings);
            housekeeping = new HousekeepingController(rooms);
            frontDesk = new FrontDeskController(guests, rooms, bookings, loyalty);
        }
    }
}
