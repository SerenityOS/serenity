/*
 * Copyright (c) 2005, 2018, Oracle and/or its affiliates. All rights reserved.
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

import nsk.share.test.LocalRandom;
import java.io.PrintStream;
import nsk.share.gc.gp.GarbageProducer;
import nsk.share.gc.tree.*;
import nsk.share.gc.gp.MemoryStrategy;
import nsk.share.log.Log;

/**
 *  Different utility methods to work with memory objects.
 */
public final class Memory {
        private static int bits = 0;
        private static int referenceSize = 0;
        private static int objectExtraSize = 0;

        private Memory() {
        }

        private static int getBits() {
                if (bits == 0)
                        bits = Integer.parseInt(System.getProperty("sun.arch.data.model"));
                return bits;
        }

        /**
         *  Get size of one object reference.
         *
         *  TODO: somehow determine the real value
         */
        public static int getReferenceSize() {
                if (referenceSize == 0)
                        referenceSize = (getBits() == 64) ? 8 : 4;
                return referenceSize;
        }

        /**
         * Get size of primitive type int.
         */
        public static int getIntSize() {
                return 4;
        }

        /**
         * Get size of primitive type boolean.
         */
        public static int getBooleanSize() {
                return 1;
        }

        /**
         * Get size of primitive type byte.
         */
        public static int getByteSize() {
                return 1;
        }

        /**
         * Get size of primitive type char.
         */
        public static int getCharSize() {
                return 2;
        }

        /**
         * Get size of primitive type short.
         */
        public static int getShortSize() {
                return 2;
        }

        /**
         * Get size of primitive type long.
         */
        public static int getLongSize() {
                return 8;
        }

        /**
         * Get size of primitive type float.
         */
        public static int getFloatSize() {
                return 4;
        }

        /**
         * Get size of primitive type double.
         */
        public static int getDoubleSize() {
                return 8;
        }

        /**
         *  Get how many extra bytes an object occupies in the heap
         *  compared to sum of it's fields.
         *
         *  TODO: somehow determine the real value
         */
        public static int getObjectExtraSize() {
                if (objectExtraSize == 0)
                        objectExtraSize = 2 * getReferenceSize();
                return objectExtraSize;
        }
        /**
         *  Get how many extra bytes an array occupies in the heap
         *  compared to sum of it's fields.
         *
         *  TODO: somehow determine the real value
         */
        public static int getArrayExtraSize() {
                return getObjectExtraSize();
        }

        /**
         * Return size of reference object (SoftReference, WeakReference, PhantomReference)
         */
        public static int getReferenceObjectSize() {
                return getReferenceSize() + getObjectExtraSize();
        }

        /**
         *  Get an approximate length of array that will occupy a given memory.
         *
         *  @param memory size of memory
         *  @param objectSize size of each object in array
         *  @return length of array
         */
        public static int getArrayLength(long memory, long objectSize) {
                int referenceSize = getReferenceSize();
                int arrayExtraSize = getArrayExtraSize();
                return (int) Math.min(
                                (memory - arrayExtraSize) / (objectSize + referenceSize),
                                Integer.MAX_VALUE
                                );
        }

        /**
         *  Get an approximate size of array of given length and object size.
         *
         *  @param length length of arary
         *  @param objectSize size of object in array
         *  @return size of array
         */
        public static long getArraySize(int length, long objectSize) {
                return getObjectExtraSize() + length * (objectSize + getReferenceSize());
        }

        /**
         *  Calculate approximate size of biggest of MemoryObjects.
         */
        public static long getMemoryObjectSize(long size) {
                return size + 2 * getReferenceSize() + getObjectExtraSize();
        }

        /**
         *  Calculate approximate size of linked list in memory.
         *
         *  @param length length of list
         *  @param size size of object
         *  @return size
         */
        public static long getListSize(int length, int size) {
                return getObjectExtraSize() + length * (getReferenceSize() + getMemoryObjectSize(size));
        }

        /**
         *  Calculate length of linear or circular linked list.
         *
         *  @param mobj head of list
         *  @return length of list
         */
        public static int getListLength(LinkedMemoryObject mobj) {
                LinkedMemoryObject tobj = mobj;
                int length = 0;
                do {
                        ++length;
                        tobj = tobj.getNext();
                } while (tobj != null && tobj != mobj);
                return length;
        }

        /**
         *  Calculate length of array of linear or circular linked lists.
         *
         *  @param mobjs array containting heads of lists
         *  @return length of all lists
         */
        public static int getListsLength(LinkedMemoryObject[] mobjs) {
                int length = 0;
                for (int i = 0; i < mobjs.length; ++i) {
                        LinkedMemoryObject mobj = mobjs[i];
                        if (mobj != null)
                                length += getListLength(mobj);
                }
                return length;
        }

        /**
         *  Calculate size of all objects in linear or circular linked list.
         *
         *  @param mobj head of list
         *  @return size of list
         */
        public static long getListSize(LinkedMemoryObject mobj) {
                LinkedMemoryObject tobj = mobj;
                long size = 0;
                do {
                        size += tobj.getSize();
                        tobj = tobj.getNext();
                } while (tobj != null && tobj != mobj);
                return size;
        }

        /**
         *  Calculate size of array of linear or circular linked lists.
         *
         *  @param mobjs array containting heads of lists
         *  @return size of all lists
         */
        public static long getListsSize(LinkedMemoryObject[] mobjs) {
                long size = 0;
                for (int i = 0; i < mobjs.length; ++i) {
                        LinkedMemoryObject mobj = mobjs[i];
                        if (mobj != null)
                                size += getListSize(mobj);
                }
                return size;
        }

        /**
         *  Create singly linked linear list of objects of fixed size.
         *
         *  @param depth length of list
         *  @param size size of each object
         *  @return head of created list or null if depth = 0
         */
        public static LinkedMemoryObject makeLinearList(int depth, int size) {
                LinkedMemoryObject mobj = null;
                for (int i = 0; i < depth; ++i)
                        mobj = new LinkedMemoryObject(size, mobj);
                return mobj;
        }

        /**
         *  Create singly linked linear list of objects of varying size.
         *
         *  @param depth length of list
         *  @param size maximum size of each object
         *  @return head of created list or null if depth = 0
         */
        public static LinkedMemoryObject makeRandomLinearList(int depth, int size) {
                if (depth == 0)
                        return null;
                LinkedMemoryObject mobj = new LinkedMemoryObject(size);
                for (int i = 0; i < depth - 1; ++i)
                        mobj = new LinkedMemoryObject(LocalRandom.nextInt(size), mobj);
                return mobj;
        }

        /**
         *  Create singly linked circular linear list of objects of fixed size.
         *
         *  @param depth length of list
         *  @param size size of each object
         *  @return head of created list or null if depth = 0
         */
        public static LinkedMemoryObject makeCircularList(int depth, int size) {
                if (depth == 0)
                        return null;
                LinkedMemoryObject mobj = new LinkedMemoryObject(size);
                LinkedMemoryObject tmpobj = mobj;
                for (int i = 1; i < depth; i++)
                        mobj = new LinkedMemoryObject(size, mobj);
                tmpobj.setNext(mobj);
                return tmpobj;
        }

        /**
         *  Create singly linked circular linear list of objects of varying size.
         *
         *  @param depth length of list
         *  @param size maximum size of each object
         *  @return head of created list or null if depth = 0
         */
        public static LinkedMemoryObject makeRandomCircularList(int depth, int size) {
                if (depth == 0)
                        return null;
                LinkedMemoryObject mobj = new LinkedMemoryObject(size);
                LinkedMemoryObject tmpobj = mobj;
                for (int i = 0; i < depth - 1; i++)
                        mobj = new LinkedMemoryObject(LocalRandom.nextInt(size), mobj);
                tmpobj.setNext(mobj);
                return tmpobj;
        }

        /**
         * Create new nonbranchy binary tree.
         *
         * Each node in the tree except leaves always has left son. A node
         * will have right son with probability branchiness.
         *
         * @param numberOfNodes number of nodes
         * @param branchiness branchiness
         * @param size size of each node
         * @return root of created tree
         */
        public static LinkedMemoryObject makeNonbranchyTree(int numberOfNodes, float branchiness, int size) {
                LinkedMemoryObject root = null;
                LinkedMemoryObject current = null;
                if (numberOfNodes == 0)
                        return null;
                else if (numberOfNodes == 1)
                        return new LinkedMemoryObject(size);
                else if (numberOfNodes == 2)
                        return new LinkedMemoryObject(size, makeNonbranchyTree(1, branchiness, size));
                else {
                        if (LocalRandom.nextFloat() < branchiness) {
                                int numberOfLeftNodes = LocalRandom.nextInt(1, numberOfNodes - 1);
                                int numberOfRightNodes = numberOfNodes - 1 - numberOfLeftNodes;
                                return new LinkedMemoryObject(
                                                size,
                                                makeNonbranchyTree(numberOfLeftNodes, branchiness, size),
                                                makeNonbranchyTree(numberOfRightNodes, branchiness, size)
                                                );
                        } else {
                                return new LinkedMemoryObject(size, makeNonbranchyTree(numberOfNodes - 1, branchiness, size));
                        }
                }
        }

        /**
         * Create a balanced tree of given height.
         *
         * @param height height of the tree
         * @param size size of each node
         * @return created tree
         */
        public static Tree makeBalancedTree(int height, long size) {
                return new Tree(makeBalancedTreeNode(height, size));
        }

        /**
         * Get a number of nodes in balanced tree of given height.
         *
         * @param heigh height of the tree
         * @return number of nodes
         */
        public static int balancedTreeNodes(int height) {
                if (height == 0)
                        return 0;
                int n = 1;
                while (height > 1) {
                        n *= 2;
                        height--;
                }
                return n * 2 - 1;
        }

        /**
         * Get approximate memory size occupied by balanced tree
         * of given height and given node size.
         *
         * @param height height of the tree
         * @param nodeSize size of each node
         * @return memory size
         */
        public static long balancedTreeSize(int height, long nodeSize) {
                return balancedTreeNodes(height) * nodeSize;
        }

        /**
         * Get a height of balanced tree with given number of nodes.
         *
         * @param nodes number of nodes
         * @return height of the tree
         */
        public static int balancedTreeHeightFromNodes(int nodes) {
                if (nodes == 0)
                        return 0;
                int h = 1;
                long n = 1;
                while (n + n - 1 <= nodes) {
                        n = n + n;
                        h = h + 1;
                }
                return h - 1;
        }

        /**
         * Get approximate height of balanced tree which will occupy
         * given memory with given node size.
         *
         * @param memory memory size
         * @param nodeSize size of each node
         * @return approximate height of the tree
         */
        public static int balancedTreeHeightFromMemory(long memory, long nodeSize) {
                return balancedTreeHeightFromNodes((int) (memory / nodeSize));
        }

        /**
         * Create balanced tree of given height and node size.
         *
         * @param height height of the tree
         * @param size size of each node
         * @return root of created tree
         */
        public static TreeNode makeBalancedTreeNode(int height, long size) {
                if (height == 0)
                        return null;
                else
                        return new TreeNode(size, makeBalancedTreeNode(height - 1, size), makeBalancedTreeNode(height - 1, size));
        }

        /**
         * Create balanced tree of given height and node size.
         *
         * @param height height of the tree
         * @param size size of each node
         * @return root of created tree
         */
        public static TreeNode makeBalancedTreeNode(int height, long size, GarbageProducer gp) {
                if (height == 0)
                        return null;
                else
                        return new TreeNode(size, gp, makeBalancedTreeNode(height - 1, size), makeBalancedTreeNode(height - 1, size));
        }

        /**
         * Determine if given tree is a balanced tree.
         *
         * @param tree tree
         * @return true if tree is balanced
         */
        public static boolean isBalancedTree(TreeNode tree) {
                return
                        tree.getActualHeight() == tree.getHeight() &&
                        tree.getShortestPath() == tree.getHeight();
        }

        /**
         *  Fill an array of MemoryObject's with new objects of given size.
         *
         *  @param array array
         *  @param count number of objects to create
         *  @param size size of each object
         */
        public static void fillArray(MemoryObject[] array, int count, int size) {
                for (int i = 0; i < count; ++i)
                        array[i] = new MemoryObject(size);
        }

        /**
         *  Fill an array of MemoryObject's with new objects of random size.
         *
         *  @param array array
         *  @param count number of objects to create
         *  @param size maximum size of each object
         */
        public static void fillArrayRandom(MemoryObject[] array, int count, int size) {
                for (int i = 0; i < count; ++i)
                        array[i] = new MemoryObject(LocalRandom.nextInt(size));
        }

        /**
         *  Fill an array of MemoryObject's with new objects of random size.
         *
         *  @param array array
         *  @param count number of objects to create
         *  @param size maximum size of each object
         */
        public static void fillArrayRandom(LinkedMemoryObject[] array, int count, int size) {
                for (int i = 0; i < count; ++i)
                        array[i] = new LinkedMemoryObject(LocalRandom.nextInt(size));
        }

        public static void dumpStatistics(PrintStream out) {
                out.println(Runtime.getRuntime().freeMemory());
                out.flush();
        }

        public static void dumpStatistics(Log log) {
                log.info(Runtime.getRuntime().freeMemory());
        }

        public static void dumpStatistics() {
                dumpStatistics(System.out);
        }
}
