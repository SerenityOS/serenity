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

package jdk.jfr.event.gc.objectcount;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.test.lib.Asserts;
import jdk.test.lib.jfr.EventNames;
import jdk.test.lib.jfr.Events;

/**
 * @test
 * @key jfr
 * @requires vm.hasJFR
 * @requires vm.gc == "Serial" | vm.gc == null
 * @library /test/lib /test/jdk
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:-UseFastUnorderedTimeStamps -XX:+UseSerialGC -XX:-UseCompressedOops -XX:-UseCompressedClassPointers -XX:MarkSweepDeadRatio=0 -XX:+IgnoreUnrecognizedVMOptions jdk.jfr.event.gc.objectcount.TestObjectCountEvent
 */
public class TestObjectCountEvent {
    private static final String objectCountEventPath = EventNames.ObjectCount;
    private static final String heapSummaryEventPath = EventNames.GCHeapSummary;

    public static void main(String[] args) throws Exception {
        Recording recording = new Recording();
        recording.enable(objectCountEventPath);
        recording.enable(heapSummaryEventPath);

        ObjectCountEventVerifier.createTestData();
        System.gc();
        recording.start();
        recording.stop();

        List<RecordedEvent> events = Events.fromRecording(recording);
        for (RecordedEvent event : events) {
            System.out.println("Event: " + event);
        }

        Optional<RecordedEvent> heapSummaryEvent = events.stream()
                                .filter(e -> Events.isEventType(e, heapSummaryEventPath))
                                .filter(e -> "After GC".equals(Events.assertField(e, "when").getValue()))
                                .findFirst();
        Asserts.assertTrue(heapSummaryEvent.isPresent(), "No heapSummary with cause='After GC'");
        System.out.println("Found heapSummaryEvent: " + heapSummaryEvent.get());
        Events.assertField(heapSummaryEvent.get(), "heapUsed").atLeast(0L).getValue();
        int gcId = Events.assertField(heapSummaryEvent.get(), "gcId").getValue();

        List<RecordedEvent> objCountEvents = events.stream()
                                .filter(e -> Events.isEventType(e, objectCountEventPath))
                                .filter(e -> isGcId(e, gcId))
                                .collect(Collectors.toList());
        Asserts.assertFalse(objCountEvents.isEmpty(), "No objCountEvents for gcId=" + gcId);
        ObjectCountEventVerifier.verify(objCountEvents);
    }

    private static boolean isGcId(RecordedEvent event, int gcId) {
        return gcId == (int)Events.assertField(event, "gcId").getValue();
    }

}
