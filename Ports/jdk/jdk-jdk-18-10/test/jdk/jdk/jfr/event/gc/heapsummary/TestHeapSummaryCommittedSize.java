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

package jdk.jfr.event.gc.heapsummary;

import java.time.Duration;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.test.lib.Asserts;
import jdk.test.lib.jfr.EventNames;
import jdk.test.lib.jfr.EventVerifier;
import jdk.test.lib.jfr.Events;
import jdk.test.lib.jfr.GCHelper;
import sun.hotspot.WhiteBox;

/**
 * @test
 * @key jfr
 * @requires vm.hasJFR
 * @requires vm.gc == "Parallel" | vm.gc == null
 * @library /test/lib /test/jdk
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -XX:+UnlockExperimentalVMOptions
                     -XX:-UseFastUnorderedTimeStamps -Xmx16m -XX:+UseParallelGC
                     -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
                     jdk.jfr.event.gc.heapsummary.TestHeapSummaryCommittedSize
 */
public class TestHeapSummaryCommittedSize {
    private final static String EVENT_NAME = EventNames.GCHeapSummary;

    public static void main(String[] args) throws Exception {
        Recording recording = new Recording();
        recording.enable(EVENT_NAME).withThreshold(Duration.ofMillis(0));

        recording.start();
        System.gc();
        recording.stop();

        boolean isAnyFound = false;
        for (RecordedEvent event : Events.fromRecording(recording)) {
            System.out.println("Event: " + event);
            if (!Events.isEventType(event, EVENT_NAME)) {
                continue;
            }
            isAnyFound = true;
            CommittedHeapSizeVerifier verifier = new CommittedHeapSizeVerifier(event);
            verifier.verify();
        }
        Asserts.assertTrue(isAnyFound, "No matching event");
    }
}

class CommittedHeapSizeVerifier extends EventVerifier {
    private final static long  MAX_UNALIGNED_COMMITTED_SIZE  = 16 * 1024 * 1024;
    private final long  MAX_ALIGNED_COMMITTED_SIZE;

    public CommittedHeapSizeVerifier(RecordedEvent event) {
        super(event);
        WhiteBox wb = WhiteBox.getWhiteBox();
        long heapAlignment = wb.getHeapAlignment();
        MAX_ALIGNED_COMMITTED_SIZE = GCHelper.alignUp(
                MAX_UNALIGNED_COMMITTED_SIZE,heapAlignment);
    }

    public void verify() throws Exception {
        Events.assertField(event, "heapSpace.committedSize").atLeast(0L).atMost(MAX_ALIGNED_COMMITTED_SIZE);
    }
}
