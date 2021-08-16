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

package jdk.jfr.api.recording.destination;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;
import jdk.test.lib.Asserts;
import jdk.test.lib.jfr.Events;
import jdk.test.lib.jfr.SimpleEventHelper;

/**
 * @test
 * @summary Test setDestination with concurrent recordings
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm -Xlog:jfr=trace jdk.jfr.api.recording.destination.TestDestMultiple
 */
public class TestDestMultiple {

    public static void main(String[] args) throws Throwable {
        Recording rA = new Recording();
        Recording rB = new Recording();

        Path destA = Paths.get(".", "recA.jfr");
        Path destB = Paths.get(".", "recB.jfr");
        rA.setDestination(destA);
        rB.setDestination(destB);

        // Enable event in one recording and disable in the other.
        // Both recordings should still get the events, since we always
        // get the "union" of all settings.
        SimpleEventHelper.enable(rA, true);
        SimpleEventHelper.enable(rB, false);

        SimpleEventHelper.createEvent(0); // To no recording

        rA.start();
        SimpleEventHelper.createEvent(1); // Only to recA

        rB.start();
        SimpleEventHelper.createEvent(2); // To both recordings

        rA.stop();

        // This event will not be in recB.
        // The reason is that recA has stopped so event is no longer enabled.
        SimpleEventHelper.createEvent(3);

        // Enable the event and create a new event for recB
        SimpleEventHelper.enable(rB, true);
        SimpleEventHelper.createEvent(4);

        rB.stop();
        SimpleEventHelper.createEvent(5); // To no recording

        rB.close();
        rA.close();

        verifyRecording(destA, 1, 2);
        verifyRecording(destB, 2, 4);
    }

    private static void verifyRecording(Path path, int... ids) throws Exception {
        Asserts.assertTrue(Files.exists(path), "Recording file does not exist: " + path);
        int countEvent = 0;
        List<RecordedEvent> events = RecordingFile.readAllEvents(path);
        for (RecordedEvent event : events) {
            int id = Events.assertField(event, "id").getValue();
            System.out.printf("Recording '%s' id=%d%n", path, id);
        }
        for (RecordedEvent event : events) {
            int id = Events.assertField(event, "id").getValue();
            System.out.printf("Recording '%s' id=%d%n", path, id);
            Asserts.assertTrue(ids.length > countEvent, "Found extra event");
            Events.assertField(event, "id").equal(ids[countEvent]);
            ++countEvent;
        }
        // We expect exactly 4 events in each file. 2 events * 2 chunks
        Asserts.assertEquals(countEvent, ids.length, "Found too few events");
    }
}
