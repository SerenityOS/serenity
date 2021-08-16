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

package jdk.jfr.jvm;

import java.io.IOException;
import java.util.List;

import jdk.jfr.Event;
import jdk.jfr.Recording;
import jdk.jfr.ValueDescriptor;
import jdk.jfr.consumer.RecordedEvent;
import jdk.test.lib.Asserts;
import jdk.test.lib.jfr.EventNames;
import jdk.test.lib.jfr.Events;

/**
 * @test TestFatEvent
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm -Dprop1=12345678901234567890123456789012345678901234567890
 *      -Dprop2=12345678901234567890123456789012345678901234567890
 *      -Dprop3=12345678901234567890123456789012345678901234567890
 *      jdk.jfr.jvm.TestFatEvent
 */
public class TestFatEvent {

    public static void main(String... args) throws Exception {
        testFatNativeEvent();
        testFatJavaEvent();
    }

    private static void testFatNativeEvent() throws Exception {
        try (Recording r = new Recording()) {
            r.enable(EventNames.JVMInformation).with("period", "everyChunk");
            r.start();
            r.stop();
            List<RecordedEvent> events = Events.fromRecording(r);
            Asserts.assertEquals(2, events.size());
            for (RecordedEvent e : events) {
                String s = e.getString("jvmArguments");
                if (s.length() < 150) {
                    throw new Exception("Expected at least 150 characters");
                }
            }
        }
    }
    private static final Long expected = Long.MAX_VALUE;

    private static void testFatJavaEvent() throws IOException, Exception {
        // This event use more than 127 bytes
        // which requires two bytes in compressed
        // integer format
        class FatEvent extends Event {
            long a = expected;
            long b = expected;
            long c = expected;
            long d = expected;
            long e = expected;
            long f = expected;
            long g = expected;
            long h = expected;
            long i = expected;
            long j = expected;
            long k = expected;
            long l = expected;
            long m = expected;
            long n = expected;
            long o = expected;
            long p = expected;
            long q = expected;
            long r = expected;
            long s = expected;
            long t = expected;
            long u = expected;
            long v = expected;
            long w = expected;
            long x = expected;
            long y = expected;
            long z = expected;
        }
        try (Recording r = new Recording()) {
            r.start();
            int eventCount = 5000; //
            for (int i = 0; i < eventCount; i++) {
                FatEvent event = new FatEvent();
                event.commit();
            }
            r.stop();
            List<RecordedEvent> events = Events.fromRecording(r);
            int count = 0;
            for (RecordedEvent event : events) {
                verifyEvent(event);
                count++;
            }
            if (count != eventCount) {
                throw new Exception("Unexpected event count " + count + ", expected " + eventCount);
            }
        }
    }

    private static void verifyEvent(RecordedEvent e) throws Exception {
        for (ValueDescriptor v : e.getEventType().getFields()) {
            String fieldName = v.getName();
            Object o = e.getValue(v.getName());
            if (fieldName.length() == 1 && !o.equals(expected)) {
                System.out.println("Expected: " + expected);
                System.out.println(e);
                throw new Exception("Unexpected value " + o + " for field " + fieldName);
            }
        }
    }
}
