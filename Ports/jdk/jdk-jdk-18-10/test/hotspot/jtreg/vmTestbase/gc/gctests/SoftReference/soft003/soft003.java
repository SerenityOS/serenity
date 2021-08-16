/*
 * Copyright (c) 2007, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @summary converted from VM Testbase gc/gctests/SoftReference/soft003.
 * VM Testbase keywords: [gc, stress, stressopt, nonconcurrent]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm -XX:-UseGCOverheadLimit gc.gctests.SoftReference.soft003.soft003 -t 1
 */

package gc.gctests.SoftReference.soft003;

import java.lang.ref.Reference;
import nsk.share.test.*;
import nsk.share.gc.*;
import nsk.share.TestFailure;
import java.lang.ref.SoftReference;

/**
 * Test that GC clears soft references before throwing OOM.
 *
 * This test creates a number of soft references, then provokes
 * GC with Algorithms.eatMemory() and checks that all references
 * have been cleared.
 */
public class soft003 extends ThreadedGCTest {

    class Worker implements Runnable, OOMStress {

        private int arrayLength;
        private int objectSize = 100;
        private Reference[] references;

        public void run() {
            for (int i = 0; i < arrayLength; ++i) {
                references[i] = new SoftReference<MemoryObject>(new MemoryObject(LocalRandom.nextInt(objectSize)));
            }
            Algorithms.eatMemory(getExecutionController());
            if (!getExecutionController().continueExecution()) {
                return;
            }
            // Check that all references have been cleared
            int n = 0;
            for (int i = 0; i < arrayLength; ++i) {
                if (references[i].get() != null) {
                    ++n;
                }
            }
            if (n != 0) {
                references = null;
                throw new TestFailure("Some of the references have been not cleared: " + n);
            }
        }

        public void Worker() {
            arrayLength = Memory.getArrayLength(runParams.getTestMemory(), Memory.getReferenceSize() + objectSize);
            System.out.println("Array size: " + arrayLength);
            references = new Reference[arrayLength];
        }
    }

    @Override
    protected Runnable createRunnable(int i) {
        return new Worker();
    }

    public static void main(String[] args) {
        GC.runTest(new soft003(), args);
    }
}
