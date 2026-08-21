package adt;

/**
 * Author: Nyong Kai Wei
 * Collection ADT Interface for Stack (LIFO) Operations.
 * This is the Linear ADT used by the Housekeeping & Task Log module:
 * every status change is pushed on, and an incorrect change can be
 * instantly undone by popping the most recent entry off.
 */
public interface StackInterface<T> {

    /**
     * Pushes a new entry onto the top of this stack.
     *
     * @param newEntry An object to be added.
     * @return true if the addition is successful.
     */
    boolean push(T newEntry);

    /**
     * Removes and returns the entry at the top of this stack.
     *
     * @return The top entry, or null if the stack is empty.
     */
    T pop();

    /**
     * Returns the entry at the top of this stack without removing it.
     *
     * @return The top entry, or null if the stack is empty.
     */
    T peek();

    /**
     * Checks whether this stack is empty.
     *
     * @return true if the stack is empty; false otherwise.
     */
    boolean isEmpty();

    /**
     * Gets the number of entries currently in this stack.
     *
     * @return The integer number of entries.
     */
    int getNumberOfEntries();

    /**
     * Removes all entries from this stack.
     */
    void clear();

    /**
     * Returns a snapshot of all entries, ordered from most-recently-pushed
     * to least-recently-pushed, WITHOUT modifying this stack. Used to
     * generate task-log reports while keeping rollback available.
     *
     * @return A ListInterface containing all entries, top-to-bottom order.
     */
    ListInterface<T> toList();
}
