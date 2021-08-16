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

import java.time.Instant;

import jdk.jfr.Recording;
import jdk.jfr.RecordingState;
import jdk.test.lib.Asserts;
import jdk.test.lib.jfr.CommonHelper;

/**
 * @test
 * @summary Verify that a recopding with a delay is started.
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib /test/jdk
 * @run main/othervm -XX:StartFlightRecording:name=TestStartDelay,delay=1s jdk.jfr.startupargs.TestStartDelayRunning
 */
public class TestStartDelayRunning {

    public static void main(String[] args) throws Exception {
        System.out.println("Test started at " + Instant.now());
        Recording r = StartupHelper.getRecording("TestStartDelay");
        CommonHelper.waitForRecordingState(r, RecordingState.RUNNING);
        System.out.println("Recording startTime = " + r.getStartTime());
        Asserts.assertNotNull(r.getStartTime(), "StartTime was null after delay");
        Asserts.assertGreaterThan(Instant.now(), r.getStartTime(), "Current time should exceed start time");
        r.stop();
        r.close();
    }
}
