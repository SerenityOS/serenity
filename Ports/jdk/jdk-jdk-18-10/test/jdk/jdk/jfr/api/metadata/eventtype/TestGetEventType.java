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
import jdk.test.lib.Asserts;

/**
 * @test
 * @summary Test getEventType()
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.api.metadata.eventtype.TestGetEventType
 */
public class TestGetEventType {

    public static void main(String[] args) throws Throwable {
        EventType type = EventType.getEventType(MyEventA.class);
        Asserts.assertEquals(type.getName(), MyEventA.class.getName(), "Wrong EventType for MyEventA");

        type = EventType.getEventType(MyEventB.class);
        Asserts.assertEquals(type.getName(), MyEventB.class.getName(), "Wrong EventType for MyEventB");

        try {
            EventType.getEventType(null);
            Asserts.fail("No exception for getEventType(null)");
        } catch (Exception e) {
            // Expected exception
        }
    }

    private static class MyEventA extends Event {
    }

    private static class MyEventB extends Event {
    }
}
