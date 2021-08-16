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
import java.util.Iterator;
import java.util.List;

import jdk.jfr.RecordingState;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;
import jdk.management.jfr.FlightRecorderMXBean;
import jdk.management.jfr.RecordingInfo;
import jdk.test.lib.Asserts;
import jdk.test.lib.jfr.EventField;
import jdk.test.lib.jfr.Events;
import jdk.test.lib.jfr.SimpleEventHelper;

/**
 * @test
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib /test/jdk
 * @run main/othervm jdk.jfr.jmx.TestClone
 */
public class TestClone {
    public static void main(String[] args) throws Exception {
        FlightRecorderMXBean bean = JmxHelper.getFlighteRecorderMXBean();

        long orgId = bean.newRecording();
        bean.startRecording(orgId);
        SimpleEventHelper.createEvent(1); // Should be in both org and clone

        long cloneId = bean.cloneRecording(orgId, false);
        Asserts.assertNotEquals(orgId, cloneId, "clone id should not be same as org id");

        List<RecordingInfo> recordings = bean.getRecordings();
        JmxHelper.verifyState(orgId, RecordingState.RUNNING, recordings);
        JmxHelper.verifyState(cloneId, RecordingState.RUNNING, recordings);

        bean.stopRecording(orgId);
        recordings = bean.getRecordings();
        JmxHelper.verifyState(orgId, RecordingState.STOPPED, recordings);
        JmxHelper.verifyState(cloneId, RecordingState.RUNNING, recordings);

        SimpleEventHelper.createEvent(2);  // Should only be in clone

        bean.stopRecording(cloneId);
        recordings = bean.getRecordings();
        JmxHelper.verifyState(orgId, RecordingState.STOPPED, recordings);
        JmxHelper.verifyState(cloneId, RecordingState.STOPPED, recordings);

        Path orgPath = Paths.get(".", "org.jfr");
        Path clonePath = Paths.get(".", "clone.jfr");
        bean.copyTo(orgId, orgPath.toString());
        bean.copyTo(cloneId, clonePath.toString());

        verifyEvents(orgPath, 1);
        verifyEvents(clonePath, 1, 2);

        bean.closeRecording(orgId);
        bean.closeRecording(cloneId);
    }

    private static void verifyEvents(Path path, int... ids) throws Exception {
        List<RecordedEvent> events = RecordingFile.readAllEvents(path);
        Iterator<RecordedEvent> iterator = events.iterator();
        for (int i=0; i<ids.length; i++) {
            Asserts.assertTrue(iterator.hasNext(), "Missing event " + ids[i]);
            EventField idField = Events.assertField(iterator.next(), "id");
            System.out.println("Event.id=" + idField.getValue());
            idField.equal(ids[i]);
        }
        if (iterator.hasNext()) {
            Asserts.fail("Got extra event: " + iterator.next());
        }
    }
}
