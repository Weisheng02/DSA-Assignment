package adt;

import java.util.Comparator;

/**
 * Author: Weisheng
 * Custom Array-Based Collection ADT Implementation
 */
public class MyArrayList<T> implements ListInterface<T> {
    private T[] array;
    private int numberOfEntries;
    private static final int DEFAULT_CAPACITY = 25;

    @SuppressWarnings("unchecked")
    public MyArrayList() {
        array = (T[]) new Object[DEFAULT_CAPACITY];
        numberOfEntries = 0;
    }

    @Override
    public boolean add(T newEntry) {
        if (numberOfEntries >= array.length) {
            doubleCapacity();
        }
        array[numberOfEntries] = newEntry;
        numberOfEntries++;
        return true;
    }

    @Override
    public T get(int index) {
        if (index >= 0 && index < numberOfEntries) {
            return array[index];
        }
        return null;
    }

    @Override
    public int getNumberOfEntries() {
        return numberOfEntries;
    }

    @Override
    public boolean isEmpty() {
        return numberOfEntries == 0;
    }

    @Override
    public void clear() {
        // Release references immediately instead of retaining removed objects
        // until the backing array is resized or garbage-collected with the list.
        for (int i = 0; i < numberOfEntries; i++)
            array[i] = null;
        numberOfEntries = 0;
    }

    // Explicit Selection Sort implementation for the ADT
    @Override
    public void sort(Comparator<T> comparator) {
        for (int i = 0; i < numberOfEntries - 1; i++) {
            int minOrMaxIdx = i;
            for (int j = i + 1; j < numberOfEntries; j++) {
                if (comparator.compare(array[j], array[minOrMaxIdx]) < 0) {
                    minOrMaxIdx = j;
                }
            }
            if (minOrMaxIdx != i) {
                T temp = array[i];
                array[i] = array[minOrMaxIdx];
                array[minOrMaxIdx] = temp;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void doubleCapacity() {
        T[] oldArray = array;
        array = (T[]) new Object[oldArray.length * 2];
        System.arraycopy(oldArray, 0, array, 0, oldArray.length);
    }
}
