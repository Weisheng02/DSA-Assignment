package adt;

/**
 * Author: Weisheng
 * Array-based Binary Search Tree implementation.
 * Supports searching, insertion, deletion, range search, and traversal.
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

    @Override
    public T remove(T entry) {
        if (entry == null || root == null) return null;

        // Use an array to hold the removed value since Java can't return multiple values
        @SuppressWarnings("unchecked")
        T[] removedValue = (T[]) new Comparable[1];
        root = removeNode(root, entry, removedValue);
        if (removedValue[0] != null) {
            numberOfEntries--;
        }
        return removedValue[0];
    }

    private Node<T> removeNode(Node<T> currentNode, T entry, T[] removedValue) {
        if (currentNode == null) {
            return null;
        }

        int comp = entry.compareTo(currentNode.data);
        if (comp < 0) {
            currentNode.left = removeNode(currentNode.left, entry, removedValue);
        } else if (comp > 0) {
            currentNode.right = removeNode(currentNode.right, entry, removedValue);
        } else {
            // Found the node to remove
            removedValue[0] = currentNode.data;

            // Case 1: leaf or only one child
            if (currentNode.left == null) {
                return currentNode.right;
            } else if (currentNode.right == null) {
                return currentNode.left;
            }

            // Case 2: two children -> replace with in-order successor
            T successor = findMinValue(currentNode.right);
            currentNode.data = successor;
            @SuppressWarnings("unchecked")
            T[] temp = (T[]) new Comparable[1];
            currentNode.right = removeNode(currentNode.right, successor, temp);
        }
        return currentNode;
    }

    // Go all the way left to find the smallest value
    private T findMinValue(Node<T> node) {
        while (node.left != null) {
            node = node.left;
        }
        return node.data;
    }

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
    public ListInterface<T> rangeSearch(T minEntry, T maxEntry) {
        ListInterface<T> result = new MyArrayList<>();
        if (minEntry == null || maxEntry == null || root == null) return result;
        rangeSearchHelper(root, minEntry, maxEntry, result);
        return result;
    }

    private void rangeSearchHelper(Node<T> node, T minEntry, T maxEntry, ListInterface<T> result) {
        if (node == null) return;

        int cmpMin = minEntry.compareTo(node.data);
        int cmpMax = maxEntry.compareTo(node.data);

        // Only go left if current node might have values >= minEntry
        if (cmpMin < 0) {
            rangeSearchHelper(node.left, minEntry, maxEntry, result);
        }

        // Add node if it's within range
        if (cmpMin <= 0 && cmpMax >= 0) {
            result.add(node.data);
        }

        // Only go right if current node might have values <= maxEntry
        if (cmpMax > 0) {
            rangeSearchHelper(node.right, minEntry, maxEntry, result);
        }
    }

    @Override
    public T getMin() {
        if (root == null) return null;
        Node<T> current = root;
        while (current.left != null) {
            current = current.left;
        }
        return current.data;
    }

    @Override
    public T getMax() {
        if (root == null) return null;
        Node<T> current = root;
        while (current.right != null) {
            current = current.right;
        }
        return current.data;
    }

    @Override
    public int getHeight() {
        return calcHeight(root);
    }

    private int calcHeight(Node<T> node) {
        if (node == null) return 0;
        int left = calcHeight(node.left);
        int right = calcHeight(node.right);
        return 1 + Math.max(left, right);
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
