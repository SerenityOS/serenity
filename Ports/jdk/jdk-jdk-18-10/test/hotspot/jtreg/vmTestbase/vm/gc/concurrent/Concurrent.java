/*
 * Copyright (c) 2010, 2020, Oracle and/or its affiliates. All rights reserved.
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
package vm.gc.concurrent;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import nsk.share.TestFailure;
import nsk.share.gc.GC;
import nsk.share.gc.Memory;
import nsk.share.gc.ThreadedGCTest;
import nsk.share.gc.gp.GarbageProducer;
import nsk.share.gc.gp.GarbageProducer1Aware;
import nsk.share.gc.gp.GarbageProducerAware;
import nsk.share.gc.gp.MemoryStrategy;
import nsk.share.gc.gp.MemoryStrategyAware;
import nsk.share.gc.tree.*;
import nsk.share.log.Log;
import nsk.share.test.ExecutionController;
import nsk.share.test.LocalRandom;

class Forest {

    // the actual size of TreeNode in bytes in the memory calculated as occupied memory / count of nodes
    static int nodeSize;

    static long treeSize;

    private static long allNodesCount;

    /* log from test */
    static Log log;


    static int treeHeight;

    static long actuallyMut = 0;
    private static Forest instance = new Forest();
    private Tree[] trees;
    private Lock[] locks;

    private int nodeGarbageSize;

    private GarbageProducer gp;
    /*
     * Create array of trees occupyng given percent of heap
     */
    static Forest createForest(long percent, int heightToSizeRatio, int nodeGarbageSize, GarbageProducer gp, Log _log) {
        log = _log;

        long size = Runtime.getRuntime().maxMemory() * percent / 100;
        treeHeight = Memory.balancedTreeHeightFromMemory(size, (int) new TreeNode(nodeGarbageSize).getTotalSize());
        int ntrees = 0;
        while (treeHeight * heightToSizeRatio > ntrees) {
            ntrees++;
            treeHeight = Memory.balancedTreeHeightFromMemory(size / ntrees, (int) new TreeNode(nodeGarbageSize).getTotalSize());
        }

        log.debug("The expected forest paramteres: tree height = " + treeHeight  + " number of trees = " + ntrees
                + " size = " +  new TreeNode(nodeGarbageSize).getTotalSize());
        Tree[] localTrees = new Tree[ntrees * 4];
        Lock[] localLocks = new Lock[ntrees * 4];
        for (int i = 0; i < ntrees * 4; i++) {
            localTrees[i] = new Tree(Memory.makeBalancedTreeNode(treeHeight, nodeGarbageSize, gp));
            localLocks[i] = new ReentrantLock();

            int numOfAttempts = 0;
            if (Concurrent.getPercentInfoByMBeans() > percent) {
                log.debug("Attempt to System.gc() before control check. (" + numOfAttempts++ + ")");
                System.gc();
                if (Concurrent.getPercentInfoByMBeans() > percent) {
                    instance.trees = new Tree[i];
                    instance.locks = new Lock[i];
                    for (int j = 0; j < i; j++) {
                        instance.trees[j] = localTrees[j];
                        instance.locks[j] = localLocks[j];
                    }
                    allNodesCount = Memory.balancedTreeNodes(treeHeight) * instance.trees.length;
                    nodeSize = (int) (ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed() / allNodesCount);
                    treeSize = Memory.balancedTreeSize(treeHeight, nodeSize);
                    instance.where = new AtomicCycleInteger(instance.trees.length);
                    instance.nodeGarbageSize = nodeGarbageSize;

                    log.debug("The forest real paramteres: tree height = " + treeHeight  + " number of trees = " + instance.trees.length
                            + " number of nodes = " + allNodesCount);
                    log.debug("Approximate node size = " + nodeSize + " calc = " + instance.trees[0].getRoot().getSize());
                    return instance;
                }
            }
        }
        throw new TestFailure("Should not reach here. The correct exit point is inside cycle");
    }


    int treesCount() {
        return trees.length;
    }

    long nodesCount() {
        return allNodesCount;
    }



    // Confirms that all trees are balanced and have the correct height.
    void checkTrees() {
        for (int i = 0; i < trees.length; i++) {
            locks[i].lock();
            checkTree(trees[i]);
            locks[i].unlock();
        }
    }

    private static void checkTree(Tree tree) {
        TreeNode root = tree.getRoot();
        int h1 = root.getHeight();
        int h2 = root.getShortestPath();
        if ((h1 != treeHeight) || (h2 != treeHeight)) {
            throw new TestFailure("The tree is not balanced expected " + treeHeight
                    + " value = " + h1 + " shortedtPath = " + h2);
        }
    }

    // Swap subtrees in 2 trees, the the path is used
    // as sequence of 1-0 to select subtree (left-reight sequence)
    static void swapSubtrees(Tree t1, Tree t2, int depth, int path) {
        TreeNode tn1 = t1.getRoot();
        TreeNode tn2 = t2.getRoot();
        for (int i = 0; i < depth; i++) {
            if ((path & 1) == 0) {
                tn1 = tn1.getLeft();
                tn2 = tn2.getLeft();
            } else {
                tn1 = tn1.getRight();
                tn2 = tn2.getRight();
            }
            path >>= 1;
        }
        TreeNode tmp;
        if ((path & 1) == 0) {
            tmp = tn1.getLeft();
            tn1.setLeft(tn2.getLeft());
            tn2.setLeft(tmp);
        } else {
            tmp = tn1.getRight();
            tn1.setRight(tn2.getRight());
            tn2.setLeft(tmp);
        }
    }


    // Interchanges two randomly selected subtrees (of same size and depth) several times
    void swapSubtrees(long count) {
        for (int i = 0; i < count; i++) {
            int index1 = LocalRandom.nextInt(trees.length);
            int index2 = LocalRandom.nextInt(trees.length);
            int depth = LocalRandom.nextInt(treeHeight);
            int path = LocalRandom.nextInt();
            locks[index1].lock();
            // Skip the round to avoid deadlocks
            if (locks[index2].tryLock()) {
                swapSubtrees(trees[index1], trees[index2], depth, path);
                actuallyMut += 2;
                locks[index2].unlock();
            }
            locks[index1].unlock();

        }

    }


    static class AtomicCycleInteger extends AtomicInteger {
        private int max;
        public AtomicCycleInteger(int cycleLength) {
            super();
            this.max = cycleLength - 1;
        }
        public int cycleIncrementAndGet() {
            for (;;) {
                int current = get();
                int next = (current == max ? 0 : current + 1);
                if (compareAndSet(current, next)) {
                    return next;
                }
            }
        }
    }

    // the index in tree array which should be chnaged during next regeneration
    AtomicCycleInteger where = null;

    // generate new full and partial trees in our forest
    void regenerateTrees(long nodesCount) {
        int full = (int) (nodesCount / Memory.balancedTreeNodes(treeHeight)) ;
        int partial = (int) nodesCount % (Memory.balancedTreeNodes(treeHeight));
        for (int i = 0; i < full; i++) {
            int idx = where.cycleIncrementAndGet();
            locks[idx].lock();
            trees[idx] = new Tree(Memory.makeBalancedTreeNode(treeHeight, nodeGarbageSize));
            locks[idx].unlock();
        }
        while (partial > 0) {
            int h = Memory.balancedTreeHeightFromNodes(partial);
            Tree newTree = new Tree(Memory.makeBalancedTreeNode(h, nodeGarbageSize));
            int idx = where.cycleIncrementAndGet();
            locks[idx].lock();
            replaceTree(trees[idx], newTree);
            locks[idx].unlock();
            partial = partial - Memory.balancedTreeNodes(h);
        }
    }


    // Given a balanced tree full and a smaller balanced tree partial,
    // replaces an appropriate subtree of full by partial, taking care
    // to preserve the shape of the full tree.
    private static void replaceTree(Tree full, Tree partial) {
        boolean dir = (partial.getHeight() % 2) == 0;
        actuallyMut++;
        replaceTreeWork(full.getRoot(), partial.getRoot(), dir);
    }

    // Called only by replaceTree (below) and by itself.
    static void replaceTreeWork(TreeNode full, TreeNode partial,
            boolean dir) {
        boolean canGoLeft = full.getLeft() != null && full.getLeft().getHeight() > partial.getHeight();
        boolean canGoRight = full.getRight() != null && full.getRight().getHeight() > partial.getHeight();
        if (canGoLeft && canGoRight) {
            if (dir) {
                replaceTreeWork(full.getLeft(), partial, !dir);
            } else {
                replaceTreeWork(full.getRight(), partial, !dir);
            }
        } else if (!canGoLeft && !canGoRight) {
            if (dir) {
                full.setLeft(partial);
            } else {
                full.setRight(partial);
            }
        } else if (!canGoLeft) {
            full.setLeft(partial);
        } else {
            full.setRight(partial);
        }
    }



}
public class Concurrent extends ThreadedGCTest implements GarbageProducerAware, GarbageProducer1Aware, MemoryStrategyAware {

    // Heap as tree
    Forest forest;

    // GP for old gargbage production
    GarbageProducer gpOld;

    // GP for young gargbage production
    GarbageProducer gpYoung;

    MemoryStrategy ms;

    private void printStatistics() {
        log.debug("Actual mutations = " + forest.actuallyMut);
    }

    private class Worker implements Runnable {

        private ExecutionController stresser;

        @Override
        public void run() {
            if (stresser == null) {
                stresser = getExecutionController();
            }
            while (stresser.continueExecution()) {
                doStep();
            }
        }
    }

    @Override
    public Runnable createRunnable(int i) {
        return new Worker();
    }

    public static int getPercentInfoByMBeans() {
        MemoryMXBean mbean = ManagementFactory.getMemoryMXBean();
        return (int) (100 * mbean.getHeapMemoryUsage().getUsed() / mbean.getHeapMemoryUsage().getMax());
    }

    private void printMem(long used, long max, String source) {
        log.debug("The Memory after allocation (" + source + "): ");
        log.debug("Used = " + used + " Max = " + max + " Percent = " + (100 * used / max));
    }

    // Command-line parameters.
    // young garbage in percent and absolute
    private static int youngPercent = 0;
    long youngGarbageSize;
    // mutation rate (parcent and absolute trees)
    private static int ptrMutRate = 50;
    long mutateTrees;
    // percent of heap to occupy by forest (long live garbage)
    private static int livePercent = 60;
    // the minimum of which should be available for forest
    // test fails if it is not possible to use 60% of heap
    private static final int MIN_AVAILABLE_MEM = 60;
    // percent of forest to reallocate each step
    private static int reallocatePercent = 30;
    long reallocateSizeInNodes;
    // sleep time in ms
    private static int sleepTime = 100;

    private void init(int longLivePercent) {
        int numberOfThreads = runParams.getNumberOfThreads();
        forest = Forest.createForest(longLivePercent, numberOfThreads,
                (int) Math.sqrt(ms.getSize(Runtime.getRuntime().maxMemory())), gpOld, log);

        youngGarbageSize = Runtime.getRuntime().maxMemory() * youngPercent / 100 / numberOfThreads;
        reallocateSizeInNodes = forest.nodesCount() * reallocatePercent / 100 / numberOfThreads;
        mutateTrees = forest.treesCount() * ptrMutRate / 100 / numberOfThreads / 2;

        log.debug("Young Gen = " + youngGarbageSize);
        log.debug("Forest contains " + forest.treesCount() + " trees and " + forest.nodesCount() + " nodes.");
        log.debug("Count of nodes to reallocate = " + reallocateSizeInNodes);
        log.debug("Count of tree pairs to exchange nodes = " + mutateTrees);
        log.debug("Sleep time = " + sleepTime);

        // print some info
        MemoryUsage mbean = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        printMem(mbean.getUsed(), mbean.getMax(), "Beans");
        printMem(Runtime.getRuntime().maxMemory() - Runtime.getRuntime().freeMemory(),
                Runtime.getRuntime().maxMemory(), "System");
    }

    @Override
    public void run() {
        try {
            init(livePercent);
        } catch (OutOfMemoryError oome) {
            if (livePercent > MIN_AVAILABLE_MEM) {
                log.debug("Unable to use " + livePercent + " use only " + MIN_AVAILABLE_MEM);
                init(MIN_AVAILABLE_MEM);
            }
        }
        super.run();
        printStatistics();
    }



    private void doStep() {
        // allocate some young garbage
        if (youngGarbageSize != 0) {
            gpYoung.create(youngGarbageSize);
        }

        // allocate some long-live garbage (attached to our trees)
        forest.regenerateTrees(reallocateSizeInNodes);

        // mutate pointers
        forest.swapSubtrees(mutateTrees);

        // sleep to give GC time for some concurrent actions
        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException ie) {
        }

        // verify trees, also read all pointers
        forest.checkTrees();
    }

    public static void main(String[] args) {
        init(args);
        GC.runTest(new Concurrent(), args);
    }

    public static void init(String[] args) {
        for (int i = 0; i < args.length; ++i) {
            if (args[i].equals("-lp")) {
                // percent of long lived objects
                livePercent = Integer.parseInt(args[++i]);
            } else if (args[i].equals("-rp")) {
                // percent of trees to reallocate
                reallocatePercent = Integer.parseInt(args[++i]);
            } else if (args[i].equals("-yp")) {
                // percent of young objects
                youngPercent = Integer.parseInt(args[++i]);
            } else if (args[i].equals("-mr")) {
                // percent of trees to exchange (mutate)
                ptrMutRate = Integer.parseInt(args[++i]);
            } else if (args[i].equals("-st")) {
                // sleep time in ms
                sleepTime = Integer.parseInt(args[++i]);
            }
        }
    }

    @Override
    public void setGarbageProducer(GarbageProducer gp) {
        this.gpOld = gp;
    }


    @Override
    public void setGarbageProducer1(GarbageProducer gpYoung) {
        this.gpYoung = gpYoung;
    }

    @Override
    public void setMemoryStrategy(MemoryStrategy ms) {
        this.ms = ms;
    }
}
