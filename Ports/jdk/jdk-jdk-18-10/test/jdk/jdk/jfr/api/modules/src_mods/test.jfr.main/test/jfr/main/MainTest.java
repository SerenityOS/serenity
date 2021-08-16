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

package test.jfr.main;

import java.io.File;
import java.io.IOException;
import java.util.List;

import jdk.jfr.AnnotationElement;
import jdk.jfr.EventType;
import jdk.jfr.FlightRecorder;
import jdk.jfr.Recording;
import jdk.jfr.ValueDescriptor;
import jdk.jfr.consumer.RecordedEvent;

import test.jfr.annotation.ModularizedAnnotation;
import test.jfr.event.ModularizedOrdinaryEvent;
import test.jfr.event.ModularizedPeriodicEvent;
import java.nio.file.Path;
import java.util.Objects;
import jdk.jfr.consumer.RecordingFile;

public class MainTest {

    private static final String HELLO_ORDINARY = "ordinary says hello";
    private static final String HELLO_PERIODIC = "periodic says hello";

    public static void main(String... args) throws Exception {
        System.out.println("Starting the test...");
        FlightRecorder.addPeriodicEvent(ModularizedPeriodicEvent.class, () -> {
            ModularizedPeriodicEvent me = new ModularizedPeriodicEvent();
            me.message = HELLO_PERIODIC;
            me.commit();
        });
        Recording r = new Recording();
        r.enable(ModularizedOrdinaryEvent.class).with("filter", "true").withoutStackTrace();
        r.enable(ModularizedPeriodicEvent.class).with("filter", "true").withoutStackTrace();
        r.start();
        ModularizedOrdinaryEvent m = new ModularizedOrdinaryEvent();
        m.message = HELLO_ORDINARY;
        m.commit();
        r.stop();
        List<RecordedEvent> events = fromRecording(r);
        System.out.println(events);
        if (events.size() == 0) {
            throw new RuntimeException("Expected two events");
        }
        assertOrdinaryEvent(events);
        assertPeriodicEvent(events);
        assertMetadata(events);
        System.out.println("Test passed.");
    }

    private static void assertMetadata(List<RecordedEvent> events) {
        for (RecordedEvent e : events) {
            EventType type = e.getEventType();
            ModularizedAnnotation maType = type.getAnnotation(ModularizedAnnotation.class);
            if (maType == null) {
                fail("Missing @ModularizedAnnotation on type " + type);
            }
            assertEquals(maType.value(), "hello type");
            assertMetaAnnotation(type.getAnnotationElements());

            ValueDescriptor messageField = type.getField("message");
            ModularizedAnnotation maField = messageField.getAnnotation(ModularizedAnnotation.class);
            if (maField == null) {
                fail("Missing @ModularizedAnnotation on field in " + type);
            }
            assertEquals(maField.value(), "hello field");
            assertMetaAnnotation(messageField.getAnnotationElements());
        }
    }

    private static void assertMetaAnnotation(List<AnnotationElement> aes) {
        assertEquals(aes.size(), 1, "@ModularizedAnnotation should only have one meta-annotation");
        AnnotationElement ae = aes.get(0);
        assertEquals(ae.getTypeName(), ModularizedAnnotation.class.getName(), "Incorrect meta-annotation");
    }

    private static void assertPeriodicEvent(List<RecordedEvent> events) {
        for (RecordedEvent e : events) {
            String message = e.getValue("message");
            if (message.equals(HELLO_ORDINARY)) {
                return;
            }
        }
        throw new RuntimeException("Could not find ordinary event in recording");
    }

    private static void assertOrdinaryEvent(List<RecordedEvent> events) {
        for (RecordedEvent e : events) {
            String message = e.getValue("message");
            if (message.equals(HELLO_PERIODIC)) {
                return;
            }
        }
        throw new RuntimeException("Could not find periodic event in recording");
    }

    public static List<RecordedEvent> fromRecording(Recording recording) throws IOException {
        return RecordingFile.readAllEvents(makeCopy(recording));
    }

    private static Path makeCopy(Recording recording) throws IOException {
        Path p = recording.getDestination();
        if (p == null) {
            File directory = new File(".");
            ProcessHandle h = ProcessHandle.current();
            p = new File(directory.getAbsolutePath(), "recording-" + recording.getId() + "-pid" + h.pid() + ".jfr").toPath();
            recording.dump(p);
        }
        return p;
    }

    private static void assertEquals(Object lhs, Object rhs) {
        assertEquals(lhs, rhs, null);
    }

    private static void assertEquals(Object lhs, Object rhs, String msg) {
        if ((lhs != rhs) && ((lhs == null) || !(lhs.equals(rhs)))) {
            msg = Objects.toString(msg, "assertEquals")
                    + ": expected " + Objects.toString(lhs)
                    + " to equal " + Objects.toString(rhs);
            fail(msg);
        }
    }

    private static void fail(String message) {
        throw new RuntimeException(message);
    }
}
