/*
 * Copyright (c) 2008, 2015, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

package com.sun.hotspot.igv.data;

import static org.junit.Assert.*;
import org.junit.*;

/**
 *
 * @author Thomas Wuerthinger
 */
public class ControllableChangedListenerTest {

    public ControllableChangedListenerTest() {
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
     * Test of isEnabled method, of class ControllableChangedListener.
     */
    @Test
    public void testBase() {

        final boolean[] hasFired = new boolean[1];
        final boolean[] shouldFire = new boolean[1];
        final Integer[] valueToFire = new Integer[1];
        ControllableChangedListener<Integer> l = new ControllableChangedListener<Integer>() {

            @Override
            public void filteredChanged(Integer value) {
                assertTrue(shouldFire[0]);
                assertEquals(valueToFire[0], value);
                hasFired[0] = true;
            }
        };

        shouldFire[0] = true;
        valueToFire[0] = 1;
        hasFired[0] = false;
        l.changed(1);
        assertTrue(hasFired[0]);

        shouldFire[0] = false;
        hasFired[0] = false;
        l.setEnabled(false);
        l.changed(1);
        assertFalse(hasFired[0]);

        shouldFire[0] = true;
        valueToFire[0] = 1;
        hasFired[0] = false;
        l.setEnabled(true);
        l.changed(1);
        assertTrue(hasFired[0]);
    }
}
