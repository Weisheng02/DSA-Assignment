package adt;

/**
 * @Weisheng
 * @param <T> The type of elements held in this collection
 */
public class MyPriorityQueue<T> implements PriorityQueueInterface<T> {

    private class Node {
        private T data;
        private int priority;
        private Node next;

        private Node(T data, int priority) {
            this.data = data;
            this.priority = priority;
            this.next = null;
        }
    }

    private Node firstNode;
    private int numberOfEntries;

    public MyPriorityQueue() {
        this.firstNode = null;
        this.numberOfEntries = 0;
    }

    @Override
    public boolean enqueue(T element, int priority) {
        if (element == null) {
            return false;
        }

        Node newNode = new Node(element, priority);

        // Case 1: Queue is empty OR the new node has higher priority than the head node
        if (isEmpty() || priority > firstNode.priority) {
            newNode.next = firstNode;
            firstNode = newNode;
        } else {
            // Case 2: Traverse the queue to find the correct insertion position
            Node currentNode = firstNode;
            while (currentNode.next != null && currentNode.next.priority >= priority) {
                currentNode = currentNode.next;
            }
            newNode.next = currentNode.next;
            currentNode.next = newNode;
        }

        numberOfEntries++;
        return true;
    }

    @Override
    public T dequeue() {
        if (isEmpty()) {
            return null;
        }

        T highestPriorityData = firstNode.data;
        firstNode = firstNode.next;
        numberOfEntries--;
        return highestPriorityData;
    }

    @Override
    public T peek() {
        if (isEmpty()) {
            return null;
        }
        return firstNode.data;
    }

    @Override
    public boolean isEmpty() {
        return numberOfEntries == 0;
    }

    @Override
    public int size() {
        return numberOfEntries;
    }

    @Override
    public void clear() {
        firstNode = null;
        numberOfEntries = 0;
    }
}