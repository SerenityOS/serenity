/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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
package jdk.jfr.event.oldobject;

import java.io.IOException;
import java.util.List;
import java.util.function.Predicate;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedClass;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedFrame;
import jdk.jfr.consumer.RecordedMethod;
import jdk.jfr.consumer.RecordedObject;
import jdk.jfr.consumer.RecordedStackTrace;
import jdk.test.lib.jfr.Events;

/**
 * Utility class to perform Old Object provocation/detection and
 * stack trace/object verification for the Old Object Sample JFR event
 */
final public class OldObjects {

    public static final int MIN_SIZE = 99901; // prime number
    public final static int LEAK_CONTEXT = 100; // length of chain assiociated with the object sample
    public final static int ROOT_CONTEXT = 100; // length of chain assoicated with the root
    public final static int MAX_CHAIN_LENGTH = LEAK_CONTEXT + ROOT_CONTEXT; // the VM should not construct chains longer than this

    private static String[] getFrames(String expectedFrame) {
        if (expectedFrame != null) {
            return new String[] { expectedFrame };
        } else {
            return null;
        }
    }

    /**
    *
    * @param r
    *            A recording
    * @param expectedFrame
    *            A frame that must be found on the stack. Null if no check is required.
    * @param fieldType
    *            The object type (of the field). Null if no check is required.
    * @param fieldName
    *            The field name. Null if no check is required.
    * @param referrerType
    *            The class name. Null if no check is required.
    * @param minDuration
    *            The minimum duration of the event, -1 if not applicable.
    * @return The count of matching events
    * @throws IOException
    */
   public static long countMatchingEvents(Recording r, String expectedFrame, Class<?> fieldType, String fieldName, Class<?> referrerType, long minDuration) throws IOException {
       return countMatchingEvents(r, getFrames(expectedFrame), fieldType, fieldName, referrerType, minDuration);
   }

    /**
     * Gets the OldObjectSample events from the provided recording through a dump
     * and counts how many events matches the provided parameters.
     *
     * @param r
     *            A recording
     * @param expectedStack
     *            Some frames that must be found on the stack. Null if no check is required.
     * @param fieldType
     *            The object type (of the field). Null if no check is required.
     * @param fieldName
     *            The field name. Null if no check is required.
     * @param referrerType
     *            The class name. Null if no check is required.
     * @param minDuration
     *            The minimum duration of the event, -1 if not applicable.
     * @return The count of matching events
     * @throws IOException
     */
    public static long countMatchingEvents(Recording r, String[] expectedStack, Class<?> fieldType, String fieldName, Class<?> referrerType, long minDuration) throws IOException {
        return countMatchingEvents(Events.fromRecording(r), fieldType, fieldName, referrerType, minDuration, expectedStack);
    }

    /**
    *
    * @param events
    *            A list of RecordedEvent.
    * @param expectedFrame
    *             A frame that must be found on the stack. Null if no check is required.
    * @param fieldType
    *            The object type (of the field). Null if no check is required.
    * @param fieldName
    *            The field name. Null if no check is required.
    * @param referrerType
    *            The class name. Null if no check is required.
    * @param minDuration
    *            The minimum duration of the event, -1 if not applicable.
    * @return The count of matching events
    * @throws IOException
    */
   public static long countMatchingEvents(List<RecordedEvent> events, String expectedFrame, Class<?> fieldType, String fieldName, Class<?> referrerType, long minDuration) throws IOException {
       return countMatchingEvents(events, fieldType, fieldName, referrerType, minDuration, getFrames(expectedFrame));
   }

    /**
     *
     * @param events
     *            The list of events to find matching events in
     * @param expectedStack
     *            Some frames that must be found on the stack. Null if no check is required.
     * @param fieldType
     *            The object type (of the field). Null if no check is required.
     * @param fieldName
     *            The field name. Null if no check is required.
     * @param referrerType
     *            The class name. Null if no check is required.
     * @param minDuration
     *            The minimum duration of the event, -1 if not applicable.
     * @return The count of matching events
     * @throws IOException
     */
    public static long countMatchingEvents(List<RecordedEvent> events, Class<?> fieldType, String fieldName, Class<?> referrerType, long minDuration, String... expectedStack) throws IOException {
        String currentThread = Thread.currentThread().getName();
        return events.stream()
                .filter(hasJavaThread(currentThread))
                .filter(fieldIsType(fieldType))
                .filter(hasFieldName(fieldName))
                .filter(isReferrerType(referrerType))
                .filter(durationAtLeast(minDuration))
                .filter(hasStackTrace(expectedStack))
                .count();
    }

    private static Predicate<RecordedEvent> hasJavaThread(String expectedThread) {
        if (expectedThread != null) {
            return e -> e.getThread() != null && expectedThread.equals(e.getThread().getJavaName());
        } else {
            return e -> true;
        }
    }

    private static Predicate<RecordedEvent> hasStackTrace(String[] expectedStack) {
        if (expectedStack != null) {
            return e -> matchingStackTrace(e.getStackTrace(), expectedStack);
        } else {
            return e -> true;
        }
    }

    private static Predicate<RecordedEvent> fieldIsType(Class<?> fieldType) {
        if (fieldType != null) {
            return e -> e.hasField("object.type") && ((RecordedClass) e.getValue("object.type")).getName().equals(fieldType.getName());
        } else {
            return e -> true;
        }
    }

    private static Predicate<RecordedEvent> hasFieldName(String fieldName) {
        if (fieldName != null) {
            return e -> {
                RecordedObject referrer = e.getValue("object.referrer");
                return referrer != null ? referrer.hasField("field.name") && referrer.getValue("field.name").equals(fieldName) : false;
            };
        } else {
            return e -> true;
        }
    }

    private static Predicate<RecordedEvent> isReferrerType(Class<?> referrerType) {
        if (referrerType != null) {
            return e -> {
                RecordedObject referrer = e.getValue("object.referrer");
                return referrer != null ? referrer.hasField("object.type") &&
                                            ((RecordedClass) referrer.getValue("object.type")).getName().equals(referrerType.getName()) : false;
            };
        } else {
            return e -> true;
        }
    }

    private static Predicate<RecordedEvent> durationAtLeast(long minDurationMs) {
        if (minDurationMs > 0) {
            return e -> e.getDuration().toMillis() >= minDurationMs;
        } else {
            return e -> true;
        }
    }

    public static boolean matchingReferrerClass(RecordedEvent event, String className) {
        RecordedObject referrer = event.getValue("object.referrer");
        if (referrer != null) {
            if (!referrer.hasField("object.type")) {
                return false;
            }

            String reportedClass = ((RecordedClass) referrer.getValue("object.type")).getName();
            if (reportedClass.equals(className)) {
                return true;
            }
        }
        return false;
    }

    public static String getReferrerFieldName(RecordedEvent event) {
        RecordedObject referrer = event.getValue("object.referrer");
        return referrer != null && referrer.hasField("field.name") ? referrer.getValue("field.name") : null;
    }

    public static boolean matchingStackTrace(RecordedStackTrace stack, String[] expectedStack) {
        if (stack == null) {
            return false;
        }

        List<RecordedFrame> frames = stack.getFrames();
        int pos = findFramePos(frames, expectedStack[0]);

        if (pos == -1) {
            return false;
        }

        for (String expectedFrame : expectedStack) {
            RecordedFrame f = frames.get(pos++);
            String frame = frameToString(f);

            if (!frame.contains(expectedFrame)) {
                return false;
            }
        }
        return true;
    }

    private static int findFramePos(List<RecordedFrame> frames, String frame) {
        int pos = 0;
        for (RecordedFrame f : frames) {
            if (frameToString(f).contains(frame)) {
                return pos;
            }
            pos++;
        }
        return -1;
    }

    private static String frameToString(RecordedFrame f) {
        RecordedMethod m = f.getMethod();
        String methodName = m.getName();
        String className = m.getType().getName();
        return className + "." + methodName;
    }

    public static void validateReferenceChainLimit(RecordedEvent e, int maxLength) {
        int length = 0;
        RecordedObject object = e.getValue("object");
        while (object != null) {
            ++length;
            RecordedObject referrer = object.getValue("referrer");
            object = referrer != null ? referrer.getValue("object") : null;
        }
        if (length > maxLength) {
            throw new RuntimeException("Reference chain max length not respected. Found a chain of length " + length);
        }
    }
}
