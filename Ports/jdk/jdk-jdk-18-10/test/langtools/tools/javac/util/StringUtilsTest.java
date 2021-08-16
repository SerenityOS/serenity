/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 8029800 8043186
 * @summary Unit test StringUtils
 * @modules jdk.compiler/com.sun.tools.javac.util
 * @run main StringUtilsTest
 */

import java.util.Locale;
import java.util.Objects;
import com.sun.tools.javac.util.StringUtils;

public class StringUtilsTest {
    public static void main(String... args) throws Exception {
        new StringUtilsTest().run();
    }

    void run() throws Exception {
        Locale.setDefault(new Locale("tr", "TR"));

        //verify the properties of the default locale:
        assertEquals("\u0131", "I".toLowerCase());
        assertEquals("\u0130", "i".toUpperCase());

        //verify the StringUtils.toLowerCase/toUpperCase do what they should:
        assertEquals("i", StringUtils.toLowerCase("I"));
        assertEquals("I", StringUtils.toUpperCase("i"));

        //verify StringUtils.caseInsensitiveIndexOf works:
        assertEquals(2, StringUtils.indexOfIgnoreCase("  lookFor", "lookfor"));
        assertEquals(11, StringUtils.indexOfIgnoreCase("  lookFor  LOOKfor", "lookfor", 11));
        assertEquals(2, StringUtils.indexOfIgnoreCase("\u0130\u0130lookFor", "lookfor"));
    }

    void assertEquals(String expected, String actual) {
        if (!Objects.equals(expected, actual)) {
            throw new IllegalStateException("expected=" + expected + "; actual=" + actual);
        }
    }

    void assertEquals(int expected, int actual) {
        if (expected != actual) {
            throw new IllegalStateException("expected=" + expected + "; actual=" + actual);
        }
    }
}
