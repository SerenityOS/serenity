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

package jdk.jfr.api.recorder;

import jdk.jfr.FlightRecorder;
import jdk.jfr.FlightRecorderListener;
import jdk.test.lib.Asserts;

/**
 * @test TestRecorderListener
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 *
 * @run main/othervm jdk.jfr.api.recorder.TestRecorderInitialized
 */
public class TestRecorderInitialized {

    static class Listener implements FlightRecorderListener {
        private boolean notified;

        @Override
        public void recorderInitialized(FlightRecorder r) {
            notified = true;
        }
    }

    public static void main(String...args) {
        Listener l1 = new Listener();

        FlightRecorder.addListener(l1);
        Asserts.assertFalse(l1.notified, "Listener shouldn't be notified unless Flight Recorder is initialized");
        // initialize Flight Recorder
        FlightRecorder.getFlightRecorder();
        Asserts.assertTrue(l1.notified, "Listener should be notified when Flight Recorder is initialized");
        l1.notified = false;

        Listener l2 = new Listener();
        FlightRecorder.addListener(l1);
        FlightRecorder.addListener(l2);
        Asserts.assertTrue(l2.notified, "Listener should be notified if Flight Recorder is already initialized");
        Asserts.assertTrue(l1.notified, "Only added listnener should be notified, if Flight Recorder is already initialized");

    }
}
