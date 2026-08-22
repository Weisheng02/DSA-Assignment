package entity;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Author: Nyong Kai Wei
 * Entity class representing a single Housekeeping status-change record.
 * Each time a room's cleaning status is updated, one HousekeepingLog entry
 * is created and pushed onto the task-log Stack, so that the most recent
 * change can be instantly rolled back if it was logged in error.
 */
public class HousekeepingLog {

    private static int idCounter = 1;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private int taskId;
    private String roomNumber;
    private String previousStatus;
    private String newStatus;
    private String staffName;
    private String timestamp;
    private String rollbackStatus;

    public HousekeepingLog(String roomNumber, String previousStatus, String newStatus, String staffName) {
        this.taskId = idCounter++;
        this.roomNumber = roomNumber;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.staffName = staffName;
        this.timestamp = LocalDateTime.now().format(FORMATTER);
        this.rollbackStatus = "Available";
    }

    public int getTaskId() {
        return taskId;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public String getPreviousStatus() {
        return previousStatus;
    }

    public String getNewStatus() {
        return newStatus;
    }

    public String getStaffName() {
        return staffName;
    }

    public String getTimestamp() {
        return timestamp;
    }

    /**
     * Describes whether this change is still available for rollback. Keeping this
     * state on the history record allows management reports to retain changes even
     * after the undo stack has popped them.
     */
    public String getRollbackStatus() {
        return rollbackStatus;
    }

    public void markRolledBack() {
        rollbackStatus = "Rolled Back";
    }

    public void markInvalidated() {
        rollbackStatus = "Invalidated";
    }

}
