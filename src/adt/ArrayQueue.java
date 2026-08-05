package adt;

/**
 * Author: Zhi Xuan
 * Custom Array-Based Collection ADT Implementation: Circular Queue (Linear
 * ADT).
 * Uses a circular array with front and rear pointers to achieve O(1) enqueue
 * and dequeue operations. The Walk-In Registrations module relies on this
 * structure to serve guests in strict FIFO (first-come-first-served) order.
 */
public class ArrayQueue<T> implements QueueInterface<T> {

    private T[] array;
    private int front;
    private int rear;
    private int numberOfEntries;
    private static final int DEFAULT_CAPACITY = 25;

    @SuppressWarnings("unchecked")
    public ArrayQueue() {
        array = (T[]) new Object[DEFAULT_CAPACITY];
        front = 0;
        rear = -1;
        numberOfEntries = 0;
    }

    @Override
    public boolean enqueue(T newEntry) {
        if (newEntry == null)
            return false;
        if (numberOfEntries >= array.length) {
            doubleCapacity();
        }
        rear = (rear + 1) % array.length;
        array[rear] = newEntry;
        numberOfEntries++;
        return true;
    }

    @Override
    public T dequeue() {
        if (isEmpty())
            return null;
        T frontEntry = array[front];
        array[front] = null; // avoid holding a stale reference
        front = (front + 1) % array.length;
        numberOfEntries--;
        return frontEntry;
    }

    @Override
    public T getFront() {
        if (isEmpty())
            return null;
        return array[front];
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
            int index = (front + i) % array.length;
            array[index] = null;
        }
        front = 0;
        rear = -1;
        numberOfEntries = 0;
    }

    @Override
    public ListInterface<T> toList() {
        ListInterface<T> result = new MyArrayList<>();
        for (int i = 0; i < numberOfEntries; i++) {
            int index = (front + i) % array.length;
            result.add(array[index]);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private void doubleCapacity() {
        T[] newArray = (T[]) new Object[array.length * 2];
        for (int i = 0; i < numberOfEntries; i++) {
            newArray[i] = array[(front + i) % array.length];
        }
        array = newArray;
        front = 0;
        rear = numberOfEntries - 1;
    }
}
