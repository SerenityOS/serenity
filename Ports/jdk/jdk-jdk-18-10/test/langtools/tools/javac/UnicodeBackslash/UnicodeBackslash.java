/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8269150
 * @summary Unicode \ u 0 0 5 C not treated as an escaping backslash
 * @run main UnicodeBackslash
 */

public class UnicodeBackslash {
    static boolean failed = false;

    public static void main(String... args) {
        //   id     source                       expected
        test("1.1", "\\]",                       "\\]");
        test("1.2", "\u005C\]",                  "\\]");
        test("1.3", "\\u005C]",                  "\\u005C]");
        test("1.4", "\u005C\u005C]",             "\\]");

        test("2.1", "\\\\]",                     "\\\\]");
        test("2.2", "\u005C\\\]",                "\\\\]");
        test("2.3", "\\u005C\\]",                "\\u005C\\]");
        test("2.4", "\\\u005C\]",                "\\\\]");
        test("2.5", "\\\\u005C]",                "\\\\u005C]");

        test("3.1", "\u005C\u005C\\]",           "\\\\]");
        test("3.2", "\u005C\\u005C\]",           "\\\\]");
        test("3.3", "\u005C\\\u005C]",           "\\\\u005C]");
        test("3.4", "\\u005C\u005C\]",           "\\u005C\\]");
        test("3.5", "\\u005C\\u005C]",           "\\u005C\\u005C]");
        test("3.6", "\\\u005C\u005C]",           "\\\\]");

        test("4.1", "\u005C\u005C\u005C\]",      "\\\\]");
        test("4.2", "\u005C\\u005C\u005C]",      "\\\\]");
        test("4.3", "\u005C\u005C\\u005C]",      "\\\\u005C]");
        test("4.4", "\\u005C\u005C\u005C]",      "\\u005C\\]");

        test("5.1", "\u005C\u005C\u005C\u005C]", "\\\\]");

        if (failed) {
            throw new RuntimeException("Unicode escapes not handled correctly");
        }
    }

    static void test(String id, String source, String expected) {
        if (!source.equals(expected)) {
            System.err.println(id + ": expected: " +  expected + ", found: " + source);
            failed = true;
        }
    }
}
