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

package jdk.jfr.jmx;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import jdk.jfr.FlightRecorder;
import jdk.jfr.Recording;
import jdk.jfr.RecordingState;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;
import jdk.management.jfr.FlightRecorderMXBean;
import jdk.test.lib.Asserts;
import jdk.test.lib.jfr.SimpleEventHelper;

/**
 * @test
 * @key jfr
 * @summary Copy a recording to file while it is running.
 * @requires vm.hasJFR
 * @library /test/lib /test/jdk
 * @run main/othervm jdk.jfr.jmx.TestCopyToRunning
 */
public class TestCopyToRunning {
    public static void main(String[] args) throws Exception {
        FlightRecorderMXBean bean = JmxHelper.getFlighteRecorderMXBean();

        long recId = bean.newRecording();
        bean.startRecording(recId);
        SimpleEventHelper.createEvent(1);

        Path path = Paths.get(".", "my.jfr");
        bean.copyTo(recId, path.toString());

        List<RecordedEvent> events = RecordingFile.readAllEvents(path);
        Asserts.assertTrue(events.iterator().hasNext(), "No events found");
        for (RecordedEvent event : events) {
            System.out.println("Event:" + event);
        }

        Recording recording = getRecording(recId);
        Asserts.assertEquals(recording.getState(), RecordingState.RUNNING, "Recording not in state running");
        bean.stopRecording(recId);
        Asserts.assertEquals(recording.getState(), RecordingState.STOPPED, "Recording not in state stopped");
        bean.closeRecording(recId);
        Asserts.assertEquals(recording.getState(), RecordingState.CLOSED, "Recording not in state closed");
    }

    private static Recording getRecording(long recId) {
        for (Recording r : FlightRecorder.getFlightRecorder().getRecordings()) {
            if (r.getId() == recId) {
                return r;
            }
        }
        Asserts.fail("Could not find recording with id " + recId);
        return null;
    }
}
