/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.api.recording.state;

import java.time.Duration;
import java.time.Instant;

import jdk.jfr.Recording;
import jdk.jfr.RecordingState;
import jdk.test.lib.Asserts;
import jdk.test.lib.jfr.CommonHelper;
import jdk.test.lib.jfr.VoidFunction;

/**
 * @test
 * @summary Test Recording state
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.api.recording.state.TestStateDuration
 */
public class TestStateDuration {

    public static void main(String[] args) throws Throwable {
        Duration duration = Duration.ofSeconds(2);
        Recording r = new Recording();
        r.setDuration(duration);
        CommonHelper.verifyRecordingState(r, RecordingState.NEW);
        Instant start = Instant.now();
        System.out.println("Recording with duration " + duration + " started at " + start);
        r.start();

        // Wait for recording to stop automatically
        System.out.println("Waiting for recording to reach STOPPED state");
        CommonHelper.waitForRecordingState(r, RecordingState.STOPPED);
        Instant stop = Instant.now();
        Duration measuredDuration = Duration.between(start, stop);
        System.out.println("Recording stopped at " + stop + ". Measured duration " + measuredDuration);
        // Timer task uses System.currentMillis, and java.time uses other source.
        Duration deltaDueToClockNotInSync = Duration.ofMillis(100);
        Asserts.assertGreaterThan(measuredDuration.plus(deltaDueToClockNotInSync), duration);
        verifyIllegalState(() -> r.start(), "start() after stop()");
        r.close();
        CommonHelper.verifyRecordingState(r, RecordingState.CLOSED);
    }

    private static void verifyIllegalState(VoidFunction f, String msg) throws Throwable {
        CommonHelper.verifyException(f, msg, IllegalStateException.class);
    }
}
