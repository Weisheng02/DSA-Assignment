package adt;

/**
 * @Weisheng
 * @param <T> The type of elements held in this collection
 */
public interface PriorityQueueInterface<T> {

    /**
     * Adds a new element into the priority queue based on its priority level.
     * Higher priority value means higher processing preference.
     */
    boolean enqueue(T element, int priority);

    /**
     * Retrieves and removes the element with the highest priority.
     */
    T dequeue();

    /**
     * Retrieves, but does not remove, the element with the highest priority.
     */
    T peek();

    /**
     * Checks if the priority queue is empty.
     */
    boolean isEmpty();

    /**
     * Returns the total number of elements in the priority queue.
     */
    int size();

    /**
     * Removes all elements from the priority queue.
     */
    void clear();
}