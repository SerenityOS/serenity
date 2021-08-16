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

import jdk.jfr.Category;
import jdk.jfr.Event;
import jdk.jfr.EventType;
import jdk.test.lib.Asserts;

/**
 * @test
 * @summary Test setName().
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.api.metadata.eventtype.TestGetCategory
 */
public class TestGetCategory {

    public static void main(String[] args) throws Throwable {

        List<String> noCategory = EventType.getEventType(NoCategory.class).getCategoryNames();
        System.out.println("noCategory=" + noCategory);
        Asserts.assertEquals(noCategory.size(), 1, "Wrong default category");
        Asserts.assertEquals(noCategory.get(0), "Uncategorized", "Wrong default category");

        List<String>  withCategory = EventType.getEventType(WithCategory.class).getCategoryNames();
        Asserts.assertEquals(withCategory.size(), 4, "Wrong category");
        Asserts.assertEquals(withCategory.get(0), "Category", "Wrong category");
        Asserts.assertEquals(withCategory.get(1), "A", "Wrong category");
        Asserts.assertEquals(withCategory.get(2), "B", "Wrong category");
        Asserts.assertEquals(withCategory.get(3), "C", "Wrong category");
    }

    private static class NoCategory extends Event {
        @SuppressWarnings("unused")
        public byte myByte;
    }

    @Category({"Category", "A", "B", "C"})
    private static class WithCategory extends Event {
        @SuppressWarnings("unused")
            public byte myByte;
    }
}
