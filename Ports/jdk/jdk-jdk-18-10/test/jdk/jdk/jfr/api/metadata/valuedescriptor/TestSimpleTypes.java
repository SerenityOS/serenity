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

package jdk.jfr.api.metadata.valuedescriptor;

import java.util.ArrayList;
import java.util.List;

import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.EventType;
import jdk.jfr.BooleanFlag;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.Percentage;
import jdk.jfr.ValueDescriptor;
import jdk.test.lib.Asserts;

/**
 * @test
 * @summary Test all basic types in ValueDescriptor.
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.api.metadata.valuedescriptor.TestSimpleTypes
 */
public class TestSimpleTypes {

    public static void main(String[] args) throws Throwable {
        EventType type = EventType.getEventType(MyEvent.class);

        List<String> expectedStrings = new ArrayList<>();
        expectedStrings.add("desc=myByteDesc; label=null; name=myByte; typename=byte; contenttype=null");
        expectedStrings.add("desc=null; label=myShortLabel; name=myShort; typename=short; contenttype=null");
        expectedStrings.add("desc=myIntDesc; label=myIntLabel; name=myInt; typename=int; contenttype=null");
        expectedStrings.add("desc=null; label=null; name=myLongName; typename=long; contenttype=null");
        expectedStrings.add("desc=myCharDesc; label=myCharLabel; name=myCharName; typename=char; contenttype=null");
        expectedStrings.add("desc=null; label=null; name=myFloat; typename=float; contenttype=jdk.jfr.Percentage");
        expectedStrings.add("desc=null; label=null; name=myDouble; typename=double; contenttype=null");
        expectedStrings.add("desc=null; label=null; name=myBoolean; typename=boolean; contenttype=jdk.jfr.BooleanFlag");
        expectedStrings.add("desc=null; label=null; name=myString; typename=java.lang.String; contenttype=null");
        expectedStrings.add("desc=null; label=null; name=myClass; typename=java.lang.Class; contenttype=null");
        expectedStrings.add("desc=null; label=null; name=myThread; typename=java.lang.Thread; contenttype=null");

        List<Long> typeIds = new ArrayList<>();
        for (ValueDescriptor d : type.getFields()) {
            if (d.getName().startsWith("my")) {
                String s = getCompareString(d);
                System.out.println("got: " + s);
                Asserts.assertTrue(expectedStrings.contains(s), "Wrong type string found");
                expectedStrings.remove(s);

                long typeId = d.getTypeId();
                Asserts.assertFalse(typeIds.contains(typeId), "TypeIds not unique");
                typeIds.add(typeId);

                Asserts.assertFalse(d.isArray(), "ValueDescriptor should not be array");
            }
        }

        if (!expectedStrings.isEmpty()) {
            System.out.println("Missing expectedStrings:");
            for (String s : expectedStrings) {
                System.out.println(s);
            }
            Asserts.fail("Not all strings found");
        }
    }

    private static String getCompareString(ValueDescriptor d) {
        return String.format("desc=%s; label=%s; name=%s; typename=%s; contenttype=%s",
                              d.getDescription(),
                              d.getLabel(),
                              d.getName(),
                              d.getTypeName(),
                              d.getContentType());
    }

    @SuppressWarnings("unused")
    private static class MyEvent extends Event {

        @Description("myByteDesc")
        public byte myByte;

        @Label("myShortLabel")
        public short myShort;

        @Label("myIntLabel")
        @Description("myIntDesc")
        public int myInt;

        @Name("myLongName")
        public long myLong;

        @Label("myCharLabel")
        @Description("myCharDesc")
        @Name("myCharName")
        protected char myChar;

        @Percentage
        protected float myFloat;
        protected double myDouble;
        @BooleanFlag
        private boolean myBoolean;
        private String myString;
        @SuppressWarnings("rawtypes")
        private Class myClass;
        private Thread myThread;
    }
}
