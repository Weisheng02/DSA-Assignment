package control;

import adt.ArrayStack;
import adt.ListInterface;
import adt.MyArrayList;
import adt.StackInterface;
import entity.HousekeepingLog;
import entity.Room;

import java.util.Comparator;

/**
 * Author: [Your Name Here]
 * Controller Class for Housekeeping & Task Log Operations (Linear ADT: Stack).
 *
 * Room statuses move forward through a fixed sequence:
 *   Dirty -> Cleaning In Progress -> Inspected -> Ready for Check-In
 * Every change is pushed onto a Stack-based task log. If a supervisor logs
 * an incorrect status, or a guest requests a late check-out mid-cleaning,
 * the most recent change can be instantly rolled back by popping the stack.
 */
public class HousekeepingController {

    private static final String[] STATUS_SEQUENCE = {
            "Dirty", "Cleaning In Progress", "Inspected", "Ready for Check-In"
    };

    private ListInterface<Room> roomList;
    private StackInterface<HousekeepingLog> taskLogStack;

    public HousekeepingController() {
        roomList = new MyArrayList<>();
        taskLogStack = new ArrayStack<>();
        seedInitialData();
    }

    // Populate this module's own working data, since the team's modules are not yet integrated.
    private void seedInitialData() {
        roomList.add(new Room("101", "Deluxe Suite", "Dirty", 350.00));
        roomList.add(new Room("102", "Presidential Suite", "Cleaning In Progress", 800.00));
        roomList.add(new Room("103", "Standard Room", "Inspected", 180.00));
        roomList.add(new Room("104", "Deluxe Suite", "Dirty", 350.00));
        roomList.add(new Room("105", "Standard Room", "Ready for Check-In", 180.00));
        roomList.add(new Room("201", "Presidential Suite", "Cleaning In Progress", 950.00));
        roomList.add(new Room("202", "Deluxe Suite", "Dirty", 400.00));
    }

    // Linear search through the room list by room number.
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

    public ListInterface<Room> getAllRooms() {
        return roomList;
    }

    public String[] getStatusSequence() {
        return STATUS_SEQUENCE;
    }

    private int getStatusIndex(String status) {
        if (status == null) return -1;
        for (int i = 0; i < STATUS_SEQUENCE.length; i++) {
            if (STATUS_SEQUENCE[i].equalsIgnoreCase(status)) return i;
        }
        return -1;
    }

    /**
     * Advances a room to the NEXT stage in the cleaning sequence.
     * @return 1 success | -1 room not found | -2 already at final stage | -3 room not currently in the housekeeping cycle (e.g. Occupied)
     */
    public int advanceRoomStatus(String roomNumber, String staffName) {
        Room room = findRoomByNumber(roomNumber);
        if (room == null) return -1;

        int currentIndex = getStatusIndex(room.getRoomStatus());
        if (currentIndex == -1) return -3;
        if (currentIndex >= STATUS_SEQUENCE.length - 1) return -2;

        String previousStatus = room.getRoomStatus();
        String newStatus = STATUS_SEQUENCE[currentIndex + 1];
        room.setRoomStatus(newStatus);

        taskLogStack.push(new HousekeepingLog(roomNumber, previousStatus, newStatus, staffName));
        return 1;
    }

    /**
     * Lets a supervisor directly set/correct a room's status (e.g. a late check-out
     * request forces a "Ready for Check-In" room back to "Dirty"). Also logged for rollback.
     * @return 1 success | -1 room not found | -2 invalid status name
     */
    public int setRoomStatus(String roomNumber, String newStatus, String staffName) {
        Room room = findRoomByNumber(roomNumber);
        if (room == null) return -1;
        if (getStatusIndex(newStatus) == -1) return -2;

        String previousStatus = room.getRoomStatus();
        room.setRoomStatus(newStatus);
        taskLogStack.push(new HousekeepingLog(roomNumber, previousStatus, newStatus, staffName));
        return 1;
    }

    /**
     * Instantly rolls back the most recent status change by popping the task-log stack
     * and restoring the affected room's previous status.
     * @return 1 success | 0 nothing to roll back
     */
    public int rollbackLastChange() {
        if (taskLogStack.isEmpty()) return 0;

        HousekeepingLog lastLog = taskLogStack.pop();
        Room room = findRoomByNumber(lastLog.getRoomNumber());
        if (room != null) {
            room.setRoomStatus(lastLog.getPreviousStatus());
        }
        return 1;
    }

    public HousekeepingLog peekLastChange() {
        return taskLogStack.isEmpty() ? null : taskLogStack.peek();
    }

    public int getLoggedTaskCount() {
        return taskLogStack.getNumberOfEntries();
    }

    // ---------------- Reports ----------------

    // Report 1: Count of rooms currently at each housekeeping stage.
    public int[] getRoomStatusSummary() {
        int[] summary = new int[STATUS_SEQUENCE.length + 1]; // [0]=total, [1..4]=each stage
        summary[0] = roomList.getNumberOfEntries();
        for (int i = 0; i < roomList.getNumberOfEntries(); i++) {
            int idx = getStatusIndex(roomList.get(i).getRoomStatus());
            if (idx != -1) summary[idx + 1]++;
        }
        return summary;
    }

    // Report 2: Multi-criteria filtered + sorted Task Log report (search + sort combined).
    public ListInterface<HousekeepingLog> getFilteredTaskLog(String roomNumberFilter, String statusFilter, boolean newestFirst) {
        ListInterface<HousekeepingLog> filtered = new MyArrayList<>();
        ListInterface<HousekeepingLog> allLogs = taskLogStack.toList();

        for (int i = 0; i < allLogs.getNumberOfEntries(); i++) {
            HousekeepingLog log = allLogs.get(i);
            boolean roomMatch = "ALL".equalsIgnoreCase(roomNumberFilter)
                    || log.getRoomNumber().equalsIgnoreCase(roomNumberFilter);
            boolean statusMatch = "ALL".equalsIgnoreCase(statusFilter)
                    || log.getNewStatus().equalsIgnoreCase(statusFilter);

            if (roomMatch && statusMatch) {
                filtered.add(log);
            }
        }

        // Explicit sort on the ADT (Selection Sort, defined inside MyArrayList).
        filtered.sort(new Comparator<HousekeepingLog>() {
            @Override
            public int compare(HousekeepingLog a, HousekeepingLog b) {
                return newestFirst ? Integer.compare(b.getTaskId(), a.getTaskId())
                                    : Integer.compare(a.getTaskId(), b.getTaskId());
            }
        });

        return filtered;
    }

    // Report 3 (bonus): Rooms not yet Ready for Check-In, sorted by how far behind they are.
    public ListInterface<Room> getRoomsNeedingAttention() {
        ListInterface<Room> result = new MyArrayList<>();
        for (int i = 0; i < roomList.getNumberOfEntries(); i++) {
            Room r = roomList.get(i);
            int idx = getStatusIndex(r.getRoomStatus());
            if (idx != -1 && idx < STATUS_SEQUENCE.length - 1) {
                result.add(r);
            }
        }

        result.sort(new Comparator<Room>() {
            @Override
            public int compare(Room a, Room b) {
                return Integer.compare(getStatusIndex(a.getRoomStatus()), getStatusIndex(b.getRoomStatus()));
            }
        });

        return result;
    }
}
