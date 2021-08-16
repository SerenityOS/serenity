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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;
import jdk.test.lib.Asserts;
import jdk.test.lib.jfr.CommonHelper;
import jdk.test.lib.jfr.Events;
import jdk.test.lib.jfr.SimpleEvent;
import jdk.test.lib.jfr.SimpleEventHelper;
import jdk.test.lib.jfr.VoidFunction;

/**
 * @test
 * @summary call copyTo() with recording in all states.
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.api.recording.dump.TestDumpState
 */
public class TestDumpState {

    public static void main(String[] args) throws Throwable {
        Recording r = new Recording();
        SimpleEventHelper.enable(r, true);

        List<Integer> expectedIds = new ArrayList<>();

        SimpleEventHelper.createEvent(0); // Recording not started, should not be included.
        verifyIOException(()->{checkEvents(r, expectedIds);}, "No Exception when dump() not started");

        r.start();
        SimpleEventHelper.createEvent(1);
        expectedIds.add(1);
        checkEvents(r, expectedIds);

        SimpleEventHelper.createEvent(2);
        expectedIds.add(2);
        checkEvents(r, expectedIds);

        r.stop();
        checkEvents(r, expectedIds);

        SimpleEventHelper.createEvent(3); // Recording stopped, should not be included.
        checkEvents(r, expectedIds);

        r.close();
        SimpleEventHelper.createEvent(4);
        verifyIOException(()->{checkEvents(r, expectedIds);}, "No Exception when dump() closed");
    }

    private static int recordingCounter = 0;
    private static void checkEvents(Recording r, List<Integer> expectedIds) throws Exception {
        Path path = Paths.get(".", String.format("%d.jfr", recordingCounter++));
        r.dump(path);
        Asserts.assertTrue(Files.exists(path), "Recording file does not exist: " + path);

        int index = 0;

        for (RecordedEvent event : RecordingFile.readAllEvents(path)) {
            Events.isEventType(event, SimpleEvent.class.getName());
            Integer id = Events.assertField(event, "id").getValue();
            System.out.println("Got event with id " + id);
            Asserts.assertGreaterThan(expectedIds.size(), index, "Too many Events found");
            Asserts.assertEquals(id, expectedIds.get(index), "Wrong id at index " + index);
            ++index;
        }
        Asserts.assertEquals(index, expectedIds.size(), "Too few Events found");
    }

    private static void verifyIOException(VoidFunction f, String msg) throws Throwable {
        CommonHelper.verifyException(f, msg, IOException.class);
    }

}
