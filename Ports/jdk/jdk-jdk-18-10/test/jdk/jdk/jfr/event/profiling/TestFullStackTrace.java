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

package jdk.jfr.event.profiling;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedFrame;
import jdk.jfr.consumer.RecordedStackTrace;
import jdk.test.lib.Asserts;
import jdk.test.lib.jfr.EventNames;
import jdk.test.lib.jfr.Events;
import jdk.test.lib.jfr.RecurseThread;

/**
 * @test
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.event.profiling.TestFullStackTrace
 */
public class TestFullStackTrace {
    private final static String EVENT_NAME = EventNames.ExecutionSample;
    private final static int MAX_DEPTH = 64; // currently hardcoded in jvm

    public static void main(String[] args) throws Throwable {
        RecurseThread[] threads = new RecurseThread[3];
        for (int i = 0; i < threads.length; ++i) {
            int depth = MAX_DEPTH - 1 + i;
            threads[i] = new RecurseThread(depth);
            threads[i].setName("recursethread-" + depth);
            threads[i].start();
        }

        for (RecurseThread thread : threads) {
            while (!thread.isInRunLoop()) {
                Thread.sleep(20);
            }
        }

        assertStackTraces(threads);

        for (RecurseThread thread : threads) {
            thread.quit();
            thread.join();
        }
    }

    private static void assertStackTraces( RecurseThread[] threads) throws Throwable {
        Recording recording= null;
        do {
            recording = new Recording();
            recording.enable(EVENT_NAME).withPeriod(Duration.ofMillis(50));
            recording.start();
            Thread.sleep(500);
            recording.stop();
        } while (!hasValidStackTraces(recording, threads));
    }

    private static boolean hasValidStackTraces(Recording recording, RecurseThread[] threads) throws Throwable {
        boolean[] isEventFound = new boolean[threads.length];

        for (RecordedEvent event : Events.fromRecording(recording)) {
            //System.out.println("Event: " + event);
            String threadName = Events.assertField(event, "sampledThread.javaName").getValue();
            long threadId = Events.assertField(event, "sampledThread.javaThreadId").getValue();

            for (int threadIndex = 0; threadIndex < threads.length; ++threadIndex) {
                RecurseThread currThread = threads[threadIndex];
                if (threadId == currThread.getId()) {
                    System.out.println("ThreadName=" + currThread.getName() + ", depth=" + currThread.totalDepth);
                    Asserts.assertEquals(threadName, currThread.getName(), "Wrong thread name");
                    if ("recurseEnd".equals(getTopMethodName(event))) {
                        isEventFound[threadIndex] = true;
                        checkEvent(event, currThread.totalDepth);
                        break;
                    }
                }
            }
        }

        for (int i = 0; i < threads.length; ++i) {
            String msg = "threadIndex=%d, recurseDepth=%d, isEventFound=%b%n";
            System.out.printf(msg, i, threads[i].totalDepth, isEventFound[i]);
        }
        for (int i = 0; i < threads.length; ++i) {
            if(!isEventFound[i]) {
               // no assertion, let's retry.
               // Could be race condition, i.e safe point during Thread.sleep
               System.out.println("Falied to validate all threads, will retry.");
               return false;
            }
        }
        return true;
    }

    public static String getTopMethodName(RecordedEvent event) {
        List<RecordedFrame> frames = event.getStackTrace().getFrames();
        Asserts.assertFalse(frames.isEmpty(), "JavaFrames was empty");
        return frames.get(0).getMethod().getName();
    }

    private static void checkEvent(RecordedEvent event, int expectedDepth) throws Throwable {
        RecordedStackTrace stacktrace = null;
        try {
            stacktrace = event.getStackTrace();
            List<RecordedFrame> frames = stacktrace.getFrames();
            Asserts.assertEquals(Math.min(MAX_DEPTH, expectedDepth), frames.size(), "Wrong stacktrace depth. Expected:" + expectedDepth);
            List<String> expectedMethods = getExpectedMethods(expectedDepth);
            Asserts.assertEquals(expectedMethods.size(), frames.size(), "Wrong expectedMethods depth. Test error.");

            for (int i = 0; i < frames.size(); ++i) {
                String name = frames.get(i).getMethod().getName();
                String expectedName = expectedMethods.get(i);
                System.out.printf("method[%d]=%s, expected=%s%n", i, name, expectedName);
                Asserts.assertEquals(name, expectedName, "Wrong method name");
            }

            boolean isTruncated = stacktrace.isTruncated();
            boolean isTruncateExpected = expectedDepth > MAX_DEPTH;
            Asserts.assertEquals(isTruncated, isTruncateExpected, "Wrong value for isTruncated. Expected:" + isTruncateExpected);

            String firstMethod = frames.get(frames.size() - 1).getMethod().getName();
            boolean isFullTrace = "run".equals(firstMethod);
            String msg = String.format("Wrong values for isTruncated=%b, isFullTrace=%b", isTruncated, isFullTrace);
            Asserts.assertTrue(isTruncated != isFullTrace, msg);
        } catch (Throwable t) {
            System.out.println(String.format("stacktrace:%n%s", stacktrace));
            throw t;
        }
    }

    private static List<String> getExpectedMethods(int depth) {
        List<String> methods = new ArrayList<>();
        methods.add("recurseEnd");
        for (int i = 0; i < depth - 2; ++i) {
            methods.add((i % 2) == 0 ? "recurseA" : "recurseB");
        }
        methods.add("run");
        if (depth > MAX_DEPTH) {
            methods = methods.subList(0, MAX_DEPTH);
        }
        return methods;
    }
}
