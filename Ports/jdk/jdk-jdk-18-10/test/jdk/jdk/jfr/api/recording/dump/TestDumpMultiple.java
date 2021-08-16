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

package jdk.jfr.api.recording.dump;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;
import jdk.test.lib.Asserts;
import jdk.test.lib.jfr.Events;
import jdk.test.lib.jfr.SimpleEventHelper;

/**
 * @test
 * @summary Test copyTo and parse file
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.api.recording.dump.TestDumpMultiple
 */
public class TestDumpMultiple {

    public static void main(String[] args) throws Exception {
        Recording rA = new Recording();
        Recording rB = new Recording();

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

        Path pathA = Paths.get(".", "recA.jfr");
        Path pathB = Paths.get(".", "recB.jfr");
        rA.dump(pathA);
        rB.dump(pathB);
        rB.close();
        rA.close();

        verifyRecording(pathA, 1, 2);
        verifyRecording(pathB, 2, 4);
    }

    private static void verifyRecording(Path path, int... ids) throws Exception {
        Asserts.assertTrue(Files.exists(path), "Recording file does not exist: " + path);
        int countEvent = 0;
        for (RecordedEvent event : RecordingFile.readAllEvents(path)) {
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
