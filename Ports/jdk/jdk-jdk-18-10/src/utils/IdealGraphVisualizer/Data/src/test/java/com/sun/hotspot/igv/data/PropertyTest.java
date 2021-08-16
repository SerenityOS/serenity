/*
 * Copyright (c) 1998, 2015, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package com.sun.hotspot.igv.data;

import static org.junit.Assert.*;
import org.junit.*;

/**
 *
 * @author Thomas Wuerthinger
 */
public class PropertyTest {

    public PropertyTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of getName method, of class Property.
     */
    @Test
    public void testGetNameAndValue() {
        final Property p = new Property("name", "value");
        assertEquals(p.getName(), "name");
        assertEquals(p.getValue(), "value");

        try {
            new Property(null, "value");
            fail();
        } catch(IllegalArgumentException e) {
        }


        try {
            new Property("name", null);
            fail();
        } catch(IllegalArgumentException e) {
        }
    }

    /**
     * Test of toString method, of class Property.
     */
    @Test
    public void testToString() {
        final Property p = new Property("name", "value");
        assertEquals(p.toString(), "name=value");
    }

    /**
     * Test of equals method, of class Property.
     */
    @Test
    public void testEquals() {
        final Property p = new Property("name", "value");
        final Object o = new Object();
        assertFalse(p.equals(o));
        assertFalse(p.equals(null));
        assertTrue(p.equals(p));

        final Property p2 = new Property("name", "value1");
        assertFalse(p.equals(p2));
        assertTrue(p.hashCode() != p2.hashCode());

        final Property p3 = new Property("name2", "value");
        assertFalse(p.equals(p3));
        assertTrue(p.hashCode() != p3.hashCode());
        assertTrue(p2.hashCode() != p3.hashCode());

        final Property p4 = new Property("name", "value");
        assertEquals(p, p4);
        assertEquals(p.hashCode(), p4.hashCode());

        final Property p5 = new Property("value", "name");
        assertFalse(p.equals(p5));
        assertTrue(p.hashCode() != p5.hashCode());
    }
}
