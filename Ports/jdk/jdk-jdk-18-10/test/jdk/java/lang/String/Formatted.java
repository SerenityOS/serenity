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
 * @bug 8203444
 * @summary Unit tests for instance versions of String#format
 * @compile Formatted.java
 * @run main Formatted
 */

import java.util.Locale;

public class Formatted {
    public static void main(String[] args) {
        test1();
    }

    /*
     * Test String#formatted(Object... args) functionality.
     */
    static void test1() {
        check("formatted(Object... args)",
                "Test this %s".formatted("and that"),
                String.format("Test this %s", "and that"));
    }

    static void check(String test, String output, String expected) {
        if (output != expected && (output == null || !output.equals(expected))) {
            System.err.println("Testing " + test + ": unexpected result");
            System.err.println("Output: " + output);
            System.err.println("Expected: " + expected);
            throw new RuntimeException();
        }
    }
}
