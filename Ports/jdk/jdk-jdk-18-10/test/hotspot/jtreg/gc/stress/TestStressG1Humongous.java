/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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

package gc.stress;

/*
 * @test TestStressG1Humongous
 * @key stress
 * @summary Stress G1 by humongous allocations in situation near OOM
 * @requires vm.gc.G1
 * @requires !vm.flightRecorder
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 * @run driver/timeout=1300 gc.stress.TestStressG1Humongous
 */

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import jdk.test.lib.Platform;
import jdk.test.lib.Utils;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;

public class TestStressG1Humongous{

    public static void main(String[] args) throws Exception {
        // Limit heap size on 32-bit platforms
        int heapSize = Platform.is32bit() ? 512 : 1024;
        // Heap size, region size, threads, humongous size, timeout
        run(heapSize, 4, 3, 1.1, 120);
        run(heapSize, 16, 5, 2.1, 120);
        run(heapSize, 32, 4, 0.6, 120);
        run(heapSize, 1, 7, 0.6, 600);
    }

    private static void run(int heapSize, int regionSize, int threads, double humongousSize, int timeout)
            throws Exception {
        ArrayList<String> options = new ArrayList<>();
        Collections.addAll(options, Utils.getTestJavaOpts());
        Collections.addAll(options,
                "-Xlog:gc=debug",
                "-Xmx" + heapSize + "m",
                "-XX:+UseG1GC",
                "-XX:G1HeapRegionSize=" + regionSize + "m",
                "-Dtimeout=" + timeout,
                "-Dthreads=" + threads,
                "-Dhumongoussize=" + humongousSize,
                "-Dregionsize=" + regionSize,
                TestStressG1HumongousImpl.class.getName()
        );
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(options);
        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        output.shouldHaveExitValue(0);
    }
}

class TestStressG1HumongousImpl {
    // Timeout in seconds
    private static final int TIMEOUT = Integer.getInteger("timeout", 60);
    private static final int THREAD_COUNT = Integer.getInteger("threads", 2);
    private static final int REGION_SIZE = Integer.getInteger("regionsize", 1) * 1024 * 1024;
    private static final int HUMONGOUS_SIZE = (int) (REGION_SIZE * Double.parseDouble(System.getProperty("humongoussize", "1.5")));
    private static final int NUMBER_OF_FREE_REGIONS = 2;

    private volatile boolean isRunning;
    private final Thread[] threads;
    private final AtomicInteger alocatedObjectsCount;
    private CountDownLatch countDownLatch;
    public static final List<Object> GARBAGE = Collections.synchronizedList(new ArrayList<>());

    public static void main(String[] args) throws InterruptedException {
        new TestStressG1HumongousImpl().run();
    }

    public TestStressG1HumongousImpl() {
        isRunning = true;
        threads = new Thread[THREAD_COUNT];
        alocatedObjectsCount = new AtomicInteger(0);
    }

    private void run() throws InterruptedException {
        new Thread(new Timer()).start();
        int checkedAmountOfHObjects = getExpectedAmountOfObjects();
        while (isRunning()) {
            countDownLatch = new CountDownLatch(THREAD_COUNT);
            startAllocationThreads(checkedAmountOfHObjects);
            countDownLatch.await();
            GARBAGE.clear();
            System.out.println("Allocated " + alocatedObjectsCount.get() + " objects.");
            alocatedObjectsCount.set(0);
        }
        System.out.println("Done!");
    }

    /**
     * Tries to fill available memory with humongous objects to get expected amount.
     * @return expected amount of humongous objects
     */
    private int getExpectedAmountOfObjects() {
        long maxMem = Runtime.getRuntime().maxMemory();
        int expectedHObjects = (int) (maxMem / HUMONGOUS_SIZE);
        // Will allocate NUMBER_OF_FREE_REGIONS region less to give some free space for VM.
        int checkedAmountOfHObjects = checkHeapCapacity(expectedHObjects) - NUMBER_OF_FREE_REGIONS;
        if (checkedAmountOfHObjects <= 0) {
            throw new RuntimeException("Cannot start testing because selected maximum heap "
                    + "is not large enough to contain more than " + NUMBER_OF_FREE_REGIONS + " regions");
        }
        return checkedAmountOfHObjects;
    }

    /**
     * Starts several threads to allocate the requested amount of humongous objects.
     * @param totalObjects total amount of object that will be created
     */
    private void startAllocationThreads(int totalObjects) {
        int objectsPerThread = totalObjects / THREAD_COUNT;
        int objectsForLastThread = objectsPerThread + totalObjects % THREAD_COUNT;
        for (int i = 0; i < THREAD_COUNT - 1; ++i) {
            threads[i] = new Thread(new AllocationThread(countDownLatch, objectsPerThread, alocatedObjectsCount));
        }
        threads[THREAD_COUNT - 1] = new Thread(new AllocationThread(countDownLatch, objectsForLastThread, alocatedObjectsCount));
        for (int i = 0; i < THREAD_COUNT; ++i) {
            threads[i].start();
        }
    }

    /**
     * Creates a humongous object of the predefined size.
     */
    private void createObject() {
        GARBAGE.add(new byte[HUMONGOUS_SIZE]);
    }

    /**
     * Tries to create the requested amount of humongous objects.
     * In case of OOME, stops creating and cleans the created garbage.
     * @param expectedObjects amount of objects based on heap size
     * @return amount of created objects
     */
    private int checkHeapCapacity(int expectedObjects) {
        int allocated = 0;
        try {
            while (isRunning() && allocated < expectedObjects) {
                createObject();
                ++allocated;
            }
        } catch (OutOfMemoryError oome) {
            GARBAGE.clear();
        }
        return allocated;
    }

    private void setDone() {
        isRunning = false;
    }

    private boolean isRunning() {
        return isRunning;
    }

    /**
     * Thread which allocates requested amount of humongous objects.
     */
    private class AllocationThread implements Runnable {

        private final int totalObjects;
        private final CountDownLatch cdl;
        private final AtomicInteger allocationCounter;

        /**
         * Creates allocation thread
         * @param cdl CountDownLatch
         * @param objects amount of objects to allocate
         * @param counter
         */
        public AllocationThread(CountDownLatch cdl, int objects, AtomicInteger counter) {
            totalObjects = objects;
            this.cdl = cdl;
            allocationCounter = counter;
        }

        @Override
        public void run() {
            int allocatedObjects = 0;
            try {
                while (isRunning && allocatedObjects < totalObjects) {
                    createObject();
                    allocatedObjects++;
                    allocationCounter.incrementAndGet();
                }

            } catch (OutOfMemoryError oome) {
                GARBAGE.clear();
                System.out.print("OOME was caught.");
                System.out.println(" Allocated in thread: " + allocatedObjects + " . Totally allocated: " + allocationCounter.get() + ".");
            } finally {
                cdl.countDown();
            }
        }
    }

    /**
     * Simple Runnable which waits TIMEOUT and sets isRunning to false.
     */
    class Timer implements Runnable {

        @Override
        public void run() {
            try {
                Thread.sleep(TIMEOUT * 1000);
            } catch (InterruptedException ex) {
            }
            setDone();
        }
    }
}
