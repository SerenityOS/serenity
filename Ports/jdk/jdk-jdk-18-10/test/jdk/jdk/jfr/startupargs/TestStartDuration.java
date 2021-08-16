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

package jdk.jfr.startupargs;

import java.time.Duration;

import jdk.jfr.Recording;
import jdk.jfr.RecordingState;
import jdk.test.lib.Asserts;
import jdk.test.lib.jfr.CommonHelper;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

/**
 * @test
 * @summary Start a recording with duration. Verify recording stops.
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib /test/jdk
 * @run main jdk.jfr.startupargs.TestStartDuration
 */
public class TestStartDuration {

    public static class TestValues {
        public static void main(String[] args) throws Exception {
            Recording r = StartupHelper.getRecording("TestStartDuration");
            Asserts.assertEquals(r.getDuration(), Duration.parse(args[0]));
            if (args.length > 1 && args[1].equals("wait")) {
                CommonHelper.waitForRecordingState(r, RecordingState.STOPPED);
            }
        }
    }

    private static void testDurationInRange(String duration, Duration durationString, boolean wait) throws Exception {
        ProcessBuilder pb = ProcessTools.createTestJvm(
            "-XX:StartFlightRecording:name=TestStartDuration,duration=" + duration, TestValues.class.getName(),
            durationString.toString(), wait ? "wait" : "");
        OutputAnalyzer out = ProcessTools.executeProcess(pb);

        out.shouldHaveExitValue(0);
    }


    private static void testDurationJavaVersion(String duration, boolean inRange) throws Exception {
        ProcessBuilder pb = ProcessTools.createTestJvm(
            "-XX:StartFlightRecording:name=TestStartDuration,duration=" + duration, "-version");
        OutputAnalyzer out = ProcessTools.executeProcess(pb);

        if (inRange) {
            out.shouldHaveExitValue(0);
        } else {
            out.shouldContain("Could not start recording, duration must be at least 1 second.");
            out.shouldHaveExitValue(1);
        }
    }

    private static void testDurationInRangeAccept(String duration) throws Exception {
        testDurationJavaVersion(duration, true);
    }

    private static void testDurationOutOfRange(String duration) throws Exception {
        testDurationJavaVersion(duration, false);
    }

    public static void main(String[] args) throws Exception {
        testDurationInRange("1s", Duration.ofSeconds(1), true);
        testDurationInRange("1234003005ns", Duration.ofNanos(1234003005L), true);
        testDurationInRange("1034ms", Duration.ofMillis(1034), false);
        testDurationInRange("32m", Duration.ofMinutes(32), false);
        testDurationInRange("65h", Duration.ofHours(65), false);
        testDurationInRange("354d", Duration.ofDays(354), false);

        // additional test for corner values, verify that JVM accepts following durations
        testDurationInRangeAccept("1000000000ns");
        testDurationInRangeAccept("1000ms");
        testDurationInRangeAccept("1m");
        testDurationInRangeAccept("1h");
        testDurationInRangeAccept("1d");

        // out-of-range durations
        testDurationOutOfRange("0s");
        testDurationOutOfRange("999ms");
        testDurationOutOfRange("999999999ns");
        testDurationOutOfRange("0m");
        testDurationOutOfRange("0h");
        testDurationOutOfRange("0d");
    }
}
