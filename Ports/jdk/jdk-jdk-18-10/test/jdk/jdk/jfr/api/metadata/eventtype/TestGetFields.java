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

import java.util.ArrayList;
import java.util.List;

import jdk.jfr.Event;
import jdk.jfr.EventType;
import jdk.jfr.ValueDescriptor;
import jdk.test.lib.Asserts;

/**
 * @test
 * @summary Test getFields()
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.api.metadata.eventtype.TestGetFields
 */
public class TestGetFields {

    public static void main(String[] args) throws Throwable {
        List<String> actuals = new ArrayList<>();
        EventType type = EventType.getEventType(MyEvent.class);
        for (ValueDescriptor d : type.getFields()) {
            if (d.getName().startsWith("my")) {
                String s = getCompareString(d);
                System.out.println("Actual: " + s);
                actuals.add(s);
            }
        }

        String[] expected = {
            "name=myByte; typename=byte",
            "name=myInt; typename=int",
            "name=myString; typename=java.lang.String",
            "name=myClass; typename=java.lang.Class",
            "name=myThread; typename=java.lang.Thread"
        };
        for (String s : expected) {
            Asserts.assertTrue(actuals.contains(s), "Missing expected value " + s);
        }
        Asserts.assertEquals(expected.length, actuals.size(), "Wrong number of fields found");
    }

    private static String getCompareString(ValueDescriptor d) {
        return String.format("name=%s; typename=%s",
                              d.getName(),
                              d.getTypeName());
    }

    @SuppressWarnings("unused")
    private static class MyEvent extends Event {
        public byte myByte;
        private int myInt;
        protected String myString;
        public static int myStatic; // Should not be included
        @SuppressWarnings("rawtypes")
        public Class myClass;
        public Thread myThread;
    }
}
