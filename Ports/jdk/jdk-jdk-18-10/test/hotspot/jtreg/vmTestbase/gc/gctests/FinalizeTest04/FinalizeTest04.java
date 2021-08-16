/*
 * Copyright (c) 2007, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @summary converted from VM Testbase gc/gctests/FinalizeTest04.
 * VM Testbase keywords: [gc, stress, stressopt, nonconcurrent]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm -XX:-UseGCOverheadLimit gc.gctests.FinalizeTest04.FinalizeTest04
 */

package gc.gctests.FinalizeTest04;

import nsk.share.test.*;
import nsk.share.gc.*;

/**
 * Test that synchronization between GC and finalizer thread
 * (if any) is correct.
 *
 * This test creates objects that do GC-related work in finalizer,
 * e.g. call System.gc(), System.runFinalization(), Algorithms.eatMemory().
 *
 * @see nsk.share.gc.Algorithms#eatMemory()
 * @see java.lang.System#gc()
 * @see java.lang.System#runFinalization()
 */
public class FinalizeTest04 extends GCTestBase {

    private int objectSize = 100;
    private int objectCount = 100;
    private ExecutionController stresser;

    private class FinMemoryObject2 extends FinMemoryObject {

        public FinMemoryObject2(int size) {
            super(size);
        }

        protected void finalize() {
            super.finalize();
            System.gc();
            Algorithms.eatMemory(stresser);
            System.runFinalization();
            System.gc();
            Algorithms.eatMemory(stresser);
            System.gc();
        }
    }

    public void run() {
        stresser = new Stresser(runParams.getStressOptions());
        stresser.start(runParams.getIterations());
        for (int i = 0; i < objectCount; ++i) {
            new FinMemoryObject2(objectSize);
        }
        Algorithms.eatMemory(stresser);
    }

    public static void main(String[] args) {
        GC.runTest(new FinalizeTest04(), args);
    }
}
