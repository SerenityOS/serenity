/*
 * Copyright (c) 2002, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @summary converted from VM Testbase gc/gctests/FinalizeTest01.
 * VM Testbase keywords: [gc, stress, stressopt, nonconcurrent, monitoring]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm -XX:-UseGCOverheadLimit gc.gctests.FinalizeTest01.FinalizeTest01
 */

package gc.gctests.FinalizeTest01;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import nsk.share.gc.*;
import nsk.share.TestFailure;
import nsk.share.test.ExecutionController;
import nsk.share.test.Stresser;

/**
 * Tests that GC works correctly with finalizers.
 *
 * Create a number of objects, some of which register their
 * creation/finalization. Then try to eat available memory
 * which forces garbage collection of created objects.
 * Garbage collector should try to collect the garbage and thus
 * call finalizers. The test checks that all finalizers have been called.
 */

/*
Reworked original test (4933478). Original comment:

Tests that objects w/finalizers will eventually finalize
(and by the way, the system doesn't crash!).

Returns exit code 0 on success, and 1 on failure.

 */
/**
 * Class with finalizer that throws Exception.
 * Used for FinalizeTest02
 */
class FinExceptMemoryObject extends FinMemoryObject {

    public FinExceptMemoryObject(int size) {
        super(size);
    }

    protected void finalize() {
        super.finalize();
        throw new RuntimeException("Exception in finalizer");
    }
}

public class FinalizeTest01 extends GCTestBase {

    private final int allocRatio = 5;
    private final int size = 1024 * 2;
    private int count = 1000;
    private static boolean throwExceptions = false;
    private ExecutionController stresser;

    private void runOne() {
        Object o;
        for (int i = 0; i < count; i++) {
            if (i % allocRatio == 0) {
                if (throwExceptions) {
                    o = new FinExceptMemoryObject(size);
                } else {
                    o = new FinMemoryObject(size);
                }
            } else {
                o = new byte[size - Memory.getObjectExtraSize()];
            }
        }
        o = null;

        MemoryMXBean mbean = ManagementFactory.getMemoryMXBean();
        long finalizationMaxTime = 1000 * 60; // 1min

        /* Provoke GC to start finalization. */
        Algorithms.eatMemory(stresser);
        if (!stresser.continueExecution()) {
            // we did not eat all memory
            return;
        }
        long waitTime = System.currentTimeMillis() + finalizationMaxTime;

        /*
         * Before we force finalization it is needed to check that we have
         * any object pending for finazlization. If not then is a GC bug.
         */
        while (FinMemoryObject.getFinalizedCount()
                + mbean.getObjectPendingFinalizationCount() == 0
                && (System.currentTimeMillis() < waitTime)) {
            System.out.println("No objects are found in the finalization queue. Waiting..");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ie) {
            }
        }
        if (FinMemoryObject.getFinalizedCount()
                + mbean.getObjectPendingFinalizationCount() == 0) {
            throw new TestFailure("Test failed. (No objects were not queued for finalization during 1min)");
        }

        /* force finalization and wait for it finishs */
        Runtime.getRuntime().runFinalization();

        boolean error = (FinMemoryObject.getLiveCount() != 0);

        /*
         * The runFinalization() starts the second finalization thread and wait until it finishs.
         * However it is a very little probability (less then 1%) that not all object are finalized yet.
         * Possibly the regular Finalizer thread have not finished its work when we check getLiveCount()
         * or GC is still clearing memory and adding objects to the queue.
         */
        waitTime = System.currentTimeMillis() + finalizationMaxTime;
        while (error && (System.currentTimeMillis() < waitTime)) {
            // wait 1 sec (it could be less due to potential InterruptedException)
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ie) {
            }
            error = (FinMemoryObject.getLiveCount() != 0);
        }

        if (error) {
            throw new TestFailure("Test failed (objects were not finalized during 1min)");
        }


        System.out.println("Allocated: " + FinMemoryObject.getAllocatedCount());
        System.out.println("Finalized: " + FinMemoryObject.getFinalizedCount());
        error = (FinMemoryObject.getLiveCount() != 0);
        if (error) {
            throw new TestFailure("Test failed.");
        }
    }

    public void run() {
        stresser = new Stresser(runParams.getStressOptions());
        stresser.start(runParams.getIterations());
        count = (int) Math.min(runParams.getTestMemory() / size, Integer.MAX_VALUE);
        System.out.println("Allocating " + count
                + " objects. 1 out of " + allocRatio
                + " will have a finalizer.");
        System.out.flush();
        runOne();
    }

    public static void main(String[] args) {
        for (int i = 0; i < args.length; ++i) {
            if (args[i].equals("-throwExceptions")) {
                throwExceptions = true;
            }
        }
        GC.runTest(new FinalizeTest01(), args);
    }
}
