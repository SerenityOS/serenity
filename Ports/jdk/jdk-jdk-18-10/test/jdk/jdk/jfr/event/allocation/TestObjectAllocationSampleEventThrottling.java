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

package jdk.jfr.event.allocation;

import static java.lang.Math.floor;

import java.util.List;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.test.lib.jfr.EventNames;
import jdk.test.lib.jfr.Events;
import jdk.test.lib.Asserts;

/**
 * @test
 * @summary Test that when an object is allocated outside a TLAB an event will be triggered.
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
*  @run main/othervm -XX:+UseTLAB -XX:TLABSize=2k -XX:-ResizeTLAB jdk.jfr.event.allocation.TestObjectAllocationSampleEventThrottling
 */

public class TestObjectAllocationSampleEventThrottling {
    private static final String EVENT_NAME = EventNames.ObjectAllocationSample;

    private static final int BYTE_ARRAY_OVERHEAD = 16; // Extra bytes used by a byte array
    private static final int OBJECT_SIZE = 100 * 1024;
    private static final int OBJECT_SIZE_ALT = OBJECT_SIZE + 8; // Object size in case of disabled CompressedOops
    private static final int OBJECTS_TO_ALLOCATE = 100;
    private static final String BYTE_ARRAY_CLASS_NAME = new byte[0].getClass().getName();
    private static int eventCount;

    // Make sure allocation isn't dead code eliminated.
    public static byte[] tmp;

    public static void main(String[] args) throws Exception {
        testZeroPerSecond();
        testThrottleSettings();
    }

    private static void testZeroPerSecond() throws Exception {
        Recording r1 = new Recording();
        setThrottle(r1, "0/s");
        r1.start();
        allocate();
        r1.stop();
        List<RecordedEvent> events = Events.fromRecording(r1);
        Asserts.assertTrue(events.isEmpty(), "throttle rate 0/s should not emit any events");
    }

    private static void testThrottleSettings() throws Exception {
        Recording r1 = new Recording();
        // 0/s will not emit any events
        setThrottle(r1, "0/s");
        r1.start();
        Recording r2 = new Recording();
        // 1/ns is a *very* high emit rate, it should trump the previous 0/s value
        // to allow the allocation sample events to be recorded.
        setThrottle(r2, "1/ns");
        r2.start();
        allocate();
        r2.stop();
        r1.stop();
        verifyRecording(r2);
        int minCount = (int) floor(OBJECTS_TO_ALLOCATE * 0.80);
        Asserts.assertGreaterThanOrEqual(eventCount, minCount, "Too few object samples allocated");
        List<RecordedEvent> events = Events.fromRecording(r1);
        Asserts.assertFalse(events.isEmpty(), "r1 should also have events");
    }

    private static void setThrottle(Recording recording, String rate) {
        recording.enable(EVENT_NAME).with("throttle", rate);
    }

    private static void allocate() {
        for (int i = 0; i < OBJECTS_TO_ALLOCATE; ++i) {
            tmp = new byte[OBJECT_SIZE - BYTE_ARRAY_OVERHEAD];
        }
    }

    private static void verifyRecording(Recording recording) throws Exception {
        for (RecordedEvent event : Events.fromRecording(recording)) {
            verify(event);
        }
    }

    private static void verify(RecordedEvent event) {
        if (Thread.currentThread().getId() != event.getThread().getJavaThreadId()) {
            return;
        }
        if (Events.assertField(event, "objectClass.name").notEmpty().getValue().equals(BYTE_ARRAY_CLASS_NAME)) {
            Events.assertField(event, "weight").atLeast(1L);
            ++eventCount;
        }
    }
}
