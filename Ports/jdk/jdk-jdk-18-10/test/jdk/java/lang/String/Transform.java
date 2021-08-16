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

/*
 * @test
 * @summary Unit tests for String#transform(Function<String, R> f)
 * @run main Transform
 */

import java.util.function.Function;
import java.util.stream.Collectors;

public class Transform {
    public static void main(String[] args) {
        test1();
    }

    /*
     * Test String#transform(Function<? super String, ? extends R> f) functionality.
     */
    static void test1() {
        simpleTransform("toUpperCase", "abc", s -> s.toUpperCase());
        simpleTransform("toLowerCase", "ABC", s -> s.toLowerCase());
        simpleTransform("substring", "John Smith", s -> s.substring(0, 4));

        String multiline = "    This is line one\n" +
                           "        This is line two\n" +
                           "            This is line three\n";
        String expected = "This is line one!\n" +
                          "    This is line two!\n" +
                          "        This is line three!\n";
        check("multiline", multiline.transform(string -> {
            return string.lines()
                         .map(s -> s.transform(t -> t.substring(4) + "!"))
                         .collect(Collectors.joining("\n", "", "\n"));
        }), expected);
    }

    static void simpleTransform(String test, String s, Function<String, String> f) {
        check(test, s.transform(f), f.apply(s));
    }

    static void check(String test, Object output, Object expected) {
        if (output != expected && (output == null || !output.equals(expected))) {
            System.err.println("Testing " + test + ": unexpected result");
            System.err.println("Output:");
            System.err.println(output);
            System.err.println("Expected:");
            System.err.println(expected);
            throw new RuntimeException();
        }
    }
}
