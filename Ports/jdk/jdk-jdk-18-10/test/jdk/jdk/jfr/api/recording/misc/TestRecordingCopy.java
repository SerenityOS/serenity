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
package jdk.jfr.api.recording.misc;

import jdk.jfr.Recording;
import jdk.jfr.RecordingState;
import jdk.jfr.consumer.RecordedEvent;
import jdk.test.lib.Asserts;
import jdk.test.lib.jfr.Events;
import jdk.test.lib.jfr.SimpleEvent;

import java.util.List;

/**
 * @test
 * @summary A simple test for Recording.copy()
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.api.recording.misc.TestRecordingCopy
 */
public class TestRecordingCopy {

    private static final int EVENT_ID = 1001;

    public static void main(String[] args) throws Exception {

        Recording original = new Recording();
        original.enable(SimpleEvent.class);

        Recording newCopy = original.copy(false);
        Asserts.assertEquals(newCopy.getState(), RecordingState.NEW);

        Recording newStoppedCopy = original.copy(true);
        Asserts.assertEquals(newStoppedCopy.getState(), RecordingState.NEW);

        original.start();

        SimpleEvent ev = new SimpleEvent();
        ev.id = EVENT_ID;
        ev.commit();

        // Verify a stopped copy
        Recording stoppedCopy = original.copy(true);
        Asserts.assertEquals(stoppedCopy.getState(), RecordingState.STOPPED);
        assertCopy(stoppedCopy, original);

        // Verify a running copy
        Recording runningCopy = original.copy(false);
        Asserts.assertEquals(runningCopy.getState(), RecordingState.RUNNING);
        assertCopy(runningCopy, original);

        original.stop();

        // Verify a stopped copy of a stopped
        stoppedCopy = original.copy(true);
        Asserts.assertEquals(stoppedCopy.getState(), RecordingState.STOPPED);
        assertCopy(stoppedCopy, original);

        // Clean-up
        original.close();
        runningCopy.stop();
        runningCopy.close();
        stoppedCopy.close();
    }

    /**
     * Verifies the basic assertions about a copied record
     */
    private static void assertCopy(Recording recording, Recording original) throws Exception {

        Asserts.assertFalse(recording.getId() == original.getId(), "id of a copied record should differ from that of the original");
        Asserts.assertFalse(recording.getName().equals(original.getName()), "name of a copied record should differ from that of the original");

        Asserts.assertEquals(recording.getSettings(), original.getSettings());
        Asserts.assertEquals(recording.getStartTime(), original.getStartTime());
        Asserts.assertEquals(recording.getMaxSize(), original.getMaxSize());
        Asserts.assertEquals(recording.getMaxAge(), original.getMaxAge());
        Asserts.assertEquals(recording.isToDisk(), original.isToDisk());
        Asserts.assertEquals(recording.getDumpOnExit(), original.getDumpOnExit());

        List<RecordedEvent> recordedEvents = Events.fromRecording(recording);
        Events.hasEvents(recordedEvents);
        Asserts.assertEquals(1, recordedEvents.size(), "Expected exactly one event");

        RecordedEvent re = recordedEvents.get(0);
        Asserts.assertEquals(EVENT_ID, re.getValue("id"));
    }

}
