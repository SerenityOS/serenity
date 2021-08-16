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

package jdk.jfr.event.gc.collection;

import static jdk.test.lib.Asserts.assertGreaterThan;
import static jdk.test.lib.Asserts.assertTrue;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;
import jdk.test.lib.jfr.AppExecutorHelper;
import jdk.test.lib.jfr.Events;
import jdk.test.lib.process.OutputAnalyzer;


/**
 * Class to verify GarbageCollection event.
 * It starts the application provoking GCGarbageCollection with enabled JFR,
 * collects events and verify the 'name' and 'cause' fields to be expected.
 * It's supposed to be invoked from tests.
 */
public class GCGarbageCollectionUtil {
    private final static String EVENT_SETTINGS_FILE =
            System.getProperty("test.src", ".") + File.separator + "gc-testsettings.jfc";

    /**
     * Verifies the 'name' and 'cause' fields of received events to be expected.
     * @param testID - a string to identify test
     * @param testFlags - VM flags including GC to start the app
     * @param gcNames - expected values for the 'name' field
     * @param gcCauses - expected values for the 'cause' field
     * @throws Exception in case of any failure
     */
    public static void test(String testID, String[] testFlags,
            String[] gcNames, String... gcCauses) throws Exception {

        String jfrFile = testID + ".jfr";

        List<String> summaryFlags = new ArrayList<>();
        Collections.addAll(summaryFlags, testFlags);
        summaryFlags.add("-Xmx100m");
        summaryFlags.add("-XX:+UnlockExperimentalVMOptions");
        summaryFlags.add("-XX:-UseFastUnorderedTimeStamps");
        summaryFlags.add("-Xlog:gc*=debug");


        String args[] = {};
        OutputAnalyzer analyzer = AppExecutorHelper.executeAndRecord(EVENT_SETTINGS_FILE, jfrFile,
                (String[])summaryFlags.toArray(new String[0]), AppGCProvoker.class.getName(), args);
        analyzer.shouldHaveExitValue(0);

        Set<String> gcValidNames = new HashSet<>();
        for (String n: gcNames) {
            gcValidNames.add(n);
        }
        Set<String> gcValidCauses = new HashSet<>();
        for (String n: gcCauses) {
            gcValidCauses.add(n);
        }

        int total = 0;
        for (RecordedEvent event : RecordingFile.readAllEvents(Paths.get(jfrFile))) {
            total++;
            System.out.println("Event: " + event);

            final String name = Events.assertField(event, "name").notEmpty().getValue();
            assertTrue(gcValidNames.contains(name), "GC name '" + name + "' not in the valid list" + gcValidNames);

            final String cause = Events.assertField(event, "cause").notEmpty().getValue();
            assertTrue(gcValidCauses.contains(cause), "GC cause '" + cause + "' not in the valid causes" + gcValidCauses);
        }
        assertGreaterThan(total, 0, "Expected at least one event");
    }
}
