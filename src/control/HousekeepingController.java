package control;

import adt.ArrayStack;
import adt.ListInterface;
import adt.MyArrayList;
import adt.StackInterface;
import entity.HousekeepingLog;
import entity.Room;

import java.util.Comparator;

/**
 * Author: Nyong Kai Wei
 * Controller Class for Housekeeping & Task Log Operations (Linear ADT: Stack).
 *
 * Room statuses move forward through a fixed sequence:
 * Dirty -> Cleaning In Progress -> Inspected -> Ready for Check-In
 * Every change is pushed onto a Stack-based task log. If a supervisor logs
 * an incorrect status, or a guest requests a late check-out mid-cleaning,
 * the most recent change can be instantly rolled back by popping the stack.
 */
public class HousekeepingController {

    private static final String[] STATUS_SEQUENCE = {
            "Dirty", "Cleaning In Progress", "Inspected", "Ready for Check-In"
    };

    private static final String[] SUMMARY_STATUS_LABELS = {
            "Dirty", "Cleaning In Progress", "Inspected", "Ready for Check-In",
            "Reserved", "Occupied", "Maintenance", "Other"
    };

    private ListInterface<Room> roomList;
    private StackInterface<HousekeepingLog> undoStack;
    private ListInterface<HousekeepingLog> taskHistory;

    public HousekeepingController(ListInterface<Room> sharedRoomList) {
        this.roomList = (sharedRoomList != null) ? sharedRoomList : new MyArrayList<>();
        this.undoStack = new ArrayStack<>();
        this.taskHistory = new MyArrayList<>();
    }

    // Linear search through the room list by room number.
    public Room findRoomByNumber(String roomNumber) {
        return ControllerDataSupport.findRoomByNumber(roomList, roomNumber);
    }

    public ListInterface<Room> getAllRooms() {
        return roomList;
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
        if (status == null)
            return -1;
        for (int i = 0; i < STATUS_SEQUENCE.length; i++) {
            if (STATUS_SEQUENCE[i].equalsIgnoreCase(status))
                return i;
        }
        return -1;
    }

    /**
     * Advances a room to the NEXT stage in the cleaning sequence.
     *
     * @return 1 success | -1 room not found | -2 already at final stage |
     *         -3 room not currently in the housekeeping cycle (e.g. Occupied) |
     *         -4 staff name is blank
     */
    public int advanceRoomStatus(String roomNumber, String staffName) {
        Room room = findRoomByNumber(roomNumber);
        if (room == null)
            return -1;
        if (!hasValidStaffName(staffName))
            return -4;

        int currentIndex = getStatusIndex(room.getRoomStatus());
        if (currentIndex == -1)
            return -3;
        if (currentIndex >= STATUS_SEQUENCE.length - 1)
            return -2;

        String previousStatus = room.getRoomStatus();
        String newStatus = STATUS_SEQUENCE[currentIndex + 1];
        room.setRoomStatus(newStatus);

        recordStatusChange(new HousekeepingLog(roomNumber.trim(), previousStatus, newStatus, staffName.trim()));
        return 1;
    }

    /**
     * Lets a supervisor directly set/correct a room's status (e.g. a late check-out
     * request forces a "Ready for Check-In" room back to "Dirty"). Also logged for
     * rollback.
     *
     * @return 1 success | 0 status was already set (no log created) |
     *         -1 room not found | -2 invalid status name | -4 staff name is blank
     */
    public int setRoomStatus(String roomNumber, String newStatus, String staffName) {
        Room room = findRoomByNumber(roomNumber);
        if (room == null)
            return -1;
        if (!hasValidStaffName(staffName))
            return -4;

        int newStatusIndex = getStatusIndex(newStatus == null ? null : newStatus.trim());
        if (newStatusIndex == -1)
            return -2;

        String canonicalStatus = STATUS_SEQUENCE[newStatusIndex];
        if (sameStatus(room.getRoomStatus(), canonicalStatus))
            return 0;

        String previousStatus = room.getRoomStatus();
        room.setRoomStatus(canonicalStatus);
        recordStatusChange(
                new HousekeepingLog(roomNumber.trim(), previousStatus, canonicalStatus, staffName.trim()));
        return 1;
    }

    private void recordStatusChange(HousekeepingLog log) {
        undoStack.push(log);
        taskHistory.add(log);
    }

    /**
     * Instantly rolls back the most recent status change by popping the undo
     * stack
     * and restoring the affected room's previous status.
     * A log can only be safely reversed while the room still has the status
     * written by that log. This allows a valid correction such as
     * Occupied -> Dirty to be restored, while still refusing to overwrite a
     * later external change.
     *
     * @return 1 success | 0 nothing to roll back | -1 room no longer exists |
     *         -2 room was changed externally / rollback is unsafe
     */
    public int rollbackLastChange() {
        if (undoStack.isEmpty())
            return 0;

        HousekeepingLog lastLog = undoStack.peek();
        Room room = findRoomByNumber(lastLog.getRoomNumber());
        if (room == null) {
            undoStack.pop();
            lastLog.markInvalidated();
            return -1;
        }

        if (!sameStatus(room.getRoomStatus(), lastLog.getNewStatus())) {
            // The log is stale because another module changed the room after this
            // housekeeping action. Discard it so one unsafe entry cannot block all
            // older rollback records forever.
            undoStack.pop();
            lastLog.markInvalidated();
            return -2;
        }

        undoStack.pop();
        room.setRoomStatus(lastLog.getPreviousStatus());
        lastLog.markRolledBack();
        return 1;
    }

    public HousekeepingLog peekLastChange() {
        return undoStack.isEmpty() ? null : undoStack.peek();
    }

    public int getLoggedTaskCount() {
        return taskHistory.getNumberOfEntries();
    }

    // ---------------- Reports ----------------

    public String[] getSummaryStatusLabels() {
        return SUMMARY_STATUS_LABELS.clone();
    }

    // Report 1: Count all rooms by status so the category totals reconcile with
    // the overall room count. Unrecognised future statuses are grouped as Other.
    public int[] getRoomStatusSummary() {
        int[] summary = new int[SUMMARY_STATUS_LABELS.length + 1];
        summary[0] = roomList.getNumberOfEntries();
        for (int i = 0; i < roomList.getNumberOfEntries(); i++) {
            String roomStatus = roomList.get(i).getRoomStatus();
            int summaryIndex = SUMMARY_STATUS_LABELS.length; // Other
            for (int j = 0; j < SUMMARY_STATUS_LABELS.length - 1; j++) {
                if (sameStatus(roomStatus, SUMMARY_STATUS_LABELS[j])) {
                    summaryIndex = j + 1;
                    break;
                }
            }
            summary[summaryIndex]++;
        }
        return summary;
    }

    // Report 2: Multi-criteria filtered + sorted Task Log report (search + sort
    // combined).
    public ListInterface<HousekeepingLog> getFilteredTaskLog(String roomNumberFilter, String statusFilter,
            boolean newestFirst) {
        ListInterface<HousekeepingLog> filtered = new MyArrayList<>();
        for (int i = 0; i < taskHistory.getNumberOfEntries(); i++) {
            HousekeepingLog log = taskHistory.get(i);
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

    // Report 3 (bonus): Rooms not yet Ready for Check-In, sorted by how far behind
    // they are.
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
