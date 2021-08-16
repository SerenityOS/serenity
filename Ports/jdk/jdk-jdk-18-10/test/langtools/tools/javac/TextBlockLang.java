/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 8223967
 * @summary Unit tests for Text Block language changes
 * @compile -encoding utf8 TextBlockLang.java
 * @run main TextBlockLang
 */

public class TextBlockLang {
    public static void main(String... args) {
        test1();
        test2();
        test3();
    }

    /*
     * Test basic string functionality.
     */
    static void test1() {
        EQ("""
           """, "");
        EQ("""
            abc
            """, "abc\n");
        EQ("""

           """, "\n");
        EQ("""
            "
            """, "\"\n");
        EQ("""
            ""
            """, "\"\"\n");
        EQ("""
           \"""
           """, "\"\"\"\n");
        EQ("""
           "\""
           """, "\"\"\"\n");
        EQ("""
           ""\"
           """, "\"\"\"\n");
        EQ("""
            \r
            """, "\r\n");
        EQ("""
            \u2022
            """, "\u2022\n");
        EQ("""
            •
            """, "\u2022\n");
        LENGTH("""
            abc
            """, 4);
    }

    /*
     * Test escape-S.
     */
    static void test2() {
        if ('\s' != ' ') {
            throw new RuntimeException("Failed character escape-S");
        }
        EQ("\s", " ");
        EQ("""
           \s
           """, " \n");
    }

    /*
     * Test escape line terminator.
     */
    static void test3() {
        EQ("""
           abc \
           """, "abc ");
        EQ("\\\n".translateEscapes(), "");
        EQ("\\\r\n".translateEscapes(), "");
        EQ("\\\r".translateEscapes(), "");
    }

    /*
     * Raise an exception if the string is not the expected length.
     */
    static void LENGTH(String string, int length) {
        if (string == null || string.length() != length) {
            System.err.println("Failed LENGTH");
            System.err.println(string + " " + length);
            throw new RuntimeException("Failed LENGTH");
        }
    }

    /*
     * Raise an exception if the two input strings are not equal.
     */
    static void EQ(String input, String expected) {
        if (input == null || expected == null || !expected.equals(input)) {
            System.err.println("Failed EQ");
            System.err.println();
            System.err.println("Input:");
            System.err.println(input.replaceAll(" ", "."));
            System.err.println();
            System.err.println("Expected:");
            System.err.println(expected.replaceAll(" ", "."));
            throw new RuntimeException();
        }
    }
}
