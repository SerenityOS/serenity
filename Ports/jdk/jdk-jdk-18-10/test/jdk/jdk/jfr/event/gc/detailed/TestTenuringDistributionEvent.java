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
package jdk.jfr.event.gc.detailed;

import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.test.lib.Asserts;
import jdk.test.lib.jfr.EventNames;
import jdk.test.lib.jfr.Events;

/**
 * @test
 * @bug 8009538
 * @requires vm.hasJFR
 * @requires vm.gc == "G1" | vm.gc == null
 * @key jfr
 * @library /test/lib /test/jdk
 * @run main/othervm -XX:NewSize=2m -XX:MaxNewSize=2m -Xmx32m -XX:+UseG1GC -XX:+NeverTenure jdk.jfr.event.gc.detailed.TestTenuringDistributionEvent
 */

public class TestTenuringDistributionEvent {
    private final static String EVENT_NAME = EventNames.TenuringDistribution;

    public static void main(String[] args) throws Exception {
        Recording recording = null;
        try {
            recording = new Recording();
            // activate the event we are interested in and start recording
            recording.enable(EVENT_NAME).withThreshold(Duration.ofMillis(0));
            recording.start();

            // Setting NewSize and MaxNewSize will limit eden, so
            // allocating 1024 20k byte arrays should trigger at
            // least a few Young GCs.
            byte[][] array = new byte[1024][];
            for (int i = 0; i < array.length; i++) {
                array[i] = new byte[20 * 1024];
            }
            recording.stop();

            // Verify recording
            List<RecordedEvent> events = Events.fromRecording(recording);
            Asserts.assertFalse(events.isEmpty(), "No events found");
            for (RecordedEvent event : events) {
                Events.assertField(event, "gcId").notEqual(-1);
                Events.assertField(event, "age").notEqual(0);
                Events.assertField(event, "size").atLeast(0L);
            }

        } catch (Throwable t) {
            if (recording != null) {
                recording.dump(Paths.get("TestTenuringDistribution.jfr"));
            }
            throw t;
        } finally {
            if (recording != null) {
                recording.close();
            }
        }
    }
}
