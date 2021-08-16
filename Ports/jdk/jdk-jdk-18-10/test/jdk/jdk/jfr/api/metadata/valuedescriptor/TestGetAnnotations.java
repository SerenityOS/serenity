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

import jdk.jfr.AnnotationElement;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.EventType;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.ValueDescriptor;
import jdk.test.lib.Asserts;

/**
 * @test
 * @summary Test ValueDescriptor.getAnnotations()
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.api.metadata.valuedescriptor.TestGetAnnotations
 */
public class TestGetAnnotations {

    public static void main(String[] args) throws Throwable {
        EventType type = EventType.getEventType(MyEvent.class);

        List<String> actual = new ArrayList<>();
        for (ValueDescriptor d : type.getFields()) {
            String descName = d.getName();
            for (AnnotationElement a : d.getAnnotationElements()) {
                String annName = a.getTypeName();
                String annValue = a.getValue("value").toString();
                actual.add(String.format("%s: %s = %s", descName, annName, annValue));
            }
        }

        System.out.println("Actual annotations:");
        for (String s : actual) {
            System.out.println(s);
        }

        String[] expected = {
            "myShortName: jdk.jfr.Label = myShortLabel",
            "myShortName: jdk.jfr.Description = myShortDesc",
            "myLongName: jdk.jfr.Description = myLongDesc",
            "myLongName: jdk.jfr.Label = myLongLabel",
        };

        for (String s : expected) {
            if (!actual.contains(s)) {
                System.out.println("Expected annotation missing: " + s);
                Asserts.fail("Not all expected annotations found");
            }
        }
    }


    private static class MyEvent extends Event {
        @Label("myShortLabel")
        @Description("myShortDesc")
        @Name("myShortName")
        public short myShort;

        @Name("myLongName")
        @Description("myLongDesc")
        @Label("myLongLabel")
        public long myLong;
    }
}
