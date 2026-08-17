package control;

import adt.ArrayStack;
import adt.ListInterface;
import adt.MyArrayList;
import adt.StackInterface;
import entity.HousekeepingLog;
import entity.Room;

import java.util.Comparator;

/**
 * Author: Kai Wei
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
        this(new MyArrayList<>());
        seedInitialData();
    }

    public HousekeepingController(ListInterface<Room> sharedRoomList) {
        this.roomList = (sharedRoomList != null) ? sharedRoomList : new MyArrayList<>();
        this.taskLogStack = new ArrayStack<>();
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

    /**
     * Supplies presentation-ready room rows to the boundary without exposing
     * entity or ADT types across the boundary/control boundary.
     */
    public String[] getRoomDisplayLines() {
        String[] lines = new String[roomList.getNumberOfEntries()];
        for (int i = 0; i < roomList.getNumberOfEntries(); i++) {
            Room room = roomList.get(i);
            lines[i] = String.format("%-10s%-20s%-22s%-10.2f",
                    room.getRoomNumber(), room.getRoomType(),
                    room.getRoomStatus(), room.getPrice());
        }
        return lines;
    }

    /** Returns a room status for UI display, or null when the room is absent. */
    public String getRoomStatus(String roomNumber) {
        Room room = findRoomByNumber(roomNumber);
        return room == null ? null : room.getRoomStatus();
    }

    public String[] getStatusSequence() {
        // Do not expose the backing array: callers (including the UI) should
        // not be able to corrupt the workflow for every controller instance.
        return STATUS_SEQUENCE.clone();
    }

    private boolean hasValidStaffName(String staffName) {
        return staffName != null && !staffName.trim().isEmpty();
    }

    private boolean sameStatus(String first, String second) {
        return first != null && second != null && first.trim().equalsIgnoreCase(second.trim());
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
     * @return 1 success | -1 room not found | -2 already at final stage |
     *         -3 room not currently in the housekeeping cycle (e.g. Occupied) |
     *         -4 staff name is blank
     */
    public int advanceRoomStatus(String roomNumber, String staffName) {
        Room room = findRoomByNumber(roomNumber);
        if (room == null) return -1;
        if (!hasValidStaffName(staffName)) return -4;

        int currentIndex = getStatusIndex(room.getRoomStatus());
        if (currentIndex == -1) return -3;
        if (currentIndex >= STATUS_SEQUENCE.length - 1) return -2;

        String previousStatus = room.getRoomStatus();
        String newStatus = STATUS_SEQUENCE[currentIndex + 1];
        room.setRoomStatus(newStatus);

        taskLogStack.push(new HousekeepingLog(roomNumber.trim(), previousStatus, newStatus, staffName.trim()));
        return 1;
    }

    /**
     * Lets a supervisor directly set/correct a room's status (e.g. a late check-out
     * request forces a "Ready for Check-In" room back to "Dirty"). Also logged for rollback.
     * @return 1 success | 0 status was already set (no log created) |
     *         -1 room not found | -2 invalid status name | -4 staff name is blank
     */
    public int setRoomStatus(String roomNumber, String newStatus, String staffName) {
        Room room = findRoomByNumber(roomNumber);
        if (room == null) return -1;
        if (!hasValidStaffName(staffName)) return -4;

        int newStatusIndex = getStatusIndex(newStatus == null ? null : newStatus.trim());
        if (newStatusIndex == -1) return -2;

        String canonicalStatus = STATUS_SEQUENCE[newStatusIndex];
        if (sameStatus(room.getRoomStatus(), canonicalStatus)) return 0;

        String previousStatus = room.getRoomStatus();
        room.setRoomStatus(canonicalStatus);
        taskLogStack.push(new HousekeepingLog(roomNumber.trim(), previousStatus, canonicalStatus, staffName.trim()));
        return 1;
    }

    /**
     * Instantly rolls back the most recent status change by popping the task-log stack
     * and restoring the affected room's previous status.
     * A log can only be safely reversed while the room still has the status
     * written by that log.  In addition, a transition whose previous status
     * was outside the housekeeping workflow is treated as unsafe: another
     * module (for example Front Desk checkout) may have written the same
     * current status in the meantime, and Room does not expose a version
     * number with which to distinguish those writes.
     *
     * @return 1 success | 0 nothing to roll back | -1 room no longer exists |
     *         -2 room was changed externally / rollback is unsafe
     */
    public int rollbackLastChange() {
        if (taskLogStack.isEmpty()) return 0;

        HousekeepingLog lastLog = taskLogStack.peek();
        Room room = findRoomByNumber(lastLog.getRoomNumber());
        if (room == null) return -1;

        if (!sameStatus(room.getRoomStatus(), lastLog.getNewStatus())
                || getStatusIndex(lastLog.getPreviousStatus()) == -1) {
            return -2;
        }

        taskLogStack.pop();
        room.setRoomStatus(lastLog.getPreviousStatus());
        return 1;
    }

    public HousekeepingLog peekLastChange() {
        return taskLogStack.isEmpty() ? null : taskLogStack.peek();
    }

    /** Returns the latest log as display text, or null when no log exists. */
    public String getLastChangeDisplayText() {
        HousekeepingLog last = peekLastChange();
        return last == null ? null : last.toString();
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

    /** Returns filtered/sorted task logs as display text for the boundary. */
    public String[] getFilteredTaskLogDisplayLines(String roomNumberFilter,
                                                    String statusFilter,
                                                    boolean newestFirst) {
        ListInterface<HousekeepingLog> logs = getFilteredTaskLog(roomNumberFilter, statusFilter, newestFirst);
        String[] lines = new String[logs.getNumberOfEntries()];
        for (int i = 0; i < logs.getNumberOfEntries(); i++) {
            lines[i] = logs.get(i).toString();
        }
        return lines;
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

    /** Returns attention-report rows as display text for the boundary. */
    public String[] getRoomsNeedingAttentionDisplayLines() {
        ListInterface<Room> rooms = getRoomsNeedingAttention();
        String[] lines = new String[rooms.getNumberOfEntries()];
        for (int i = 0; i < rooms.getNumberOfEntries(); i++) {
            Room room = rooms.get(i);
            lines[i] = room.getRoomNumber() + " - " + room.getRoomStatus();
        }
        return lines;
    }
}
