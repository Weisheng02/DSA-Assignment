package adt;

/**
 * Author: Weisheng
 * Array-based Priority Queue Collection ADT Implementation
 */
public class MyPriorityQueue<T extends Comparable<T>> implements PriorityQueueInterface<T> {
    private T[] array;
    private int numberOfEntries;
    private static final int DEFAULT_CAPACITY = 25;

    @SuppressWarnings("unchecked")
    public MyPriorityQueue() {
        array = (T[]) new Comparable[DEFAULT_CAPACITY];
        numberOfEntries = 0;
    }

    @Override
    public boolean enqueue(T newEntry) {
        if (numberOfEntries >= array.length) {
            doubleCapacity();
        }

        int index = numberOfEntries - 1;
        while (index >= 0 && newEntry.compareTo(array[index]) > 0) {
            array[index + 1] = array[index];
            index--;
        }
        array[index + 1] = newEntry;
        numberOfEntries++;
        return true;
    }

    @Override
    public T dequeue() {
        if (isEmpty()) return null;
        T front = array[0];
        for (int i = 0; i < numberOfEntries - 1; i++) {
            array[i] = array[i + 1];
        }
        array[numberOfEntries - 1] = null;
        numberOfEntries--;
        return front;
    }

    @Override
    public T getFront() {
        if (isEmpty()) return null;
        return array[0];
    }

    @Override
    public boolean isEmpty() {
        return numberOfEntries == 0;
    }

    @Override
    public void clear() {
        for (int i = 0; i < numberOfEntries; i++) {
            array[i] = null;
        }
        numberOfEntries = 0;
    }

    @Override
    public int getNumberOfEntries() {
        return numberOfEntries;
    }

    @SuppressWarnings("unchecked")
    private void doubleCapacity() {
        T[] oldArray = array;
        array = (T[]) new Comparable[oldArray.length * 2];
        System.arraycopy(oldArray, 0, array, 0, oldArray.length);
    }
}
