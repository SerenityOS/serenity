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

package jdk.jfr.api.recording.event;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.test.lib.Asserts;
import jdk.test.lib.jfr.EventNames;
import jdk.test.lib.jfr.Events;

/**
 * @test
 * @summary Enable/disable event by name during recording.
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.api.recording.event.TestReEnableName
 */
public class TestReEnableName {

    public static void main(String[] args) throws Throwable {
        testRecordingWithEnabledEvent(EventNames.FileWrite, false);
        testRecordingWithEnabledEvent(EventNames.FileWrite, true);
    }

    // Loop and enable/disable events. Create events each loop.
    // Verify we only get events created during enabled.
    private static void testRecordingWithEnabledEvent(String eventname, boolean enabled) throws Exception {
        System.out.println("Testing enabled=" + enabled + " for " + eventname);
        final Path pathDisabled = Paths.get(".", "disabled.txt").toAbsolutePath();
        final Path pathEnabled = Paths.get(".", "enabled.txt").toAbsolutePath();

        Recording r = new Recording();
        if (enabled) {
            r.enable(eventname).withoutThreshold().withoutStackTrace();
        } else {
            r.disable(eventname).withoutThreshold().withoutStackTrace();
        }
        r.start();

        // We should only get events for pathEnabled, not for pathDisabled.
        int countExpectedEvents = 0;
        for (int i = 0; i < 10; ++i) {
            if (enabled) {
                Files.write(pathEnabled, "E".getBytes());
                ++countExpectedEvents;
                r.disable(eventname);
            } else {
                Files.write(pathDisabled, "D".getBytes());
                r.enable(eventname);
            }
            enabled = !enabled;
        }
        r.stop();

        int countFoundEvents = 0;
        for (RecordedEvent event : Events.fromRecording(r)) {
            System.out.printf("Event %s%n", event);
            Asserts.assertEquals(eventname, event.getEventType().getName(), "Wrong event type");
            String path = Events.assertField(event, "path").getValue();
            System.out.println(path);
            if (pathEnabled.toString().equals(path)) {
                ++countFoundEvents;
            }
        }
        Asserts.assertGreaterThanOrEqual(countFoundEvents, countExpectedEvents, "Too few events found");

        r.close();
    }
}
