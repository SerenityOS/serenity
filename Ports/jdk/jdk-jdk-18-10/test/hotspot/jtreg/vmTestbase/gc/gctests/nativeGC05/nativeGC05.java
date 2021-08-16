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
 * @key randomness
 *
 * @summary converted from VM Testbase gc/gctests/nativeGC05.
 * VM Testbase keywords: [gc]
 * VM Testbase readme:
 * ********************************
 * set TIMEOUT = 20
 * *******************************
 * This test creates a 2 dimensional matrix of (100X100)10,000 elements.
 * Each element in this matrix houses the address of a "Cell" that
 * occupies about 100 bytes. The total memory occupied by this structure is
 * about 1M.
 * Once this structure, has been built, 5 threads are let loose that
 * randomly choose an element in this matrix and set its contents to null
 * effectively creating 100bytes of garbage. The threads continue to act
 * until all 5 threads combined have "nulled out" half the cells in the matrix,
 * which amounts to 0.5Meg of garbage. The indices of each "nulled out"
 * cell is stored in a stack. This information is later used during
 * the refilling stage by the native function, kickOffRefiller();
 * The native function, kickOffRefiller() refills all the lacunae in the
 * matrix with new cells.
 * This process of nulling out" and refilling is repeated 50 times.
 * Every iteration produces  0.5 Meg
 * of garbage. The maximum amount of live memory at use at any time is 1Meg.
 * If no garbage collection takes place during any of the ten iterations,
 * the total amount(live + garbage) of heap space consumed at the end
 * of the program is 0.5*50 + 1 = 26Meg.
 * The test fails if an OutOfMemory Exception is thrown.
 *        -----------------------------        --------
 *       |     |     |     |     |     |       | 100  |
 *       |     |     |     |     |  *--|------>| bytes|
 *       |     |     |     |     |     |       --------
 *        -----------------------------
 *       .     .     .     .     .     .
 *       .     .     .     .     .     .
 *       .     .     .     .     .     .
 *       .
 *       |     |     |     |     |     |
 *       |     |     |     |     |     |
 *       |     |     |     |     |     |
 *        ------------------------------
 *       |     |     |     |     |     |
 *       |     |     |     |     |     |
 *       |     |     |     |     |     |
 *        ------------------------------
 *       |     |     |     |     |     |
 *       |     |     |     |     |     |
 *       |     |     |     |     |     |
 *        ------------------------------
 *       |     |     |     |     |     |
 *       |     |     |     |     |     |
 *       |     |     |     |     |     |
 *        -----------------------------
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm/native gc.gctests.nativeGC05.nativeGC05
 */

package gc.gctests.nativeGC05;

import nsk.share.TestFailure;
import nsk.share.test.*;
import nsk.share.gc.*;
import java.util.Stack;

public class nativeGC05 extends GCTestBase {
        private final int threadCount = 5;
        private Stack<IndexPair> emptiedLocations = new Stack<IndexPair>();
        private Matrix matrix = new Matrix(100, 100);

        public native void kickOffRefillers(Matrix matrix, Stack s);

        private class CellEmptier extends Thread {
                public CellEmptier() {
                }

                boolean keepEmptying(){
                        int numberOfCells;
                        int matrixSize;

                        matrixSize = matrix.returnArrayBound();
                        numberOfCells = (matrixSize + 1) * (matrixSize + 1) ;
                        if (matrix.getCellCount() < numberOfCells/2)
                                return true;
                        else
                                return false;
                }

                public void run() {
                        int i, j, matrixSize,emptyCells;

                        matrixSize = matrix.returnArrayBound();
                        while (keepEmptying()) {
                                i = LocalRandom.nextInt(0, matrixSize);
                                j = LocalRandom.nextInt(0, matrixSize);
                                emptiedLocations.push(new IndexPair(i,j)); //Register empty node
                                matrix.clear(i,j);
                        }
                }
        }


        private class StackDump extends Thread {
                public StackDump() {
                }

                public void run() {
                        int emptyCells;

                        while (true) {
                                emptyCells = emptiedLocations.size();
                                System.out.println("Number of empty cells = " + emptyCells);
                        }
                }
        }


        public void run() {
                Thread emptierArray[] = new Thread[threadCount];
                int count = 0;
                int memoryReserve[] = new int[10000];

                //Create 5 CellEmptier Threads. There should be about 1Meg of
                // created garbage by the times these 5 threads return.

                try {
                        while (count < 50) {
                                for (int i = 0; i < threadCount; i++)
                                        emptierArray[i] = new CellEmptier();
                                for (int i = 0; i < threadCount; i++)
                                        emptierArray[i].start();

                                // wait for "emptier" threads to finish their job

                                int i = 0;
                                while (i < threadCount) {
                                        try {
                                                emptierArray[i].join();
                                        } catch(InterruptedException e) {
                                        }
                                        i++;
                                }

                                // Now start refilling.

                                kickOffRefillers(matrix, emptiedLocations);

                                // reset count of cells emptied out and start again.
                                matrix.resetCellCount();
                                count++;
                        }
                } catch (OutOfMemoryError e) {
                        memoryReserve = null;
                        System.gc();
                        throw new TestFailure("Test Failed at " + count + "th iteration.");
                }
                System.out.println("Test passed.");
        }

        public static void main(String args[]) {
                GC.runTest(new nativeGC05(), args);
        }

        static {
                System.loadLibrary("nativeGC05");
        }
}
