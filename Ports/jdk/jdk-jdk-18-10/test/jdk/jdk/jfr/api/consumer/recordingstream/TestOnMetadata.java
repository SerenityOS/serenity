/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.api.consumer.recordingstream;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import jdk.jfr.Event;
import jdk.jfr.EventType;
import jdk.jfr.FlightRecorder;
import jdk.jfr.Recording;
import jdk.jfr.Registered;
import jdk.jfr.consumer.EventStream;
import jdk.jfr.consumer.MetadataEvent;
import jdk.jfr.consumer.RecordingStream;

/**
 * @test
 * @summary Tests RecordingStream::onMetadata(...)
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib /test/jdk
 * @run main/othervm jdk.jfr.api.consumer.recordingstream.TestOnMetadata
 */
public class TestOnMetadata {

    public static void main(String... args) throws Throwable {
        testDirectoryStream(true, false);
        testFileStream(true, false);

        testAddAfterStart();
        testRemove();
        testNull();
        testUnmodifiable();
    }

    private static void testUnmodifiable() throws Exception {
        AtomicBoolean fail = new AtomicBoolean();
        try (RecordingStream r = new RecordingStream()) {
            r.onMetadata(m -> {
                EventType t = FlightRecorder.getFlightRecorder().getEventTypes().get(0);
                try {
                    m.getEventTypes().add(t);
                    System.out.println("Should not be able to modify getEventTypes()");
                    fail.set(true);
                } catch (UnsupportedOperationException uoe) {
                    // as expected;
                }
                try {
                    m.getRemovedEventTypes().add(t);
                    System.out.println("Should not be able to modify getRemovedEventTypes()");
                    fail.set(true);
                } catch (UnsupportedOperationException uoe) {
                    // as expected;
                }
                try {
                    m.getAddedEventTypes().add(t);
                    System.out.println("Should not be able to modify getAddedEventTypes()");
                    fail.set(true);
                } catch (UnsupportedOperationException uoe) {
                    // as expected;
                }
                r.close();
            });
            r.start();
            r.awaitTermination();
            if (fail.get()) {
                throw new Exception("Metadata event could be mofified");
            }
        }
    }

    private static void testNull() throws Exception {
        try (RecordingStream r = new RecordingStream()) {
            try {
                r.onMetadata(null);
                throw new Exception("Expected NullPointerException");
            } catch (NullPointerException e) {
                // as expected;
            }
        }
    }

    private static void testRemove() throws Exception {
        class RemoveEvent extends Event {
        }
        AtomicBoolean receviedMetadata = new AtomicBoolean();
        try (RecordingStream r = new RecordingStream()) {
            Consumer<MetadataEvent> m = e -> {
                receviedMetadata.set(true);
            };
            r.onMetadata(m);
            r.remove(m);
            r.onEvent(e -> {
                r.close();
            });
            r.remove(m);
            r.startAsync();
            RemoveEvent t = new RemoveEvent();
            t.commit();
            r.awaitTermination();
            if (receviedMetadata.get()) {
                throw new Exception("Unexpected MetadataEvent!");
            }
        }
    }

    private static void testAddAfterStart() throws Exception {
        try (RecordingStream rs = new RecordingStream()) {
            rs.startAsync();
            rs.onMetadata(m -> {
            });
            throw new Exception("Expected exception if handler is added after start");
        } catch (IllegalStateException ise) {
            // as expected
        }
    }

    private static void testFileStream(boolean ordered, boolean reuse) throws Exception {
        class Spider extends Event {

        }
        AtomicInteger counter = new AtomicInteger();
        try (Recording rs = new Recording()) {
            rs.start(); // event 1
            rotateChunk();
            FlightRecorder.register(Spider.class);
            final EventType spiderType = EventType.getEventType(Spider.class);
            // event 2
            rotateChunk();
            FlightRecorder.unregister(Spider.class);
            // event 3
            rs.stop();
            Path p = Paths.get("test-file-stream-jfr");
            rs.dump(p);
            try (EventStream s = EventStream.openFile(p)) {
                System.out.println("Testing file: ordered=" + ordered + " reuse=" + reuse);

                s.setOrdered(ordered);
                s.setReuse(reuse);
                s.onMetadata(e -> {
                    int count = counter.get();
                    if (count == 1) {
                        assertinitialEventypes(e);
                    }
                    if (count == 2) {
                        assertAddedEventType(e, spiderType);
                    }
                    if (count == 3) {
                        assertRemovedEventType(e, spiderType);
                    }
                });
                s.start();
                if (counter.get() > 3) {
                    throw new Exception("Unexpected number of Metadata events");
                }
            }
        }
    }

    private static void testDirectoryStream(boolean ordered, boolean reuse) throws Throwable {
        @Registered(false)
        class Turtle extends Event {
        }

        class AssertEventTypes implements Consumer<MetadataEvent> {
            private final Semaphore semaphore = new Semaphore(0);
            private volatile Throwable exception;
            private volatile Consumer<MetadataEvent> assertMethod;

            @Override
            public void accept(MetadataEvent t) {
                try {
                    assertMethod.accept(t);
                } catch (Throwable e) {
                    this.exception = e;
                    e.printStackTrace();
                }
                semaphore.release();
            }

            public void await() throws Throwable {
                semaphore.acquire();
                if (exception != null) {
                    throw exception;
                }
            }
        }

        try (RecordingStream rs = new RecordingStream()) {
            System.out.println("Testing directory: ordered=" + ordered + " reuse=" + reuse);
            rs.setOrdered(ordered);
            rs.setReuse(reuse);

            AssertEventTypes assertion = new AssertEventTypes();

            // Check initial event types
            assertion.assertMethod = e -> assertinitialEventypes(e);
            rs.onMetadata(assertion);
            rs.startAsync();
            assertion.await();

            // Check added event type
            assertion.assertMethod = e -> assertAddedEventType(e, EventType.getEventType(Turtle.class));
            FlightRecorder.register(Turtle.class);
            final EventType turtleType = EventType.getEventType(Turtle.class);
            assertion.await();

            // Check removal of turtle event
            assertion.assertMethod = e -> assertRemovedEventType(e, turtleType);
            FlightRecorder.unregister(Turtle.class);
            rotateChunk();
            assertion.await();
        }
    }

    private static void assertRemovedEventType(MetadataEvent m, EventType removedType) {
        List<EventType> eventTypes = FlightRecorder.getFlightRecorder().getEventTypes();
        List<EventType> added = m.getAddedEventTypes();
        List<EventType> all = m.getEventTypes();
        List<EventType> removed = m.getRemovedEventTypes();

        assertEventTypes(all, eventTypes);
        assertEventTypes(added, Collections.emptyList());
        assertEventTypes(removed, List.of(removedType));
    }

    private static void assertAddedEventType(MetadataEvent m, EventType addedType) {
        List<EventType> eventTypes = FlightRecorder.getFlightRecorder().getEventTypes();
        List<EventType> added = m.getAddedEventTypes();
        List<EventType> all = m.getEventTypes();
        List<EventType> removed = m.getRemovedEventTypes();

        assertEventTypes(all, eventTypes);
        assertEventTypes(added, List.of(addedType));
        assertEventTypes(removed, Collections.emptyList());
    }

    private static void assertinitialEventypes(MetadataEvent m) {
        List<EventType> added = m.getAddedEventTypes();
        List<EventType> all = m.getEventTypes();
        List<EventType> removed = m.getRemovedEventTypes();
        List<EventType> eventTypes = FlightRecorder.getFlightRecorder().getEventTypes();

        assertEventTypes(all, eventTypes);
        assertEventTypes(added, eventTypes);
        assertEventTypes(removed, Collections.emptyList());
    }

    private static void assertEventTypes(List<EventType> eventTypes, List<EventType> expected) {
        if (eventTypes.size() != expected.size()) {
            fail(eventTypes, expected);
        }
        Set<Long> set = new HashSet<>();
        for (EventType eb : expected) {
            set.add(eb.getId());
        }
        for (EventType ea : eventTypes) {
            if (!set.contains(ea.getId())) {
                fail(eventTypes, expected);
            }
        }
    }

    private static void fail(List<EventType> evenTypes, List<EventType> expected) {
        System.out.println("Event types don't match");
        System.out.println("Expected:");
        for (EventType t : expected) {
            System.out.println(t.getName());
        }
        System.out.println("Got:");
        for (EventType t : expected) {
            System.out.println(t.getName());
        }
        throw new RuntimeException("EventTypes don't match!");
    }

    private static void rotateChunk() {
        try (Recording r = new Recording()) {
            r.start();
        }
    }
}
