package adt;

/**
 * Author: Weisheng
 * Collection ADT Interface for Queue (FIFO) Operations.
 * This is the Linear ADT used by the Walk-In Registrations & Standard Booking
 * module: incoming guests are enqueued in chronological order, and the next
 * guest to be served is always dequeued from the front.
 */
public interface QueueInterface<T> {

    /**
     * Adds a new entry to the back of this queue.
     * @param newEntry An object to be added.
     * @return true if the addition is successful.
     */
    boolean enqueue(T newEntry);

    /**
     * Removes and returns the entry at the front of this queue.
     * @return The front entry, or null if the queue is empty.
     */
    T dequeue();

    /**
     * Returns the entry at the front of this queue without removing it.
     * @return The front entry, or null if the queue is empty.
     */
    T getFront();

    /**
     * Checks whether this queue is empty.
     * @return true if the queue is empty; false otherwise.
     */
    boolean isEmpty();

    /**
     * Gets the number of entries currently in this queue.
     * @return The integer number of entries.
     */
    int getNumberOfEntries();

    /**
     * Removes all entries from this queue.
     */
    void clear();

    /**
     * Returns a snapshot of all entries, ordered from front to back,
     * WITHOUT modifying this queue. Used to generate reports while
     * keeping the queue intact.
     * @return A ListInterface containing all entries, front-to-back order.
     */
    ListInterface<T> toList();
}
