/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.jmx;

import java.io.IOException;
import java.util.List;

import jdk.jfr.FlightRecorder;
import jdk.jfr.Recording;
import jdk.management.jfr.FlightRecorderMXBean;
import jdk.management.jfr.RecordingInfo;
import jdk.test.lib.jfr.SimpleEvent;

/**
 * @test
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib /test/jdk
 * @run main/othervm jdk.jfr.jmx.TestSnapshot
 */
public class TestSnapshot {

    public static void main(String[] args) throws Exception {
        testEmpty();
        testStopped();
    }

    private static void testStopped() throws IOException {
        try (Recording r = new Recording()) {
            r.enable(SimpleEvent.class);
            r.start();
            SimpleEvent se = new SimpleEvent();
            se.commit();
            r.stop();

            try (Recording snapshot = FlightRecorder.getFlightRecorder().takeSnapshot()) {
                r.close();
                FlightRecorderMXBean mxBean = JmxHelper.getFlighteRecorderMXBean();
                List<RecordingInfo> recs = mxBean.getRecordings();
                JmxHelper.verifyEquals(recs.get(0), snapshot);
            }
        }
    }

    private static void testEmpty() throws IOException {
        try (Recording snapshot = FlightRecorder.getFlightRecorder().takeSnapshot()) {
            FlightRecorderMXBean mxBean = JmxHelper.getFlighteRecorderMXBean();
            List<RecordingInfo> recs = mxBean.getRecordings();
            JmxHelper.verifyEquals(recs.get(0), snapshot);
        }
    }
}
