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

package jdk.jfr.jvm;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jdk.jfr.Event;
import jdk.jfr.EventFactory;
import jdk.jfr.Recording;
import jdk.jfr.ValueDescriptor;
import jdk.jfr.consumer.RecordedEvent;
import jdk.test.lib.Asserts;
import jdk.test.lib.jfr.EventTypePrototype;
import jdk.test.lib.jfr.Events;
import jdk.test.lib.jfr.Stressor;

/**
 * @test TestLargeJavaEvent512k
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @modules jdk.jfr/jdk.jfr.internal
 *    java.base/jdk.internal.org.objectweb.asm
 * @run main/othervm jdk.jfr.jvm.TestLargeJavaEvent512k
 */
public class TestLargeJavaEvent512k {
    static boolean error;
    static void setError() {
        error = true;
    }
    static boolean hasError() {
        return error;
    }

    public static void main(String... args) throws Exception {
        final String name = "MyLargeJavaEvent512k"; // name of synthetically generated event
        final String fieldNamePrefix = "myfield";
        final int numberOfFields = 64; // 64*8k = 512k event size
        final Map<String, Object> eventMap = new HashMap<>();
        final int numberOfThreads = 10; // 10 threads will run the test
        final int numberOfEventsPerThread = 50; // each thread will generate 50 events

        List<ValueDescriptor> fields = new ArrayList<>();
        for (int i = 0; i < numberOfFields; ++i) {
            String fieldName = fieldNamePrefix + i;
            eventMap.put(fieldName, largeString());
            fields.add(new ValueDescriptor(String.class, fieldName));
        }

        EventTypePrototype prototype = new EventTypePrototype(name,Collections.emptyList(),  fields);

        EventFactory ef = EventFactory.create(prototype.getAnnotations(), prototype.getFields());

        Recording r = new Recording();
        r.enable(prototype.getName()).withThreshold(Duration.ofNanos(0)).withoutStackTrace();
        r.start();

        Thread.UncaughtExceptionHandler eh = (t, e) -> TestLargeJavaEvent512k.setError();

        Stressor.execute(numberOfThreads, eh, () -> {
            for (int n = 0; n < numberOfEventsPerThread; ++n) {
                try {
                    Event event = ef.newEvent();
                    setEventValues(event, ef, prototype, eventMap);
                    event.commit();
                    Thread.sleep(1);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
        r.stop();
        try {
            if (hasError()) {
                throw new RuntimeException("One (or several) of the threads had an exception/error, test failed");
            }
            verifyEvents(r, numberOfThreads, numberOfEventsPerThread, eventMap);
        } finally {
            r.close();
        }
    }

    private static void verifyEvents(Recording r, int numberOfThreads, int iterations, Map<String, Object> fields) throws IOException {
        List<RecordedEvent> recordedEvents = Events.fromRecording(r);
        Events.hasEvents(recordedEvents);
        int eventCount = 0;
        for (RecordedEvent re : recordedEvents) {
            verify(re, fields);
            eventCount++;
        }
        System.out.println("Number of expected events: " + numberOfThreads * iterations);
        System.out.println("Number of events found: " + eventCount);
        Asserts.assertEquals(numberOfThreads * iterations, eventCount, "Unexpected number of events");
    }

    // each row is 64 chars for 128 rows == 8192 chars
    private static String largeString() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 128; ++i) {
            builder.append("cygwinapacygwinapacygwinapacygwinapacygwinapacygwinapacygwinapa ");
        }
        return builder.toString();
    }

    private static void setEventValues(Event event, EventFactory f, EventTypePrototype prototype, Map<String, Object> fields) {
        for (Map.Entry<String, Object> entry : fields.entrySet()) {
            int index = prototype.getFieldIndex(entry.getKey());
            event.set(index, entry.getValue());
        }
    }

    private static void verify(RecordedEvent event, Map<String, Object> fields) {
        for (Map.Entry<String, Object> entry : fields.entrySet()) {
            String fieldName = entry.getKey();
            Object value = event.getValue(fieldName);
            Object expected = fields.get(fieldName);
            Asserts.assertEQ(value, expected);
        }
    }
}
