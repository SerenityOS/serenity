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
 * @summary converted from VM Testbase gc/gctests/WeakReference/weak004.
 * VM Testbase keywords: [gc, stress, stressopt, nonconcurrent]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm -XX:-UseGCOverheadLimit gc.gctests.WeakReference.weak004.weak004 -t 1
 */

package gc.gctests.WeakReference.weak004;

import nsk.share.gc.*;
import java.lang.ref.WeakReference;

/**
 * Test that GC correctly clears weak references.
 *
 * This test is the same as weak003 except that it creates
 * weak references to same object.
 */
public class weak004 extends ThreadedGCTest {

    class Worker implements Runnable, OOMStress {

        private int arrayLength;
        private int objectSize = 100;
        private WeakReference[] references;

        private void makeReferences() {
            MemoryObject obj = new MemoryObject(objectSize);
            references = new WeakReference[arrayLength];
            for (int i = 0; i < arrayLength; ++i) {
                references[i] = new WeakReference<MemoryObject>(obj);
            }
        }

        public void run() {
            arrayLength = Memory.getArrayLength(runParams.getTestMemory() - objectSize, Memory.getReferenceSize() + objectSize);
            System.out.println("Array size: " + arrayLength);
            makeReferences();
            Algorithms.eatMemory(getExecutionController());
            if (!getExecutionController().continueExecution()) {
                return;
            }
            int n = 0;
            for (int i = 0; i < arrayLength; ++i) {
                if (references[i].get() != null) {
                    ++n;
                }
            }
            if (n != 0) {
                log.error("Some of the references have been not cleared: " + n);
                setFailed(true);
            }
        }
    }

    @Override
    protected Runnable createRunnable(int i) {
        return new Worker();
    }

    public static void main(String[] args) {
        GC.runTest(new weak004(), args);
    }
}
