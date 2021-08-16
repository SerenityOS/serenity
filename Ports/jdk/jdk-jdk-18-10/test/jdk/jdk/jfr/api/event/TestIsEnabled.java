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

import jdk.jfr.EventType;
import jdk.jfr.Recording;
import jdk.test.lib.Asserts;
import jdk.test.lib.jfr.SimpleEvent;

/**
 * @test
 * @summary Test Event.isEnabled()
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.api.event.TestIsEnabled
 */

public class TestIsEnabled {

    public static void main(String[] args) throws Exception {
        assertDisabled("Event enabled with no recording");

        Recording r = new Recording();
        assertDisabled("Event enabled at new Recording()");

        r.enable(SimpleEvent.class);
        assertDisabled("Event enabled before r.start()");

        r.start();

        // enable/disable by class
        assertEnabled("Event not enabled after r.start()");
        r.disable(SimpleEvent.class);
        assertDisabled("Event enabled after r.disable()");
        r.enable(SimpleEvent.class);
        assertEnabled("Event disabled afer r.enable()");

        // enable/disable by event setting name
        String eventSettingName = String.valueOf(EventType.getEventType(SimpleEvent.class).getId());
        System.out.println("eventSettingName=" + eventSettingName);

        r.disable(eventSettingName);
        assertDisabled("Event enabled after r.disable(name)");
        r.enable(eventSettingName);
        assertEnabled("Event disabled after r.enable(name)");

        r.stop();
        assertDisabled("Event enabled after r.stop()");

        r.close();
        assertDisabled("Event enabled after r.close()");
    }

    private static void assertEnabled(String msg) {
        SimpleEvent event = new SimpleEvent();
        Asserts.assertTrue(event.isEnabled(), msg);
    }

    private static void assertDisabled(String msg) {
        SimpleEvent event = new SimpleEvent();
        Asserts.assertFalse(event.isEnabled(), msg);
    }

}
