/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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
import jdk.jfr.FlightRecorder;
import jdk.jfr.Name;
import jdk.jfr.Recording;

/**
 * @test
 * @summary Start a FlightRecording without any settings (not even default).
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.startupargs.TestStartNoSettings
 *      -XX:StartFlightRecording:settings=none
 */
public class TestStartNoSettings {

    @Name("UserEvent")
    static class UserEvent extends Event {
    }

    public static void main(String[] a) throws Exception {
        boolean userEnabled = false;
        try (Recording r = new Recording()) {
            r.start();
            UserEvent e = new UserEvent();
            e.commit();
            for (EventType et : FlightRecorder.getFlightRecorder().getEventTypes()) {
                if (et.isEnabled()) {
                    if (!et.getName().equals("UserEvent")) {
                        throw new Exception("Only 'UserEvent' should be enabled");
                    }
                    userEnabled = true;
                }
            }
        }

        if (!userEnabled)  {
            throw new Exception("Expected 'UserEvent' to be enabled with -XX:StartFlightRecording:settings=none");
        }
    }
}
