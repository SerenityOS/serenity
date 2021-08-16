/*
 * Copyright (c) 2002, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @key randomness
 *
 * @summary converted from VM Testbase gc/gctests/WeakReferenceGC.
 * VM Testbase keywords: [gc, nonconcurrent]
 * VM Testbase readme:
 * *******************************************
 * set timeout = 25 when running this test
 * *******************************************
 * This tests checks to see if the garbage collector enqueues
 * a weak reference when referrent has been turned to garbage.
 * Weak references are enqueued in  a non-deterministic way
 * by the garbage collector, so the test uses a heuristic to
 * determine whether the references are being enqueued in a timely
 * manner which in turn determines whether outcome of the test.
 * IF the user invokes the test with the following command line
 * parameters :
 *   java WeakReferenceGC -qFactor 0.9 -gcCount 5
 * the test expects that 90% of all objects with only weak references
 * pointing to them will be enqueued within 5 calls to the garbage collector.
 * When I ran the test, I consistently got figures of 98% enqueueing
 * with just 1 call to the garbage collector. So if this test fails,
 * at its current settings, the garbage collector is not performing as well
 * as it used to.
 * The test creates circular linked lists of size 0.1Meg each. The number
 * of lists created can be set via the -numLists flag. The default
 * value is 50.
 * The circular linked lists have both strong and weak references pointing
 * to them. The strong and weak references are kept in arrays.
 * The strong references are all nulled out and System.gc() is called
 * explicitly and the heuristic is applied. If the test does not
 * satisfy the heuristic or an OutOfMemory exception is thrown,
 * the test fails.
 * Array containing    Each circular linked list        Array containing
 * weak references     is 0.1 Meg each and has          strong references
 * to linked lists.    a weak reference, W<n> and a     to linked lists.
 *                     strong reference, x<n>
 *                     pointing to it.
 *  ----      ----------------------------        -----
 * |    |     |   ----             ---- <-|      |     |
 * | W1 |-->  -->|    |---.......>|    |   <---- |  x1 |
 * |    |     -->|    |<---.......|    |<--      |     |
 *  ----      |   ----       1000  ---   |        -----
 *            ---------------------------
 *   .                                              .
 *   .                                              .
 *   .                                              .
 *   .                                              .
 *   .                                              .  10
 *   .                                              .
 *  ----      ----------------------------        -----
 * |    |     |   ----             ---- <-|      |     |
 * | Wn |-->  -->|    |---.......>|    |   <---- |  xn |
 * |    |     -->|    |<---.......|    |<--      |     |
 *  ----      |   ----       1000  ---   |        -----
 *            ---------------------------
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm
 *      gc.gctests.WeakReferenceGC.WeakReferenceGC
 *      -numList 50
 *      -qFactor 0.9
 *      -gcCount 5
 *      -iter 100
 */

package gc.gctests.WeakReferenceGC;

import java.util.*;
import java.lang.ref.*;
import nsk.share.TestFailure;
import nsk.share.gc.GC;
import nsk.share.gc.ThreadedGCTest;
import nsk.share.gc.gp.GarbageUtils;

public class WeakReferenceGC extends ThreadedGCTest {

        // A heuristic is used to determine the outcome(pass/fail
        // status) of a test. If 90% of all objects that have
        // _only_ weak references pointing to them are garbage
        // collected with 5 explicit calls to the garbage collector
        // the test is deemed a pass. The following two variables
        // are used to define this heuristic: gcCount, qFactor

        static String args[];

        CircularLinkedList holder[];

        int loopCount = 100; // # of times test is performed

        int memory_reserve[];
        int gcCount = 5;
        int numLists = 50; // number of linked lists
        float qFactor = (float) 0.9;
        ReferenceQueue refQueue;
        Vector results;
        WeakReference wholder[];

        public static void main(String[] args) {
                WeakReferenceGC.args = args;
                GC.runTest(new WeakReferenceGC(), args);
        }

        WeakReferenceGC() {
                holder = new CircularLinkedList[numLists];
                wholder = new WeakReference[numLists];
                refQueue = new ReferenceQueue();
                results = new Vector();
        }

        protected Runnable createRunnable(int i) {
                return i > 0 ? null : new Worker();
        }

        private void dumpTestResults() {
                double objectsRecovered;

                System.out.println("Percentage of Objects" + "  # of GC's");
                System.out.println("    Recovered         " + " Required");
                for (int i = 0; i < results.size(); i++) {
                        Statistic s = (Statistic) results.elementAt(i);
                        objectsRecovered = Math.rint((float) (s.numEnqueued)
                                        / (float) (numLists) * 100.0);
                        System.out.println("        " + objectsRecovered
                                        + " %             " + s.iterations);
                }
        }

        private boolean hasPassed() {
                boolean passed;
                passed = true; // assume passed till proven otherwise

                for (int i = 0; i < results.size(); i++) {
                        Statistic s = (Statistic) results.elementAt(i);
                        if ((s.iterations > gcCount)
                                        || (s.numEnqueued < (int) (numLists * qFactor))) {
                                passed = false;
                                break; // test failed
                        }
                }
                return passed;
        }

        private void parseTestParams(String args[]) {
                for (int i = 0; i < args.length; i++) {
                        if (args[i].compareTo("-numList") == 0) {
                                numLists = Integer.valueOf(args[++i]).intValue();
                        } else if (args[i].compareTo("-qFactor") == 0) {
                                qFactor = Float.valueOf(args[++i]).floatValue();
                        } else if (args[i].compareTo("-gcCount") == 0) {
                                gcCount = Integer.valueOf(args[++i]).intValue();
                        } else if (args[i].compareTo("-iter") == 0) {
                                loopCount = Integer.valueOf(args[++i]).intValue();
                                // } else {
                                // System.err.println("usage : " +
                                // "java WeakReferenceGC [-numList <n>] " +
                                // "[-qFactor <0.x>] [-gcCount <n>] [-iter <n>]");
                                // throw new TestBug("Invalid arguments");
                        }
                }
        }

        private void persistentGC() {
                int numEnqueued, iter, qCriterion;

                numEnqueued = 0; // number of weakReference enqueued
                iter = 0;
                qCriterion = (int) (numLists * qFactor);

                while ((numEnqueued < qCriterion) && (iter <= gcCount)) {
                        iter++;
                        if (!getExecutionController().continueExecution()) {
                                return;
                        }
                        if (GarbageUtils.eatMemory(getExecutionController()) == 0) {
                                return; // We were unable to provoke OOME before timeout is over
                        }
                        try {
                                while ((numEnqueued < numLists) &&
                                       (refQueue.remove(1000) != null)) {
                                        numEnqueued++;
                                }
                        } catch (InterruptedException ie) {
                        }
                }
                results.addElement((new Statistic(iter, numEnqueued)));
        }

        private void runTest() {
                int iter;
                iter = 1;
                try {
                        do {
                                for (int i = 0; i < numLists; i++) {
                                        holder[i] = new CircularLinkedList();
                                        holder[i].addNelements(1000);
                                        wholder[i] = new WeakReference(holder[i], refQueue);
                                }

                                for (int i = 0; i < numLists; i++) {
                                        holder[i] = null;
                                }

                                if (!getExecutionController().continueExecution()) {
                                        return;
                                }
                                persistentGC();

                                iter++;
                        } while (iter <= loopCount);
                } catch (OutOfMemoryError e) {
                        memory_reserve = null;
                        System.gc();
                        throw new TestFailure("Failed at iteration=" + loopCount);
                }
        }

        //We can't override run() method in WeakReferenceGC, so we are carrying out it to Worker
        private class Worker implements Runnable {
                @Override
                public void run() {
                        parseTestParams(args);
                        runTest();
                        dumpTestResults();
                        boolean passed = hasPassed();
                        if (passed == true) {
                                log.info("Test passed.");
                        } else {
                                log.error("Test failed.");
                                setFailed(true);
                        }
                }
        }
}

class Statistic {
        int iterations;
        int numEnqueued;

        Statistic(int i, int num) {
                iterations = i;
                numEnqueued = num;
        }

}
