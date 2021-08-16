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

package jdk.jfr.event.gc.detailed;

import java.io.IOException;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;
import jdk.test.lib.Asserts;
import jdk.test.lib.jfr.EventNames;

/**
 * @test
 * @key jfr
 * @requires vm.hasJFR
 *
 * @requires vm.gc == "G1" | vm.gc == null
 * @library /test/lib /test/jdk
 * @run main jdk.jfr.event.gc.detailed.TestG1ConcurrentModeFailureEvent
 */

public class TestG1ConcurrentModeFailureEvent {

    private final static String EVENT_NAME = EventNames.ConcurrentModeFailure;
    private final static String EVENT_SETTINGS_FILE = System.getProperty("test.src", ".") + File.separator + "concurrentmodefailure-testsettings.jfc";
    private final static String JFR_FILE = "TestG1ConcurrentModeFailureEvent.jfr";
    private final static int BYTES_TO_ALLOCATE = 1024 * 512;

    public static void main(String[] args) throws Exception {
        String[] vmFlags = {"-Xmx512m", "-Xms512m", "-XX:MaxTenuringThreshold=0", "-Xlog:gc*=debug:testG1GC.log",
            "-XX:+UseG1GC", "-XX:+UnlockExperimentalVMOptions", "-XX:-UseFastUnorderedTimeStamps"};

        if (!ExecuteOOMApp.execute(EVENT_SETTINGS_FILE, JFR_FILE, vmFlags, BYTES_TO_ALLOCATE)) {
            System.out.println("OOM happened in the other thread(not test thread). Skip test.");
            // Skip test, process terminates due to the OOME error in the different thread
            return;
        }

        Optional<RecordedEvent> event = RecordingFile.readAllEvents(Paths.get(JFR_FILE)).stream().findFirst();
        if (event.isPresent()) {
            Asserts.assertEquals(EVENT_NAME, event.get().getEventType().getName(), "Wrong event type");
        } else {
            // No event received. Check if test did trigger the event.
            boolean isEventTriggered = fileContainsString("testG1GC.log", "concurrent-mark-abort");
            System.out.println("isEventTriggered=" +isEventTriggered);
            Asserts.assertFalse(isEventTriggered, "Event found in log, but not in JFR");
        }
    }

    private static boolean fileContainsString(String filename, String text) throws IOException {
        Path p = Paths.get(filename);
        for (String line : Files.readAllLines(p, Charset.defaultCharset())) {
            if (line.contains(text)) {
                return true;
            }
        }
        return false;
    }
}
