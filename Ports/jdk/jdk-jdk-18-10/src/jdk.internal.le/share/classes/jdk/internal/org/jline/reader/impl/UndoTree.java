/*
 * Copyright (c) 2002-2016, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package jdk.internal.org.jline.reader.impl;

import java.util.function.Consumer;

/**
 * Simple undo tree.
 * Note that the first added state can't be undone
 */
public class UndoTree<T> {

    private final Consumer<T> state;
    private final Node parent;
    private Node current;

    public UndoTree(Consumer<T> s) {
        state = s;
        parent = new Node(null);
        parent.left = parent;
        clear();
    }

    public void clear() {
        current = parent;
    }

    public void newState(T state) {
        Node node = new Node(state);
        current.right = node;
        node.left = current;
        current = node;
    }

    public boolean canUndo() {
        return current.left != parent;
    }

    public boolean canRedo() {
        return current.right != null;
    }

    public void undo() {
        if (!canUndo()) {
            throw new IllegalStateException("Cannot undo.");
        }
        current = current.left;
        state.accept(current.state);
    }

    public void redo() {
        if (!canRedo()) {
            throw new IllegalStateException("Cannot redo.");
        }
        current = current.right;
        state.accept(current.state);
    }

    private class Node {
        private final T state;
        private Node left = null;
        private Node right = null;

        public Node(T s) {
            state = s;
        }
    }

}
