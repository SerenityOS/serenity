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
 * @summary converted from VM Testbase gc/memory/Array/ArrayJuggle/Juggle2.
 * VM Testbase keywords: [gc, stress, stressopt, nonconcurrent]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm -Xlog:gc=debug:gc.log gc.memory.Array.ArrayJuggle.Juggle2.Juggle2
 */

package gc.memory.Array.ArrayJuggle.Juggle2;

import nsk.share.test.*;
import nsk.share.gc.*;

/**
 * Test that tries to confuse the GC.
 *
 * This program initializes a main array and launches some threads
 * which modify and copy portions of the array to try to confuse
 * the GC.
 */
public class Juggle2 extends ThreadedGCTest {
        private int arraySize = 1000;
        private int objectSize = 1000;
        private int maxLinkLength = 100;
        private int maxCopySize = arraySize / 10;
        private int threadsCount = 30;
        private LinkedMemoryObject mainArray[];

        private class MainArrayWalker implements Runnable {
                public void run() {
                        int index = LocalRandom.nextInt(arraySize);
                        int cellSize = LocalRandom.nextInt(objectSize);
                        mainArray[index] = new LinkedMemoryObject(cellSize);
                        //mainArray[index] = Memory.makeLinearList(maxLinkLength, objectSize);
                }

                public String toString() {
                        return "Thread-A";
                }
        }

        private class LinkMaker implements Runnable {
                private int n;

                public void run() {
                        int index = LocalRandom.nextInt(arraySize);
                        // Sometimes clear the reference so the lists do not become too large
                        if (++n == maxLinkLength) {
                                mainArray[index] = null;
                                n = 0;
                        }
                        //for (int i = 0; i < thisChainLength; ++i)
                        //      mainArray[index] = new LinkedMemoryObject(cellSize, mainArray[index]);
                        //Memory.makeLinearList(maxLinkLength, objectSize);
                        mainArray[index] = Memory.makeRandomLinearList(maxLinkLength, objectSize);
                }

                public String toString() {
                        return "Thread-B";
                }
        }

        private class CopyingThread implements Runnable {
                private LinkedMemoryObject localArray[];
                private int currentIndex = 0;

                public CopyingThread() {
                        localArray = new LinkedMemoryObject[maxCopySize];
                        for (int i = 0; i < maxCopySize; ++i)
                                localArray[i] = new LinkedMemoryObject(0);
                }

                public void run() {
                        int toCopy = LocalRandom.nextInt(maxCopySize);
                        int mainIndex = LocalRandom.nextInt(arraySize);
                        for (int i = 0; i < toCopy; i++) {
                                localArray[currentIndex].setNext(mainArray[mainIndex]);
                                currentIndex = (currentIndex + 1) % maxCopySize;
                                mainIndex = (mainIndex + 1) % arraySize;
                        }
                }

                public String toString() {
                        return "Thread-C";
                }
        }

        protected Runnable createRunnable(int i) {
                switch (i % 3) {
                case 0:
                        return new MainArrayWalker();
                case 1:
                        return new LinkMaker();
                case 2:
                default:
                        return new CopyingThread();
                }
        }

        public void run() {
                // arraySize * (objectSize + referenceSize) + threadsCount * (referenceSize arraySize/10 * (referenceSize + objectSize)) = memory
                long referenceSize = Memory.getReferenceSize();
                long objectExtraSize = Memory.getObjectExtraSize();
                arraySize = Memory.getArrayLength(
                        runParams.getTestMemory(),
                        Memory.getListSize(maxLinkLength, objectSize)
                );
                maxCopySize = arraySize / 10 - 1;
                arraySize = arraySize * 9 / 10 - 1;
                System.out.println("Array size: " + arraySize);
                mainArray  = new LinkedMemoryObject[arraySize];
                Memory.fillArrayRandom(mainArray, arraySize, objectSize);
                super.run();
        }

        public static void main(String args[]) {
                GC.runTest(new Juggle2(), args);
        }
}
