/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.test.lib.jfr.EventNames;
import jdk.test.lib.jfr.Events;

/**
 * @test
 * @bug 8058552
 * @requires vm.hasJFR
 * @requires vm.gc == "G1" | vm.gc == null
 * @key jfr
 * @summary Test checks that flags of type size_t are being sent to the jfr
 * @library /test/lib
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:-UseFastUnorderedTimeStamps -XX:+UseG1GC -XX:+UseTLAB -XX:MinTLABSize=3k -XX:OldSize=30m -XX:YoungPLABSize=3k -XX:MaxDirectMemorySize=5M  jdk.jfr.event.runtime.TestSizeTFlags
 */
public class TestSizeTFlags {
    private static final String EVENT_NAME = EventNames.UnsignedLongFlag;
    private static final int NUMBER_OF_FLAGS_TO_CHECK = 4;
    private static final long MIN_TLAB_SIZE_FLAG_VALUE = 3*1024L;
    private static final long OLD_SIZE_FLAG_VALUE = 30*1024*1024L;
    private static final long YOUNG_PLAB_SIZE_FLAG_VALUE = 3*1024L;
    private static final long MAX_DIRECT_MEMORY_SIZE_FLAG_VALUE = 5*1024*1024L;

    // Test run java with some of the flags of type size_t.
    // Goals are
    //  - to check that flags are reported to the jfr;
    //  - to make sure values are as expected.
    public static void main(String[] args) throws Exception {
        final boolean[] flagsFoundWithExpectedValue = new boolean[NUMBER_OF_FLAGS_TO_CHECK];
        Recording recording = null;
        try {
            recording = new Recording();
            recording.enable(EVENT_NAME).withThreshold(Duration.ofMillis(0));
            recording.start();
            recording.stop();

            for (final RecordedEvent event : Events.fromRecording(recording)) {
                final String recordedFlagName = Events.assertField(event, "name").getValue();
                final long value = Events.assertField(event, "value").getValue();
                switch (recordedFlagName) {
                    case "MinTLABSize": {
                        flagsFoundWithExpectedValue[0] = MIN_TLAB_SIZE_FLAG_VALUE == value;
                        continue;
                    }
                    case "OldSize": {
                        flagsFoundWithExpectedValue[1] = OLD_SIZE_FLAG_VALUE == value;
                        continue;
                    }
                    case "YoungPLABSize": {
                        flagsFoundWithExpectedValue[2] = YOUNG_PLAB_SIZE_FLAG_VALUE == value;
                        continue;
                    }
                    case "MaxDirectMemorySize": {
                        flagsFoundWithExpectedValue[3] = MAX_DIRECT_MEMORY_SIZE_FLAG_VALUE == value;
                        continue;
                    }
                    default: {
                        continue;
                    }
                }
            }

            for (int i = 0; i < flagsFoundWithExpectedValue.length; ++i) {
                assertTrue(flagsFoundWithExpectedValue[i], "Flag not found or value error!");
            }

        } catch (Throwable e) {
            if (recording != null) {
                recording.dump(Paths.get("failed.jfr"));
            }
            throw e;
        } finally {
            if (recording != null) {
                recording.close();
            }
        }
    }
}
