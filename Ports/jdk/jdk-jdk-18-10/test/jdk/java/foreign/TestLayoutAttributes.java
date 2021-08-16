/*
 *  Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  version 2 for more details (a copy is included in the LICENSE file that
 *  accompanied this code).
 *
 *  You should have received a copy of the GNU General Public License version
 *  2 along with this work; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *  Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *  or visit www.oracle.com if you need additional information or have any
 *  questions.
 */

/*
 * @test
 * @run testng TestLayoutAttributes
 */

import jdk.incubator.foreign.MemoryLayout;
import jdk.incubator.foreign.MemoryLayouts;
import org.testng.annotations.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class TestLayoutAttributes {

    @Test
    public void testAttribute() {
        MemoryLayout ml = MemoryLayouts.BITS_32_LE
                .withAttribute("MyAttribute", 10L);
        assertEquals((long) ml.attribute("MyAttribute").orElseThrow(), 10L);
    }

    @Test
    public void testAttributeOverwrite() {
        MemoryLayout ml = MemoryLayouts.BITS_32_LE
                .withAttribute("MyAttribute", 10L);
        assertEquals((long) ml.attribute("MyAttribute").orElseThrow(), 10L);
        ml = ml.withAttribute("MyAttribute", 11L);
        assertEquals((long) ml.attribute("MyAttribute").orElseThrow(), 11L);
    }

    @Test
    public void testAttributeNonExistent() {
        MemoryLayout ml = MemoryLayouts.BITS_32_LE
                .withAttribute("MyAttribute", 10L);
        assertTrue(ml.attribute("Foo").isEmpty());
    }

    @Test
    public void testNameAttribute() {
        MemoryLayout ml = MemoryLayouts.BITS_32_LE
                .withName("foo");
        assertEquals(ml.name().orElseThrow(), "foo");
        assertEquals(ml.attribute(MemoryLayout.LAYOUT_NAME).orElseThrow(), "foo");
    }

    @Test
    public void testAttributesStream() {
        MemoryLayout ml = MemoryLayouts.BITS_32_LE
                .withName("foo")
                .withAttribute("MyAttribute", 10L);
        List<String> attribs = ml.attributes().collect(Collectors.toList());
        assertEquals(attribs.size(), 2);
        assertTrue(attribs.contains("MyAttribute"));
        assertTrue(attribs.contains(MemoryLayout.LAYOUT_NAME));
    }
}
