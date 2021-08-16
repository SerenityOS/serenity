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

package jdk.jfr.api.flightrecorder;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jdk.jfr.FlightRecorder;
import jdk.jfr.Recording;
import jdk.jfr.RecordingState;
import jdk.jfr.consumer.RecordedEvent;
import jdk.test.lib.Asserts;
import jdk.test.lib.jfr.Events;
import jdk.test.lib.jfr.SimpleEvent;

/**
 * @test
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.api.flightrecorder.TestSnapshot
 */
public class TestSnapshot {
    private final static int RECORDING_COUNT = 5;

    public static void main(String[] args) throws Exception {
        testEmpty();
        testStopped();
        testOngoingDisk();
        testOngoingMemory();
        testMultiple();
    }

    private static void testMultiple() throws IOException {
        FlightRecorder recorder = FlightRecorder.getFlightRecorder();
        List<Recording> recordings = new ArrayList<>();
        long size = 0;
        for (int i = 0; i < RECORDING_COUNT; i++) {
            Recording r = new Recording();
            r.enable(SimpleEvent.class);
            r.start();
            SimpleEvent se = new SimpleEvent();
            se.commit();
            r.stop();
            recordings.add(r);
            size += r.getSize();
        }
        try (Recording snapshot = recorder.takeSnapshot()) {
            Asserts.assertEquals(snapshot.getSize(), size);
            Asserts.assertGreaterThanOrEqual(snapshot.getStartTime(), recordings.get(0).getStartTime());
            Asserts.assertLessThanOrEqual(snapshot.getStopTime(), recordings.get(RECORDING_COUNT - 1).getStopTime());
            Asserts.assertGreaterThanOrEqual(snapshot.getDuration(), Duration.ZERO);
            assertStaticOptions(snapshot);
            try (InputStream is = snapshot.getStream(null, null)) {
                Asserts.assertNotNull(is);
            }

            List<RecordedEvent> events = Events.fromRecording(snapshot);
            Events.hasEvents(events);
            Asserts.assertEquals(events.size(), RECORDING_COUNT);
            for (int i = 0; i < RECORDING_COUNT; i++) {
                Asserts.assertEquals(events.get(i).getEventType().getName(), SimpleEvent.class.getName());
            }
        }
        for (Recording r : recordings) {
            r.close();
        }
    }
    private static void testOngoingMemory() throws IOException {
        testOngoing(false);
    }

    private static void testOngoingDisk() throws IOException {
        testOngoing(true);
    }

    private static void testOngoing(boolean disk) throws IOException {
        FlightRecorder recorder = FlightRecorder.getFlightRecorder();
        try (Recording r = new Recording()) {
            r.setToDisk(disk);
            r.enable(SimpleEvent.class);
            r.start();
            SimpleEvent se = new SimpleEvent();
            se.commit();

            try (Recording snapshot = recorder.takeSnapshot()) {

                Asserts.assertGreaterThan(snapshot.getSize(), 0L);
                Asserts.assertGreaterThanOrEqual(snapshot.getStartTime(), r.getStartTime());
                Asserts.assertGreaterThanOrEqual(snapshot.getStopTime(), r.getStartTime());
                Asserts.assertGreaterThanOrEqual(snapshot.getDuration(), Duration.ZERO);
                assertStaticOptions(snapshot);
                try (InputStream is = snapshot.getStream(null, null)) {
                    Asserts.assertNotNull(is);
                }

                List<RecordedEvent> events = Events.fromRecording(snapshot);
                Events.hasEvents(events);
                Asserts.assertEquals(events.size(), 1);
                Asserts.assertEquals(events.get(0).getEventType().getName(), SimpleEvent.class.getName());
            }

            r.stop();
        }
    }

    private static void assertStaticOptions(Recording snapshot) {
        Asserts.assertTrue(snapshot.getName().startsWith("Snapshot"), "Recording name should begin with 'Snapshot'");
        Asserts.assertEquals(snapshot.getMaxAge(), null);
        Asserts.assertEquals(snapshot.getMaxSize(), 0L);
        Asserts.assertTrue(snapshot.getSettings().isEmpty());
        Asserts.assertEquals(snapshot.getState(), RecordingState.STOPPED);
        Asserts.assertEquals(snapshot.getDumpOnExit(), false);
        Asserts.assertEquals(snapshot.getDestination(), null);
    }

    private static void testStopped() throws IOException {
        FlightRecorder recorder = FlightRecorder.getFlightRecorder();
        try (Recording r = new Recording()) {
            r.enable(SimpleEvent.class);
            r.start();
            SimpleEvent se = new SimpleEvent();
            se.commit();
            r.stop();

            try (Recording snapshot = recorder.takeSnapshot()) {

                Asserts.assertEquals(snapshot.getSize(), r.getSize());
                Asserts.assertGreaterThanOrEqual(snapshot.getStartTime(), r.getStartTime());
                Asserts.assertLessThanOrEqual(snapshot.getStopTime(), r.getStopTime());
                Asserts.assertGreaterThanOrEqual(snapshot.getDuration(), Duration.ZERO);
                assertStaticOptions(snapshot);
                try (InputStream is = snapshot.getStream(null, null)) {
                    Asserts.assertNotNull(is);
                }

                List<RecordedEvent> events = Events.fromRecording(snapshot);
                Events.hasEvents(events);
                Asserts.assertEquals(events.size(), 1);
                Asserts.assertEquals(events.get(0).getEventType().getName(), SimpleEvent.class.getName());
            }
        }
    }

    private static void testEmpty() throws IOException {
        FlightRecorder recorder = FlightRecorder.getFlightRecorder();
        Instant before = Instant.now().minusNanos(1);
        try (Recording snapshot = recorder.takeSnapshot()) {
            Instant after = Instant.now().plusNanos(1);
            Asserts.assertEquals(snapshot.getSize(), 0L);
            Asserts.assertLessThan(before, snapshot.getStartTime());
            Asserts.assertGreaterThan(after, snapshot.getStopTime());
            Asserts.assertEquals(snapshot.getStartTime(), snapshot.getStopTime());
            Asserts.assertEquals(snapshot.getDuration(), Duration.ZERO);
            assertStaticOptions(snapshot);
            Asserts.assertEquals(snapshot.getStream(null, null), null);
        }
    }

}
