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
import jdk.test.lib.jfr.SimpleEventHelper;

/**
 * @test
 * @summary Call setDestination() when recording in different states
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.api.recording.destination.TestDestState
 */
public class TestDestState {

    public static void main(String[] args) throws Throwable {
        Recording r = new Recording();
        SimpleEventHelper.enable(r, true);

        final Path newDest = Paths.get(".", "new.jfr");
        r.setDestination(newDest);
        System.out.println("new dest: " + r.getDestination());
        Asserts.assertEquals(newDest, r.getDestination(), "Wrong get/set dest when new");

        r.start();
        SimpleEventHelper.createEvent(0);
        Thread.sleep(100);
        final Path runningDest = Paths.get(".", "running.jfr");
        r.setDestination(runningDest);
        System.out.println("running dest: " + r.getDestination());
        Asserts.assertEquals(runningDest, r.getDestination(), "Wrong get/set dest when running");
        SimpleEventHelper.createEvent(1);

        r.stop();
        SimpleEventHelper.createEvent(2);

        // Expect recording to be saved at destination that was set when
        // the recording was stopped, which is runningDest.
        Asserts.assertTrue(Files.exists(runningDest), "No recording file: " + runningDest);
        List<RecordedEvent> events = RecordingFile.readAllEvents(runningDest);
        Asserts.assertFalse(events.isEmpty(), "No event found");
        System.out.printf("Found event %s%n", events.get(0).getEventType().getName());
        r.close();
    }

}
