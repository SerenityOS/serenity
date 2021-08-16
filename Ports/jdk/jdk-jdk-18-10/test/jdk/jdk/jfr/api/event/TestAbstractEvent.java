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

package jdk.jfr.api.event;

import java.io.IOException;

import jdk.jfr.Event;
import jdk.jfr.EventType;
import jdk.jfr.Experimental;
import jdk.jfr.FlightRecorder;
import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;
import jdk.test.lib.Asserts;
import jdk.test.lib.jfr.Events;

/**
 * @test
 * @summary Tests that abstract events are not part of metadata
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.api.event.TestAbstractEvent
 */
public class TestAbstractEvent {

    // Should not be included in metadata
    @Experimental
    static abstract class BaseEvent extends Event {
    }

    // Should be included
    static class ConcreteEvent extends BaseEvent {
    }

    public static void main(String... args) throws IOException {
        try {
            EventType.getEventType(BaseEvent.class);
            Asserts.fail("Should not accept abstract event classes");
        } catch (IllegalArgumentException iae) {
            // OK, as expected
        }

        try {
            FlightRecorder.register(BaseEvent.class);
            Asserts.fail("Should not accept registering abstract event classes");
        } catch (IllegalArgumentException iae) {
            // OK, as expected
        }

        try {
            FlightRecorder.unregister(BaseEvent.class);
            Asserts.fail("Should not accept unregistering abstract event classes");
        } catch (IllegalArgumentException iae) {
            // OK, as expected
        }


        Recording r = new Recording();
        try {
            r.enable(BaseEvent.class);
            Asserts.fail("Should not accept abstract event classes");
        } catch (IllegalArgumentException iae) {
            // OK, as expected
        }
        r.start();

        ConcreteEvent event = new ConcreteEvent();
        event.commit();
        r.stop();
        RecordingFile rf = Events.copyTo(r);
        RecordedEvent re = rf.readEvent();
        if (!re.getEventType().getName().equals(ConcreteEvent.class.getName())) {
            Asserts.fail("Expected " + ConcreteEvent.class.getName() + " event to be part of recording. Found " + re.getEventType().getName());
        }
        if (rf.hasMoreEvents()) {
            Asserts.fail("Expected only one event");
        }
        rf.close();
        EventType concreteEventType = null;
        for (EventType type : FlightRecorder.getFlightRecorder().getEventTypes()) {
            if (type.getName().equals(BaseEvent.class.getName())) {
                Asserts.fail("Abstract events should not be part of metadata");
            }
            if (type.getName().equals(ConcreteEvent.class.getName())) {
                concreteEventType = type;
            }
        }
        Asserts.assertTrue(concreteEventType != null, "Could not find " + ConcreteEvent.class.getName() + " in metadata");
        Experimental exp = concreteEventType.getAnnotation(Experimental.class);
        Asserts.assertTrue(exp != null, "Could not find inherited annotation" + Experimental.class.getName() + " from abstract event class");
    }
}
