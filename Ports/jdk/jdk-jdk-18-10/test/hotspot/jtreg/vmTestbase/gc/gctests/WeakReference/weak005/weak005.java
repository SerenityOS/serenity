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
 * @summary converted from VM Testbase gc/gctests/WeakReference/weak005.
 * VM Testbase keywords: [gc, stress, stressopt, nonconcurrent]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm -XX:-UseGCOverheadLimit gc.gctests.WeakReference.weak005.weak005 -t 1
 */

package gc.gctests.WeakReference.weak005;

import nsk.share.gc.*;
import java.lang.ref.WeakReference;

/**
 * Test that GC correctly clears weak references.
 *
 * This test creates a number of weak references,
 * each of which points to the next, then provokes
 * GC with Algorithms.eatMemory(). The test succeeds
 * if last reference has been cleared.
 */
public class weak005 extends ThreadedGCTest {

    class Worker implements Runnable, OOMStress {

        private int length = 10000;
        private int objectSize = 10000;
        private WeakReference[] references;

        public Worker() {
            System.out.println("Array size: " + length);
            System.out.println("Object size: " + objectSize);
            references = new WeakReference[length];
        }

        private void makeReferences() {
            references[length - 1] = null;
            MemoryObject obj = new MemoryObject(objectSize);
            references[0] = new WeakReference(obj);
            for (int i = 1; i < length; ++i) {
                references[i] = new WeakReference(references[i - 1]);
            }
            for (int i = 0; i < length - 1; ++i) {
                references[i] = null;
            }
        }

        public void run() {
            makeReferences();
            Algorithms.eatMemory(getExecutionController());
            if (!getExecutionController().continueExecution()) {
                return;
            }
            if (references[length - 1].get() != null) {
                log.error("Last weak reference has not been cleared");
                setFailed(true);
            }
        }
    }

    @Override
    protected Runnable createRunnable(int i) {
        return new Worker();
    }

    public static void main(String[] args) {
        GC.runTest(new weak005(), args);
    }
}
