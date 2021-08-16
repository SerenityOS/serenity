/*
 * Copyright (c) 2001, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4428772
 * @summary Testing recognition of "NaN" and "Infinity" strings
 * @author Joseph D. Darcy
 */


public class NaNInfinityParsing {
    /*
     * Regression tests for:
     * 4428772 -- Establish invariant for Float & Double classes and
     * their string representations
     *
     * Added capability for parse{Float, Double} and related methods
     * to recognize "NaN" and "Infinity" strings so that
     * parseDouble(toString(d)) will always return the original
     * floating-point value.
     */

    static String NaNStrings[] = {
        "NaN",
        "+NaN",
        "-NaN"
    };

    static String infinityStrings[] = {
        "Infinity",
        "+Infinity",
        "-Infinity",
    };

    static String invalidStrings[] = {
        "+",
        "-",
        "@",
        "N",
        "Na",
        "Nan",
        "NaNf",
        "NaNd",
        "NaNF",
        "NaND",
        "+N",
        "+Na",
        "+Nan",
        "+NaNf",
        "+NaNd",
        "+NaNF",
        "+NaND",
        "-N",
        "-Na",
        "-Nan",
        "-NaNf",
        "-NaNd",
        "-NaNF",
        "-NaND",
        "I",
        "In",
        "Inf",
        "Infi",
        "Infin",
        "Infini",
        "Infinit",
        "InfinitY",
        "Infinityf",
        "InfinityF",
        "Infinityd",
        "InfinityD",
        "+I",
        "+In",
        "+Inf",
        "+Infi",
        "+Infin",
        "+Infini",
        "+Infinit",
        "+InfinitY",
        "+Infinityf",
        "+InfinityF",
        "+Infinityd",
        "+InfinityD",
        "-I",
        "-In",
        "-Inf",
        "-Infi",
        "-Infin",
        "-Infini",
        "-Infinit",
        "-InfinitY",
        "-Infinityf",
        "-InfinityF",
        "-Infinityd",
        "-InfinityD",
        "NaNInfinity",
        "InfinityNaN",
        "nan",
        "infinity"
    };

    public static void main(String [] argv) throws Exception {
        int i;
        double d;

        // Test valid NaN strings
        for(i = 0; i < NaNStrings.length; i++) {
            if(!Double.isNaN(d=Double.parseDouble(NaNStrings[i]))) {
                throw new RuntimeException("NaN string ``" + NaNStrings[i]
                                           + "'' did not parse as a NaN; returned " +
                                           d + " instead.");
            }
        }

        // Test valid Infinity strings
        for(i = 0; i < infinityStrings.length; i++) {
            if(!Double.isInfinite(d=Double.parseDouble(infinityStrings[i]))) {
                throw new RuntimeException("Infinity string ``" +
                                           infinityStrings[i] +
                                           "'' did not parse as infinity; returned " +
                                           d + "instead.");
            }
            // check sign of result

            boolean negative = (infinityStrings[i].charAt(0) == '-');
            if(d != (negative?Double.NEGATIVE_INFINITY:
                          Double.POSITIVE_INFINITY))
                throw new RuntimeException("Infinity has wrong sign;" +
                                           (negative?"positive instead of negative.":
                                            "negative instead of positive."));
        }

        // Test almost valid strings
        for(i = 0; i < invalidStrings.length; i++) {
            try {
                double result;
                d = Double.parseDouble(invalidStrings[i]);
                throw new RuntimeException("Invalid string ``" +
                                           invalidStrings[i]
                                           +"'' parsed as " + d + ".");
            }
            catch(NumberFormatException e) {
                // expected
            }
        }

    }
}
