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

package jdk.jfr.api.recording.options;

import java.time.Duration;
import java.time.Instant;

import jdk.jfr.Recording;
import jdk.jfr.RecordingState;
import jdk.test.lib.Asserts;
import jdk.test.lib.jfr.CommonHelper;

/**
 * @test
 * @summary Test setDuration(). Verify recording is stopped automatically.
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.api.recording.options.TestDuration
 */
public class TestDuration {

    public static void main(String[] args) throws Throwable {
        final Duration duration = Duration.ofSeconds(1);
        Recording r = new Recording();
        r.setDuration(duration);
        Asserts.assertEquals(duration, r.getDuration(), "Wrong get/set duration");

        r.start();
        Instant afterStart = Instant.now();
        CommonHelper.waitForRecordingState(r, RecordingState.STOPPED);

        Instant afterStop = Instant.now();
        Asserts.assertLessThanOrEqual(r.getStopTime(), afterStop, "getStopTime() > afterStop");
        long durationMillis = Duration.between(afterStart, r.getStopTime()).toMillis();

        // Performance of test servers varies too much to make a strict check of actual duration.
        // We only check that recording stops before timeout of 20 seconds.
        System.out.printf("Recording stopped after %d ms, expected above 1000 ms%n", durationMillis);
        r.close();
    }

}
