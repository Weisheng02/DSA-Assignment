package adt;

/**
 * Author: [Your Name Here]
 * Custom Array-Based Collection ADT Implementation: Stack (Linear ADT).
 * Supports O(1) push/pop/peek, used to give the Housekeeping module its
 * instant-rollback capability.
 */
public class ArrayStack<T> implements StackInterface<T> {

    private T[] array;
    private int numberOfEntries;
    private static final int DEFAULT_CAPACITY = 25;

    @SuppressWarnings("unchecked")
    public ArrayStack() {
        array = (T[]) new Object[DEFAULT_CAPACITY];
        numberOfEntries = 0;
    }

    @Override
    public boolean push(T newEntry) {
        if (newEntry == null) return false;
        if (numberOfEntries >= array.length) {
            doubleCapacity();
        }
        array[numberOfEntries] = newEntry;
        numberOfEntries++;
        return true;
    }

    @Override
    public T pop() {
        if (isEmpty()) return null;
        numberOfEntries--;
        T top = array[numberOfEntries];
        array[numberOfEntries] = null; // avoid holding a stale reference
        return top;
    }

    @Override
    public T peek() {
        if (isEmpty()) return null;
        return array[numberOfEntries - 1];
    }

    @Override
    public boolean isEmpty() {
        return numberOfEntries == 0;
    }

    @Override
    public int getNumberOfEntries() {
        return numberOfEntries;
    }

    @Override
    public void clear() {
        for (int i = 0; i < numberOfEntries; i++) {
            array[i] = null;
        }
        numberOfEntries = 0;
    }

    @Override
    public ListInterface<T> toList() {
        ListInterface<T> result = new MyArrayList<>();
        for (int i = numberOfEntries - 1; i >= 0; i--) {
            result.add(array[i]);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private void doubleCapacity() {
        T[] oldArray = array;
        array = (T[]) new Object[oldArray.length * 2];
        System.arraycopy(oldArray, 0, array, 0, oldArray.length);
    }
}
