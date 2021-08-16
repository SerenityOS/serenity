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

package jdk.jfr.event.gc.detailed;

import java.util.List;

import static gc.testlibrary.Allocation.blackHole;
import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.test.lib.jfr.EventNames;
import jdk.test.lib.jfr.Events;

/**
 * @test TestZAllocationStallEvent
 * @requires vm.hasJFR & vm.gc.Z
 * @key jfr
 * @library /test/lib /test/jdk /test/hotspot/jtreg
 * @run main/othervm -XX:+UseZGC -Xmx32M jdk.jfr.event.gc.detailed.TestZAllocationStallEvent
 */

public class TestZAllocationStallEvent {
    public static void main(String[] args) throws Exception {
        try (Recording recording = new Recording()) {
            // Activate the event we are interested in and start recording
            recording.enable(EventNames.ZAllocationStall);
            recording.start();

            // Allocate many large objects quickly, to outrun the GC
            for (int i = 0; i < 100; i++) {
                blackHole(new byte[16 * 1024 * 1024]);
            }

            recording.stop();

            // Verify recording
            List<RecordedEvent> events = Events.fromRecording(recording);
            System.out.println("Events: " + events.size());
            Events.hasEvents(events);
            for (RecordedEvent event : Events.fromRecording(recording)) {
                Events.assertField(event, "size").atLeast(2L * 1024 * 1024);
            }
        }
    }
}
