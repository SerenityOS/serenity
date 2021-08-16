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

import java.util.Arrays;

import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Enabled;
import jdk.jfr.Event;
import jdk.jfr.EventType;
import jdk.jfr.Label;
import jdk.test.lib.Asserts;

/**
 * @test
 * @summary Test getAnnotations()
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.api.metadata.eventtype.TestGetAnnotation
 */
public class TestGetAnnotation {

    public static void main(String[] args) throws Throwable {
        EventType type = EventType.getEventType(MyEvent.class);

        Label label = type.getAnnotation(Label.class);
        if (label == null) {
            Asserts.fail("Annotation label was null");
        }
        Asserts.assertEquals(label.value(), "myLabel", "Wrong value for annotation label");

        Category category = type.getAnnotation(Category.class);
        if (category == null) {
            Asserts.fail("Annotation @Description was null");
        }

        Asserts.assertTrue(Arrays.equals(category.value(), new String[] {"Stuff"}), "Wrong value for annotation enabled");

        Description description = type.getAnnotation(Description.class);
        if (description != null) {
            Asserts.fail("Annotation description should be null");
        }

        try {
            type.getAnnotation(null);
            Asserts.fail("No exception when getAnnotation(null)");
        } catch(Exception e) {
            // Expected exception
        }
    }

    @Label("myLabel")
    @Enabled(false)
    @Category("Stuff")
    private static class MyEvent extends Event {
    }
}
