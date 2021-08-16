/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * @test
 * @bug 4914802 8257511
 * @summary Test StringBuilder.insert sanity tests
 * @run testng/othervm -XX:-CompactStrings Insert
 * @run testng/othervm -XX:+CompactStrings Insert
 */
@Test
public class Insert {

    public void insertFalse() {
        // Caused an infinite loop before 4914802
        StringBuilder sb = new StringBuilder();
        assertEquals("false", sb.insert(0, false).toString());
    }

    public void insertOffset() {
        // 8254082 made the String variant cause an AIOOBE, fixed in 8257511
        assertEquals("efabc", new StringBuilder("abc").insert(0, "def",                      1, 3).toString());
        assertEquals("efabc", new StringBuilder("abc").insert(0, new StringBuilder("def"),   1, 3).toString());
        // insert(I[CII) and insert(ILjava/lang/CharSequence;II) are inconsistently specified
        assertEquals("efabc", new StringBuilder("abc").insert(0, new char[] {'d', 'e', 'f'}, 1, 2).toString());
    }

}
