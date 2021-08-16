/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import jdk.jfr.Event;
import jdk.jfr.EventType;
import jdk.jfr.FlightRecorder;
import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordingFile;
import jdk.jfr.consumer.RecordedEvent;

public class GetFlightRecorder {
    private static class TestEvent extends Event {
    }
    private static class SimpleEvent extends Event {
        public int id;
    }
    public static void main(String args[]) throws Exception {
        EventType type = EventType.getEventType(TestEvent.class); // This class is loaded before recording has started.
        if (type.isEnabled()) {
            throw new RuntimeException("Expected event to be disabled before recording start");
        }

        // (1) make sure you can obtain the flight recorder without error.
        System.out.println("jdk.jfr.FlightRecorder.getFlightRecorder() = " + FlightRecorder.getFlightRecorder());

        // (2) test that the event class loaded before recording can still work.
        Recording r = new Recording();
        r.start();
        if (!type.isEnabled()) {
            throw new RuntimeException("Expected event to be enabled during recording");
        }
        TestEvent testEvent = new TestEvent();
        testEvent.commit();
        loadEventClassDuringRecording();
        r.stop();
        if (type.isEnabled()) {
            throw new RuntimeException("Expected event to be disabled after recording stopped");
        }
        System.out.println("Checking SimpleEvent");
        hasEvent(r, SimpleEvent.class.getName());
        System.out.println("OK");

        System.out.println("Checking TestEvent");
        hasEvent(r, TestEvent.class.getName());
        System.out.println("OK");
    }

    // Classes that are loaded during a recording
    // should get instrumentation on class load
    private static void loadEventClassDuringRecording() {
        SimpleEvent event = new SimpleEvent();
        event.commit();
    }

    public static List<RecordedEvent> fromRecording(Recording recording) throws IOException {
        return RecordingFile.readAllEvents(makeCopy(recording));
    }

    private static Path makeCopy(Recording recording) throws IOException {
        Path p = recording.getDestination();
        if (p == null) {
            File directory = new File(".");
            // FIXME: Must come up with a way to give human-readable name
            // this will at least not clash when running parallel.
            ProcessHandle h = ProcessHandle.current();
            p = new File(directory.getAbsolutePath(), "recording-" + recording.getId() + "-pid" + h.pid() + ".jfr").toPath();
            recording.dump(p);
        }
        return p;
    }

    public static void hasEvent(Recording r, String name) throws IOException {
        List<RecordedEvent> events = fromRecording(r);
        hasEvents(events);
        hasEvent(events, name);
    }

    public static void hasEvents(List<RecordedEvent> events) {
        if (events.isEmpty()) {
            throw new RuntimeException("No events");
        }
    }

    public static void hasEvent(List<RecordedEvent> events, String name) throws IOException {
        if (!containsEvent(events, name)) {
            throw new RuntimeException("Missing event " + name  + " in recording " + events.toString());
        }
    }

    private static boolean containsEvent(List<RecordedEvent> events, String name) {
        for (RecordedEvent event : events) {
            if (event.getEventType().getName().equals(name)) {
                return true;
            }
        }
        return false;
    }
}
