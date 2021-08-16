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

import java.util.List;
import java.util.Objects;

import jdk.jfr.AnnotationElement;
import jdk.jfr.Description;
import jdk.jfr.Enabled;
import jdk.jfr.Event;
import jdk.jfr.EventType;
import jdk.jfr.Label;
import jdk.jfr.Period;
import jdk.test.lib.Asserts;

/**
 * @test
 * @summary Test getAnnotations()
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.api.metadata.eventtype.TestGetAnnotations
 */
public class TestGetAnnotations {

    private final static String MY_LABEL = "myLabel";
    private final static String MY_DESCRIPTION = "myDesc";

    public static void main(String[] args) throws Throwable {
        EventType type = EventType.getEventType(MyEvent.class);
        List<AnnotationElement> annos = type.getAnnotationElements();
        Asserts.assertEquals(annos.size(), 2, "Wrong number of annotations");
        assertAnnotation(annos, "jdk.jfr.Label", MY_LABEL);
        assertAnnotation(annos, "jdk.jfr.Description", MY_DESCRIPTION);
    }

    private static void assertAnnotation(List<AnnotationElement> annos, String name, Object expectedValue) {
        for (AnnotationElement a : annos) {
            if (a.getTypeName().equals(name)) {
                Object value = a.getValue("value");
                Asserts.assertTrue(Objects.deepEquals(value, expectedValue), "Found annotation " + name + " but value was "+ value +" but expected " + expectedValue);
            }
        }
    }

    @Label(MY_LABEL)
    @Description(MY_DESCRIPTION)
    @Enabled(false) // not sticky annotation (with @Metadata)
    @Period("1 m")  // not sticky annotation (with @Metadata)
    private static class MyEvent extends Event {
    }
}
