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

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.test.lib.Asserts;
import jdk.test.lib.jfr.EventNames;
import jdk.test.lib.jfr.Events;

/**
 * @test
 * @key jfr
 * @requires vm.hasJFR
 * @requires vm.gc == "G1" | vm.gc == null
 * @library /test/lib /test/jdk
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:-UseFastUnorderedTimeStamps -XX:G1HeapRegionSize=1m -Xmx64m -Xmn16m -XX:+UseG1GC jdk.jfr.event.gc.detailed.TestEvacuationInfoEvent
 */
public class TestEvacuationInfoEvent {
    private final static String EVENT_INFO_NAME = EventNames.EvacuationInformation;
    private final static String EVENT_FAILED_NAME = EventNames.EvacuationFailed;

    public static void main(String[] args) throws Throwable {
        final long g1HeapRegionSize = 1024 * 1024;
        Recording recording = new Recording();
        recording.enable(EVENT_INFO_NAME).withThreshold(Duration.ofMillis(0));
        recording.enable(EVENT_FAILED_NAME).withThreshold(Duration.ofMillis(0));
        recording.start();
        allocate();
        recording.stop();

        List<RecordedEvent> events = Events.fromRecording(recording);
        Asserts.assertFalse(events.isEmpty(), "No events found");
        for (RecordedEvent event : events) {
            if (!Events.isEventType(event, EVENT_INFO_NAME)) {
                continue;
            }
            System.out.println("Event: " + event);

            int setRegions = Events.assertField(event, "cSetRegions").atLeast(0).getValue();
            long setUsedAfter = Events.assertField(event, "cSetUsedAfter").atLeast(0L).getValue();
            long setUsedBefore = Events.assertField(event, "cSetUsedBefore").atLeast(setUsedAfter).getValue();
            int allocationRegions = Events.assertField(event, "allocationRegions").atLeast(0).getValue();
            long allocRegionsUsedBefore = Events.assertField(event, "allocationRegionsUsedBefore").atLeast(0L).getValue();
            long allocRegionsUsedAfter = Events.assertField(event, "allocationRegionsUsedAfter").atLeast(0L).getValue();
            long bytesCopied = Events.assertField(event, "bytesCopied").atLeast(0L).getValue();
            int regionsFreed = Events.assertField(event, "regionsFreed").atLeast(0).getValue();

            Asserts.assertEquals(allocRegionsUsedBefore + bytesCopied, allocRegionsUsedAfter, "allocRegionsUsedBefore + bytesCopied = allocRegionsUsedAfter");
            Asserts.assertGreaterThanOrEqual(setRegions, regionsFreed, "setRegions >= regionsFreed");
            Asserts.assertGreaterThanOrEqual(g1HeapRegionSize * allocationRegions, allocRegionsUsedAfter, "G1HeapRegionSize * allocationRegions >= allocationRegionsUsedAfter");
            Asserts.assertGreaterThanOrEqual(g1HeapRegionSize * setRegions, setUsedAfter, "G1HeapRegionSize * setRegions >= setUsedAfter");
            Asserts.assertGreaterThanOrEqual(g1HeapRegionSize * setRegions, setUsedBefore, "G1HeapRegionSize * setRegions >= setUsedBefore");
            Asserts.assertGreaterThanOrEqual(g1HeapRegionSize, allocRegionsUsedBefore, "G1HeapRegionSize >= allocRegionsUsedBefore");

            int gcId = Events.assertField(event, "gcId").getValue();
            boolean isEvacuationFailed = containsEvacuationFailed(events, gcId);
            if (isEvacuationFailed) {
                Asserts.assertGreaterThan(setUsedAfter, 0L, "EvacuationFailure -> setUsedAfter > 0");
                Asserts.assertGreaterThan(setRegions, regionsFreed, "EvacuationFailure -> setRegions > regionsFreed");
            } else {
                Asserts.assertEquals(setUsedAfter, 0L, "No EvacuationFailure -> setUsedAfter = 0");
                Asserts.assertEquals(setRegions, regionsFreed, "No EvacuationFailure -> setRegions = regionsFreed");
            }
        }
    }

    private static boolean containsEvacuationFailed(List<RecordedEvent> events, int gcId) {
        Optional<RecordedEvent> failedEvent = events.stream()
                                .filter(e -> Events.isEventType(e, EVENT_FAILED_NAME))
                                .filter(e -> gcId == (int)Events.assertField(e, "gcId").getValue())
                                .findAny();
        System.out.println("Failed event: " + (failedEvent.isPresent() ? failedEvent.get() : "None"));
        return failedEvent.isPresent();
    }

    public static DummyObject[] dummys = new DummyObject[6000];

        /**
         * Allocate memory to trigger garbage collections.
         * We want the allocated objects to have different life time, because we want both "young" and "old" objects.
         * This is done by keeping the objects in an array and step the current index by a small random number in the loop.
         * The loop will continue until we have allocated a fixed number of bytes.
         */
        private static void allocate() {
            Random r = new Random(0);
            long bytesToAllocate = 256 * 1024 * 1024;
            int currPos = 0;
            while (bytesToAllocate > 0) {
                int allocSize = 1000 + (r.nextInt(4000));
                bytesToAllocate -= allocSize;
                dummys[currPos] = new DummyObject(allocSize);

                // Skip a few positions to get different duration on the objects.
                currPos = (currPos + r.nextInt(20)) % dummys.length;
            }
            for (int c=0; c<dummys.length; c++) {
                dummys[c] = null;
            }
            System.gc();
        }

        public static class DummyObject {
            public byte[] payload;
            DummyObject(int size) {
            payload = new byte[size];
        }
    }
}
