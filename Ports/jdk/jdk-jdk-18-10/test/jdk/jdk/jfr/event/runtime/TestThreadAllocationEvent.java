/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.event.runtime;

import static jdk.test.lib.Asserts.assertGreaterThan;
import static jdk.test.lib.Asserts.assertTrue;

import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

import com.sun.management.ThreadMXBean;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedThread;
import jdk.test.lib.jfr.EventNames;
import jdk.test.lib.jfr.Events;

/**
 * @test
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @modules jdk.jfr
 *          jdk.management
 *
 * @run main/othervm -XX:-UseTLAB jdk.jfr.event.runtime.TestThreadAllocationEvent
 */

/**
 * The test will create a few threads that will allocate memory for a short time.
 * During this time a number of thread_allocation events will be generated.
 * The test will verify:
 * 1. That number of allocated bytes is not decreasing for a thread.
 *  - This assumption is only true when not using TLABs. For this reason the
 *    test is run with -XX:-UseTLAB. When using TLABs, the code calculating the
 *    allocated bytes is using the current TLAB to do as good of an approximation
 *    as possible, but this introduces a race which might double count the current
 *    TLAB when it is full and in the middle of being switched out.
 * 2. That sum of allocated bytes approximately matches value in ThreadMXBean.
 */
public class TestThreadAllocationEvent {
    private static final String EVENT_NAME = EventNames.ThreadAllocationStatistics;
    private static final String testThreadName = "testThread-";
    private static final long eventPeriodMillis = 50;

    // The value in both the JFR event and in the ThreadMXBean is documented as
    // an "approximation" of number of bytes allocated.
    // To not give any false errors, we allow an error margin of 5 mb.
    // The test will typically allocate over 600 mb, so 5 mb is an error of less than 1%.
    private static final long allowedTotalAllocatedDiff = 5000000;

    public static void main(String[] args) throws Throwable {
        Recording recording = new Recording();
        recording.enable(EVENT_NAME).withPeriod(Duration.ofMillis(eventPeriodMillis));
        recording.start();

        AllocatorThread[] threads = new AllocatorThread[4];
        CountDownLatch allocationsDoneLatch = new CountDownLatch(threads.length);
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new AllocatorThread(allocationsDoneLatch, 1000 * (i + 1));
            threads[i].setName(testThreadName + i);
            threads[i].setDaemon(true);
            threads[i].start();
        }

        // Take regular measurements while the threads are allocating memory.
        // Stop measurement when all threads are ready.
        try {
            allocationsDoneLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // Verify that number of allocated bytes is not decreasing.
        recording.stop();
        verifyAllocationsNotDecreasing(Events.fromRecording(recording), threads);

        // Now allocations are done and threads are waiting to die.
        // Make a new instant recording to get total number of allocated bytes.
        // The reason for this extra recording is to make sure we get a JFR event
        // after all allocations are done so we can compare the JFR value with
        // the value reported by ThreadMXBean.
        recording = new Recording();
        recording.enable(EVENT_NAME);
        recording.start();
        recording.stop();
        verifyTotalAllocated(Events.fromRecording(recording), threads);
    }

    /**
     * Verify that the allocated value never decreases.
     * We only compare our own allocator threads. The reason for that is that other threads
     * may start/stop at any time, and we do not know if other thread names are unique.
     */
     private static void verifyAllocationsNotDecreasing(List<RecordedEvent> events, AllocatorThread[] threads) {
         Collections.sort(events, (u,v) -> u.getEndTime().compareTo(v.getEndTime()));
         long[] prevAllocated = new long[threads.length];
         for (RecordedEvent event : events) {
             RecordedThread rt = Events.assertField(event, "thread").notNull().getValue(); // Check that we have a thread.
             String name = rt.getJavaName();
             for (int i = 0; i < threads.length; i++) {
                 if (name.equals(threads[i].getName())) {
                     long curr = Events.assertField(event, "allocated").atLeast(prevAllocated[i]).getValue();
                     prevAllocated[i] = curr;
                 }
             }
         }

         for (int i = 0; i < threads.length; i++) {
             assertGreaterThan(prevAllocated[i], 0L, "No allocations for thread " + threads[i].getName());
         }
     }

     /**
      * Verify that total allocated bytes in JFR event approximately matches the value in ThreadMXBean.
      */
    private static void verifyTotalAllocated(List<RecordedEvent> events, AllocatorThread[] threads) {
        boolean[] isEventFound = new boolean[threads.length];
        for (RecordedEvent event : events) {
            RecordedThread rt = Events.assertField(event, "thread").notNull().getValue();
            String name = rt.getJavaName();
            for (int i = 0; i < threads.length; ++i) {
                if (name.equals(threads[i].getName())) {
                    System.out.println("Event:" + event);
                    long maxAllowed = threads[i].totalAllocated + allowedTotalAllocatedDiff;
                    long minAllowed = Math.max(0, threads[i].totalAllocated - allowedTotalAllocatedDiff);
                    Events.assertField(event, "allocated").atLeast(minAllowed).atMost(maxAllowed);
                    isEventFound[i] = true;
                }
            }
        }
        for (int i = 0; i < threads.length; ++i) {
            assertTrue(isEventFound[i], "No event for thread id " + i);
        }
    }

    /**
     * Thread that does a number of allocations and records total number of
     * bytes allocated as reported by ThreadMXBean.
     */
    public static class AllocatorThread extends Thread {
        private volatile long totalAllocated = -1;
        private final int averageAllocationSize;
        public byte[] buffer;
        private final CountDownLatch allocationsDoneLatch;

        public AllocatorThread(CountDownLatch allocationsDoneLatch, int averageAllocationSize) {
            this.allocationsDoneLatch = allocationsDoneLatch;
            this.averageAllocationSize = averageAllocationSize;
        }

        @Override
        public void run() {
            Random rand = new Random();
            int allocationSizeBase = averageAllocationSize / 2;
            int allocationSizeRandom = averageAllocationSize;
            for (int batches=0; batches<100; batches++) {
                for (int i=0; i<1500; i++) {
                    buffer = new byte[rand.nextInt(allocationSizeRandom) + allocationSizeBase];
                }
                try {
                    // No need to allocate too much data between JFR events, so do a short sleep.
                    Thread.sleep(eventPeriodMillis / 5);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            totalAllocated = getThreadAllocatedBytes();
            allocationsDoneLatch.countDown();

            // Need to keep thread alive so we can get the final JFR event.
            // This is a daemon thread, so it will finish when the main thread finishes.
            while (true) {
                Thread.yield();
            }
        }

        private long getThreadAllocatedBytes() {
            ThreadMXBean bean = (ThreadMXBean) ManagementFactory.getThreadMXBean();
            return bean.getThreadAllocatedBytes(Thread.currentThread().getId());
        }
    }

}
