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

import java.time.Duration;
import java.time.Instant;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.test.lib.jfr.Events;
import jdk.test.lib.jfr.GCHelper;


public class YoungGarbageCollectionEvent {

    private static final String EVENT_NAME = GCHelper.event_young_garbage_collection;

    public static void test() throws Exception {
        Recording recording = new Recording();
        recording.enable(EVENT_NAME).withThreshold(Duration.ofMillis(0));
        recording.start();
        triggerGC();
        recording.stop();

        boolean isAnyFound = false;
        for (RecordedEvent event : Events.fromRecording(recording)) {
            if (!EVENT_NAME.equals(event.getEventType().getName())) {
                continue;
            }
            System.out.println("Event: " + event);
            isAnyFound = true;
            Events.assertField(event, "gcId").atLeast(0);
            Events.assertField(event, "tenuringThreshold").atLeast(1);

            Instant startTime = event.getStartTime();
            Instant endTime = event.getEndTime();
            Duration duration = event.getDuration();
            assertGreaterThan(startTime, Instant.EPOCH, "startTime should be at least 0");
            assertGreaterThan(endTime, Instant.EPOCH, "endTime should be at least 0");
            // TODO: Maybe we should accept duration of 0, but old test did not.
            assertGreaterThan(duration, Duration.ZERO, "Duration should be above 0");
            assertGreaterThan(endTime, startTime, "End time should be after start time");
        }
        assertTrue(isAnyFound, "No matching event found");
    }

    public static byte[] garbage;
    private static void triggerGC() {
        for (int i = 0; i < 3072; i++) {
            garbage = new byte[1024];
        }
    }
}
