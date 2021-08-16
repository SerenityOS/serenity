/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.event.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputFilter.Status;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedClass;
import jdk.jfr.consumer.RecordedEvent;
import jdk.test.lib.jfr.EventNames;
import jdk.test.lib.jfr.Events;
import jdk.test.lib.serial.SerialObjectBuilder;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static java.lang.System.out;
import static org.testng.Assert.*;

/*
 * @test
 * @bug 8261160
 * @summary Add a deserialization JFR event
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run testng/othervm jdk.jfr.event.io.TestDeserializationEvent
 */
public class TestDeserializationEvent {

    public record R() implements Serializable { }

    @DataProvider(name = "scenarios")
    public Object[][] scenarios() throws Exception {
        byte[] ba1 = serialize(new R());
        byte[] ba2 = serialize(new int[] { 56, 67, 58, 59, 60 });
        byte[] ba3 = serialize(new R[] { new R(), new R() });
        byte[] ba4 = serialize(new char[][] { new char[] {'a', 'b'}, new char[] {'c'} });

        // data provider columns- 1:id, 2:deserialize-operation, 3:expected-event-checkers
        return new Object[][] {
            {   1,  // single stream object, R
                (Runnable)() -> deserialize(ba1),
                List.of(
                    Set.of(
                        assertFilterStatus(null),
                        assertType(R.class),
                        assertArrayLength(-1),
                        assertObjectReferences(1),
                        assertDepth(1),
                        assertHasBytesRead(),
                        assertExceptionType(null),
                        assertExceptionMessage(null))) },
            {   2,  // primitive int array
                (Runnable)() -> deserialize(ba2),
                List.of(
                    Set.of(  // TC_CLASS, for array class int[]
                        assertType(int[].class),
                        assertArrayLength(-1)),
                    Set.of(  // TC_ARRAY, actual array
                        assertType(int[].class),
                        assertArrayLength(5))) },
            {   3,  // reference array, R
                (Runnable)() -> deserialize(ba3),
                List.of(
                    Set.of(  // TC_CLASS, for array class R[]
                        assertType(R[].class),
                        assertArrayLength(-1)),
                    Set.of(  // TC_ARRAY, actual array
                        assertType(R[].class),
                        assertArrayLength(2)),
                    Set.of(  // TC_CLASS, for R
                        assertType(R.class),
                        assertArrayLength(-1)),
                    Set.of(  // TC_REFERENCE, for TC_CLASS relating second stream obj
                        assertType(null),
                        assertArrayLength(-1))) },
            {  4,  // multi-dimensional prim char array
               (Runnable)() -> deserialize(ba4),
               List.of(
                    Set.of(  // TC_CLASS, for array class char[][]
                        assertType(char[][].class),
                        assertArrayLength(-1),
                        assertDepth(1)),
                    Set.of(  // TC_ARRAY, actual char[][] array
                        assertType(char[][].class),
                        assertArrayLength(2),
                        assertDepth(1)),
                    Set.of(  // TC_CLASS, for array class char[]
                        assertType(char[].class),
                        assertArrayLength(-1),
                        assertDepth(2)),
                    Set.of(  // TC_ARRAY, first char[] array
                        assertType(char[].class),
                        assertArrayLength(2),
                        assertDepth(2)),
                    Set.of(  // TC_REFERENCE, for TC_CLASS relating to second stream array
                        assertType(null),
                        assertArrayLength(-1),
                        assertDepth(2)),
                    Set.of(  // TC_ARRAY, second char[] array
                        assertType(char[].class),
                        assertArrayLength(1),
                        assertDepth(2))) }
        };
    }

    @Test(dataProvider = "scenarios")
    public void test(int id,
                     Runnable thunk,
                     List<Set<Consumer<RecordedEvent>>> expectedValuesChecker)
       throws IOException
    {
        try (Recording recording = new Recording()) {
            recording.enable(EventNames.Deserialization);
            recording.start();
            thunk.run();
            recording.stop();
            List<RecordedEvent> events = Events.fromRecording(recording);
            assertEquals(events.size(), expectedValuesChecker.size());
            assertEventList(events, expectedValuesChecker);
        }
    }

    static final Class<InvalidClassException> ICE = InvalidClassException.class;

    @DataProvider(name = "filterDisallowedValues")
    public Object[][] filterDisallowedValues() {
        return new Object[][] {
                { Status.REJECTED,   "REJECTED" },
                { null,              null       }
        };
    }

    @Test(dataProvider = "filterDisallowedValues")
    public void testFilterDisallow(Status filterStatus,
                                   String expectedValue)
        throws Exception
    {
        try (Recording recording = new Recording();
             var bais = new ByteArrayInputStream(serialize(new R()));
             var ois = new ObjectInputStream(bais)) {
            ois.setObjectInputFilter(fv -> filterStatus);
            recording.enable(EventNames.Deserialization);
            recording.start();
            assertThrows(ICE, () -> ois.readObject());
            recording.stop();
            List<RecordedEvent> events = Events.fromRecording(recording);
            assertEquals(events.size(), 1);
            assertEquals(events.get(0).getEventType().getName(), "jdk.Deserialization");
            assertFilterConfigured(true).accept(events.get(0));
            assertFilterStatus(expectedValue).accept(events.get(0));
        }
    }

    @DataProvider(name = "filterAllowedValues")
    public Object[][] filterAllowedValues() {
        return new Object[][] {
                { Status.ALLOWED,   "ALLOWED"   },
                { Status.UNDECIDED, "UNDECIDED" },
        };
    }

    @Test(dataProvider = "filterAllowedValues")
    public void testFilterAllowed(Status filterStatus,
                                  String expectedValue) throws Exception {
        try (Recording recording = new Recording();
             var bais = new ByteArrayInputStream(serialize(new R()));
             var ois = new ObjectInputStream(bais)) {
            ois.setObjectInputFilter(fv -> filterStatus);
            recording.enable(EventNames.Deserialization);
            recording.start();
            ois.readObject();
            recording.stop();
            List<RecordedEvent> events = Events.fromRecording(recording);
            assertEquals(events.size(), 1);
            assertEquals(events.get(0).getEventType().getName(), "jdk.Deserialization");
            assertFilterConfigured(true).accept(events.get(0));
            assertFilterStatus(expectedValue).accept(events.get(0));
        }
    }

    static class XYZException extends RuntimeException {
        XYZException(String msg) { super(msg); }
    }

    @Test
    public void testException() throws Exception {
        try (Recording recording = new Recording();
             var bais = new ByteArrayInputStream(serialize(new R()));
             var ois = new ObjectInputStream(bais)) {
            ois.setObjectInputFilter(fv -> { throw new XYZException("I am a bad filter!!!"); });
            recording.enable(EventNames.Deserialization);
            recording.start();
            assertThrows(ICE, () -> ois.readObject());
            recording.stop();
            List<RecordedEvent> events = Events.fromRecording(recording);
            assertEquals(events.size(), 1);
            assertEquals(events.get(0).getEventType().getName(), "jdk.Deserialization");
            assertFilterConfigured(true).accept(events.get(0));
            assertExceptionType(XYZException.class).accept(events.get(0));
            assertExceptionMessage("I am a bad filter!!!").accept(events.get(0));
        }
    }

    static void assertEventList(List<RecordedEvent> actualEvents,
                                List<Set<Consumer<RecordedEvent>>> expectedValuesChecker) {
        int found = 0;
        for (RecordedEvent recordedEvent : actualEvents) {
            assertEquals(recordedEvent.getEventType().getName(), "jdk.Deserialization");
            out.println("Checking recorded event:" + recordedEvent);
            Set<Consumer<RecordedEvent>> checkers = expectedValuesChecker.get(found);
            for (Consumer<RecordedEvent> checker : checkers) {
                out.println("  checking:" + checker);
                checker.accept(recordedEvent);
            }
            assertFilterConfigured(false).accept(recordedEvent); // no filter expected
            assertExceptionType(null).accept(recordedEvent);     // no exception type expected
            assertExceptionMessage(null).accept(recordedEvent);  // no exception message expected
            found++;
        }
        assertEquals(found, expectedValuesChecker.size());
    }

    static Consumer<RecordedEvent> assertFilterConfigured(boolean expectedValue) {
        return new Consumer<>() {
            @Override public void accept(RecordedEvent recordedEvent) {
                assertTrue(recordedEvent.hasField("filterConfigured"));
                assertEquals((boolean)recordedEvent.getValue("filterConfigured"), expectedValue);
            }
            @Override public String toString() {
                return "assertFilterConfigured, expectedValue=" + expectedValue;
            }
        };
    }

    static Consumer<RecordedEvent> assertFilterStatus(String expectedValue) {
        return new Consumer<>() {
            @Override public void accept(RecordedEvent recordedEvent) {
                assertTrue(recordedEvent.hasField("filterStatus"));
                assertEquals(recordedEvent.getValue("filterStatus"), expectedValue);
            }
            @Override public String toString() {
                return "assertFilterStatus, expectedValue=" + expectedValue;
            }
        };
    }

    static Consumer<RecordedEvent> assertType(Class<?> expectedValue) {
        return new Consumer<>() {
            @Override public void accept(RecordedEvent recordedEvent) {
                assertTrue(recordedEvent.hasField("type"));
                assertClassOrNull(expectedValue, recordedEvent, "type");
            }
            @Override public String toString() {
                return "assertType, expectedValue=" + expectedValue;
            }
        };
    }

    static Consumer<RecordedEvent> assertArrayLength(int expectedValue) {
        return new Consumer<>() {
            @Override public void accept(RecordedEvent recordedEvent) {
                assertTrue(recordedEvent.hasField("arrayLength"));
                assertEquals((int)recordedEvent.getValue("arrayLength"), expectedValue);
            }
            @Override public String toString() {
                return "assertArrayLength, expectedValue=" + expectedValue;
            }
        };
    }

    static Consumer<RecordedEvent> assertObjectReferences(long expectedValue) {
        return new Consumer<>() {
            @Override public void accept(RecordedEvent recordedEvent) {
                assertTrue(recordedEvent.hasField("objectReferences"));
                assertEquals((long)recordedEvent.getValue("objectReferences"), expectedValue);
            }
            @Override public String toString() {
                return "assertObjectReferences, expectedValue=" + expectedValue;
            }
        };
    }

    static Consumer<RecordedEvent> assertDepth(long expectedValue) {
        return new Consumer<>() {
            @Override public void accept(RecordedEvent recordedEvent) {
                assertTrue(recordedEvent.hasField("depth"));
                assertEquals((long)recordedEvent.getValue("depth"), expectedValue);
            }
            @Override public String toString() {
                return "assertDepth, expectedValue=" + expectedValue;
            }
        };
    }

    static Consumer<RecordedEvent> assertHasBytesRead() {
        return new Consumer<>() {
            @Override public void accept(RecordedEvent recordedEvent) {
                assertTrue(recordedEvent.hasField("bytesRead"));
            }
            @Override public String toString() {
                return "assertHasBytesRead,";
            }
        };
    }

    static Consumer<RecordedEvent> assertBytesRead(long expectedValue) {
        return new Consumer<>() {
            @Override public void accept(RecordedEvent recordedEvent) {
                assertHasBytesRead().accept(recordedEvent);
                assertEquals((long)recordedEvent.getValue("bytesRead"), expectedValue);
            }
            @Override public String toString() {
                return "assertBytesRead, expectedValue=" + expectedValue;
            }
        };
    }

    static Consumer<RecordedEvent> assertExceptionType(Class<?> expectedValue) {
        return new Consumer<>() {
            @Override public void accept(RecordedEvent recordedEvent) {
                assertTrue(recordedEvent.hasField("exceptionType"));
                assertClassOrNull(expectedValue, recordedEvent, "exceptionType");
            }
            @Override public String toString() {
                return "assertExceptionType, expectedValue=" + expectedValue;
            }
        };
    }

    static Consumer<RecordedEvent> assertExceptionMessage(String expectedValue) {
        return new Consumer<>() {
            @Override public void accept(RecordedEvent recordedEvent) {
                assertTrue(recordedEvent.hasField("exceptionMessage"));
                assertEquals(recordedEvent.getValue("exceptionMessage"), expectedValue);
            }
            @Override public String toString() {
                return "assertExceptionMessage, expectedValue=" + expectedValue;
            }
        };
    }

    static void assertClassOrNull(Class<?> expectedValue,
                                  RecordedEvent recordedEvent,
                                  String valueName) {
        if (expectedValue == null && recordedEvent.getValue(valueName) == null)
            return;

        if (recordedEvent.getValue(valueName) instanceof RecordedClass recordedClass)
            assertEquals(recordedClass.getName(), expectedValue.getName());
        else
            fail("Expected RecordedClass, got:" + recordedEvent.getValue(valueName).getClass());
    }

    static <T> byte[] serialize(T obj) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(obj);
        oos.close();
        return baos.toByteArray();
    }

    @SuppressWarnings("unchecked")
    static <T> T deserialize(byte[] streamBytes) {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(streamBytes);
            ObjectInputStream ois  = new ObjectInputStream(bais);
            return (T) ois.readObject();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ---
    static volatile boolean initializedFoo; // false
    // Do not inadvertently initialize this class, Foo.
    static class Foo implements Serializable {
        static { TestDeserializationEvent.initializedFoo = true; }
    }

    /**
     * Checks that the creation and recording of the Deserialization event does
     * not inadvertently trigger initialization of the class of the stream
     * object, when deserialization is rejected by the filter.
     */
    @Test
    public void testRejectedClassNotInitialized() throws Exception {
        byte[] bytes = SerialObjectBuilder.newBuilder("Foo").build();
        assertFalse(initializedFoo);  // sanity

        try (Recording recording = new Recording();
             var bais = new ByteArrayInputStream(bytes);
             var ois = new ObjectInputStream(bais)) {
            ois.setObjectInputFilter(fv -> Status.REJECTED);
            recording.enable(EventNames.Deserialization);
            recording.start();
            assertThrows(ICE, () -> ois.readObject());
            recording.stop();
            List<RecordedEvent> events = Events.fromRecording(recording);
            assertEquals(events.size(), 1);
            assertEquals(events.get(0).getEventType().getName(), "jdk.Deserialization");
            assertFilterConfigured(true).accept(events.get(0));
            assertFilterStatus("REJECTED").accept(events.get(0));
            assertFalse(initializedFoo);
            assertType(Foo.class);
        }
    }
}
