package adt;

import java.util.Comparator;

/**
 * Author: Weisheng
 * Collection ADT Interface for List Operations
 */
public interface ListInterface<T> {
    boolean add(T newEntry);

    T get(int index);

    int getNumberOfEntries();

    boolean isEmpty();

    void clear();

    void sort(Comparator<T> comparator);
}