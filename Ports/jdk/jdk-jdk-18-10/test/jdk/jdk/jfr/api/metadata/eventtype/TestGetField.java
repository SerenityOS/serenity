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
import jdk.jfr.ValueDescriptor;
import jdk.test.lib.Asserts;

/**
 * @test
 * @summary Test getField()
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.api.metadata.eventtype.TestGetField
 */
public class TestGetField {

    public static void main(String[] args) throws Throwable {
        EventType type = EventType.getEventType(MyEvent.class);

        ValueDescriptor v = type.getField("myByte");
        Asserts.assertNotNull(v, "getField(myByte) was null");
        Asserts.assertEquals(v.getTypeName(), "byte", "myByte was not type byte");

        v = type.getField("myInt");
        Asserts.assertNotNull(v, "getField(myInt) was null");
        Asserts.assertEquals(v.getTypeName(), "int", "myInt was not type int");

        v = type.getField("eventThread.group.name");
        Asserts.assertNotNull(v, "getField(eventThread.group.name) was null");
        Asserts.assertEquals(v.getTypeName(), "java.lang.String", "eventThread.group.name was not type java.lang.String");

        v = type.getField("myStatic");
        Asserts.assertNull(v, "got static field");

        v = type.getField("notAField");
        Asserts.assertNull(v, "got field that does not exist");

        v = type.getField("");
        Asserts.assertNull(v, "got field for empty name");


        try {
            v = type.getField(null);
            Asserts.fail("No Exception when getField(null)");
        } catch (NullPointerException e) {
            // Expected exception
        }
    }

    @SuppressWarnings("unused")
    private static class MyEvent extends Event {
        public byte myByte;
        private int myInt;
        public static int myStatic; // Should not be included
    }
}
