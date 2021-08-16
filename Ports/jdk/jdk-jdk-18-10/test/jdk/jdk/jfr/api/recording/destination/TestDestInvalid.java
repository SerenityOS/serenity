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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;
import jdk.test.lib.Asserts;
import jdk.test.lib.jfr.CommonHelper;
import jdk.test.lib.jfr.EventNames;
import jdk.test.lib.jfr.VoidFunction;

/**
 * @test
 * @summary Test setDestination to invalid paths
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.api.recording.destination.TestDestInvalid
 */
public class TestDestInvalid {

    public static void main(String[] args) throws Throwable {
        Recording r = new Recording();
        r.enable(EventNames.OSInformation);
        r.setToDisk(true);

        Asserts.assertNull(r.getDestination(), "dest not null by default");

        // Set destination to empty path (same as curr dir, not a file)
        verifyException(()->{r.setDestination(Paths.get(""));}, "No exception for setDestination(\"\")", IOException.class);
        System.out.println("1 destination: " + r.getDestination());
        Asserts.assertNull(r.getDestination(), "default dest not null after failed setDest");

        // Set dest to a valid path. This should be kept when a new setDest fails.
        Path dest = Paths.get(".", "my.jfr");
        r.setDestination(dest);
        System.out.println("2 destination: " + r.getDestination());
        Asserts.assertEquals(dest, r.getDestination(), "Wrong get/set dest");

        // Null is allowed for setDestination()
        r.setDestination(null);
        System.out.println("3 destination: " + r.getDestination());
        Asserts.assertNull(r.getDestination(), "dest not null after setDest(null)");

        // Reset dest to correct value and make ssure it is not overwritten
        r.setDestination(dest);
        System.out.println("4 destination: " + r.getDestination());
        Asserts.assertEquals(dest, r.getDestination(), "Wrong get/set dest");

        // Set destination to an existing dir. Old dest is saved.
        verifyException(()->{r.setDestination(Paths.get("."));}, "No exception for setDestination(.)", IOException.class);
        System.out.println("5 destination: " + r.getDestination());
        Asserts.assertEquals(dest, r.getDestination(), "Wrong get/set dest");

        // Set destination to a non-existing dir. Old dest is saved.
        verifyException(()->{r.setDestination(Paths.get(".", "missingdir", "my.jfr"));}, "No exception for setDestination(dirNotExists)", IOException.class);
        System.out.println("6 destination: " + r.getDestination());
        Asserts.assertEquals(dest, r.getDestination(), "Wrong get/set dest");

        // Verify that it works with the old setDest value.
        r.start();
        r.stop();
        r.close();
        Asserts.assertTrue(Files.exists(dest), "No recording file: " + dest);
        List<RecordedEvent> events = RecordingFile.readAllEvents(dest);
        Asserts.assertFalse(events.isEmpty(), "No event found");
        System.out.printf("Found event %s in %s%n", events.get(0).getEventType().getName(), dest.toString());
    }

    private static void verifyException(VoidFunction f, String msg, Class<?> exceptionClass) throws Throwable {
        CommonHelper.verifyException(f, msg, IOException.class);
    }

}
