/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.event.os;

import java.util.List;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.test.lib.jfr.EventNames;
import jdk.test.lib.jfr.Events;


/**
 * @test
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.event.os.TestCPULoad
 */
public class TestCPULoad {
    private final static String EVENT_NAME = EventNames.CPULoad;

    public static void main(String[] args) throws Throwable {
        Recording recording = new Recording();
        recording.enable(EVENT_NAME);
        recording.start();
        // Need to sleep so a time delta can be calculated
        Thread.sleep(100);
        recording.stop();

        List<RecordedEvent> events = Events.fromRecording(recording);
        if (events.isEmpty()) {
            // CPU Load events are unreliable on Windows because
            // the way processes are identified with perf. counters.
            // See BUG 8010378.
            // Workaround is to detect Windows and allow
            // test to pass if events are missing.
            if (isWindows()) {
                return;
            }
            throw new AssertionError("Expected at least one event");
        }
        for (RecordedEvent event : events) {
            System.out.println("Event: " + event);
            for (String loadName : loadNames) {
                Events.assertField(event, loadName).atLeast(0.0f).atMost(1.0f);
            }
        }
    }

    private static final String[] loadNames = {"jvmUser", "jvmSystem", "machineTotal"};

    private static boolean isWindows() {
        return System.getProperty("os.name").startsWith("Windows");
    }
}
