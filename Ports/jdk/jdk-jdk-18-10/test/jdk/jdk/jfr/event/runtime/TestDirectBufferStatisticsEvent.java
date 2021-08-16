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

package jdk.jfr.event.runtime;

import java.nio.ByteBuffer;
import java.util.List;

import jdk.internal.misc.VM;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.test.lib.Asserts;
import jdk.test.lib.jfr.EventNames;
import jdk.test.lib.jfr.Events;

/**
 * @test
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 * @run main/othervm -XX:MaxDirectMemorySize=128m jdk.jfr.event.runtime.TestDirectBufferStatisticsEvent
 * @run main/othervm jdk.jfr.event.runtime.TestDirectBufferStatisticsEvent
 */
public class TestDirectBufferStatisticsEvent {

    private static final String EVENT_PATH = EventNames.DirectBufferStatistics;

    public static void main(String[] args) throws Throwable {
        try (Recording recording = new Recording()) {
            recording.enable(EVENT_PATH);
            recording.start();
            int rounds = 16;
            int size = 1 * 1024 * 1024; // 1M
            for (int i = 0; i < rounds; i++) {
                ByteBuffer.allocateDirect(size);
            }
            recording.stop();

            List<RecordedEvent> events = Events.fromRecording(recording);
            Events.hasEvents(events);

            long count = 0;
            long totalCapacity = 0;
            long memoryUsed = 0;

            for (RecordedEvent event : events) {
                System.out.println(event);
                Asserts.assertTrue(Events.isEventType(event, EVENT_PATH), "Wrong event type");
                count = Math.max(count, Events.assertField(event, "count").getValue());
                totalCapacity = Math.max(totalCapacity, Events.assertField(event, "totalCapacity").getValue());
                memoryUsed = Math.max(memoryUsed, Events.assertField(event, "memoryUsed").getValue());
                Asserts.assertEquals(VM.maxDirectMemory(), Events.assertField(event, "maxCapacity").getValue());
            }

            Asserts.assertGreaterThanOrEqual(count, (long)rounds, "Too few count in statistics event");
            Asserts.assertGreaterThanOrEqual(totalCapacity, (long)(rounds * size), "Too few totalCapacity in statistics event");
            Asserts.assertGreaterThanOrEqual(memoryUsed, totalCapacity, "Too few memoryUsed in statistics event");
        }
    }
}
