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

import jdk.jfr.Event;
import jdk.jfr.EventType;
import jdk.jfr.Recording;
import jdk.test.lib.Asserts;

/**
 * @test
 * @summary Test Event.isEnabled() with multiple recordings
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.api.event.TestIsEnabledMultiple
 */

public class TestIsEnabledMultiple {

    enum When {
        BeforeStart, DuringRecording
    }

    enum RecState {
        New, Running
    }

    enum EnableState {
        Enabled, Disabled
    }

    public static void main(String[] args) throws Exception {
        for (RecState recStateA : RecState.values()) {
            for (RecState recStateB : RecState.values()) {
                for (EnableState enableStateA : EnableState.values()) {
                    for (EnableState enableStateB : EnableState.values()) {
                        assertEnabled(recStateA, recStateB, enableStateA, enableStateB);
                    }
                }
            }
        }
    }

    private static void assertEnabled(RecState recStateA, RecState recStateB, EnableState enableStateA, EnableState enableStateB) {

        Recording a = new Recording();
        Recording b = new Recording();
        assertEnablement(false); // no recording running

        if (enableStateA == EnableState.Disabled) {
            a.disable(MyEvent.class);
        }
        if (enableStateA == EnableState.Enabled) {
            a.enable(MyEvent.class);
        }
        if (enableStateB == EnableState.Disabled) {
            b.disable(MyEvent.class);
        }
        if (enableStateB == EnableState.Enabled) {
            b.enable(MyEvent.class);
        }
        if ( enableStateA == EnableState.Disabled && recStateA == RecState.Running) {
            if ( enableStateB == EnableState.Disabled && recStateB == RecState.Running) {
                System.out.println();
            }
        }
        if (recStateA == RecState.Running) {
            a.start();
        }
        if (recStateB == RecState.Running) {
            b.start();
        }

        System.out.println("Recording a is " + a.getState() + ". Event is " + enableStateA);
        System.out.println("Recording b is " + b.getState() + ". Event is " + enableStateB);
        // an event is enabled if at least one recording is running
        // and the event is on by default or has been enabled.
        boolean aEnabled = recStateA == RecState.Running && enableStateA == EnableState.Enabled;
        boolean bEnabled = recStateB == RecState.Running && enableStateB == EnableState.Enabled;
        boolean enabled = aEnabled || bEnabled;
        System.out.println("Expected enablement: " + enabled);
        System.out.println();
        assertEnablement(enabled);
        if (recStateA == RecState.Running) {
            a.stop();
        }
        if (recStateB == RecState.Running) {
            b.stop();
        }
        assertEnablement(false); // no recording running
        a.close();
        b.close();
    }

    private static void assertEnablement(boolean enabled) {
        Asserts.assertEQ(EventType.getEventType(MyEvent.class).isEnabled(), enabled, "Event enablement not as expected");
    }

    @SuppressWarnings("unused")
    private static class MyEvent extends Event {
        public String msg;
    }

}
