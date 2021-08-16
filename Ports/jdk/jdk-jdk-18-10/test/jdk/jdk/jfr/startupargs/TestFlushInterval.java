/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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

import jdk.jfr.FlightRecorder;
import jdk.jfr.Recording;
import jdk.jfr.internal.PlatformRecording;
import jdk.jfr.internal.PrivateAccess;

/**
 * @test
 * @summary Start a recording with a flush interval
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib /test/jdk
 * @modules jdk.jfr/jdk.jfr.internal
 * @run main/othervm -XX:StartFlightRecording:flush-interval=2s jdk.jfr.startupargs.TestFlushInterval
 */
public class TestFlushInterval {

    public static void main(String[] args) throws Exception {
        for (Recording r : FlightRecorder.getFlightRecorder().getRecordings()) {
            PrivateAccess p = PrivateAccess.getInstance();
            PlatformRecording pr = p.getPlatformRecording(r);
            Duration d = pr.getFlushInterval();
            if (d.equals(Duration.ofSeconds(2))) {
                return; //OK
            } else {
                throw new Exception("Unexpected flush-interval " + d);
            }
        }
        throw new Exception("No recording found");
    }

}
