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


import static jdk.test.lib.Asserts.assertEquals;

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
 * @summary Basic test for setDestination with disk=false
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.api.recording.destination.TestDestToDiskFalse
 */
public class TestDestToDiskFalse {

    public static void main(String[] args) throws Throwable {
        final Path dest = Paths.get(".", "my.jfr");
        Recording r = new Recording();
        SimpleEventHelper.enable(r, true);
        r.setToDisk(false);
        r.setDestination(dest);
        Asserts.assertEquals(dest, r.getDestination(), "Wrong get/set destination");
        r.start();
        SimpleEventHelper.createEvent(0);
        r.stop();

        // setToDisk(false) should not effect setDestination.
        // We should still get a file when the recording stops.
        Asserts.assertTrue(Files.exists(dest), "No recording file: " + dest);
        System.out.printf("File size=%d, getSize()=%d%n", Files.size(dest), r.getSize());
        Asserts.assertNotEquals(Files.size(dest), 0L, "File length 0. Should at least be some bytes");

        List<RecordedEvent> events = RecordingFile.readAllEvents(dest);
        Asserts.assertFalse(events.isEmpty(), "No event found");
        System.out.printf("Found event %s%n", events.get(0).getEventType().getName());

        assertEquals(r.getSize(), 0L, "getSize() should return 0, chunks should have been released at stop");
        // getDestination() should return null after recording have been written to file.
        Asserts.assertNull(r.getDestination(), "getDestination() should return null after file created");

        r.close();
    }

}
