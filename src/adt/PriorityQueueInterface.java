package adt;

/**
 * Author: Weisheng
 * Priority Queue ADT Interface
 */
public interface PriorityQueueInterface<T extends Comparable<T>> {
    boolean enqueue(T newEntry);
    T dequeue();
    T getFront();
    boolean isEmpty();
    void clear();
    int getNumberOfEntries();
}
