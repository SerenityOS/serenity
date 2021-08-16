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

package jdk.jfr.api.metadata.eventtype;

import jdk.jfr.Event;
import jdk.jfr.EventType;
import jdk.jfr.FlightRecorder;
import jdk.test.lib.Asserts;
import jdk.test.lib.jfr.Events;

/**
 * @test
 * @summary Test getDefaultValues()
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib /test/jdk
 * @run main/othervm jdk.jfr.api.metadata.eventtype.TestGetDefaultValues
 */
public class TestGetDefaultValues {

    private static class DefaultEvent extends Event {
    }

    public static void main(String[] args) throws Throwable {
        testDefaultEvent();
        testCustomEvent();
        testCustomWithPeriod();
    }

    private static void testCustomWithPeriod() {
        Runnable hook = new Runnable() {
            @Override
            public void run() {
            }
        };
        EventType customEventType = EventType.getEventType(EventWithCustomSettings.class);
        FlightRecorder.addPeriodicEvent(EventWithCustomSettings.class, hook);
        Asserts.assertEquals(customEventType.getSettingDescriptors().size(), 4, "Wrong number of settings");
        assertDefaultValue(customEventType, "period", "10 s");
        assertDefaultValue(customEventType, "enabled", "false");
        assertDefaultValue(customEventType, "setting1", "none");
        assertDefaultValue(customEventType, "setting2", "none");

        FlightRecorder.removePeriodicEvent(hook);
        Asserts.assertEquals(customEventType.getSettingDescriptors().size(), 5, "Wrong number of settings");
        assertDefaultValue(customEventType, "threshold", "100 ms");
        assertDefaultValue(customEventType, "stackTrace", "true");
        assertDefaultValue(customEventType, "enabled", "false");
        assertDefaultValue(customEventType, "setting1", "none");
        assertDefaultValue(customEventType, "setting2", "none");

    }

    private static void testCustomEvent() {
        EventType customizedEventType = EventType.getEventType(EventWithCustomSettings.class);
        Asserts.assertEquals(customizedEventType.getSettingDescriptors().size(), 5, "Wrong number of default values");
        assertDefaultValue(customizedEventType, "setting1", "none");
        assertDefaultValue(customizedEventType, "setting2", "none");
        assertDefaultValue(customizedEventType, "threshold", "100 ms");
        assertDefaultValue(customizedEventType, "stackTrace", "true");
        assertDefaultValue(customizedEventType, "enabled", "false");
    }

    private static void testDefaultEvent() {
        EventType defaultEventType = EventType.getEventType(DefaultEvent.class);
        Asserts.assertEquals(defaultEventType.getSettingDescriptors().size(), 3, "Wrong number of default values");
        assertDefaultValue(defaultEventType, "threshold", "0 ns");
        assertDefaultValue(defaultEventType, "stackTrace", "true");
        assertDefaultValue(defaultEventType, "enabled", "true");
    }

    private static void assertDefaultValue(EventType eventType, String name, String expected) {
        String value = Events.getSetting(eventType, name).getDefaultValue();
        Asserts.assertEquals(value, expected, "Incorrect value for " + name);
    }
}
