package adt;

import java.util.function.Function;

/**
 * Author: Weisheng
 * Linked-node Binary Search Tree implementation.
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

    /** Mutable return holder used by recursive deletion without unsafe generic arrays. */
    private static class ValueHolder<E> {
        private E value;
    }

    public BinarySearchTree() {
        root = null;
        numberOfEntries = 0;
    }

    @Override
    public boolean add(T newEntry) {
        if (newEntry == null) return false;
        // This BST is used for unique identifiers (confirmation number, room
        // number and reward ID). Rejecting duplicate keys also keeps search,
        // range search and rebalance under one simple, consistent rule.
        if (contains(newEntry)) return false;
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
        } else if (comp > 0) {
            currentNode.right = addNode(currentNode.right, newEntry);
        }
        return currentNode;
    }

    @Override
    public T remove(T entry) {
        if (entry == null || root == null) return null;

        ValueHolder<T> removedValue = new ValueHolder<>();
        root = removeNode(root, entry, removedValue);
        if (removedValue.value != null) {
            numberOfEntries--;
        }
        return removedValue.value;
    }

    private Node<T> removeNode(Node<T> currentNode, T entry, ValueHolder<T> removedValue) {
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
            removedValue.value = currentNode.data;

            // Case 1: leaf or only one child
            if (currentNode.left == null) {
                return currentNode.right;
            } else if (currentNode.right == null) {
                return currentNode.left;
            }

            // Case 2: two children -> replace with in-order successor
            T successor = findMinValue(currentNode.right);
            currentNode.data = successor;
            ValueHolder<T> temp = new ValueHolder<>();
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

    /**
     * Pre-Order Traversal: Root -> Left -> Right
     * Useful for creating a copy of the tree or serializing tree structure.
     * Time Complexity: O(n)
     */
    @Override
    public ListInterface<T> preOrderTraversal() {
        ListInterface<T> resultList = new MyArrayList<>();
        preOrder(root, resultList);
        return resultList;
    }

    private void preOrder(Node<T> currentNode, ListInterface<T> list) {
        if (currentNode != null) {
            list.add(currentNode.data);
            preOrder(currentNode.left, list);
            preOrder(currentNode.right, list);
        }
    }

    /**
     * Post-Order Traversal: Left -> Right -> Root
     * Useful for bottom-up operations like node deletion or calculating subtree metrics.
     * Time Complexity: O(n)
     */
    @Override
    public ListInterface<T> postOrderTraversal() {
        ListInterface<T> resultList = new MyArrayList<>();
        postOrder(root, resultList);
        return resultList;
    }

    private void postOrder(Node<T> currentNode, ListInterface<T> list) {
        if (currentNode != null) {
            postOrder(currentNode.left, list);
            postOrder(currentNode.right, list);
            list.add(currentNode.data);
        }
    }

    /**
     * Rebalances an unbalanced BST into a height-balanced BST.
     * Algorithm Steps:
     * 1. Collect all elements in sorted order via in-order traversal (O(n)).
     * 2. Build a balanced tree by picking the middle element recursively (O(n)).
     * Total Time Complexity: O(n), Space Complexity: O(n).
     */
    @Override
    public void rebalance() {
        ListInterface<T> sortedList = inOrderTraversal();
        root = buildBalancedTree(sortedList, 0, sortedList.getNumberOfEntries() - 1);
    }

    private Node<T> buildBalancedTree(ListInterface<T> list, int start, int end) {
        if (start > end) {
            return null;
        }
        int mid = start + (end - start) / 2;
        Node<T> node = new Node<>(list.get(mid));
        node.left = buildBalancedTree(list, start, mid - 1);
        node.right = buildBalancedTree(list, mid + 1, end);
        return node;
    }

    /**
     * Calculates the total leaf nodes in the tree (nodes with no children).
     * Time Complexity: O(n)
     */
    @Override
    public int getLeafCount() {
        return countLeaves(root);
    }

    private int countLeaves(Node<T> node) {
        if (node == null) return 0;
        if (node.left == null && node.right == null) return 1;
        return countLeaves(node.left) + countLeaves(node.right);
    }

    /**
     * Checks if the BST is height-balanced.
     * Height-balanced definition: For every node, |height(left) - height(right)| <= 1.
     * Time Complexity: O(n)
     */
    @Override
    public boolean isBalanced() {
        return checkBalanceHeight(root) != -1;
    }

    private int checkBalanceHeight(Node<T> node) {
        if (node == null) return 0;
        int leftH = checkBalanceHeight(node.left);
        if (leftH == -1) return -1;
        int rightH = checkBalanceHeight(node.right);
        if (rightH == -1) return -1;

        if (Math.abs(leftH - rightH) > 1) return -1;
        return 1 + Math.max(leftH, rightH);
    }

    /**
     * Prints an ASCII visualization of the tree structure.
     */
    @Override
    public void printTree() {
        System.out.print(getTreeDisplayText());
    }

    @Override
    public String getTreeDisplayText() {
        return getTreeDisplayText(String::valueOf);
    }

    @Override
    public String getTreeDisplayText(Function<T, String> labelFormatter) {
        if (root == null) return " (Empty Tree)\n";
        StringBuilder output = new StringBuilder();
        Function<T, String> formatter = labelFormatter == null ? String::valueOf : labelFormatter;
        appendTreeDisplay(root, "", true, output, formatter);
        return output.toString();
    }

    private void appendTreeDisplay(Node<T> node, String prefix, boolean isTail, StringBuilder output,
            Function<T, String> labelFormatter) {
        if (node == null) return;
        output.append(prefix).append(isTail ? "└── " : "├── ")
                .append(labelFormatter.apply(node.data)).append('\n');
        String newPrefix = prefix + (isTail ? "    " : "│   ");
        boolean hasRight = node.right != null;
        boolean hasLeft = node.left != null;
        if (hasLeft && hasRight) {
            appendTreeDisplay(node.right, newPrefix, false, output, labelFormatter);
            appendTreeDisplay(node.left, newPrefix, true, output, labelFormatter);
        } else if (hasLeft) {
            appendTreeDisplay(node.left, newPrefix, true, output, labelFormatter);
        } else if (hasRight) {
            appendTreeDisplay(node.right, newPrefix, true, output, labelFormatter);
        }
    }

    @Override
    public String getTopDownTreeDisplayText(Function<T, String> labelFormatter) {
        if (root == null) return " (Empty Tree)\n";

        Function<T, String> formatter = labelFormatter == null ? String::valueOf : labelFormatter;
        int height = getHeight();
        // A complete top-down layout doubles in width at each level. Fall back
        // to the compact side view if a highly unbalanced tree becomes too wide.
        if (height > 6) {
            return "(Tree is taller than 6 levels; compact side view shown.)\n"
                    + getTreeDisplayText(formatter);
        }

        int maxLabelLength = getMaxLabelLength(root, formatter);
        int cellWidth = Math.max(3, maxLabelLength + 2);
        int leafSlots = 1 << (height - 1);
        int canvasWidth = leafSlots * cellWidth;
        char[][] canvas = new char[height * 2 - 1][canvasWidth + 1];
        for (int row = 0; row < canvas.length; row++) {
            for (int column = 0; column < canvas[row].length; column++)
                canvas[row][column] = ' ';
        }

        int rootCentre = canvasWidth / 2;
        int firstOffset = height == 1 ? 0 : canvasWidth / 4;
        placeTopDownNode(root, 0, rootCentre, firstOffset, canvas, formatter);

        StringBuilder output = new StringBuilder();
        for (char[] row : canvas) {
            int lastCharacter = row.length - 1;
            while (lastCharacter >= 0 && row[lastCharacter] == ' ')
                lastCharacter--;
            if (lastCharacter >= 0)
                output.append(row, 0, lastCharacter + 1);
            output.append('\n');
        }
        return output.toString();
    }

    private int getMaxLabelLength(Node<T> node, Function<T, String> formatter) {
        if (node == null) return 0;
        String label = formatLabel(node.data, formatter);
        return Math.max(label.length(),
                Math.max(getMaxLabelLength(node.left, formatter), getMaxLabelLength(node.right, formatter)));
    }

    private void placeTopDownNode(Node<T> node, int depth, int centre, int childOffset,
            char[][] canvas, Function<T, String> formatter) {
        if (node == null) return;

        String label = formatLabel(node.data, formatter);
        writeText(canvas[depth * 2], centre - label.length() / 2, label);

        int nextOffset = Math.max(1, childOffset / 2);
        if (node.left != null) {
            int childCentre = centre - childOffset;
            canvas[depth * 2 + 1][(centre + childCentre) / 2] = '/';
            placeTopDownNode(node.left, depth + 1, childCentre, nextOffset, canvas, formatter);
        }
        if (node.right != null) {
            int childCentre = centre + childOffset;
            canvas[depth * 2 + 1][(centre + childCentre) / 2] = '\\';
            placeTopDownNode(node.right, depth + 1, childCentre, nextOffset, canvas, formatter);
        }
    }

    private String formatLabel(T data, Function<T, String> formatter) {
        String label = formatter.apply(data);
        return label == null ? "null" : label.replace('\n', ' ').replace('\r', ' ');
    }

    private void writeText(char[] row, int start, String text) {
        for (int i = 0; i < text.length() && start + i < row.length; i++) {
            if (start + i >= 0)
                row[start + i] = text.charAt(i);
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
