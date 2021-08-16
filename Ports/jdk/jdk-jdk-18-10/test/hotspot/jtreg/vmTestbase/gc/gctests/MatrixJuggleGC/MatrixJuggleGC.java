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
 * @summary converted from VM Testbase gc/gctests/MatrixJuggleGC.
 * VM Testbase keywords: [gc, stress, stressopt, nonconcurrent]
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
 * until all 5 threads combined have "nulled out" half the cells in the matrix.
 * At this point, 5 refiller threads proceed to refill the empty
 * matrix elements with new cells.
 * Once the refiller threads have refilled all the empty matrix elements
 * with new cells, the cycle begins all over again with the 5 "emptier"
 * threads "nulling out" cells randomly.
 * This is repeated 50 times.  Every iteration produces  0.5 Meg
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
 * @run main/othervm gc.gctests.MatrixJuggleGC.MatrixJuggleGC -iterations 1000000
 */

package gc.gctests.MatrixJuggleGC;

import nsk.share.test.*;
import nsk.share.gc.*;
import java.util.Stack;
import java.util.EmptyStackException;

public class MatrixJuggleGC extends GCTestBase {
        private int threadCount = 5;
        private Matrix cm = new Matrix(100, 100);
        private Stack<IndexPair> emptiedLocations = new Stack<IndexPair>();

        private class CellEmptier extends Thread {
                private boolean keepEmptying(){
                        int numberOfCells;
                        int matrixSize;

                        matrixSize = cm.returnArrayBound();
                        numberOfCells = (matrixSize + 1) * (matrixSize + 1) ;
                        if (cm.getCellCount() < numberOfCells/2)
                                return true;
                        else
                                return false;
                }

                public void run() {
                        int i, j, matrixSize,emptyCells;

                        matrixSize = cm.returnArrayBound();
                        while (keepEmptying()) {
                                i = LocalRandom.nextInt(0, matrixSize);
                                j = LocalRandom.nextInt(0, matrixSize);
                                emptiedLocations.push(new IndexPair(i,j));
                                cm.clear(i, j);
                        }
                }
        }

        private class CellRefiller extends Thread {
                public void run() {
                        int i, j, emptyCells;
                        while (!emptiedLocations.empty()) {
                                try {
                                        IndexPair pair = emptiedLocations.pop();
                                        cm.repopulate(pair.getI(), pair.getJ());
                                } catch (EmptyStackException e) {
                                        break;
                                }
                        }
                }
        }

        private class StackDump extends Thread {
                public void run() {
                        int emptyCells;
                        while (true) {
                                emptyCells = emptiedLocations.size();
                                System.out.println("Number of empty cells = " + emptyCells);
                        }
                }
        }

        private void runIteration() {
                Thread emptierArray[] = new Thread[threadCount];
                Thread fillerArray[]  = new Thread[threadCount];
                for (int i = 0; i < threadCount; i++)
                        emptierArray[i] = new CellEmptier();
                for (int i = 0; i < threadCount; i++)
                        emptierArray[i].start();

                // wait for "emptier" threads to finish their job

                int i = 0;
                while (i < threadCount) {
                        try {
                                emptierArray[i].join();
                        } catch(InterruptedException e) {}
                        i++;
                }

                // Now start refilling.

                for (i = 0; i < threadCount; i++)
                        fillerArray[i] = new CellRefiller();
                for (i = 0; i < threadCount; i++)
                        fillerArray[i].start();

                i = 0;
                while (i < threadCount ){
                        try {
                                fillerArray[i].join();
                        } catch(InterruptedException e){}
                        i++;
                }
                // reset count of cells
                cm.resetCellCount();
        }

        public void run() {
                threadCount = runParams.getNumberOfThreads();
                Stresser stresser = new Stresser(runParams.getStressOptions());
                stresser.start(runParams.getIterations());
                while (stresser.iteration())
                        runIteration();
                stresser.finish();
        }

        public static void main(String args[]) {
                GC.runTest(new MatrixJuggleGC(), args);
        }
}
