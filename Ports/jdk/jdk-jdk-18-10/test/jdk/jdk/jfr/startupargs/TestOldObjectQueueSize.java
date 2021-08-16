/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.startupargs;

import java.util.ArrayList;
import java.util.List;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.internal.test.WhiteBox;
import jdk.test.lib.jfr.EventNames;
import jdk.test.lib.jfr.Events;

/**
 * @test
 * @summary Test -XX:FlightRecorderOptions:old-object-queue-size
 * @requires vm.hasJFR
 * @modules jdk.jfr/jdk.jfr.internal.test
 * @library /test/lib
 * @key jfr
 *
 * @run main/othervm -XX:TLABSize=2k -XX:FlightRecorderOptions:old-object-queue-size=0 jdk.jfr.startupargs.TestOldObjectQueueSize off
 * @run main/othervm -XX:TLABSize=2k -Xlog:gc+tlab=trace -XX:FlightRecorderOptions:old-object-queue-size=10000 jdk.jfr.startupargs.TestOldObjectQueueSize many
 * @run main/othervm -XX:TLABSize=2k -Xlog:gc+tlab=trace -XX:FlightRecorderOptions:old-object-queue-size=1000000 jdk.jfr.startupargs.TestOldObjectQueueSize many
 */
public class TestOldObjectQueueSize {

    private static final int OBJECT_COUNT = 10_000;
    private static final int DEFAULT_OLD_OBJECT_QUEUE_SIZE = 256;

    public final static List<Object> leak = new ArrayList<>(OBJECT_COUNT);

    public static void main(String[] args) throws Exception {
        WhiteBox.setWriteAllObjectSamples(true);
        System.out.println("TESTING: " + args[0]);

        String amount = args[0];
        try (Recording recording = new Recording()) {
            recording.enable(EventNames.OldObjectSample).withoutStackTrace().with("cutoff", "infinity");
            recording.start();
            // It's hard to get a larger number of samples without running into OOM
            // since the TLAB size increases when there is more allocation.
            // Allocate 200 MB, which should give at least 400 samples if the TLAB
            // size manages to stay below 512kb, which logging with -Xlog:gc+tlab=trace indicates.
            for (int i = 0; i < OBJECT_COUNT; i++) {
                leak.add(new byte[20_000]);
            }
            recording.stop();
            List<RecordedEvent> events = Events.fromRecording(recording);
            System.out.println("Found samples: " + events.size());
            if (amount.equals("off")) {
                if (!events.isEmpty()) {
                    throw new Exception("No objects shuld be emitted when queue size 0");
                }
            }
            if (amount.equals("many")) {
                // Sanity check that we at least have managed to set a value above the default
                if (events.size() <= DEFAULT_OLD_OBJECT_QUEUE_SIZE) {
                    throw new Exception("Should be at least be " + DEFAULT_OLD_OBJECT_QUEUE_SIZE + ", but found " + events.size());
                }
            }
        }
    }
}
