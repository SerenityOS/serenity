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
 * @summary converted from VM Testbase gc/memory/Churn/Churn4.
 * VM Testbase keywords: [gc, stress, stressopt, nonconcurrent]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm gc.memory.Churn.Churn4.Churn4
 */

package gc.memory.Churn.Churn4;

import nsk.share.test.*;
import nsk.share.gc.*;

/**
 *  Test that GC works with memory that is churn over.
 *
 *  This test starts a number of threads that create objects,
 *  keep references to them in array and overwrite them.
 *  The test checks that GC is able to collect these objects.
 *
 *  This test is the same as Churn1 except that it creates objects
 *  1/10 of which will have non-trivial finalizer.
 *
 *  @see gc.memory.Churn.Churn1.Churn1
 */
public class Churn4 extends ThreadedGCTest {
        private int multiplier = 10;
        private int sizeOfArray;

        private class ThreadObject implements Runnable {
                private MemoryObject[] objectArray = new MemoryObject[sizeOfArray];
                int allocationMod = 0;
                int allocationModulus = 10;

                public ThreadObject() {
                        for (int i = 0; i < sizeOfArray; i ++) {
                                if (allocationMod == 0)
                                        objectArray[i] = new FinMemoryObject(multiplier);
                                else
                                        objectArray[i] = new FinMemoryObject1(multiplier * i);
                                allocationMod++;
                                allocationMod %= allocationModulus;
                        }
                }

                public void run() {
                        int index = LocalRandom.nextInt(sizeOfArray);
                        objectArray[index] = new FinMemoryObject1(multiplier * index);
                        allocationMod++;
                        allocationMod %= allocationModulus;
                }
        }

        protected Runnable createRunnable(int i) {
                return new ThreadObject();
        }

        public void run() {
                sizeOfArray = (int) Math.min(Math.sqrt(runParams.getTestMemory() * 2 / runParams.getNumberOfThreads() / multiplier), Integer.MAX_VALUE);
                System.out.println("Size of array: " + sizeOfArray);
                super.run();
        }

        public static void main(String args[]) {
                GC.runTest(new Churn4(), args);
        }
}
