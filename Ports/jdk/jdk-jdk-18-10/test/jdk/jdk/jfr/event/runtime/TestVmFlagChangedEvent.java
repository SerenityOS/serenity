/*
 * Copyright (c) 2013, 2019, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.event.runtime;

import static jdk.test.lib.Asserts.assertEquals;
import static jdk.test.lib.Asserts.assertTrue;

import java.lang.management.ManagementFactory;

import com.sun.management.HotSpotDiagnosticMXBean;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.test.lib.jfr.EventNames;
import jdk.test.lib.jfr.Events;

/**
 * @test TestVmFlagChangedEvent
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @modules jdk.jfr
 *          jdk.management
 *
 * @run main/othervm jdk.jfr.event.runtime.TestVmFlagChangedEvent
 */
public final class TestVmFlagChangedEvent {

    public static void main(String[] args) throws Throwable {
        EventFlag[] eventFlags = {
            new EventFlag(EventNames.StringFlagChanged, "HeapDumpPath", "/a/sample/path"),
            new EventFlag(EventNames.BooleanFlagChanged, "HeapDumpOnOutOfMemoryError", "true")
        };

        Recording recording = new Recording();
        for (EventFlag eventFlag : eventFlags) {
            recording.enable(eventFlag.eventName);
        }

        recording.start();
        HotSpotDiagnosticMXBean mbean = ManagementFactory.getPlatformMXBean(HotSpotDiagnosticMXBean.class);
        for (EventFlag eventFlag : eventFlags) {
            eventFlag.update(mbean);
        }
        recording.stop();

        for (RecordedEvent event : Events.fromRecording(recording)) {
            final String flagName = Events.assertField(event, "name").getValue();
            System.out.println("flag name=" + flagName);
            for (EventFlag eventFlag : eventFlags) {
                if (flagName.equals(eventFlag.eventLabel)) {
                    System.out.println("Event:" + event);
                    Object newValue = Events.assertField(event, "newValue").getValue();
                    Object oldValue = Events.assertField(event, "oldValue").getValue();
                    System.out.println("newValue:" + asText(newValue));
                    System.out.println("oldValue:" + asText(oldValue));
                    assertEquals(eventFlag.newValue, asText(newValue), "Wrong new value: expected" + eventFlag.newValue);
                    assertEquals(eventFlag.oldValue, asText(oldValue), "Wrong old value: expected" + eventFlag.oldValue);
                    Events.assertField(event, "origin").equal("Management");
                    eventFlag.isFound = true;
                    break;
                }
            }
        }
        for (EventFlag eventFlag : eventFlags) {
            assertTrue(eventFlag.isFound, "Missing flag change for: " + eventFlag.eventLabel);
        }
    }

    private static String asText(Object value) {
        if (value == null) {
            return ""; // HotSpotDiagnosticMXBean interface return "" for unset values
        }
        return String.valueOf(value);
    }

    private static class EventFlag {
        final String eventName;
        final String eventLabel;
        String newValue;
        String oldValue;
        boolean isFound = false;

        EventFlag(String eventName, String eventLabel, String newValue) {
            this.eventName = eventName;
            this.eventLabel = eventLabel;
            this.newValue = newValue;
        }

        void update(HotSpotDiagnosticMXBean mbean) {
            this.oldValue = mbean.getVMOption(eventLabel).getValue();
            mbean.setVMOption(eventLabel, newValue);
        }
    }
}
