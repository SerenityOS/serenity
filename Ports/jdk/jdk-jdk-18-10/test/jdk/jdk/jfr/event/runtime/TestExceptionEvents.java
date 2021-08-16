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

import java.time.Duration;
import java.util.List;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedClass;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedFrame;
import jdk.jfr.consumer.RecordedStackTrace;
import jdk.jfr.consumer.RecordedThread;
import jdk.test.lib.Asserts;
import jdk.test.lib.jfr.EventNames;
import jdk.test.lib.jfr.Events;

/**
 * @test
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.event.runtime.TestExceptionEvents
 */
public class TestExceptionEvents {

    private static final String EXCEPTION_EVENT_PATH = EventNames.JavaExceptionThrow;
    private static final String ERROR_EVENT_PATH  = EventNames.JavaErrorThrow;
    private static final String EXCEPTION_STATISTICS_PATH = EventNames.ExceptionStatistics;

    private static final String EXCEPTION_MESSAGE = "exceptiontest";
    private static final String ERROR_MESSAGE = "errortest";
    private static final String THROWABLE_MESSAGE = "throwabletest";

    private static final int ITERATIONS = 10;

    private static int exceptionCount = 0;

    public static void main(String[] args) throws Throwable {
        Recording recording = createRecording();

        List<RecordedEvent> events = Events.fromRecording(recording);
        checkStatisticsEvent(events, exceptionCount);
        checkThrowableEvents(events, EXCEPTION_EVENT_PATH, ITERATIONS, MyException.class, EXCEPTION_MESSAGE);
        checkThrowableEvents(events, ERROR_EVENT_PATH, ITERATIONS, MyError.class, ERROR_MESSAGE);
        checkThrowableEvents(events, EXCEPTION_EVENT_PATH, ITERATIONS, MyThrowable.class, THROWABLE_MESSAGE);
        checkExceptionStackTrace();
    }

    private static void checkExceptionStackTrace() throws Exception {
        @SuppressWarnings("serial")
        class TestError extends Error {
        }
        @SuppressWarnings("serial")
        class TestException extends Exception {
        }

        try (Recording r = new Recording()) {
            r.enable(EventNames.JavaErrorThrow).withStackTrace();
            r.enable(EventNames.JavaExceptionThrow).withStackTrace();
            r.start();
            try {
                throw new TestError();
            } catch (Error e) {
                System.out.println(e.getClass() + " thrown!");
            }
            try {
                throw new TestException();
            } catch (Exception e) {
                System.out.println(e.getClass() + " thrown!");
            }
            try {
                throw new Exception();
            } catch (Exception e) {
                System.out.println(e.getClass() + " thrown!");
            }
            r.stop();
            List<RecordedEvent> events = Events.fromRecording(r);
            Events.hasEvents(events);
            for (RecordedEvent e : events) {
                RecordedStackTrace rs = e.getStackTrace();
                RecordedClass rc = e.getValue("thrownClass");
                List<RecordedFrame> frames = rs.getFrames();
                RecordedFrame topFrame = frames.get(0);
                System.out.println(rc.getName() + " Top frame: " + topFrame.getMethod().getName());
                if (!topFrame.getMethod().getName().equals("<init>")) {
                    throw new Exception("Expected name of top frame to be <init>");
                }
            }
        }
    }

    private static Recording createRecording() throws Exception {
        Recording recording = new Recording();
        recording.enable(EXCEPTION_STATISTICS_PATH);
        recording.enable(EXCEPTION_EVENT_PATH).withThreshold(Duration.ofMillis(0));
        recording.enable(ERROR_EVENT_PATH).withThreshold(Duration.ofMillis(0));
        recording.start();

        for (int i = 0; i < ITERATIONS; i++) {
            try {
                throw new MyException(EXCEPTION_MESSAGE);
            } catch (MyException e) {
                exceptionCount++;
            }
            try {
                throw new MyError(ERROR_MESSAGE);
            } catch (MyError e) {
                exceptionCount++;
            }
            try {
                throw new MyThrowable(THROWABLE_MESSAGE);
            } catch (MyThrowable t) {
                exceptionCount++;
            }
        }
        recording.stop();
        return recording;
    }


    private static void checkStatisticsEvent(List<RecordedEvent> events, long minCount) throws Exception {
        // Events are not guaranteed to be in chronological order, take highest value.
        long count = -1;
        for(RecordedEvent event : events) {
            if (Events.isEventType(event, EXCEPTION_STATISTICS_PATH)) {
                System.out.println("Event: " + event);
                count = Math.max(count, Events.assertField(event, "throwables").getValue());
                System.out.println("count=" +  count);
            }
        }
        Asserts.assertTrue(count != -1, "No events of type " + EXCEPTION_STATISTICS_PATH);
        Asserts.assertGreaterThanOrEqual(count, minCount, "Too few exception count in statistics event");
    }

    private static void checkThrowableEvents(List<RecordedEvent> events, String eventName,
        int excpectedEvents, Class<?> expectedClass, String expectedMessage) throws Exception {
        int count = 0;
        for(RecordedEvent event : events) {
            if (Events.isEventType(event, eventName)) {
                String message = Events.assertField(event, "message").getValue();
                if (expectedMessage.equals(message)) {
                    RecordedThread t = event.getThread();
                    String threadName = t.getJavaName();
                    if (threadName != null && threadName.equals(Thread.currentThread().getName())) {
                        RecordedClass jc = event.getValue("thrownClass");
                        if (jc.getName().equals(expectedClass.getName())) {
                            count++;
                        }
                    }
                }
            }
        }
        Asserts.assertEquals(count, excpectedEvents, "Wrong event count for type " + eventName);
    }

    private static class MyException extends Exception {
        private static final long serialVersionUID = -2614309279743448910L;
        public MyException(String msg) {
            super(msg);
        }
    }

    private static class MyError extends Error {
        private static final long serialVersionUID = -8519872786387358196L;
        public MyError(String msg) {
            super(msg);
        }
    }

    private static class MyThrowable extends Throwable {
        private static final long serialVersionUID = -7929442863511070361L;
        public MyThrowable(String msg) {
            super(msg);
        }
    }
}
