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
import java.util.Arrays;
import java.util.List;

import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.EventType;
import jdk.jfr.Label;
import jdk.test.lib.Asserts;

/**
 * @test
 * @summary Test descriptive annotations for EventType
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.api.metadata.eventtype.TestGetDescription
 */
public class TestGetDescription {

    public static void main(String[] args) throws Throwable {
        EventType defaultType = EventType.getEventType(DefaultEvent.class);
        System.out.printf("defaultType.category='%s'%n", defaultType.getCategoryNames());
        System.out.printf("defaultType.desc='%s'%n", defaultType.getDescription());
        System.out.printf("defaultType.label='%s'%n", defaultType.getLabel());

        List<String> defaultCategory = Arrays.asList(new String[] {"Uncategorized"});
        Asserts.assertEquals(defaultType.getCategoryNames(), defaultCategory, "Wrong default category");
        Asserts.assertNull(defaultType.getDescription(), "Wrong default description");
        Asserts.assertEquals(defaultType.getLabel(), null, "Wrong default label"); // JavaDoc says "not null"

        EventType annotatedType = EventType.getEventType(AnnotatedEvent.class);
        System.out.printf("annotated.category='%s'%n", annotatedType.getCategoryNames());
        System.out.printf("annotated.desc='%s'%n", annotatedType.getDescription());
        System.out.printf("annotated.label='%s'%n", annotatedType.getLabel());

        List<String> expectedCategorNames = new ArrayList<>();
        expectedCategorNames.add("MyCategory");
        Asserts.assertEquals(annotatedType.getCategoryNames(), expectedCategorNames, "Wrong default category");
        Asserts.assertEquals(annotatedType.getDescription(), "MyDesc", "Wrong default description");
        Asserts.assertEquals(annotatedType.getLabel(), "MyLabel", "Wrong default label");
    }

    private static class DefaultEvent extends Event {
    }

    @Category("MyCategory")
    @Description("MyDesc")
    @Label("MyLabel")
    private static class AnnotatedEvent extends Event {
    }
}
