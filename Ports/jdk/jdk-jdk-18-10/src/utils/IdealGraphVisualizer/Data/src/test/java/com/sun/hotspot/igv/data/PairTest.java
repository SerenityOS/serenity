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
public class PairTest {

    public PairTest() {
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
     * Test of getLeft method, of class Pair.
     */
    @Test
    public void testBase() {
        Pair p = new Pair();
        assertTrue(p.getLeft() == null);
        assertTrue(p.getRight() == null);
        assertEquals("[null/null]", p.toString());
        assertFalse(p.equals(null));

        Pair<Integer, Integer> p2 = new Pair(1, 2);
        assertTrue(p2.getLeft().intValue() == 1);
        assertTrue(p2.getRight().intValue() == 2);
        assertFalse(p.equals(p2));
        assertFalse(p2.equals(p));
        assertFalse(p.hashCode() == p2.hashCode());
        assertEquals("[1/2]", p2.toString());

        Pair p3 = new Pair(1, 2);
        assertTrue(p2.equals(p3));
        assertTrue(p2.hashCode() == p3.hashCode());

        p2.setLeft(2);
        assertFalse(p2.equals(p3));
        assertTrue(p2.getLeft().intValue() == 2);
        assertTrue(p2.getRight().intValue() == 2);
        assertFalse(p2.hashCode() == p3.hashCode());
        assertEquals("[2/2]", p2.toString());

        p2.setRight(1);
        assertFalse(p2.equals(p3));
        assertTrue(p2.getLeft().intValue() == 2);
        assertTrue(p2.getRight().intValue() == 1);
        assertFalse(p2.hashCode() == p3.hashCode());
        assertEquals("[2/1]", p2.toString());

        p3.setLeft(2);
        p3.setRight(1);
        assertTrue(p2.hashCode() == p3.hashCode());
        assertTrue(p2.equals(p3));
    }
}
