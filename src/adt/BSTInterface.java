package adt;

import java.util.function.Function;

/**
 * Author: Weisheng
 * Non-Linear Data Structure Interface: Binary Search Tree (BST)
 * Conceptual reference: BMCS2063 Chapter 9 Binary Trees lecture and practical
 * materials. 
 * @param <T> The type of elements held in this tree, must implement Comparable.
 */
public interface BSTInterface<T extends Comparable<T>> {

    /**
     * Adds a new entry to this binary search tree.
     *
     * @param newEntry An object to be added.
     * @return true if the addition is successful; false if newEntry is null or
     *         an equal key already exists.
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
     *
     * @param entry An object to search for.
     * @return The matched entry if found; null otherwise.
     */
    T search(T entry);

    /**
     * Checks if an entry exists in this BST.
     *
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
     *
     * @return The minimum entry, or null if tree is empty.
     */
    T getMin();

    /**
     * Finds the largest element in this BST.
     *
     * @return The maximum entry, or null if tree is empty.
     */
    T getMax();

    /**
     * Calculates the height of this tree.
     *
     * @return The height (0 if empty).
     */
    int getHeight();

    /**
     * Returns all entries in sorted order via in-order traversal (Left -> Root ->
     * Right).
     *
     * @return A ListInterface containing all entries in ascending order.
     */
    ListInterface<T> inOrderTraversal();

    /**
     * Returns all entries via pre-order traversal (Root -> Left -> Right).
     *
     * @return A ListInterface containing entries in pre-order sequence.
     */
    ListInterface<T> preOrderTraversal();

    /**
     * Returns all entries via post-order traversal (Left -> Right -> Root).
     *
     * @return A ListInterface containing entries in post-order sequence.
     */
    ListInterface<T> postOrderTraversal();

    /**
     * Rebalances the BST into a height-balanced BST.
     * Extracts sorted entries via in-order traversal and rebuilds recursively.
     */
    void rebalance();

    /**
     * Counts the total number of leaf nodes in the tree (nodes with 0 children).
     *
     * @return Total leaf node count.
     */
    int getLeafCount();

    /**
     * Checks if the BST is height-balanced (height difference between left and
     * right subtrees <= 1 at every node).
     *
     * @return true if balanced; false otherwise.
     */
    boolean isBalanced();

    /**
     * Prints an ASCII visual representation of the tree structure.
     */
    void printTree();

    /**
     * Returns the same ASCII tree representation without writing to the console.
     */
    String getTreeDisplayText();

    /** Returns the ASCII tree using a caller-supplied compact node label. */
    String getTreeDisplayText(Function<T, String> labelFormatter);

    /**
     * Returns a top-down tree diagram with the root centred above its left and
     * right subtrees. Compact labels should be used so the diagram fits in a
     * normal console window.
     */
    String getTopDownTreeDisplayText(Function<T, String> labelFormatter);

    /**
     * Gets the total number of entries stored in this tree.
     *
     * @return The total entry count.
     */
    int getNumberOfEntries();

    /**
     * Checks whether this tree is empty.
     *
     * @return true if empty; false otherwise.
     */
    boolean isEmpty();

    /**
     * Removes all entries from this tree.
     */
    void clear();
}
