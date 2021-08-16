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

package jdk.jfr.api.flightrecorder;

import static jdk.test.lib.Asserts.assertEquals;
import static jdk.test.lib.Asserts.assertFalse;
import static jdk.test.lib.Asserts.assertTrue;

import java.util.List;

import jdk.jfr.FlightRecorder;
import jdk.jfr.Recording;

/**
 * @test
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.api.flightrecorder.TestGetRecordings
 */
public class TestGetRecordings {

    public static void main(String[] args) throws Throwable {
        FlightRecorder recorder = FlightRecorder.getFlightRecorder();

        // Recording should be empty at start.
        List<Recording> recordings = recorder.getRecordings();
        assertTrue(recordings.isEmpty(), "recordings should be empty at start");

        // Create first recording
        Recording r1 = new Recording();
        recordings = recorder.getRecordings();
        assertEquals(recordings.size(), 1, "Expected 1 recording");
        assertTrue(recordings.contains(r1), "r1 should be in list");

        // Create second recording
        Recording r2 = new Recording();
        recordings = recorder.getRecordings();
        assertEquals(recordings.size(), 2, "Expected 2 recordings");
        assertTrue(recordings.contains(r2), "r2 should be in list");
        assertTrue(recordings.contains(r1), "r1 should still be in list");

        // Close first recording
        r1.close();
        recordings = recorder.getRecordings();
        assertEquals(recordings.size(), 1, "Expected 1 remaining recording");
        assertTrue(recordings.contains(r2), "r2 should still be in list");
        assertFalse(recordings.contains(r1), "r1 should be removed");

        // Close second recording
        r2.close();
        recordings = recorder.getRecordings();
        assertTrue(recordings.isEmpty(), "recordings should be empty after close");

        // Create recording with new Recording()
        Recording r3 = new Recording();
        recordings = recorder.getRecordings();
        assertTrue(recordings.contains(r3 ), "new Recording() should be in list");
        r3.close();
    }

}
