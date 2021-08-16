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

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @test
 * @summary Basic strip, stripLeading, stripTrailing functionality
 * @bug 8200377
 * @run main/othervm Strip
 */

public class Strip {
   public static void main(String... arg) {
        testStrip();
        testWhitespace();
    }

    /*
     * Test basic stripping routines
     */
    static void testStrip() {
        equal("   abc   ".strip(), "abc");
        equal("   abc   ".stripLeading(), "abc   ");
        equal("   abc   ".stripTrailing(), "   abc");
        equal("   abc\u2022   ".strip(), "abc\u2022");
        equal("   abc\u2022   ".stripLeading(), "abc\u2022   ");
        equal("   abc\u2022   ".stripTrailing(), "   abc\u2022");
        equal("".strip(), "");
        equal("".stripLeading(), "");
        equal("".stripTrailing(), "");
        equal("\b".strip(), "\b");
        equal("\b".stripLeading(), "\b");
        equal("\b".stripTrailing(), "\b");
    }

    /*
     * Test full whitespace range
     */
    static void testWhitespace() {
        StringBuilder sb = new StringBuilder(64);
        IntStream.range(1, 0xFFFF).filter(c -> Character.isWhitespace(c))
                .forEach(c -> sb.append((char)c));
        String whiteSpace = sb.toString();

        String testString = whiteSpace + "abc" + whiteSpace;
        equal(testString.strip(), "abc");
        equal(testString.stripLeading(), "abc"  + whiteSpace);
        equal(testString.stripTrailing(), whiteSpace + "abc");
    }

    /*
     * Report difference in result.
     */
    static void report(String message, String inputTag, String input,
                       String outputTag, String output) {
        System.err.println(message);
        System.err.println();
        System.err.println(inputTag);
        System.err.println(input.codePoints()
                .mapToObj(c -> (Integer)c)
                .collect(Collectors.toList()));
        System.err.println();
        System.err.println(outputTag);
        System.err.println(output.codePoints()
                .mapToObj(c -> (Integer)c)
                .collect(Collectors.toList()));
        throw new RuntimeException();
    }

    /*
     * Raise an exception if the two inputs are not equivalent.
     */
    static void equal(String input, String expected) {
        if (input == null || expected == null || !expected.equals(input)) {
            report("Failed equal", "Input:", input, "Expected:", expected);
        }
    }
}
