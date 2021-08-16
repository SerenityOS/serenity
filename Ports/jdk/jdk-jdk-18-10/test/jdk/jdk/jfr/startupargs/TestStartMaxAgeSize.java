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

/**
 * @test
 * @summary Start a recording with delay. Verify recording starts later.
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib /test/jdk
 * @run main/othervm -XX:StartFlightRecording:name=TestStartMaxAgeSize,maxage=10s,maxsize=1000000 jdk.jfr.startupargs.TestStartMaxAgeSize
 */
public class TestStartMaxAgeSize {

    public static void main(String[] args) throws Exception {
        Recording r = StartupHelper.getRecording("TestStartMaxAgeSize");
        CommonHelper.verifyRecordingState(r, RecordingState.RUNNING);
        Asserts.assertEquals(r.getMaxAge(), Duration.ofSeconds(10), "Wrong maxAge");
        Asserts.assertEquals(r.getMaxSize(), 1000000L, "Wrong maxSize");
        r.stop();
        r.close();
    }

}
