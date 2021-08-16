/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.StringJoiner;


import jdk.jfr.Event;
import jdk.jfr.EventType;
import jdk.jfr.FlightRecorder;
import jdk.jfr.Name;
import jdk.jfr.Recording;
import jdk.jfr.Registered;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;
import jdk.test.lib.Asserts;
import jdk.test.lib.Utils;

/**
 * @test
 * @summary Verifies that all methods in RecordingFIle are working
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm -Xlog:jfr*=info jdk.jfr.api.consumer.TestRecordingFile
 */
public class TestRecordingFile {

    static class TestEvent1 extends Event {
    }

    static class TestEvent2 extends Event {
    }

    static class TestEvent3 extends Event {
    }

    private static String TEST_CLASS_BASE = "TestRecordingFile$TestEvent";
    private final static int EVENT_COUNT = 3;
    private final static int HEADER_SIZE = 68;
    private final static long METADATA_OFFSET = 24;

    public static void main(String[] args) throws Throwable {
        Path valid = Utils.createTempFile("three-event-recording", ".jfr");

        // create some recording data
        try (Recording r = new Recording()) {
            r.enable(TestEvent1.class).withoutStackTrace();
            r.enable(TestEvent2.class).withoutStackTrace();
            r.enable(TestEvent3.class).withoutStackTrace();
            r.start();
            TestEvent1 t1 = new TestEvent1();
            t1.commit();
            TestEvent2 t2 = new TestEvent2();
            t2.commit();
            TestEvent3 t3 = new TestEvent3();
            t3.commit();
            r.stop();
            r.dump(valid);
        }
        Path brokenWithZeros = createBrokenWIthZeros(valid);
        Path brokenMetadata = createBrokenMetadata(valid);
        // prepare event sets
        testNewRecordingFile(valid, brokenWithZeros);
        testIterate(valid, brokenWithZeros);
        testReadAllEvents(valid, brokenWithZeros);
        testReadEventTypes(valid, brokenMetadata);
        testClose(valid);
        testReadEventTypesMultiChunk();
        testReadEventTypeWithUnregistration(false, false);
        testReadEventTypeWithUnregistration(false, true);
        testReadEventTypeWithUnregistration(true, false);
        testReadEventTypeWithUnregistration(true, true);
    }

    private static void testReadEventTypeWithUnregistration(boolean disk, boolean background) throws Exception {
        FlightRecorder.register(Event1.class);
        FlightRecorder.register(Event2.class);
        FlightRecorder.register(Event3.class);
        Recording backgrundRecording = new Recording();
        if (disk) {
            backgrundRecording.setToDisk(disk);
        }
        if (background) {
            backgrundRecording.start();
        }
        recordAndVerify(disk, background,new int[] {1,2, 3}, new int[] {});
        FlightRecorder.unregister(Event2.class);
        recordAndVerify(disk, background,  new int[] {1, 3}, new int[] {2});
        FlightRecorder.unregister(Event1.class);
        FlightRecorder.register(Event2.class);
        recordAndVerify(disk,background, new int[] {2, 3}, new int[] {1});
        FlightRecorder.unregister(Event3.class);
        FlightRecorder.register(Event3.class);
        FlightRecorder.unregister(Event2.class);
        FlightRecorder.unregister(Event3.class);
        FlightRecorder.register(Event1.class);
        FlightRecorder.unregister(Event1.class);
        FlightRecorder.register(Event1.class);
        FlightRecorder.register(Event2.class);
        recordAndVerify(disk, background,new int[] {1, 2}, new int[] {3});
        if (background) {
            backgrundRecording.close();
        }
    }

    private static void recordAndVerify(boolean disk, boolean background,  int[] shouldExist, int[] shouldNotExist) throws Exception {
        StringJoiner sb  = new StringJoiner("-");
        for (int i = 0; i <shouldExist.length; i++) {
            sb.add(Integer.toString(shouldExist[i]));
        }
        System.out.println("Verifying recordings: disk=" + disk + " background=" + background);
        System.out.println("Should exist: " + Arrays.toString(shouldExist));
        System.out.println("Should not exist: " + Arrays.toString(shouldNotExist));

        Path p = Utils.createTempFile(sb.toString(), ".jfr");
        System.out.println("Filename: " + p);
        try (Recording r = new Recording()) {
            r.start();
            r.stop();
            r.dump(p);
            try (RecordingFile f = new RecordingFile(p)) {
                List<EventType> types = f.readEventTypes();
                for (int i = 0; i< shouldExist.length; i++) {
                    assertHasEventType(types, "Event" + shouldExist[i]);
                }
                for (int i = 0; i< shouldNotExist.length; i++) {
                    assertMissingEventType(types, "Event" + shouldNotExist[i]);
                }
            }
        }
        System.out.println();
        System.out.println();
    }

    @Registered(false)
    @Name("Event1")
    private static class Event1 extends Event {
    }
    @Registered(false)
    @Name("Event2")
    private static class Event2 extends Event {
    }
    @Registered(false)
    @Name("Event3")
    private static class Event3 extends Event {
    }

    private static void testReadEventTypesMultiChunk() throws Exception {

        Path twoEventTypes = Utils.createTempFile("two-event-types", ".jfr");
        Path threeEventTypes = Utils.createTempFile("three-event-types", ".jfr");
       try (Recording r1 = new Recording()) {
           r1.start();
           FlightRecorder.register(Event1.class);
           try (Recording r2 = new Recording()) {
               r2.start();
               FlightRecorder.register(Event2.class);

               // Ensure that metadata are written twice.
               try (Recording rotator = new Recording()) {
                   rotator.start();
                   rotator.stop();
               }
               r2.stop();
               r2.dump(twoEventTypes);;
           }
           FlightRecorder.register(Event3.class);
           r1.stop();
           r1.dump(threeEventTypes);;
       }
       try (RecordingFile f = new RecordingFile(twoEventTypes)) {
           List<EventType> types = f.readEventTypes();
           assertUniqueEventTypes(types);
           assertHasEventType(types, "Event1");
           assertHasEventType(types, "Event2");
           assertMissingEventType(types, "Event3");
       }
       try (RecordingFile f = new RecordingFile(threeEventTypes)) {
           List<EventType> types = f.readEventTypes();
           assertUniqueEventTypes(types);
           assertHasEventType(types, "Event1");
           assertHasEventType(types, "Event2");
           assertHasEventType(types, "Event3");
       }

    }

    private static void assertMissingEventType(List<EventType> types,String name) throws Exception {
        EventType type = findEventType(types, name);
        if (type != null) {
            throw new Exception("Found unexpected event type " + name);
        }
    }

    private static void assertHasEventType(List<EventType> types,String name) throws Exception {
        EventType type = findEventType(types, name);
        if (type == null) {
            throw new Exception("Missing event type " + name);
        }
    }

    private static EventType findEventType(List<EventType> types, String name) {
        for (EventType t : types) {
            if (t.getName().equals(name)) {
                return t;
            }
        }
        return null;
    }

    private static void assertUniqueEventTypes(List<EventType> types) {
        HashSet<Long> ids = new HashSet<>();
        for (EventType type : types) {
            ids.add(type.getId());
        }
        Asserts.assertEquals(types.size(), ids.size(), "Event types repeated. " + types);
    }

    private static Path createBrokenWIthZeros(Path valid) throws Exception {
        try {
            Path broken = Utils.createTempFile("broken-events", ".jfr");
            Files.delete(broken);
            Files.copy(valid, broken);
            RandomAccessFile raf = new RandomAccessFile(broken.toFile(), "rw");
            raf.seek(HEADER_SIZE);
            int size = (int) Files.size(broken);
            byte[] ones = new byte[size - HEADER_SIZE];
            for (int i = 0; i < ones.length; i++) {
                ones[i] = (byte) 0xFF;
            }
            raf.write(ones, 0, ones.length);
            raf.close();
            return broken;
        } catch (IOException ioe) {
            throw new Exception("Could not produce a broken file " + valid, ioe);
        }
    }

    private static Path createBrokenMetadata(Path valid) throws Exception {
        try {
            Path broken = Utils.createTempFile("broken-metadata", ".jfr");
            Files.delete(broken);
            Files.copy(valid, broken);
            RandomAccessFile raf = new RandomAccessFile(broken.toFile(), "rw");
            raf.seek(METADATA_OFFSET);
            long metadataOffset = raf.readLong();
            raf.seek(metadataOffset);
            raf.writeLong(Long.MAX_VALUE);
            raf.writeLong(Long.MAX_VALUE);
            raf.close();
            return broken;
        } catch (IOException ioe) {
            throw new Exception("Could not produce a broken EventSet from file " + valid, ioe);
        }
    }

    private static void testReadEventTypes(Path valid, Path broken) throws Exception {
        try (RecordingFile validFile = new RecordingFile(valid)) {
            List<EventType> types = validFile.readEventTypes();
            if (types.size() < EVENT_COUNT) {
                throw new Exception("Expected at least " + EVENT_COUNT + " event type but got " + types.toString());
            }
            int counter = 0;
            for (Class<?> testClass : Arrays.asList(TestEvent1.class, TestEvent2.class, TestEvent3.class)) {
                for (EventType t : types) {
                    if (t.getName().equals(testClass.getName())) {
                        counter++;
                    }
                }
            }
            if (counter != 3) {
                throw new Exception("Returned incorrect event types");
            }
        }
        try (RecordingFile brokenFile = new RecordingFile(broken)) {
            brokenFile.readEventTypes();
            throw new Exception("Expected IOException when getting Event Types from broken recording");
        } catch (IOException ise) {
            // OK
        }
    }

    private static void testNewRecordingFile(Path valid, Path broken) throws Exception {
        try (RecordingFile r = new RecordingFile(null)) {
            throw new Exception("Expected NullPointerException");
        } catch (NullPointerException npe) {
            // OK
        }
        try (RecordingFile r = new RecordingFile(Paths.get("hjhjsdfhkjshdfkj.jfr"))) {
            throw new Exception("Expected FileNotFoundException");
        } catch (FileNotFoundException npe) {
            // OK
        }
        Path testFile = Utils.createTempFile("test-empty-file", ".jfr");
        try (RecordingFile r = new RecordingFile(testFile)) {
            throw new Exception("Expected IOException if file is empty");
        } catch (IOException e) {
            // OK
        }
        FileWriter fr = new FileWriter(testFile.toFile());
        fr.write("whatever");
        fr.close();
        try (RecordingFile r = new RecordingFile(Paths.get("hjhjsdfhkjshdfkj.jfr"))) {
            throw new Exception("Expected IOException if magic is incorrect");
        } catch (IOException e) {
            // OK
        }

        try (RecordingFile r = new RecordingFile(valid)) {
        }
    }

    private static void testClose(Path valid) throws Exception {
        RecordingFile f = new RecordingFile(valid);
        f.close();

        try {
            f.readEvent();
            throw new Exception("Should not be able to read event from closed recording");
        } catch (IOException e) {
            if (!e.getMessage().equals("Stream Closed")) {
                throw new Exception("Expected 'Stream Closed' in exception message for a closed stream. Got '" + e.getMessage() +"'.");
            }
            // OK
        }
        try {
            f.readEventTypes();
            throw new Exception("Should not be able to read event from closed recording");
        } catch (IOException e) {
            if (!e.getMessage().equals("Stream Closed")) {
                throw new Exception("Expected 'Stream Closed' in exception message for a closed stream. Got '" + e.getMessage() +"'.");
            }
            // OK
        }
        // close twice
        f.close();
    }

    private static void testIterate(Path valid, Path broken) throws Exception {
        try (RecordingFile validFile = new RecordingFile(valid)) {
            for (int i = 0; i < EVENT_COUNT; i++) {
                if (!validFile.hasMoreEvents()) {
                    throw new Exception("Not all events available");
                }
                RecordedEvent r = validFile.readEvent();
                if (r == null) {
                    throw new Exception("Missing event");
                }
                if (!r.getEventType().getName().contains(TEST_CLASS_BASE)) {
                    throw new Exception("Incorrect event in recording file " + r);
                }
            }
            if (validFile.hasMoreEvents()) {
                throw new Exception("Should not be more than " + EVENT_COUNT + " in recording");
            }
        }
        try (RecordingFile brokenFile = new RecordingFile(broken)) {
            brokenFile.readEvent();
            throw new Exception("Expected IOException for broken recording");
        } catch (IOException ise) {
            // OK
        }
    }

    private static void testReadAllEvents(Path valid, Path broken) throws Exception {
        try {
            RecordingFile.readAllEvents(broken);
            throw new Exception("Expected IOException when reading all events for broken recording");
        } catch (IOException ioe) {
            // OK as expected
        }
    }
}
