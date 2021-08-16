/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.io.*;
import java.lang.reflect.Field;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/*
 * @test
 * @bug 8077559
 * @summary Tests Compact String. This one is testing
 *          if Compact String enable/disable VM Options is indeed working in String class,
 *          it's verified by testing if the VM option affect coder and
 *          COMPACT_STRINGS field in String class.
 * @modules java.base/java.lang:open
 * @run testng/othervm -XX:+CompactStrings -DCompactStringEnabled=true VMOptionsTest
 * @run testng/othervm -XX:-CompactStrings -DCompactStringEnabled=false VMOptionsTest
 */

public class VMOptionsTest {
    boolean compactStringEnabled;
    // corresponding "COMPACT_STRINGS" field in String class.
    Field COMPACT_STRINGS;
    // corresponding "coder" field in String class.
    Field coder;

    // corresponding coder type in String class.
    final byte LATIN1 = 0;
    final byte UTF16  = 1;

    @BeforeClass
    public void setUp() throws Exception {
        compactStringEnabled = Boolean.valueOf(System.getProperty("CompactStringEnabled", null));
        COMPACT_STRINGS = String.class.getDeclaredField("COMPACT_STRINGS");
        COMPACT_STRINGS.setAccessible(true);
        coder = String.class.getDeclaredField("coder");
        coder.setAccessible(true);
    }

    @DataProvider
    public Object[][] provider() {
        return new Object[][] {
                new Object[] {"", LATIN1},
                new Object[] {"abc", LATIN1},
                new Object[] {"A\uff21", UTF16},
                new Object[] {"\uff21\uff22", UTF16}
        };
    }

    /*
     * verify the coder field in String objects.
     */
    @Test(dataProvider = "provider")
    public void testCoder(String str, byte expected) throws Exception {
        byte c = (byte) coder.get(str);
        expected = compactStringEnabled ? expected : UTF16;
        assertEquals(c, expected);
    }

    /*
     * verify the COMPACT_STRINGS flag in String objects.
     */
    @Test(dataProvider = "provider")
    public void testCompactStringFlag(String str, byte ignore) throws Exception {
        assertTrue(COMPACT_STRINGS.get(str).equals(compactStringEnabled));
    }
}
