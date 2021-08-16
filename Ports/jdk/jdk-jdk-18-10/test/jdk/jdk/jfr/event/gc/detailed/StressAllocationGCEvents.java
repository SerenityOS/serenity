/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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
package jdk.jfr.event.gc.detailed;

import static jdk.test.lib.Asserts.assertEquals;
import static jdk.test.lib.Asserts.assertNotEquals;
import static jdk.test.lib.Asserts.assertTrue;
import jdk.test.lib.Utils;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedFrame;
import jdk.jfr.consumer.RecordedStackTrace;
import jdk.jfr.consumer.RecordedThread;
import jdk.test.lib.jfr.EventNames;
import jdk.test.lib.jfr.Events;

/**
 * Starts several threads which allocate a lot of objects that remain in young
 * and old generations with defined ratio. Expects event
 * vm/gc/detailed/allocation_requiring_gc recorded.
 */
public class StressAllocationGCEvents {

    private Semaphore threadsCompleted;

    public void run(String[] args) throws Exception {
        threadsCompleted = new Semaphore(0);
        System.out.println("Total memory= " + Runtime.getRuntime().maxMemory() + " bytes");

        int obj_size = DEFAULT_OBJ_SIZE;
        if (args.length > 0) {
            obj_size = Integer.parseInt(args[0]);
        }

        System.out.println("Objects size= " + obj_size + " bytes");
        ExecutorService executor
                = Executors.newFixedThreadPool(THREAD_COUNT, new NamedThreadFactory());
        Recording r = new Recording();
        r.enable(EVENT_NAME_ALLOCATION_REQUIRING_GC);
        r.start();

        System.out.println("Starting " + THREAD_COUNT + " threads");

        for (int i = 0; i < THREAD_COUNT; i++) {
            executor.execute(new Runner(obj_size));
        }

        // Wait for all threads to complete
        threadsCompleted.acquire(THREAD_COUNT);
        executor.shutdownNow();

        if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
            System.err.println("Thread pool did not terminate after 10 seconds after shutdown");
        }

        r.stop();

        List<RecordedEvent> allocationEvents
                = Events.fromRecording(r);

        System.out.println(EVENT_NAME_ALLOCATION_REQUIRING_GC + " " + allocationEvents.size());

        Events.hasEvents(allocationEvents);

        // check stacktrace depth
        for (RecordedEvent event : allocationEvents) {
            checkEvent(event);
        }
    }

    class Runner extends Thread {

        public Runner(int obj_size) {
            this.startTime = System.currentTimeMillis();
            this.OBJ_SIZE = obj_size;
            this.OLD_OBJ_COUNT = Math.max(1, (int) ((float) Runtime.getRuntime().maxMemory() / 2 / THREAD_COUNT / OBJ_SIZE));
            this.old_garbage = new Object[OLD_OBJ_COUNT];
            this.r = new Random(Utils.getRandomInstance().nextLong());

            System.out.println(String.format("In \"%s\" old objects count  = %d, recursion depth = %d",
                    this.getName(), OLD_OBJ_COUNT, RECURSION_DEPTH));
        }

        @Override
        public void run() {
            diver(RECURSION_DEPTH);
            threadsCompleted.release();
            System.out.println("Completed after " + (System.currentTimeMillis() - startTime) + " ms");
        }

        private void diver(int stack) {
            if (stack > 1) {
                diver(stack - 1);
            } else {
                long endTime = startTime + (SECONDS_TO_RUN * 1000);
                while (endTime > System.currentTimeMillis()) {
                    byte[] garbage = new byte[OBJ_SIZE];
                    if (r.nextInt(100) > OLD_GEN_RATE) {
                        old_garbage[r.nextInt(OLD_OBJ_COUNT)] = garbage;
                    }
                }
            }
        }

        private final long startTime;
        private final Object[] old_garbage;
        private final int OBJ_SIZE;
        private final int OLD_OBJ_COUNT;
        private final Random r;
    }

    ///< check stacktrace depth
    private void checkEvent(RecordedEvent event) throws Exception {
        // skip check if allocation failure comes not from diver

        RecordedThread thread = event.getThread();
        String threadName = thread.getJavaName();

        if (!threadName.contains(THREAD_NAME)) {
            System.out.println("Skip event not from pool (from internals)");
            System.out.println(" Thread Id: " + thread.getJavaThreadId()
                    + " Thread name: " + threadName);
            return;
        }

        RecordedStackTrace stackTrace = event.getStackTrace();

        List<RecordedFrame> frames = stackTrace.getFrames();
        //String[] stacktrace = StackTraceHelper.buildStackTraceFromFrames(frames);

        if (!(frames.get(0).getMethod().getName().equals(DIVER_FRAME_NAME))) {
            System.out.println("Skip stacktrace check for: \n"
                    + String.join("\n", threadName));
            return;
        }

        assertTrue(frames.size() > RECURSION_DEPTH,
                "Stack trace should contain at least one more entry than the ones generated by the test recursion");
        for (int i = 0; i < RECURSION_DEPTH; i++) {
            assertEquals(frames.get(i).getMethod().getName(), DIVER_FRAME_NAME,
                    "Frame " + i + " is wrong: \n"
                    + String.join("\n", threadName));
        }
        assertNotEquals(frames.get(RECURSION_DEPTH).getMethod().getName(), DIVER_FRAME_NAME,
                "Too many diver frames: \n"
                + String.join("\n", threadName));
    }

    class NamedThreadFactory implements ThreadFactory {

        private int threadNum = 0;

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, THREAD_NAME + (threadNum++));
        }
    }

    // Because each thread will keep some number of objects live we need to limit
    // the number of threads to ensure we don't run out of heap space. The big
    // allocation tests uses 256m heap and 1m allocations, so a 64 thread limit
    // should be fine.
    private final static int THREAD_COUNT_LIMIT = 64;
    private final static int THREAD_COUNT = Math.min(1 + (int) (Runtime.getRuntime().availableProcessors() * 2), THREAD_COUNT_LIMIT);
    private final static int SECONDS_TO_RUN = 60;
    private final static int DEFAULT_OBJ_SIZE = 1024;
    private final static int OLD_GEN_RATE = 60; // from 0 to 100
    private final static int RECURSION_DEPTH = 5;
    private final static String EVENT_NAME_ALLOCATION_REQUIRING_GC = EventNames.AllocationRequiringGC;
    private static final String THREAD_NAME = "JFRTest-";
    private static final String DIVER_FRAME_NAME = "StressAllocationGCEvents$Runner.diver";
}
