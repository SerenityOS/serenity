/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @key stress randomness
 *
 * @summary converted from VM Testbase gc/gctests/JumbleGC002.
 * VM Testbase keywords: [gc, stress, stressopt, nonconcurrent, quarantine]
 * VM Testbase readme:
 * DESCRIPTION
 *     The test checks that Garbage Collector can manage jumble in the JVM. The
 *     test fails if any unexpected exceptions and errors are thrown or the JVM
 *     is not crashed.
 *     The test starts a number of threads that is set in *.cfg file or calculates
 *     that value based on the machine. All threads have
 *     java.util.Vector field anf they fill that vector with 4 types of objects:
 *         1. Initialized long[]
 *         2. Uninitialized double[]
 *         3. Initialized int[]
 *         4. A nsk.share.gc.NonbranchyTree (number of nodes and their size depend
 *            on valkue returned by Runtime.maxMemory())
 *     As soon as the vector is filled, each thread removes half elements of it and
 *     then fills those places of the vector again. However, all threads use just
 *     about 10% of maximum amount of memory that JVM attemts to use, so
 *     OutOfMemoryError is treated as a failure. That means GC does not work
 *     quickly enough to destroy all objects that do not have references. The
 *     procedure of filling and cleaning of the vector is repeated for
 *     INTERNAL_ITERATIONS times.
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm -XX:-UseGCOverheadLimit gc.gctests.JumbleGC002.JumbleGC002
 */

package gc.gctests.JumbleGC002;

import java.io.*;
import java.util.*;

import nsk.share.*;
import nsk.share.gc.*;
import nsk.share.test.LocalRandom;

/**
 * This test simply does Algorithms.eatMemory() in a loop
 * in multiple threads.
 */
public class JumbleGC002 extends ThreadedGCTest {

    // The test should fill just about 10% of the heap
    final static double PART_OF_HEAP = 0.1;
    // Maximum number of elements in an array of primitive types
    final static int ARRAY_MAX_LENGTH = 10;
    // Internal number of iterations to create new objects and to drop
    // references
    final static int INTERNAL_ITERATIONS = 150;
    // Size of core for each node of a tree
    final static int EACH_NODE_SIZE = 1;
    // Number of bytes that arrays of primitive types take in the vector
    final static long PRIMITIVE_ARRAYS_SIZE = (long) (8 * ARRAY_MAX_LENGTH
            + 8 * ARRAY_MAX_LENGTH + 4 * ARRAY_MAX_LENGTH);

    private class Eater implements Runnable {

        private Vector vector;
        int numberOfElements;
        int numberOfQuarters;
        int id;
        int nodes;

        public Eater(int id, int numberOfQuarters, int nodes) {
            this.numberOfQuarters = numberOfQuarters;
            numberOfElements = 4 * numberOfQuarters;
            this.id = id;
            this.nodes = nodes;
        }

        public void run() {
            // Make jumble in the heap!
            initVector();
            while (getExecutionController().continueExecution()) {
                fillVector();
                cleanVector();
            }
        }

        // Initialize the vector and build appropriate number of cells in it
        private void initVector() {
            vector = new Vector();
            for (int i = 0; i < numberOfElements; i++) {
                vector.addElement(null);
            }
        }

        // Fill the vector. It is devided into quarters. Each quarters has an
        // initialized array of long and int, and uninitialized array of double.
        // Each array has not more than ARRAY_MAX_LENGTH elements. The fourth
        // element in the quarter is a NonbranchyTree.
        private void fillVector() {
            for (int i = 0; i < numberOfQuarters; i++) {

                // Append initialized long[]
                int length = LocalRandom.nextInt(ARRAY_MAX_LENGTH);
                long[] l = new long[length];
                for (int j = 0; j < length; j++) {
                    l[j] = (long) j;
                }
                if (vector.elementAt(4 * i) == null) {
                    vector.setElementAt(l, 4 * i);
                }

                // Append not initialized double[]
                length = LocalRandom.nextInt(ARRAY_MAX_LENGTH);
                double[] d = new double[length];
                if (vector.elementAt(4 * i + 1) == null) {
                    vector.setElementAt(d, 4 * i + 1);
                }

                // Append initialized int[]
                length = LocalRandom.nextInt(ARRAY_MAX_LENGTH);
                int[] n = new int[length];
                for (int j = 0; j < length; j++) {
                    n[j] = j;
                }
                if (vector.elementAt(4 * i + 2) == null) {
                    vector.setElementAt(n, 4 * i + 2);
                }

                // Append a tree. Every even thread has a "bent" tree.
                NonbranchyTree tree = new NonbranchyTree(nodes, 0.3f, EACH_NODE_SIZE);
                if (id % 2 == 0) {
                    tree.bend();
                }
                if (vector.elementAt(4 * i + 3) == null) {
                    vector.setElementAt(tree, 4 * i + 3);
                }
            }
        }

        // Drop references to half of the elements of the vector
        private void cleanVector() {
            int index = LocalRandom.nextInt(numberOfElements / 2);
            for (int i = index; i < index + numberOfElements / 2; i++) {
                vector.setElementAt(null, i);
            }
        }
    }

    protected Runnable createRunnable(int i) {
        // Perform calculations specific to the test
        long memoryForThread = (long) (Runtime.getRuntime().maxMemory() * PART_OF_HEAP / runParams.getNumberOfThreads());
        int numberOfQuarters;

        if (i == 0) {
            // The very first thread
            numberOfQuarters = 1;
        } else {
            // All other threads
            numberOfQuarters = 8;
        }

        // Calculate number of nodes for a tree depending on number of
        // elements in the Vector

        double freeMemory = (double) memoryForThread / numberOfQuarters
                - (double) PRIMITIVE_ARRAYS_SIZE;
        int nodes = (int) (freeMemory / (NonbranchyTree.MIN_NODE_SIZE + EACH_NODE_SIZE));
        nodes = Math.max(1, nodes);
        log.debug("Thread " + i + " has a tree with "
                + nodes + " node(s).");

        return new Eater(i, numberOfQuarters, nodes);
    }

    public static void main(String args[]) {
        GC.runTest(new JumbleGC002(), args);
    }
}
