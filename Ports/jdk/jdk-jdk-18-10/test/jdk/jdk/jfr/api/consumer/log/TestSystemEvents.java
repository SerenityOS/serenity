/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
package jdk.jfr.api.consumer.log;

import jdk.jfr.Event;
import jdk.jfr.FlightRecorder;
import jdk.jfr.Name;
import jdk.jfr.Period;

/**
 * @test
 * @summary Tests that only system events are emitted
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @build jdk.jfr.api.consumer.log.LogAnalyzer
 * @run main/othervm
 *     -Xlog:jfr+event+system=trace:file=system.log
 *     -XX:StartFlightRecording jdk.jfr.api.consumer.log.TestSystemEvents
 */
public class TestSystemEvents {
    // Testing with -XX:StartFlightRecording, since it's
    // a likely use case and there could be issues
    // with starting the stream before main.
    @Period("1 s")
    @Name("UserDefined")
    static class UserEvent extends Event {
    }

    public static void main(String... args) throws Exception {
        FlightRecorder.addPeriodicEvent(UserEvent.class, () -> {
            UserEvent e = new UserEvent();
            e.commit();
        });
        LogAnalyzer la = new LogAnalyzer("system.log");
        la.await("CPULoad"); // emitted 1/s
        la.shouldNotContain("UserDefined");
    }
}
