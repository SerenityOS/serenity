/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @test
 * @summary Basic isBlank functionality
 * @bug 8200436
 * @run main/othervm IsBlank
 */

public class IsBlank {
   public static void main(String... arg) {
        testIsBlank();
        testWhitespace();
    }

    /*
     * Test with strings
     */
    static void testIsBlank() {
        test("", true);
        test(" ", true);
        test(" \t", true);
        test("  \u1680", true);
        test("   abc   ", false);
        test("   abc\u2022", false);
    }

    /*
     * Test full whitespace range
     */
    static void testWhitespace() {
        StringBuilder sb = new StringBuilder(64);
        IntStream.range(1, 0xFFFF).filter(c -> Character.isWhitespace(c))
                .forEach(c -> sb.append((char)c));
        String whiteSpace = sb.toString();

        test(whiteSpace, true);
        test(whiteSpace + "abc" + whiteSpace, false);
    }

    /*
     * Raise an exception if the two inputs are not equivalent.
     */
    static void test(String input, boolean expected) {
        if (input.isBlank() != expected) {
            System.err.format("Failed test, Input: %s, Expected: %b%n", input, expected);
            throw new RuntimeException();
        }
    }
}
