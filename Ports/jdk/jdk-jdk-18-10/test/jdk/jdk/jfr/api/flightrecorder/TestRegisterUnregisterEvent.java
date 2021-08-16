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

import jdk.jfr.Event;
import jdk.jfr.EventType;
import jdk.jfr.FlightRecorder;

/**
 * @test
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.api.flightrecorder.TestRegisterUnregisterEvent
 */
public class TestRegisterUnregisterEvent {

    public static void main(String[] args) throws Throwable {
        // Register before Flight Recorder is started
        FlightRecorder.register(MyEvent.class);
        // Repeat
        FlightRecorder.register(MyEvent.class);

        FlightRecorder recorder = FlightRecorder.getFlightRecorder();
        int count = 0;
        for (EventType et : recorder.getEventTypes()) {
            if (et.getName().equals(MyEvent.class.getName())) {
                count++;
            }
        }
        assertEquals(1, count);

        FlightRecorder.unregister(MyEvent.class);

        count = 0;
        for (EventType et : recorder.getEventTypes()) {
            if (et.getName().equals(MyEvent.class.getName())) {
                count++;
            }
        }
        assertEquals(0, count);

        FlightRecorder.register(MyEvent.class);

        count = 0;
        for (EventType et : recorder.getEventTypes()) {
            if (et.getName().equals(MyEvent.class.getName())) {
                count++;
            }
        }
        assertEquals(1, count);

    }
}

class MyEvent extends Event {
}
