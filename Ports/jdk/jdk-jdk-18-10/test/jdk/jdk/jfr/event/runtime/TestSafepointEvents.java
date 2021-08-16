/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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
package jdk.jfr.event.runtime;

import static jdk.test.lib.Asserts.assertTrue;

import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;

import jdk.test.lib.Asserts;
import jdk.test.lib.jfr.EventNames;
import jdk.test.lib.jfr.Events;
import sun.hotspot.WhiteBox;

/**
 * @test TestSafepointEvents
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:.
 *                   -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *                   jdk.jfr.event.runtime.TestSafepointEvents
 */
public class TestSafepointEvents {

    static final String[] EVENT_NAMES = new String[] {
        EventNames.SafepointBegin,
        EventNames.SafepointStateSynchronization,
        EventNames.SafepointCleanup,
        EventNames.SafepointCleanupTask,
        EventNames.SafepointEnd
    };

    public static void main(String[] args) throws Exception {
        Recording recording = new Recording();
        for (String name : EVENT_NAMES) {
            recording.enable(name).withThreshold(Duration.ofMillis(0));
        }
        recording.start();
        WhiteBox.getWhiteBox().forceSafepoint();
        recording.stop();

        try {
            // Verify that each event type was seen at least once
            for (String name : EVENT_NAMES) {
                boolean found = false;
                for (RecordedEvent event : Events.fromRecording(recording)) {
                    found = event.getEventType().getName().equals(name);
                    if (found) {
                        break;
                    }
                }
                assertTrue(found, "Expected event from test [" + name + "]");
            }

            // Collect all events grouped by safepoint id
            SortedMap<Long, Set<String>> safepointIds = new TreeMap<>();
            for (RecordedEvent event : Events.fromRecording(recording)) {
                Long safepointId = event.getValue("safepointId");
                if (!safepointIds.containsKey(safepointId)) {
                    safepointIds.put(safepointId, new HashSet<>());
                }
                safepointIds.get(safepointId).add(event.getEventType().getName());
            }

            // The last safepoint may be related to stopping the recording and can thus be
            // incomplete - so if there is more than one, ignore the last one
            if (safepointIds.size() > 1) {
                safepointIds.remove(safepointIds.lastKey());
            }
            Asserts.assertGreaterThanOrEqual(safepointIds.size(), 1, "At least 1 safepoint must have occured");

            // Verify that each safepoint id has an occurence of every event type,
            // this ensures that all events related to a given safepoint had the same id
            for (Set<String> safepointEvents : safepointIds.values()) {
                for (String name : EVENT_NAMES) {
                    assertTrue(safepointEvents.contains(name), "Expected event '" + name + "' to be present");
                }
            }
        } catch (Throwable e) {
            recording.dump(Paths.get("failed.jfr"));
            throw e;
        } finally {
            recording.close();
        }
    }
}
