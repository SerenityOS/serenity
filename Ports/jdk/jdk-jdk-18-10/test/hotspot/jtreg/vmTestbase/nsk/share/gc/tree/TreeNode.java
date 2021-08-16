/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.share.gc.tree;

import nsk.share.gc.Memory;
import nsk.share.gc.gp.GarbageProducer;

/**
 * A node of binary tree.
 *
 * A height of subtree defined by this node is kept in the node.
 * Additionaly, a byte array is used to control how much memory
 * this node will occupy.
 */
public final class TreeNode {
        private TreeNode left, right;
        // The height of the tree
        private int height;
        private int size;
        private Object storage;


        /**
         * Create a tree node that will occupy approximately given memory.
         *
         * @param memory memory
         */
        public TreeNode(long memory) {
                int length = (int) (memory - (4 * 2 + 2 * Memory.getReferenceSize() + Memory.getObjectExtraSize()));
                if (length > 0)
                        storage = new byte[length];
                size = length;
        }

        /**
         * Create a tree node that will occupy approximately given memory.
         *
         * @param memory memory
         */
        public TreeNode(long size, GarbageProducer gp) {
                int length = (int) (size - (4 * 2 + 2 * Memory.getReferenceSize() + Memory.getObjectExtraSize()));
                if (length > 0)
                        storage = gp.create(length);
                size = length;
        }
        /**
         * Create a tree node that will occupy approximately given memory
         * with given left and right children.
         *
         * @param memory memory
         * @param left left child
         * @param right right child
         */
        public TreeNode(long memory, TreeNode left, TreeNode right) {
                this(memory);
                setLeft(left);
                setRight(right);
                setHeight(1 + Math.max(left == null ? 0 : left.getHeight(), right == null ? 0 : right.getHeight()));
        }

        /**
         * Create a tree node that will occupy approximately given memory
         * with given left and right children.
         *
         * @param memory memory
         * @param left left child
         * @param right right child
         */
        public TreeNode(long memory, GarbageProducer gp, TreeNode left, TreeNode right) {
                this(memory, gp);
                setLeft(left);
                setRight(right);
                setHeight(1 + Math.max(left == null ? 0 : left.getHeight(), right == null ? 0 : right.getHeight()));
        }

        /**
         * Get memory that this node occupies.
         *
         * @return memory size
         */
        public long getSize() {
                int length = storage == null ? 0 : size;
                return length + 4 * 2 + 2 * Memory.getReferenceSize() + Memory.getObjectExtraSize();
        }

        /**
         * Get memory that this subtree occupies.
         *
         * @return memory size
         */
        public long getTotalSize() {
                long memory = getSize();
                if (left != null)
                        memory += left.getSize();
                if (right != null)
                        memory += right.getSize();
                return memory;
        }

        /**
         * Follow path determined by bits given integer and depth.
         *
         * @param path path encoded in integer
         * @param depth depth to follow
         * @return end of the path
         */
        public TreeNode follow(int path, int depth) {
                if (depth == 0)
                        return this;
                TreeNode current = this;
                TreeNode prev = null;
                for (int i = 0; i < depth; ++i) {
                        prev = current;
                        if ((path & 1) == 0)
                                current = current.left;
                        else
                                current = current.right;
                        path >>= 1;
                }
                return prev;
        }

        /**
         * Swap left child of this node with left child of another node.
         *
         * @param another another node
         */
        public void swapLeft(TreeNode another) {
                TreeNode tmp = another.left;
                another.left = left;
                left = tmp;
        }
        /**
         * Swap right child of this node with right child of another node.
         *
         * @param another another node
         */
        public void swapRight(TreeNode another) {
                TreeNode tmp = another.right;
                another.right = right;
                right = tmp;
        }

        public final TreeNode getLeft() {
                return left;
        }

        public final void setLeft(TreeNode left) {
                this.left = left;
        }

        public final TreeNode getRight() {
                return right;
        }

        public final void setRight(TreeNode right) {
                this.right = right;
        }

        public final int getHeight() {
                return height;
        }

        public boolean hasLeft() {
                return left != null;
        }

        public boolean hasRight() {
                return right != null;
        }

        /**
         * Get actual height of the tree.
         */
        public int getActualHeight() {
                return 1 + Math.max(left == null ? 0 : left.getActualHeight(), right == null ? 0 : right.getActualHeight());
        }

        /**
         * Get lenght of shortest path from root to leaf in the tree.
         */
        public int getShortestPath() {
                return 1 + Math.min(left == null ? 0 : left.getActualHeight(), right == null ? 0 : right.getActualHeight());
        }

        public final void setHeight(int value) {
                this.height = value;
        }
}
