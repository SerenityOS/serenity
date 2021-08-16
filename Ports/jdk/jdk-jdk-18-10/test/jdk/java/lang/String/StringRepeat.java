/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @summary This exercises String#repeat patterns and limits.
 * @run main/othervm StringRepeat
 */

/*
 * @test
 * @summary This exercises String#repeat patterns with 16 * 1024 * 1024 repeats.
 * @requires os.maxMemory >= 2G
 * @run main/othervm -Xmx2g StringRepeat 16777216
 */

import java.nio.CharBuffer;

public class StringRepeat {
    public static void main(String... args) {
        if (args.length > 0) {
            REPEATS = new int[args.length];
            for (int i = 0; i < args.length; ++i) {
                REPEATS[i] = Integer.parseInt(args[i]);
            }
        }
        test1();
        test2();
    }

    /*
     * Default varitions of repeat count.
     */
    static int[] REPEATS = {
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16,
        32, 64, 128, 256, 512, 1024, 64 * 1024, 1024 * 1024
    };

    /*
     * Varitions of Strings.
     */
    static String[] STRINGS = new String[] {
            "", "\0",  " ", "a", "$", "\u2022",
            "ab", "abc", "abcd", "abcde",
            "The quick brown fox jumps over the lazy dog."
    };

    /*
     * Repeat String function tests.
     */
    static void test1() {
        for (int repeat : REPEATS) {
            for (String string : STRINGS) {
                long limit = (long)string.length() * (long)repeat;

                if ((long)(Integer.MAX_VALUE >> 1) <= limit) {
                    break;
                }

                verify(string.repeat(repeat), string, repeat);
            }
        }
    }

    /*
     * Repeat String exception tests.
     */
    static void test2() {
        try {
            "abc".repeat(-1);
            throw new RuntimeException("No exception for negative repeat count");
        } catch (IllegalArgumentException ex) {
            // Correct
        }

        try {
            "abc".repeat(Integer.MAX_VALUE - 1);
            throw new RuntimeException("No exception for large repeat count");
        } catch (OutOfMemoryError ex) {
            // Correct
        }
    }

    static String truncate(String string) {
        if (string.length() < 80) {
            return string;
        }
        return string.substring(0, 80) + "...";
    }

    /*
     * Verify string repeat patterns.
     */
    static void verify(String result, String string, int repeat) {
        if (string.isEmpty() || repeat == 0) {
            if (!result.isEmpty()) {
                System.err.format("\"%s\".repeat(%d)%n", truncate(string), repeat);
                System.err.format("Result \"%s\"%n", truncate(result));
                System.err.format("Result expected to be empty, found string of length %d%n", result.length());
                throw new RuntimeException();
            }
        } else {
            int expected = 0;
            int count = 0;
            for (int offset = result.indexOf(string, expected);
                 0 <= offset;
                 offset = result.indexOf(string, expected)) {
                count++;
                if (offset != expected) {
                    System.err.format("\"%s\".repeat(%d)%n", truncate(string), repeat);
                    System.err.format("Result \"%s\"%n", truncate(result));
                    System.err.format("Repeat expected at %d, found at = %d%n", expected, offset);
                    throw new RuntimeException();
                }
                expected += string.length();
            }
            if (count != repeat) {
                System.err.format("\"%s\".repeat(%d)%n", truncate(string), repeat);
                System.err.format("Result \"%s\"%n", truncate(result));
                System.err.format("Repeat count expected to be %d, found %d%n", repeat, count);
                throw new RuntimeException();
            }
        }
    }
}
