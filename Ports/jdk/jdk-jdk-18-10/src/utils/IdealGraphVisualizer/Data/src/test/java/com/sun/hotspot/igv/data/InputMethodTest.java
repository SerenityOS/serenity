/*
 * Copyright (c) 2011, 2021, Oracle and/or its affiliates. All rights reserved.
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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import org.junit.*;

/**
 *
 * @author Thomas
 */
public class InputMethodTest {

    public InputMethodTest() {
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
     * Test of getBytecodes method, of class InputMethod.
     */
    @Test
    public void testGetSetBytecodes() {

        final String input = "0 iload_0\n" +
                             "1 iconst_1\n" +
                             "2 if_icmpne 7\n" +
                             "5 iconst_1\n" +
                             "6 ireturn\n" +
                             "7 iconst_0\n" +
                             "8 ireturn";

        final Group g = new Group(null);
        InputMethod m = new InputMethod(g, "name", "shortName", -1);
        m.setBytecodes(input);

        assertThat(m.getBytecodes().size(), is(7));

        assertThat(m.getBytecodes().get(0).getBci(), is(0));
        assertThat(m.getBytecodes().get(1).getBci(), is(1));
        assertThat(m.getBytecodes().get(2).getBci(), is(2));
        assertThat(m.getBytecodes().get(3).getBci(), is(5));

        assertThat(m.getBytecodes().get(0).getName(), is("iload_0"));
        assertThat(m.getBytecodes().get(1).getName(), is("iconst_1"));
        assertThat(m.getBytecodes().get(2).getName(), is("if_icmpne"));
        assertThat(m.getBytecodes().get(2).getOperands(), is("7"));
        assertThat(m.getBytecodes().get(6).getName(), is("ireturn"));

        assertThat(m.getBytecodes().get(2).getInlined(), nullValue());
        assertThat(m.getBytecodes().get(6).getInlined(), nullValue());
    }


}
