/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.jvm;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.internal.JVM;
import jdk.jfr.Recording;

import jdk.test.lib.jfr.EventNames;
import jdk.test.lib.jfr.Events;

import static jdk.test.lib.Asserts.assertTrue;

/**
 * @test
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @modules jdk.jfr/jdk.jfr.internal
 * @run main/othervm jdk.jfr.jvm.TestThreadExclusion
 */

/**
 * Starts and stops a number of threads in order.
 * Verifies that events are in the same order.
 */
public class TestThreadExclusion {
    private final static String EVENT_NAME_THREAD_START = EventNames.ThreadStart;
    private final static String EVENT_NAME_THREAD_END = EventNames.ThreadEnd;
    private static final String THREAD_NAME_PREFIX = "TestThread-";
    private static JVM jvm;

    public static void main(String[] args) throws Throwable {
        // Test Java Thread Start event
        Recording recording = new Recording();
        recording.enable(EVENT_NAME_THREAD_START).withThreshold(Duration.ofMillis(0));
        recording.enable(EVENT_NAME_THREAD_END).withThreshold(Duration.ofMillis(0));
        recording.start();
        LatchedThread[] threads = startThreads();
        long[] javaThreadIds = getJavaThreadIds(threads);
        stopThreads(threads);
        recording.stop();
        List<RecordedEvent> events = Events.fromRecording(recording);
        verifyThreadExclusion(events, javaThreadIds);
    }

    private static void verifyThreadExclusion(List<RecordedEvent> events, long[] javaThreadIds) throws Exception {
        for (RecordedEvent event : events) {
            System.out.println("Event:" + event);
            final long eventJavaThreadId = event.getThread().getJavaThreadId();
            for (int i = 0; i < javaThreadIds.length; ++i) {
                if (eventJavaThreadId == javaThreadIds[i]) {
                    throw new Exception("Event " + event.getEventType().getName() + " has a thread id " + eventJavaThreadId + " that should have been excluded");
                }
            }
        }
    }

    private static LatchedThread[] startThreads() {
        LatchedThread threads[] = new LatchedThread[10];
        ThreadGroup threadGroup = new ThreadGroup("TestThreadGroup");
        jvm = JVM.getJVM();
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new LatchedThread(threadGroup, THREAD_NAME_PREFIX + i);
            jvm.exclude(threads[i]);
            threads[i].startThread();
            System.out.println("Started thread id=" + threads[i].getId());
        }
        return threads;
    }

    private static long[] getJavaThreadIds(LatchedThread[] threads) {
        long[] javaThreadIds = new long[threads.length];
        for (int i = 0; i < threads.length; ++i) {
            javaThreadIds[i] = threads[i].getId();
        }
        return javaThreadIds;
    }

    private static void stopThreads(LatchedThread[] threads) {
        for (LatchedThread thread : threads) {
            assertTrue(jvm.isExcluded(thread), "Thread " + thread + "should be excluded");
            thread.stopThread();
            while (thread.isAlive()) {
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static class LatchedThread extends Thread {
        private final CountDownLatch start = new CountDownLatch(1);
        private final CountDownLatch stop = new CountDownLatch(1);

        public LatchedThread(ThreadGroup threadGroup, String name) {
            super(threadGroup, name);
        }

        public void run() {
            start.countDown();
            try {
                stop.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        public void startThread() {
            this.start();
            try {
                start.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        public void stopThread() {
            stop.countDown();
        }
    }
}
