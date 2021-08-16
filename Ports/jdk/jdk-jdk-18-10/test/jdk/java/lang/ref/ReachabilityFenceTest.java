/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 8133348
 * @summary Tests if reachabilityFence is working
 *
 * @requires vm.opt.DeoptimizeALot != true
 *
 * @run main/othervm -Xint                   -Dpremature=false ReachabilityFenceTest
 * @run main/othervm -XX:TieredStopAtLevel=1 -Dpremature=true  ReachabilityFenceTest
 * @run main/othervm -XX:TieredStopAtLevel=2 -Dpremature=true  ReachabilityFenceTest
 * @run main/othervm -XX:TieredStopAtLevel=3 -Dpremature=true  ReachabilityFenceTest
 * @run main/othervm -XX:TieredStopAtLevel=4 -Dpremature=true  ReachabilityFenceTest
 */

import java.lang.ref.Reference;
import java.util.concurrent.atomic.AtomicBoolean;

public class ReachabilityFenceTest {

    /*
     * Implementation notes:
     *
     * This test has positive and negative parts.
     *
     * Negative test is "nonFenced", and it tests that absent of reachabilityFence, the object can
     * be prematurely finalized -- this validates the test itself. Not every VM mode is expected to
     * prematurely finalize the objects, and -Dpremature option communicates that to test. If a VM mode
     * passes the negative test, then our understanding of what could happen is correct, and we can
     * go forward.
     *
     * Positive test is "fenced", and it checks that given the reachabilityFence at the end of the block,
     * the object cannot be finalized. There is no sense running a positive test when premature finalization
     * is not expected. It is a job for negative test to verify that invariant.
     *
     * The test methods should be appropriately compiled, therefore we do several iterations.
     */

    // Enough to OSR and compile
    static final int LOOP_ITERS = Integer.getInteger("LOOP_ITERS", 50000);

    // Enough after which to start triggering GC and finalization
    static final int WARMUP_LOOP_ITERS = LOOP_ITERS - Integer.getInteger("GC_ITERS", 100);

    // Enough to switch from an OSR'ed method to compiled method
    static final int MAIN_ITERS = 3;

    static final boolean PREMATURE_FINALIZATION = Boolean.getBoolean("premature");

    public static void main(String... args) {
        // Negative test
        boolean finalized = false;
        for (int c = 0; !finalized && c < MAIN_ITERS; c++) {
            finalized |= nonFenced();
        }

        if (PREMATURE_FINALIZATION && !finalized) {
            throw new IllegalStateException("The object had never been finalized before timeout reached.");
        }

        if (!PREMATURE_FINALIZATION && finalized) {
            throw new IllegalStateException("The object had been finalized without a fence, even though we don't expect it.");
        }

        if (!PREMATURE_FINALIZATION)
            return;

        // Positive test
        finalized = false;
        for (int c = 0; !finalized && c < MAIN_ITERS; c++) {
            finalized |= fenced();
        }

        if (finalized) {
            throw new IllegalStateException("The object had been prematurely finalized.");
        }
    }

    public static boolean nonFenced() {
        AtomicBoolean finalized = new AtomicBoolean();
        MyFinalizeable o = new MyFinalizeable(finalized);

        for (int i = 0; i < LOOP_ITERS; i++) {
            if (finalized.get()) break;
            if (i > WARMUP_LOOP_ITERS) {
                System.gc();
                System.runFinalization();
            }
        }

        return finalized.get();
    }

    public static boolean fenced() {
        AtomicBoolean finalized = new AtomicBoolean();
        MyFinalizeable o = new MyFinalizeable(finalized);

        for (int i = 0; i < LOOP_ITERS; i++) {
            if (finalized.get()) break;
            if (i > WARMUP_LOOP_ITERS) {
                System.gc();
                System.runFinalization();
            }
        }

        try {
            return finalized.get();
        } finally {
            Reference.reachabilityFence(o);
        }
    }

    private static class MyFinalizeable {
        private final AtomicBoolean finalized;

        public MyFinalizeable(AtomicBoolean b) {
            this.finalized = b;
        }

        @Override
        protected void finalize() throws Throwable {
            super.finalize();
            finalized.set(true);
        }
    }
}
