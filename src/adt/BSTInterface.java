package adt;

/**
 * Author: Weisheng
 * Non-Linear Data Structure Interface: Binary Search Tree (BST)
 *
 * @param <T> The type of elements held in this tree, must implement Comparable.
 */
public interface BSTInterface<T extends Comparable<T>> {

    /**
     * Adds a new entry to this binary search tree.
     * @param newEntry An object to be added.
     * @return true if the addition is successful; false if newEntry is null.
     */
    boolean add(T newEntry);

    /**
     * Removes a specific entry from this binary search tree.
     * Handles leaf node, single child, and two children cases.
     *
     * @param entry The object to remove.
     * @return The removed entry if found; null otherwise.
     */
    T remove(T entry);

    /**
     * Searches for a specific entry in the tree.
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
     * Retrieves all entries whose keys fall within [minEntry, maxEntry].
     * Only traverses subtrees that may contain results.
     *
     * @param minEntry The lower boundary.
     * @param maxEntry The upper boundary.
     * @return A ListInterface containing matching entries in sorted order.
     */
    ListInterface<T> rangeSearch(T minEntry, T maxEntry);

    /**
     * Finds the smallest element in this BST.
     * @return The minimum entry, or null if tree is empty.
     */
    T getMin();

    /**
     * Finds the largest element in this BST.
     * @return The maximum entry, or null if tree is empty.
     */
    T getMax();

    /**
     * Calculates the height of this tree.
     * @return The height (0 if empty).
     */
    int getHeight();

    /**
     * Returns all entries in sorted order via in-order traversal.
     * @return A ListInterface containing all entries in ascending order.
     */
    ListInterface<T> inOrderTraversal();

    /**
     * Gets the number of entries in this tree.
     * @return The number of entries.
     */
    int getNumberOfEntries();

    /**
     * Checks whether this tree is empty.
     * @return true if empty; false otherwise.
     */
    boolean isEmpty();

    /**
     * Removes all entries from this tree.
     */
    void clear();
}
