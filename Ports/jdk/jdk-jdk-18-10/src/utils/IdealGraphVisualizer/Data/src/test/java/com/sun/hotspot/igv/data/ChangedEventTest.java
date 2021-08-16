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

import static org.junit.Assert.assertEquals;
import org.junit.*;

/**
 *
 * @author Thomas Wuerthinger
 */
public class ChangedEventTest {

    public ChangedEventTest() {
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
     * Test of addListener method, of class Event.
     */
    @Test
    public void testBase() {

        ChangedEvent<Integer> e = new ChangedEvent<>(5);
        final int[] fireCount = new int[1];

        e.addListener(new ChangedListener<Integer>() {
            @Override
            public void changed(Integer s) {
                assertEquals(s.intValue(), 5);
                fireCount[0]++;
            }
        });

        e.fire();
        assertEquals(1, fireCount[0]);

        e.fire();
        assertEquals(2, fireCount[0]);

        e.beginAtomic();

        e.fire();
        assertEquals(2, fireCount[0]);

        e.fire();
        assertEquals(2, fireCount[0]);

        e.fire();
        assertEquals(2, fireCount[0]);

        e.endAtomic();
        assertEquals(3, fireCount[0]);

    }


}
