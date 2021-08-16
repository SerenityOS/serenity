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

import java.io.File;
import java.nio.file.Paths;
import java.util.List;

import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;
import jdk.test.lib.Asserts;
import jdk.test.lib.jfr.Events;

public class PromotionFailedEvent {

    private final static String EVENT_SETTINGS_FILE = System.getProperty("test.src", ".") + File.separator + "promotionfailed-testsettings.jfc";
    private final static int BYTES_TO_ALLOCATE = 1024;

    public static void test(String testName, String[] vmFlags) throws Throwable {
        String jfr_file = testName + ".jfr";

        if (!ExecuteOOMApp.execute(EVENT_SETTINGS_FILE, jfr_file, vmFlags, BYTES_TO_ALLOCATE)) {
            System.out.println("OOM happened in the other thread(not test thread). Skip test.");
            // Skip test, process terminates due to the OOME error in the different thread
            return;
        }

        // This test can not always trigger the expected event.
        // Test is ok even if no events found.
        List<RecordedEvent> events = RecordingFile.readAllEvents(Paths.get(jfr_file));
        for (RecordedEvent event : events) {
            System.out.println("Event: " + event);
            long smallestSize = Events.assertField(event, "promotionFailed.smallestSize").atLeast(1L).getValue();
            long firstSize = Events.assertField(event, "promotionFailed.firstSize").atLeast(smallestSize).getValue();
            long totalSize = Events.assertField(event, "promotionFailed.totalSize").atLeast(firstSize).getValue();
            long objectCount = Events.assertField(event, "promotionFailed.objectCount").atLeast(1L).getValue();
            Asserts.assertLessThanOrEqual(smallestSize * objectCount, totalSize, "smallestSize * objectCount <= totalSize");
        }
    }
}
