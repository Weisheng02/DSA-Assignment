package adt;

/**
 * Author: Weisheng
 * Non-Linear Data Structure Interface: Binary Search Tree (BST)
 */
public interface BSTInterface<T extends Comparable<T>> {
    /**
     * Adds a new entry to this binary search tree non-linearly.
     * @param newEntry An object to be added.
     * @return true if the addition is successful.
     */
    boolean add(T newEntry);

    /**
     * Non-linear tree search for a specific entry.
     * @param entry An object to search for.
     * @return The matched entry if found; null otherwise.
     */
    T search(T entry);

    /**
     * Checks if an entry exists in this BST.
     * @param entry An object to check.
     * @return true if present; false otherwise.
     */
    boolean contains(T entry);

    /**
     * Performs an in-order traversal of the tree and returns entries in sorted order.
     * @return A ListInterface containing entries in in-order sequence.
     */
    ListInterface<T> inOrderTraversal();

    /**
     * Gets the number of entries currently in this tree.
     * @return The integer number of entries.
     */
    int getNumberOfEntries();

    /**
     * Checks whether this tree is empty.
     * @return true if the tree is empty; false otherwise.
     */
    boolean isEmpty();

    /**
     * Removes all entries from this tree.
     */
    void clear();
}
