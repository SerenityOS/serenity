/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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

import static jdk.test.lib.Asserts.assertEquals;
import static jdk.test.lib.Asserts.assertNull;
import static jdk.test.lib.Asserts.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import jdk.jfr.EventType;
import jdk.jfr.Recording;
import jdk.jfr.Timespan;
import jdk.jfr.Timestamp;
import jdk.jfr.ValueDescriptor;
import jdk.jfr.consumer.RecordedEvent;
import jdk.test.lib.jfr.EventNames;
import jdk.test.lib.jfr.Events;

/**
 * @test
 * @summary Tests that the recording properties are properly reflected in the ActiveRecording event
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.event.runtime.TestActiveRecordingEvent
 */
public final class TestActiveRecordingEvent {

    private static final String ACTIVE_RECORDING_EVENT_NAME = EventNames.ActiveRecording;

    private static final Path MY_JFR_FILEPATH = Paths.get("", "my.jfr");

    private static final long MAX_SIZE = 1000000L;

    private static final Duration MAX_AGE = Duration.ofDays(1);

    private static final Duration REC_DURATION = Duration.ofMinutes(10);

    private static final String REC_NAME = "MYNAME";

    public static void main(String[] args) throws Throwable {
        testWithPath(null);
        testWithPath(MY_JFR_FILEPATH);
    }

    private static void testWithPath(Path path) throws Throwable {
        Recording recording = new Recording();
        recording.enable(ACTIVE_RECORDING_EVENT_NAME);

        recording.setDuration(REC_DURATION);
        recording.setMaxSize(MAX_SIZE);
        recording.setMaxAge(MAX_AGE);
        recording.setName(REC_NAME);
        if (path != null) {
            recording.setToDisk(true);
            recording.setDestination(path);
        }

        long tsBeforeStart = Instant.now().toEpochMilli();
        recording.start();
        recording.stop();
        long tsAfterStop = Instant.now().toEpochMilli();

        List<RecordedEvent> events = Events.fromRecording(recording);

        Events.hasEvents(events);
        RecordedEvent ev = events.get(0);

        // Duration must be kept in milliseconds
        assertEquals(REC_DURATION.toMillis(), ev.getValue("recordingDuration"));

        assertEquals(MAX_SIZE, ev.getValue("maxSize"));

        // maxAge must be kept in milliseconds
        assertEquals(MAX_AGE.toMillis(), ev.getValue("maxAge"));

        EventType evType = ev.getEventType();
        ValueDescriptor durationField = evType.getField("recordingDuration");
        assertEquals(durationField.getAnnotation(Timespan.class).value(), Timespan.MILLISECONDS);

        if (path == null) {
            assertNull(ev.getValue("destination"));
        } else {
            assertEquals(path.toAbsolutePath().toString(), ev.getValue("destination").toString());
        }

        ValueDescriptor recordingStartField = evType.getField("recordingStart");
        assertEquals(recordingStartField.getAnnotation(Timestamp.class).value(), Timestamp.MILLISECONDS_SINCE_EPOCH);

        long tsRecordingStart = ev.getValue("recordingStart");
        assertTrue(tsBeforeStart <= tsRecordingStart);
        assertTrue(tsAfterStop >= tsRecordingStart);

        assertEquals(recording.getId(), ev.getValue("id"));

        ValueDescriptor maxAgeField = evType.getField("maxAge");
        assertEquals(maxAgeField.getAnnotation(Timespan.class).value(), Timespan.MILLISECONDS);
    }
}
