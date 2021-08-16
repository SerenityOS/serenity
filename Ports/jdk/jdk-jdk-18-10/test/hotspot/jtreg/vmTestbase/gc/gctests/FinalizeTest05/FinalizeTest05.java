/*
 * Copyright (c) 2009, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @summary converted from VM Testbase gc/gctests/FinalizeTest05.
 * VM Testbase keywords: [gc, stress, stressopt, nonconcurrent]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm -XX:-UseGCOverheadLimit gc.gctests.FinalizeTest05.FinalizeTest05
 */

package gc.gctests.FinalizeTest05;

import nsk.share.gc.*;
import java.util.*;
import nsk.share.TestFailure;
import nsk.share.test.ExecutionController;
import nsk.share.test.Stresser;

/**
 */
public class FinalizeTest05 extends GCTestBase {

    private final int allocRatio = 5;
    private final int size = 1024 * 2;
    private int count = 1000;
    private ExecutionController stresser;

    private void runOne() {
        ArrayList objs = new ArrayList(count);
        Object o;
        for (int i = 0; i < count; i++) {
            if (i % allocRatio == 0) {
                o = new FinMemoryObject(size);
                objs.add(o);
            } else {
                o = new byte[size - Memory.getObjectExtraSize()];
            }
        }
        FinMemoryObject.dumpStatistics();
        o = null;

        /* force finalization  */
        Algorithms.eatMemory(stresser);
        System.gc();
        Runtime.getRuntime().runFinalization();
        System.gc();
        Runtime.getRuntime().runFinalization();

        FinMemoryObject.dumpStatistics();

        boolean error;
        System.out.println("Allocated: " + FinMemoryObject.getAllocatedCount());
        System.out.println("Finalized: " + FinMemoryObject.getFinalizedCount());
        error = (FinMemoryObject.getFinalizedCount() != 0);

        // Just hit the objs array to do say VM that we use this object
        // and it should be alive in this point.
        objs.clear();
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
        GC.runTest(new FinalizeTest05(), args);
    }
}
