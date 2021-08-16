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

package jdk.jfr.event.gc.refstat;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.test.lib.Asserts;
import jdk.test.lib.jfr.EventNames;
import jdk.test.lib.jfr.Events;
import jdk.test.lib.jfr.GCHelper;


public class RefStatEvent {

    private static final String gcEventPath = EventNames.GarbageCollection;
    private static final String refStatsEventPath = EventNames.GCReferenceStatistics;
    public static byte[] garbage;

    public static void test(String gcName) throws Exception {
        Recording recording = new Recording();
        recording.enable(refStatsEventPath).withThreshold(Duration.ofMillis(0));
        recording.enable(gcEventPath).withThreshold(Duration.ofMillis(0));

        recording.start();
        GCHelper.callSystemGc(6, true);
        recording.stop();

        System.out.println("gcName=" + gcName);
        List<RecordedEvent> events = GCHelper.removeFirstAndLastGC(Events.fromRecording(recording));
        Events.hasEvents(events);
        for (RecordedEvent event : events) {
            System.out.println("Event: " + event);
        }

        for (RecordedEvent event : events) {
            if (!Events.isEventType(event, gcEventPath)) {
                continue;
            }
            System.out.println("Event: " + event);
            final int gcId = Events.assertField(event, "gcId").getValue();
            final String name = Events.assertField(event, "name").notEmpty().getValue();
            if (gcName.equals(name)) {
                verifyRefStatEvents(events, gcId);
            }
        }
    }

    // Check that we have a refStat event for each type for this GC.
    private static void verifyRefStatEvents(List<RecordedEvent> events, int gcId) {
        List<String> expectedTypes = Arrays.asList("Soft reference", "Weak reference", "Final reference", "Phantom reference");
        List<String> actualTypes = new ArrayList<>();
        try {
            for (RecordedEvent event : events) {
                if (!Events.isEventType(event, refStatsEventPath)) {
                    continue;
                }
                Events.assertField(event, "count").atLeast(0L);
                if (Events.assertField(event, "gcId").isEqual(gcId)) {
                    actualTypes.add(Events.assertField(event, "type").notEmpty().getValue());
                }
            }

            Asserts.assertEquals(actualTypes.size(), expectedTypes.size(), "Wrong number of refStat events");
            Asserts.assertTrue(expectedTypes.containsAll(actualTypes), "Found unknown refStat types");
            Asserts.assertTrue(actualTypes.containsAll(expectedTypes), "Missning refStat types");
        } catch (Exception e) {
            System.out.println("Expected refStatTypes: " + expectedTypes.stream().collect(Collectors.joining(", ")));
            System.out.println("Got refStatTypes: " + actualTypes.stream().collect(Collectors.joining(", ")));
            throw e;
        }
    }

}
