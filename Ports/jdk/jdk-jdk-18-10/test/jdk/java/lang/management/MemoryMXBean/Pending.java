/*
 * Copyright (c) 2003, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     4530538
 * @summary Basic unit test of
 *          RuntimeMXBean.getObjectPendingFinalizationCount()
 *          1. GC and runFinalization() to get the current pending number
 *          2. Create some number of objects with reference and without ref.
 *          3. Clear all the references
 *          4. GC and runFinalization() and the finalizable objects should
 *             be garbage collected.
 * @author  Alexei Guibadoulline and Mandy Chung
 *
 * @modules java.base/jdk.internal.misc
 *          java.management
 */

import java.lang.management.*;

public class Pending {
    static final int NO_REF_COUNT = 600;
    static final int REF_COUNT = 500;
    static final int TOTAL_FINALIZABLE = (NO_REF_COUNT + REF_COUNT);
    private static int finalized = 0;
    private static MemoryMXBean mbean
        = ManagementFactory.getMemoryMXBean();

    private static final String INDENT = "      ";
    private static void printFinalizerInstanceCount() {
        if (!trace) return;

        int count = jdk.internal.misc.VM.getFinalRefCount();
        System.out.println(INDENT + "Finalizable object Count = " + count);

        count = jdk.internal.misc.VM.getPeakFinalRefCount();
        System.out.println(INDENT + "Peak Finalizable object Count = " + count);
    }

    private static boolean trace = false;
    public static void main(String argv[]) throws Exception {
        if (argv.length > 0 && argv[0].equals("trace")) {
            trace = true;
        }

        try {
            if (trace) {
                // Turn on verbose:gc to track GC
                mbean.setVerbose(true);
            }
            test();
        } finally {
            if (trace) {
                mbean.setVerbose(false);
            }
        }
        System.out.println("Test passed.");
    }

    // Keep objs public so the optimizer will not remove them too early.
    public static Object[] objs = null;

    private static void test() throws Exception {
        // Clean the memory and remove all objects that are pending
        // finalization
        System.gc();
        Snapshot snapshot = getSnapshotAfterFinalization();

        System.out.println("Number of objects pending for finalization:");
        System.out.println("   Before creating object: " + snapshot);
        printFinalizerInstanceCount();

        // Create objects without saving reference. Should be removed at next GC.
        for (int i = 0; i < NO_REF_COUNT; i++) {
            new MyObject();
        }

        snapshot = getSnapshot();
        System.out.println("   Afer creating objects with no ref: " + snapshot);
        printFinalizerInstanceCount();

        // Create objects and save references.
        objs = new Object[REF_COUNT];
        for (int i = 0; i < REF_COUNT; i++) {
            objs[i] = new MyObject();
        }
        snapshot = getSnapshot();
        System.out.println("   Afer creating objects with ref: " + snapshot);
        printFinalizerInstanceCount();

        // Now check the expected count - GC and runFinalization will be
        // invoked.
        checkFinalizerCount(NO_REF_COUNT, 0);

        // Clean the memory and remove all objects that are pending
        // finalization again
        objs = null;
        snapshot = getSnapshot();
        System.out.println("Clear all references finalized = " + snapshot);
        printFinalizerInstanceCount();

        checkFinalizerCount(TOTAL_FINALIZABLE, NO_REF_COUNT);

        snapshot = getSnapshot();
        printFinalizerInstanceCount();

        // Check the mbean now
        if (snapshot.curFinalized != TOTAL_FINALIZABLE) {
            throw new RuntimeException("Wrong number of finalized objects "
                                     + snapshot + ". Expected "
                                     + TOTAL_FINALIZABLE);
        }

        if (snapshot.curPending != 0) {
            throw new RuntimeException("Wrong number of objects pending "
                                     + " end = " + snapshot);
        }

    }

    private static void checkFinalizerCount(int expectedTotal, int curFinalized)
        throws Exception {
        int prevCount = -1;
        Snapshot snapshot = getSnapshot();
        if (snapshot.curFinalized != curFinalized) {
            throw new RuntimeException(
                    "Unexpected finalized objects: " + snapshot +
                    " but expected = " + curFinalized);
        }
        int MAX_GC_LOOP = 6;
        for (int i = 1;
             snapshot.curFinalized != expectedTotal && i <= MAX_GC_LOOP;
             i++) {
            System.gc();
            snapshot = getSnapshotAfterFinalization();

            if (snapshot.curFinalized == expectedTotal &&
                snapshot.curPending != 0) {
                throw new RuntimeException(
                    "Unexpected current number of objects pending for " +
                    "finalization: " + snapshot + " but expected = 0");
            }

            System.out.println("   After runFinalization " + i + ": " + snapshot);
            printFinalizerInstanceCount();

            try {
                Thread.sleep(1000);
            } catch (Exception e) {
                throw e;
            }
        }
        if (snapshot.curFinalized != expectedTotal) {
            throw new RuntimeException(
                "Unexpected current number of objects pending for " +
                "finalization: " + snapshot + " but expected > 0");
        }
    }

    private static Object lock = new Object();
    private static class MyObject {
        Object[] dummy = new Object[10];
        public void finalize () {
            synchronized (lock) {
                finalized++;
            }
        }
    }

    static class Snapshot {
        public int curFinalized;
        public int curPending;
        Snapshot(int f, int p) {
            curFinalized = f;
            curPending = p;
        }
        public String toString() {
            return "Current finalized = " + curFinalized +
                   " Current pending = " + curPending;
        }
    }

    private static Snapshot getSnapshot() {
        synchronized (lock) {
            int curCount = mbean.getObjectPendingFinalizationCount();
            return new Snapshot(finalized, curCount);
        }
    }

    // Repeat getSnapshot until no pending finalization.
    private static Snapshot getSnapshotAfterFinalization() throws Exception {
        int loopCount = 0;
        Snapshot snapshot = null;
        while (loopCount < 100) {
            Runtime.getRuntime().runFinalization();
            Thread.sleep(50);
            snapshot = getSnapshot();
            if (snapshot.curPending == 0) {
                return snapshot;
            }
            ++loopCount;
            System.out.println("Waiting for curPending to be 0. snapshot=" + snapshot);
        }
        String msg = "Objects pending finalization is not 0. snapshot=%s";
        throw new RuntimeException(String.format(msg, snapshot));
    }
}
