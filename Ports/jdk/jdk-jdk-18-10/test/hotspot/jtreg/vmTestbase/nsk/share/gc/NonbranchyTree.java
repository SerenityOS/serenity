/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package nsk.share.gc;

import nsk.share.test.ExecutionController;
import nsk.share.test.LocalRandom;

import java.io.PrintStream;

/**
 * <tt>NonbranchyTree</tt> defines a tree structure. Each node of the tree
 * always has one son. A node may have the second son with probability
 * <tt>branchiness</tt>.
 */
public class NonbranchyTree {

    /** Minimal size of each node (in bytes) */
    public final static int MIN_NODE_SIZE = 20;
    private final Node root;
    private final float branchiness;
    private final ExecutionController controller;

    /**
     * Creates a new tree with number of nodes not more than
     * <tt>numberOfNodes</tt>. The implementation uses recursion to build the
     * tree, so if <tt>StackOverflowError</tt> or <tt>OutOfMemoryError</tt> is
     * thrown, the recursion is stopped and the method finishes building of the
     * tree. Each node consists of <tt>byte[]</tt> of length <tt>size</tt>.
     *
     * @param numberOfNodes maximum number of nodes for the tree.
     * @param branchiness probability for each node to have the second son.
     * @param size number of bytes to store in a node.
     *
     * @throws <i>IllegalArgumentException</i> if <tt>numberOfNodes</tt> is
     *         less than 1; or <tt>branchiness</tt> is greater than 1, or less
     *         or equal than 0; or <tt>size</tt> is less than 1.
     *
     */
    public NonbranchyTree(int numberOfNodes, float branchiness, int size) {
        this(numberOfNodes, branchiness, size, null);
    }

    public NonbranchyTree(int numberOfNodes, float branchiness, int size, ExecutionController controller) {
        if (numberOfNodes < 1) {
            throw new IllegalArgumentException("Illegal number of nodes: "
                    + numberOfNodes + ", must be at least 1.");
        }
        if ((branchiness >= 1) || (branchiness <= 0)) {
            throw new IllegalArgumentException("Illegal value of branchiness: "
                    + branchiness + ", must be greater than 0 and less than 1.");
        }
        if (size < 1) {
            throw new IllegalArgumentException("Illegal size of nodes: "
                    + size + ", must be at least 1.");
        }
        // ensure that LocalRandom is loaded and has enough memory
        LocalRandom.nextBoolean();
        this.branchiness = branchiness;
        this.controller = controller;
        this.root = createTree(numberOfNodes, size);
    }

    // Create a new tree with specified number of nodes and size of each node
    private Node createTree(int numberOfNodes, int size) {
        // Make sure we respect the controller and stop test after
        // given time.
        if (controller != null && !controller.continueExecution()) {
            return null;
        }

        Node node = new Node(size);
        try {
            if (numberOfNodes == 0) {
                // No more nodes need to be built
                return null;
            } else if (numberOfNodes == 1) {
                return node;
            } else if (numberOfNodes == 2) {
                node.left = createTree(1, size);
                return node;
            } else {
                // Create a few nodes
                if (makeRightNode()) {
                    // The node will have two sons
                    int leftNodes = 1 + LocalRandom.nextInt(numberOfNodes - 2);
                    int rightNodes = numberOfNodes - 1 - leftNodes;

                    node.left = createTree(leftNodes, size);
                    node.right = createTree(rightNodes, size);
                } else {
                    // The node will have just one son
                    Node leftTree = createTree(numberOfNodes - 1, size);
                    node.left = leftTree;
                }
                return node;
            } // if
        } catch(StackOverflowError e) {
            // No more memory for such long tree
            return node;
        } catch(OutOfMemoryError e) {
            // No more memory for such long tree
            return node;
        } // try
    } // createTree()

    // Define the "branchiness" of the tree
    private boolean makeRightNode() {
        return (LocalRandom.nextFloat() < branchiness);
    }

    /**
     * Bends the tree. A son of a leaf of the tree is set to the root node.
     *
     */
    public void bend() {
        bend(root);
    }

    // Bend the tree: make a reference from a leat of the tree to the specified
    // node
    private void bend(Node markedNode) {
        Node node = root;

        while ( (node.left != null) || (node.right != null) )
            node = node.left;
        node.right = markedNode;
    }

    /**
     * Prints the whole tree from the root to the defined PrintStream.
     *
     * @param out PrintStream to print the tree in
     *
     */
    public void print(PrintStream out) {
        print(out, root);
    }

    // Print the sub-tree from the specified node and down
    private void print(PrintStream out, Node node) {
        node.print(out);
        if (node.left != null)
            print(out, node.left);
        if (node.right != null)
            print(out, node.right);
    }
}

// The class defines a node of a tree
class Node {
    Node left;
    Node right;
    byte[] core;

    Node(int size) {
        left = null;
        right = null;
        core = new byte[size];

        // Initizlize the core array
        for (int i = 0; i < size; i++)
            core[i] = (byte) i;
    }

    // Print the node info
    void print(PrintStream out) {
        out.println("node = " + this + " (" + left + ", " + right + ")");
    }
}
