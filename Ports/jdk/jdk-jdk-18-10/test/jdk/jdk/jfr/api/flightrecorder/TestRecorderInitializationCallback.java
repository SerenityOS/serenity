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

package jdk.jfr.api.flightrecorder;

import static jdk.test.lib.Asserts.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;

import jdk.jfr.FlightRecorder;
import jdk.jfr.FlightRecorderListener;

/**
 * @test
 * @summary Test Flight Recorder initialization callback is only called once
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.api.flightrecorder.TestRecorderInitializationCallback
 */
public class TestRecorderInitializationCallback {

    private static class TestListener implements FlightRecorderListener {
        private final AtomicInteger count = new AtomicInteger();

        @Override
        public void recorderInitialized(FlightRecorder recorder) {
            count.incrementAndGet();
            System.out.println("recorderInitialized: " + recorder + " count=" + count);
            // Get the recorder again, should not trigger listener
            FlightRecorder.getFlightRecorder();
        }
    }

    public static void main(String[] args) throws Throwable {
        TestListener t = new TestListener();
        FlightRecorder.addListener(t);
        // trigger initialization
        FlightRecorder.getFlightRecorder();
        assertEquals(1, t.count.intValue(), "Expected 1 notification, got " + t.count);
    }
}
