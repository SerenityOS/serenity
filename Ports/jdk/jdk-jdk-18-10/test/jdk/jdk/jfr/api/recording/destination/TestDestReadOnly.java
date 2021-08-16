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
import jdk.test.lib.jfr.FileHelper;
import jdk.test.lib.jfr.VoidFunction;

/**
 * @test
 * @summary Test setDestination to read-only dir
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.api.recording.destination.TestDestReadOnly
 */
public class TestDestReadOnly {

    public static void main(String[] args) throws Throwable {
        Path readOnlyDir = FileHelper.createReadOnlyDir(Paths.get(".", "readonly"));
        if (!FileHelper.isReadOnlyPath(readOnlyDir)) {
            System.out.println("Failed to create read-only dir. Running as root?. Test ignored");
            return;
        }

        Path readOnlyDest = Paths.get(readOnlyDir.toString(), "readonly.jfr");
        Recording r = new Recording();
        r.enable(EventNames.OSInformation);
        r.setToDisk(true);
        verifyException(()->{r.setDestination(readOnlyDest);}, "No exception for setDestination to read-only dir");

        System.out.println("r.getDestination() = " + r.getDestination());

        // Verify that it works if we set destination to a writable dir.
        Path dest = Paths.get(".", "my.jfr");
        r.setDestination(dest);
        r.start();
        r.stop();
        r.close();
        Asserts.assertTrue(Files.exists(dest), "No recording file: " + dest);
        List<RecordedEvent> events = RecordingFile.readAllEvents(dest);
        Asserts.assertFalse(events.isEmpty(), "No event found");
        System.out.printf("Found event %s in %s%n", events.get(0).getEventType().getName(), dest.toString());
    }

    private static void verifyException(VoidFunction f, String msg) throws Throwable {
        CommonHelper.verifyException(f, msg, IOException.class);
    }

}
