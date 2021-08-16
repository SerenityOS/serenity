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

import static jdk.test.lib.Asserts.assertFalse;
import static jdk.test.lib.Asserts.assertTrue;

import java.util.HashSet;
import java.util.Set;

import jdk.jfr.Event;
import jdk.jfr.EventType;
import jdk.jfr.FlightRecorder;
import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.test.lib.jfr.Events;

/**
 * @test
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm/timeout=600 jdk.jfr.api.flightrecorder.TestGetEventTypes
 */
public class TestGetEventTypes {

    public static void main(String[] args) throws Throwable {
        Recording r1 = new Recording();
        r1.setToDisk(true);

        MyEvent myEvent = new MyEvent();
        EventType t = EventType.getEventType(MyEvent.class);
        System.out.println(t.getName());
        boolean isMyEventFound = false;
        for (EventType eventType : FlightRecorder.getFlightRecorder().getEventTypes()) {
            System.out.println(": eventType: " + eventType.getName());
            r1.enable(eventType.getName());
            if (eventType.getName().equals(MyEvent.class.getName())) {
                isMyEventFound = true;
            }
        }
        assertTrue(isMyEventFound, "EventType for MyEvent not found");

        r1.start();
        myEvent.begin();
        myEvent.end();
        myEvent.commit();
        r1.stop();

        Set<String> eventTypeNames = new HashSet<String>();
        for (EventType et : FlightRecorder.getFlightRecorder().getEventTypes()) {
            assertFalse(eventTypeNames.contains(et.getName()), "EventType returned twice: " + et.getName());
            eventTypeNames.add(et.getName());
        }

        isMyEventFound = false;
        for (RecordedEvent event : Events.fromRecording(r1)) {
            final String name = event.getEventType().getName();
            System.out.println("event.getEventType: " + name);
            assertTrue(eventTypeNames.contains(name), "Missing EventType: " + name);
            if (name.equals(MyEvent.class.getName())) {
                isMyEventFound = true;
            }
        }
        r1.close();
        assertTrue(isMyEventFound, "Event for MyEvent not found");
    }

    private static class MyEvent extends Event {
    }

}
