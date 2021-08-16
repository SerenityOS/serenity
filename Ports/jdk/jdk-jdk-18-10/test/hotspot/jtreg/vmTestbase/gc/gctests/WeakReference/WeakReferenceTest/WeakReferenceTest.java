/*
 * Copyright (c) 2004, 2018, Oracle and/or its affiliates. All rights reserved.
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
package gc.gctests.WeakReference;

import java.util.Random;
import java.util.ArrayList;
import java.lang.ref.WeakReference;
import nsk.share.TestFailure;
import nsk.share.gc.GC;
import nsk.share.gc.GCTestBase;

/**
 * Original description from JRockit test:
 *
 * Tests for the WeakReference handling in JRockit.
 *
 * This test must be run with a mx value set, as well as with
 * -XXfullsystemgc, to ensure Runtime.maxMemory() doesn't return 0
 * and that System.gc() gets run when called from Java.
 */
public class WeakReferenceTest extends GCTestBase {

    /**
     * Test that all WeakReferences has been cleared after
     * the GC has run. This test should not throw an OOME
     * during the test execution.
     *
     */
    @Override
    public void run() {
        long seed;
        int minSize;
        int maxSize;
        double memPercentToFill;
        long totalMemAlloced = 0;
        long memToAlloc = 0;
        Runtime r = Runtime.getRuntime();

        seed = runParams.getSeed();
        minSize = 2048;
        maxSize = 32768;

        memPercentToFill = 0.45;

        memToAlloc = (long) (r.maxMemory() * memPercentToFill);

        Random rndGenerator = new Random(seed);
        long multiplier = maxSize - minSize;
        ArrayList arrWeakRefs = new ArrayList();

        try {
            while (totalMemAlloced < memToAlloc) {
                int allocationSize = ((int) (rndGenerator.nextDouble()
                        * multiplier)) + minSize;
                byte[] tmp = new byte[allocationSize];

                arrWeakRefs.add(new WeakReference(tmp));
                totalMemAlloced += allocationSize;

                // Make sure the temporary object is dereferenced
                tmp = null;
            }

            System.gc();

            long numberOfNotNulledObjects = 0;

            for (int i = 0; i < arrWeakRefs.size(); i++) {
                WeakReference wr = (WeakReference) arrWeakRefs.get(i);
                Object o = wr.get();

                if (o != null) {
                    numberOfNotNulledObjects++;
                }
            }

            if (numberOfNotNulledObjects > 0) {
                throw new TestFailure(numberOfNotNulledObjects + " out of "
                        + arrWeakRefs.size() + " WeakReferences was not "
                        + "null after the GC had run");
            }

            log.info("All WeakReferences was cleared after the "
                    + "GC had run");
        } catch (OutOfMemoryError oome) {
            throw new TestFailure("OutOfMemoryException was thrown. This should "
                    + "not happen during the execution of this test.");
        } finally {
            arrWeakRefs = null;
        }
    }

    public static void main(String[] args) {
        GC.runTest(new WeakReferenceTest(), args);
    }
}
