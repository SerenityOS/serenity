/*
 * Copyright (c) 2010, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @key stress
 *
 * @summary converted from VM Testbase gc/huge/quicklook/largeheap/Access.
 * VM Testbase keywords: [gc, stress, stressopt, nonconcurrent]
 * VM Testbase readme:
 * DESCRIPTION
 *     The test is intended to test 64-bit VM for large memory and heap
 *     functionalities. This test checks that no unexpected exceptions and errors
 *     are thrown or the JVM is not crashed during allocation of different
 *     objects within 32-bit address range.
 *     First of all the test checks that the maximum amount of memory that
 *     the Java virtual machine will attempt to use (Runtime.maxMemory()) is
 *     greater than 4G. If that value is less than 4G, the test passes, otherwise
 *     it starts testing.
 *     A number of threads is started. That number is set in *.cfg file or is
 *     calculated by the test itself based on the machine (see
 *     nsk.share.gc.Algorithms.getThreadsCount() method).
 *     Each thread creates 9 arrays - one of each of the following types: byte,
 *     short, char, int, long, boolean, double, float, Object. All arrays have
 *     the same size, and that size is calculated so that all threads together are
 *     supposed to eat about 1.3G of the heap (30% of 4G).
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm -XX:-UseGCOverheadLimit gc.huge.quicklook.largeheap.Access.access
 */

package gc.huge.quicklook.largeheap.Access;

import java.util.ArrayList;
import nsk.share.TestFailure;
import nsk.share.gc.*;
import nsk.share.test.LocalRandom;

public class access extends ThreadedGCTest {

    // The test should fill just about 30% of 4G range (32-bit address range)
    final static double PART_OF_HEAP = 0.3;
    final static long GIGOBYTE = 1024 * 1024 * 1024;
    // Approximate number of bytes for one element of each tested type
    // (byte, short, char, int, long, double, float, Object)
    final static long TYPES_SIZE =
            Memory.getByteSize()
            + Memory.getShortSize()
            + Memory.getIntSize()
            + Memory.getCharSize()
            + Memory.getLongSize()
            + Memory.getDoubleSize()
            + Memory.getFloatSize();
    //+ Memory.getBooleanSize()
    //+ Memory.getReferenceObjectSize();
    private final static int STORAGE_SIZE_DIM1 = 65536;
    private final static int STORAGE_SIZE_DIM2 = (int) (4 * GIGOBYTE / Memory.getLongSize() / STORAGE_SIZE_DIM1);
    // An array to eat 4G of heap
    private static long[][] storage = new long[STORAGE_SIZE_DIM1][];
    private volatile boolean is4GAllocated = false;
    private final Object lock = new Object();

    private class Worker implements Runnable {

        private int arraySize;
        private ArrayList<Object> list;

        public Worker(int arraySize) {
            this.arraySize = arraySize;
            list = new ArrayList<Object>();
        }

        public void run() {
            try {
                synchronized (lock) {
                    while (!is4GAllocated && getExecutionController().continueExecution()) {
                        try {
                            lock.wait(100);
                        } catch (InterruptedException ie) {
                        }
                    }
                }
                while (getExecutionController().continueExecution()) {
                    byte[] barray = new byte[arraySize];
                    short[] sarray = new short[arraySize];
                    char[] carray = new char[arraySize];
                    int[] iarray = new int[arraySize];
                    long[] larray = new long[arraySize];
                    double[] darray = new double[arraySize];
                    float[] farray = new float[arraySize];
                    list.add(barray);
                    list.add(sarray);
                    list.add(carray);
                    list.add(iarray);
                    list.add(larray);
                    list.add(darray);
                    list.add(darray);
                    for (int i = 0; i < arraySize; i++) {
                        larray[i] = (long) (i + 42);
                        darray[i] = (double) (42 * i);
                        farray[i] = (float) (0.6 * i);
                        if (i % 10000 == 0 &&
                            getExecutionController().continueExecution()) {
                            return;
                        }
                    }

                    for (int i = arraySize - 1; i > 0; i -= 10) {
                        if (larray[i] != (long) (i + 42)) {
                            throw new TestFailure("The value = "
                                    + larray[i] + " when expected ="
                                    + (long) (i + 42));
                        }
                        if (darray[i] - (double) (42 * i) > 0.001) {
                            throw new TestFailure("The value = "
                                    + darray[i] + " when expected ="
                                    + (double) (i + 42));
                        }
                        if (farray[i] - (float) (0.6 * i) > 0.001) {
                            throw new TestFailure("The value = "
                                    + farray[i] + " when expected ="
                                    + (float) (i + 42));
                        }
                        if (i % 10000 == 0 &&
                            getExecutionController().continueExecution()) {
                            return;
                        }
                    }
                    for (Object obj : list) {
                        // check hashcode just to avoid optimization
                        if (obj.hashCode() == -1) {
                            throw new TestFailure("Unexpected hashcode");
                        }
                    }
                }
            } finally {
                list.clear();
            }
        }
    }

    class MainWorker implements Runnable {

        @Override
        public void run() {
            // ensure LocalRandom is loaded and has enough memory
            LocalRandom.init();
            synchronized (lock) {
                for (int i = 0; i < STORAGE_SIZE_DIM1; i++) {
                    if (!getExecutionController().continueExecution()) {
                        log.debug("Test run out of time before 4G were allocated");
                        lock.notifyAll();
                        return;
                    }
                    storage[i] = new long[STORAGE_SIZE_DIM2];
                }
                log.debug("The 4G are allocated, starting to test");
                is4GAllocated = true;
                lock.notifyAll();
            }
            while (getExecutionController().continueExecution()) {
                int i = LocalRandom.nextInt(STORAGE_SIZE_DIM1);
                int j = LocalRandom.nextInt(STORAGE_SIZE_DIM2);
                long value = LocalRandom.nextLong();
                storage[i][j] = value;
                if (storage[i][j] != value) {
                    throw new TestFailure("The value = "
                            + storage[i][j] + " when expected ="
                            + value);
                }
            }

        }
    }

    @Override
    public void run() {
        if (testConditions()) {
            super.run();
        }
    }

    @Override
    protected Runnable createRunnable(int i) {
        // Size of array for all types
        long proposedSize = (long) ((Runtime.getRuntime().maxMemory() - 4 * GIGOBYTE * PART_OF_HEAP)
                / (runParams.getNumberOfThreads() - 1) / TYPES_SIZE);
        int arraySize = Algorithms.getArraySize(proposedSize);
        if (i == 0) {
            return new MainWorker();
        }
        return new Worker(arraySize);
    }

    public boolean testConditions() {
        long maxMemory = Runtime.getRuntime().maxMemory();
        // If a machine has less than 4G for heap, there is nothing to test,
        // so exit peacefully
        if (maxMemory < 5 * GIGOBYTE) {
            log.debug("Heap is less than 5G ("
                    + maxMemory + " bytes), nothing to "
                    + "test");
            return false;
        }
        return true;
    }

    public static void main(String[] args) {
        GC.runTest(new access(), args);
    }
}
