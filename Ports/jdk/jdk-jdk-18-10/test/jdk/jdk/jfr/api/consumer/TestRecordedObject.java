/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.api.consumer;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import jdk.jfr.Event;
import jdk.jfr.Recording;
import jdk.jfr.StackTrace;
import jdk.jfr.Timespan;
import jdk.jfr.Timestamp;
import jdk.jfr.Unsigned;
import jdk.jfr.consumer.RecordedClass;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedObject;
import jdk.jfr.consumer.RecordedThread;
import jdk.test.lib.Asserts;
import jdk.test.lib.jfr.Events;

/**
 * @test
 * @summary Verifies the methods of the RecordedObject
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.api.consumer.TestRecordedObject
 */
public class TestRecordedObject {

    private final static boolean BOOLEAN_VALUE = true;
    private final static byte VALUE = 47;
    private final static String STRING_VALUE = "47";
    private final static Class<?> CLASS_VALUE = String.class;
    private final static Thread THREAD_VALUE = Thread.currentThread();
    private final static Instant INSTANT_VALUE = Instant.now();
    private final static Duration DURATION_VALUE = Duration.ofSeconds(47);

    @StackTrace(false)
    static final class EventWithValues extends Event {
        boolean booleanField = BOOLEAN_VALUE;
        byte byteField = VALUE;
        char charField = VALUE;
        short shortField = VALUE;
        int intField = VALUE;
        long longField = VALUE;
        float floatField = VALUE;
        double doubleField = VALUE;
        String stringField = STRING_VALUE;
        Class<?> classField = CLASS_VALUE;
        Thread threadField = THREAD_VALUE;
        @Timespan(Timespan.NANOSECONDS)
        long durationField = DURATION_VALUE.toNanos();
        @Timestamp(Timestamp.MILLISECONDS_SINCE_EPOCH)
        long instantField = INSTANT_VALUE.toEpochMilli();
        Thread nullField = null;
        Class<?> nullField2 = null;

        @Timespan(Timespan.MICROSECONDS)
        long durationMicros = DURATION_VALUE.toNanos() / 1000;

        @Timespan(Timespan.MILLISECONDS)
        long durationMillis = DURATION_VALUE.toMillis();

        @Timespan(Timespan.SECONDS)
        long durationSeconds = DURATION_VALUE.toSeconds();

        @Timestamp(Timestamp.MILLISECONDS_SINCE_EPOCH)
        long instantMillis = 1000;

        @Timestamp(Timespan.TICKS)
        long instantTicks = 0;

        @Unsigned
        byte unsignedByte = Byte.MIN_VALUE;
        @Unsigned
        char unsignedChar = 'q';
        @Unsigned
        short unsignedShort = Short.MIN_VALUE;
        @Unsigned
        int unsignedInt = Integer.MIN_VALUE;
        @Unsigned
        long unsignedLong = Long.MIN_VALUE; // unsigned should be ignored
        @Unsigned
        float unsignedFloat = Float.MIN_VALUE; // unsigned should be ignored
        @Unsigned
        double unsignedDouble = Double.MIN_VALUE; // unsigned should be ignored

    }

    private final static Set<String> ALL = createAll();

    public static void main(String[] args) throws Throwable {

        RecordedObject event = makeRecordedObject();

        // Primitives
        testGetBoolean(event);
        testGetByte(event);
        testGetChar(event);
        testGetShort(event);
        testGetInt(event);
        testGetLong(event);
        testGetDouble(event);
        testGetFloat(event);

        // // Complex types
        testGetString(event);
        testGetInstant(event);
        testGetDuration(event);
        testGetThread(event);
        testGetClass(event);

        // Misc.
        testNestedNames(event);
        testTimeUnits(event);
        testUnsigned(event);
    }

    private static void testUnsigned(RecordedObject event) {
        // Unsigned byte value
        Asserts.assertEquals(event.getByte("unsignedByte"), Byte.MIN_VALUE);
        Asserts.assertEquals(event.getInt("unsignedByte"), Byte.toUnsignedInt(Byte.MIN_VALUE));
        Asserts.assertEquals(event.getLong("unsignedByte"), Byte.toUnsignedLong(Byte.MIN_VALUE));
        Asserts.assertEquals(event.getShort("unsignedByte"), (short)Byte.toUnsignedInt(Byte.MIN_VALUE));

        // Unsigned char, nothing should happen, it is unsigned
        Asserts.assertEquals(event.getChar("unsignedChar"), 'q');
        Asserts.assertEquals(event.getInt("unsignedChar"), (int)'q');
        Asserts.assertEquals(event.getLong("unsignedChar"), (long)'q');

        // Unsigned short
        Asserts.assertEquals(event.getShort("unsignedShort"), Short.MIN_VALUE);
        Asserts.assertEquals(event.getInt("unsignedShort"), Short.toUnsignedInt(Short.MIN_VALUE));
        Asserts.assertEquals(event.getLong("unsignedShort"), Short.toUnsignedLong(Short.MIN_VALUE));

        // Unsigned int
        Asserts.assertEquals(event.getInt("unsignedInt"), Integer.MIN_VALUE);
        Asserts.assertEquals(event.getLong("unsignedInt"), Integer.toUnsignedLong(Integer.MIN_VALUE));

        // Unsigned long, nothing should happen
        Asserts.assertEquals(event.getLong("unsignedLong"), Long.MIN_VALUE);

        // Unsigned float, nothing should happen
        Asserts.assertEquals(event.getFloat("unsignedFloat"), Float.MIN_VALUE);

        // Unsigned double, nothing should happen
        Asserts.assertEquals(event.getDouble("unsignedDouble"), Double.MIN_VALUE);
    }

    private static void testTimeUnits(RecordedObject event) {
        Asserts.assertEquals(event.getDuration("durationMicros"), DURATION_VALUE);
        Asserts.assertEquals(event.getDuration("durationMillis"), DURATION_VALUE);
        Asserts.assertEquals(event.getDuration("durationSeconds"), DURATION_VALUE);
        Asserts.assertEquals(event.getInstant("instantMillis").toEpochMilli(), 1000L);
        if (!event.getInstant("instantTicks").isBefore(INSTANT_VALUE)) {
            throw new AssertionError("Expected start time of JVM to before call to Instant.now()");
        }
    }

    private static void testNestedNames(RecordedObject event) {
        RecordedThread t = event.getValue("threadField");

        // Nested with getValue
        try {
            event.getValue("nullField.javaName");
            throw new AssertionError("Expected NullPointerException");
        } catch (NullPointerException npe) {
            // OK, expected;
        }
        try {
            event.getValue("nullField.does.not.exist");
            throw new AssertionError("Expected IllegalArgumentException");
        } catch (IllegalArgumentException iae) {
            // OK, expected;
        }

        // Nested getLong
        try {
            event.getLong("nullField.javaName");
            throw new AssertionError("Expected NullPointerException");
        } catch (NullPointerException npe) {
            // OK, expected;
        }
        try {
            event.getLong("nullField.does.not.exist");
            throw new AssertionError("Expected IllegalArgumentException");
        } catch (IllegalArgumentException npe) {
            // OK, expected;
        }
        if (t.getOSThreadId() != event.getLong("threadField.osThreadId")) {
            throw new AssertionError("Incorrect result from nested long value");
        }

        // Nested getString
        try {
            event.getString("nullField.osThreadId");
            throw new AssertionError("Expected IllegalArgumentException");
        } catch (IllegalArgumentException npe) {
            // OK, expected;
        }
        try {
            event.getLong("nullField.does.not.exist");
            throw new AssertionError("Expected IllegalArgumentException");
        } catch (IllegalArgumentException npe) {
            // OK, expected;
        }
        if (!t.getJavaName().equals(event.getString("threadField.javaName"))) {
            throw new AssertionError("Incorrect result from nested long value");
        }

        // Nested getClass
        try {
            event.getClass("nullField.osThreadId");
            throw new AssertionError("Expected IllegalArgumentException");
        } catch (IllegalArgumentException npe) {
            // OK, expected;
        }
        try {
            event.getClass("nullField.does.not.exist");
            throw new AssertionError("Expected IllegalArgumentException");
        } catch (IllegalArgumentException npe) {
            // OK, expected;
        }

        // Nested getThread
        try {
            event.getThread("nullField2.name");
            throw new AssertionError("Expected IllegalArgumentException");
        } catch (IllegalArgumentException npe) {
            // OK, expected;
        }
        try {
            event.getThread("nullField2.does.not.exist");
            throw new AssertionError("Expected IllegalArgumentException");
        } catch (IllegalArgumentException npe) {
            // OK, expected;
        }
    }

    private static void testGetBoolean(RecordedObject e) {
        assertGetter(x -> e.getBoolean(x), BOOLEAN_VALUE, "boolean");
    }

    private static void testGetByte(RecordedObject e) {
        assertGetter(x -> e.getByte(x), (byte) VALUE, "byte");
    }

    private static void testGetChar(RecordedObject e) {
        assertGetter(x -> e.getChar(x), (char) VALUE, "char");
    }

    private static void testGetShort(RecordedObject e) {
        assertGetter(x -> e.getShort(x), (short) VALUE, "byte", "short");
    }

    private static void testGetInt(RecordedObject e) {
        assertGetter(x -> e.getInt(x), (int) VALUE, "byte", "char", "short", "int");
    }

    private static void testGetLong(RecordedObject e) {
        assertGetter(x -> e.getLong(x), (long) VALUE, "byte", "char", "short", "int", "long");
    }

    private static void testGetFloat(RecordedObject e) {
        assertGetter(x -> e.getFloat(x), (float) VALUE, "byte", "char", "short", "int", "long", "float");
    }

    private static void testGetDouble(RecordedObject e) {
        assertGetter(x -> e.getDouble(x), (double) VALUE, "byte", "char", "short", "int", "long", "float", "double");
    }

    private static void testGetString(RecordedObject e) {
        assertGetter(x -> e.getString(x), STRING_VALUE, "string");
    }

    private static void testGetInstant(RecordedObject e) {
        assertGetter(x -> e.getInstant(x), Instant.ofEpochMilli(INSTANT_VALUE.toEpochMilli()), "instant");
    }

    private static void testGetDuration(RecordedObject e) {
        assertGetter(x -> e.getDuration(x), DURATION_VALUE, "duration");
    }

    private static void testGetThread(RecordedObject e) {
        RecordedThread thread = e.getValue("threadField");
        if (!thread.getJavaName().equals(THREAD_VALUE.getName())) {
            throw new AssertionError("Expected thread to have name " + THREAD_VALUE.getName());
        }
        assertGetter(x -> {
            // OK to access nullField if it is correct type
            // Chose a second null field with class type
            if ("nullField".equals(x)) {
                return e.getThread("nullField2");
            } else {
                return e.getThread(x);
            }

        }, thread, "thread");
    }

    private static void testGetClass(RecordedObject e) {
        RecordedClass clazz = e.getValue("classField");
        if (!clazz.getName().equals(CLASS_VALUE.getName())) {
            throw new AssertionError("Expected class to have name " + CLASS_VALUE.getName());
        }
        assertGetter(x -> e.getClass(x), clazz, "class");
    }

    private static <T> void assertGetter(Function<String, T> f, T expectedValue, String... validTypes) {
        Set<String> valids = new HashSet<String>(Arrays.asList(validTypes));
        Set<String> invalids = new HashSet<String>(ALL);
        invalids.removeAll(valids);
        for (String valid : valids) {
            T result = f.apply(valid + "Field");
            if (!expectedValue.equals(result)) {
                throw new AssertionError("Incorrect return value " + result + ". Expected " + expectedValue);
            }
        }
        for (String invalid : invalids) {
            try {
                f.apply(invalid + "Field");
            } catch (IllegalArgumentException iae) {
                // OK, as expected
            } catch (Exception e) {
                throw new AssertionError("Unexpected exception for invalid field " + invalid + ". " + e.getClass().getName() + " : " + e.getMessage(), e);
            }
        }
        String[] illegals = { "missingField", "nullField.javaName.does.not.exist", "nullField" };
        for (String illegal : illegals) {
            try {
                f.apply(illegal);
                throw new AssertionError("Expected IllegalArgumentException when accessing " + illegal);
            } catch (IllegalArgumentException iae) {
                // OK, as expected
            } catch (Exception e) {
                throw new AssertionError("Expected IllegalArgumentException. Got " + e.getClass().getName() + " : " + e.getMessage(), e);
            }
        }
        try {
            f.apply(null);
            throw new AssertionError("Expected NullpointerException exception when passing in null value");
        } catch (NullPointerException iae) {
            // OK, as expected
        } catch (Exception e) {
            throw new AssertionError("Expected NullpointerException. Got " + e.getClass().getName() + " : " + e.getMessage(), e);
        }
    }

    private static RecordedObject makeRecordedObject() throws IOException {
        try (Recording r = new Recording()) {
            r.start();
            EventWithValues t = new EventWithValues();
            t.commit();
            r.stop();
            List<RecordedEvent> events = Events.fromRecording(r);
            Events.hasEvents(events);
            return events.get(0);
        }
    }

    private static Set<String> createAll() {
        Set<String> set = new HashSet<>();
        set.add("boolean");
        set.add("byte");
        set.add("char");
        set.add("short");
        set.add("int");
        set.add("long");
        set.add("float");
        set.add("double");
        set.add("string");
        set.add("class");
        set.add("thread");
        set.add("instant");
        set.add("duration");
        return set;
    }
}
