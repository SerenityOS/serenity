/*
 * Copyright (c) 2011, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @summary converted from VM Testbase gc/gctests/OneeFinalizerTest.
 * VM Testbase keywords: [gc, stress, stressopt, nonconcurrent, jrockit]
 * VM Testbase readme:
 * DESCRIPTION
 * Regression test that verifies that only one finalizer gets called.
 *
 * COMMENTS
 * This test was ported from JRockit test suite.
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm
 *      -XX:-UseGCOverheadLimit
 *      -Xlog:gc:gc.log
 *      gc.gctests.OneeFinalizerTest.OneeFinalizerTest
 */

package gc.gctests.OneeFinalizerTest;

import nsk.share.TestFailure;
import nsk.share.gc.GC;
import nsk.share.gc.GCTestBase;
import nsk.share.gc.gp.GarbageUtils;
import nsk.share.test.Stresser;

/**
 * Test that verifies that finalize() method is invoking only once.
 */
public class OneeFinalizerTest extends GCTestBase {

    private GlobalSafeCounter[] finalizerCounters = null;

    /**
     * Helper class used for counting number of calls to finalizers.
     */
    protected class GlobalSafeCounter {

        private int counter;

        /**
         * Constructor that inits the global counter to 0.
         */
        protected GlobalSafeCounter() {
            counter = 0;
        }

        /**
         * Reset the global counter to 0.
         */
        protected final void resetCounter() {
            synchronized (this) {
                counter = 0;
            }
        }

        /**
         * Increase the global counter by 1.
         */
        protected final void increaseCounter() {
            synchronized (this) {
                counter++;
            }
        }

        /**
         * Retrieve the global counter value.
         *
         * @return value of the global counter
         */
        protected final int getCounterValue() {
            int value;

            synchronized (this) {
                value = counter;
            }

            return value;
        }
    }

    /**
     * Helper class the implements finalize(), and that increments
     * the global counters for each finalize() invokation.
     */
    protected class FinalizedObject {

        private final int counterIndex;

        /**
         * Constructor for the helper object which implements finalize().
         *
         * @param index Index for the counter in the global array, that
         * corresponds to this object.
         */
        protected FinalizedObject(int index) {
            counterIndex = index;
        }

        /**
         * Increases the global counter for this object when finalize()
         * gets called (to make sure each finalizer gets called onee).
         */
        @Override
        protected final void finalize() {
            finalizerCounters[counterIndex].increaseCounter();
        }
    }

    private void initOneeFinalizerTest(int numberOfObjects) {
        // NOTE: Set to null in case it's been used before (to prevent OOM)
        finalizerCounters = null;
        finalizerCounters = new GlobalSafeCounter[numberOfObjects];

        for (int i = 0; i < numberOfObjects; i++) {
            finalizerCounters[i] = new GlobalSafeCounter();
        }
    }

    /**
     * Tests that the finalize() method on each FinalizedObject instance
     * has been called exactly one time.
     */
    @Override
    public void run() {


        int numberOfObjects = 2000;

        initOneeFinalizerTest(numberOfObjects);

        FinalizedObject[] testObjects = new FinalizedObject[numberOfObjects];

        // creates garbage
        for (int i = 0; i < numberOfObjects; i++) {
            testObjects[i] = new FinalizedObject(i);
        }

        if (testObjects[0].hashCode() == 212_85_06) {
            System.out.println("Bingo!!!");
        }

        testObjects = null;

        Stresser stresser = new Stresser(runParams.getStressOptions());
        stresser.start(0);
        /* force finalization  */
        GarbageUtils.eatMemory(stresser);
        if (!stresser.continueExecution()) {
            // may be we didn't eat all memory and didn't provoke GC
            System.out.println("Passed without check");
            return;
        }
        System.gc();
        System.runFinalization();
        System.gc();
        System.runFinalization();
        System.gc();

        int numberOfFinalizersRunMoreThanOnce = 0;
        int numberOfFinalizersNotRun = 0;

        for (int i = 0; i < numberOfObjects; i++) {
            int counter = finalizerCounters[i].getCounterValue();
            if (counter > 1) {
                numberOfFinalizersRunMoreThanOnce++;
                System.err.println("Object #" + i + " counter = " + counter);
            } else if (counter == 0) {
                System.err.println("WARNING: Finalizer not run for object #" + i);
                numberOfFinalizersNotRun++;
            }
        }

        if (numberOfFinalizersNotRun > 0) {
            System.err.println("WARNING: " + numberOfFinalizersNotRun + " finalizers not run");
        }

        if (numberOfFinalizersRunMoreThanOnce != 0) {
            throw new TestFailure("OneeFinalizerTest failed. " + numberOfFinalizersRunMoreThanOnce + " errors");
        }
        System.out.println("Test passed.");
    }

    public static void main(String[] args) {
        GC.runTest(new OneeFinalizerTest(), args);
    }
}
