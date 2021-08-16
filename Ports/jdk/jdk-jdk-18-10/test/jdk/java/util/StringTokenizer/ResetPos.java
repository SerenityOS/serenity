/*
 * Copyright (c) 1999, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 4238266 @summary Reset the currentposition of
 * StringTokenizer if delimiters changed in a invocation of nextToken() after
 * invoking hasMoreTokens()
 */


import java.util.StringTokenizer;

public class ResetPos {

    static void checkValue(String val, String checkVal) {
        System.out.println("Comparing \""+ val + "\" <----> \"" + checkVal +
                "\"");
        if (!val.equals(checkVal))
            throw new RuntimeException("Test failed");
    }

    public static void main(String[] argv) {
        // Simple test
        StringTokenizer st1 = new StringTokenizer("ab", "b", true);
        checkValue("a", st1.nextToken("b"));
        st1.hasMoreTokens();
        checkValue("b", st1.nextToken(""));

        // Test with retDelims set to true
        StringTokenizer st2 = new StringTokenizer("abcd efg", "abc", true);
        st2.hasMoreTokens();
        checkValue("a", st2.nextToken("bc"));
        st2.hasMoreTokens();
        checkValue("b", st2.nextToken());
        st2.hasMoreTokens();
        checkValue("cd", st2.nextToken(" ef"));
        st2.hasMoreTokens();
        checkValue(" ", st2.nextToken(" "));
        st2.hasMoreTokens();
        checkValue("ef", st2.nextToken("g"));
        st2.hasMoreTokens();
        checkValue("g", st2.nextToken("g"));

        // Test with changing delimiters
        StringTokenizer st3 = new StringTokenizer("this is,a interesting,sentence of small, words", ",");
        st3.hasMoreTokens();
        checkValue("this is", st3.nextToken()); // "this is"
        st3.hasMoreTokens();
        checkValue(",a", st3.nextToken(" "));   // ",a"
        st3.hasMoreTokens();
        checkValue(" interesting", st3.nextToken(",")); // " interesting"
        st3.hasMoreTokens();
        checkValue(",sentence", st3.nextToken(" ")); // ",sentence"
        st3.hasMoreTokens();
        checkValue(" of small", st3.nextToken(",")); // " of small"
        st3.hasMoreTokens();
        checkValue(" words", st3.nextToken()); // " words"
    }
}
