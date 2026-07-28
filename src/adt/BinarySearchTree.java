package adt;

/**
 * Author: Weisheng
 * Non-Linear Data Structure Implementation: Binary Search Tree (BST)
 * Supports O(log n) Tree Searching and In-Order Traversal.
 */
public class BinarySearchTree<T extends Comparable<T>> implements BSTInterface<T> {

    private Node<T> root;
    private int numberOfEntries;

    private static class Node<T> {
        private T data;
        private Node<T> left;
        private Node<T> right;

        public Node(T data) {
            this.data = data;
            this.left = null;
            this.right = null;
        }
    }

    public BinarySearchTree() {
        root = null;
        numberOfEntries = 0;
    }

    @Override
    public boolean add(T newEntry) {
        if (newEntry == null) return false;
        root = addNode(root, newEntry);
        numberOfEntries++;
        return true;
    }

    private Node<T> addNode(Node<T> currentNode, T newEntry) {
        if (currentNode == null) {
            return new Node<>(newEntry);
        }

        int comp = newEntry.compareTo(currentNode.data);
        if (comp < 0) {
            currentNode.left = addNode(currentNode.left, newEntry);
        } else {
            currentNode.right = addNode(currentNode.right, newEntry);
        }
        return currentNode;
    }

    // Non-linear BST Search Algorithm O(log n)
    @Override
    public T search(T entry) {
        if (entry == null || root == null) return null;
        return searchNode(root, entry);
    }

    private T searchNode(Node<T> currentNode, T entry) {
        if (currentNode == null) {
            return null;
        }

        int comp = entry.compareTo(currentNode.data);
        if (comp == 0) {
            return currentNode.data;
        } else if (comp < 0) {
            return searchNode(currentNode.left, entry);
        } else {
            return searchNode(currentNode.right, entry);
        }
    }

    @Override
    public boolean contains(T entry) {
        return search(entry) != null;
    }

    @Override
    public ListInterface<T> inOrderTraversal() {
        ListInterface<T> resultList = new MyArrayList<>();
        inOrder(root, resultList);
        return resultList;
    }

    private void inOrder(Node<T> currentNode, ListInterface<T> list) {
        if (currentNode != null) {
            inOrder(currentNode.left, list);
            list.add(currentNode.data);
            inOrder(currentNode.right, list);
        }
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
        root = null;
        numberOfEntries = 0;
    }
}
