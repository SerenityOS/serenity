/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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

import jdk.jfr.Event;
import jdk.jfr.EventType;
import jdk.jfr.Recording;
import jdk.test.lib.Asserts;
import jdk.test.lib.jfr.Events;
import jdk.test.lib.jfr.SimpleEvent;

/**
 * @test
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm -XX:FlightRecorderOptions:retransform=false jdk.jfr.startupargs.TestRetransform
 * @run main/othervm -XX:FlightRecorderOptions:retransform=true jdk.jfr.startupargs.TestRetransform
 */
public class TestRetransform {
    private static class TestEvent extends Event {
    }
    public static void main(String[] args) throws Exception {
        EventType type = EventType.getEventType(TestEvent.class);
        if (type.isEnabled()) {
            Asserts.fail("Expected event to be disabled before recording start");
        }
        Recording r = new Recording();
        r.start();
        if (!type.isEnabled()) {
            Asserts.fail("Expected event to be enabled during recording");
        }
        TestEvent testEvent = new TestEvent();
        testEvent.commit();
        loadEventClassDuringRecording();
        r.stop();
        if (type.isEnabled()) {
            Asserts.fail("Expected event to be disabled after recording stopped");
        }
        Events.hasEvent(r, SimpleEvent.class.getName());
        Events.hasEvent(r, TestEvent.class.getName());
    }

    // Classes that are loaded during a recording
    // should get instrumentation on class load
    private static void loadEventClassDuringRecording() {
        SimpleEvent event = new SimpleEvent();
        event.commit();
    }

}
