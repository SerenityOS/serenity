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

import java.util.Collections;

import jdk.jfr.EventFactory;
import jdk.jfr.EventType;
import jdk.jfr.FlightRecorder;
import jdk.test.lib.Asserts;


/**
 * @test
 * @summary Verifies that EventFactory can register the same event twice
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.api.event.TestEventFactoryRegisterTwice
 */
public class TestEventFactoryRegisterTwice {

    public static void main(String[] args) throws Exception {
        EventFactory factory = EventFactory.create(Collections.emptyList(), Collections.emptyList());

        EventType eventType = factory.getEventType();
        Asserts.assertNotNull(eventType);

        // Now, register the event
        factory.register();

        verifyRegistered(eventType);

        // Now, register the event again
        factory.register();

        verifyRegistered(eventType);
    }

    private static void verifyRegistered(EventType eventType) {
        // Verify  the event is registered
        boolean found = false;
        for (EventType type : FlightRecorder.getFlightRecorder().getEventTypes()) {
            if (eventType.getId() == type.getId()) {
                found = true;
            }
        }
        if(!found) {
            Asserts.fail("Event not registered");
        }
    }
}
