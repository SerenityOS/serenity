/*
 * Copyright (c) 2003, Oracle and/or its affiliates. All rights reserved.
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
 *I
 * @test
 * @bug 4900727
 * @summary Unit test for supplementary characters (JSR-204)
 */

import java.util.StringTokenizer;

public class Supplementary {
    public static void main(String[] args) {
        String text =
            "ab\uD800\uDC00\uD800\uDC01cd\uD800\uDC00\uD800xy \uD801\uDC00z\t123\uDCFF456";
        String delims = " \t\r\n\f.\uD800\uDC00,:;";
        String[] expected =
            {"ab", "\uD800\uDC01cd", "\uD800xy", "\uD801\uDC00z", "123\uDCFF456" };
        testTokenizer(text, delims, expected);

        delims = " \t\r\n\f.,:;\uDCFF";
        expected = new String[] { "ab\uD800\uDC00\uD800\uDC01cd\uD800\uDC00\uD800xy",
                                  "\uD801\uDC00z",
                                  "123",
                                  "456" };
        testTokenizer(text, delims, expected);

        delims = "\uD800";
        expected = new String[] { "ab\uD800\uDC00\uD800\uDC01cd\uD800\uDC00",
                                  "xy \uD801\uDC00z\t123\uDCFF456" };
        testTokenizer(text, delims, expected);
    }

    static void testTokenizer(String text, String delims, String[] expected) {
        StringTokenizer tokenizer = new StringTokenizer(text, delims);
        int n = tokenizer.countTokens();
        if (n != expected.length) {
            throw new RuntimeException("countToken(): wrong value " + n
                                       + ", expected " + expected.length);
        }
        int i = 0;
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            if (!token.equals(expected[i++])) {
                throw new RuntimeException("nextToken(): wrong token. got \""
                                           + token + "\", expected \"" + expected[i-1]);
            }
        }
        if (i != expected.length) {
            throw new RuntimeException("unexpected the number of tokens: " + i
                                       + ", expected " + expected.length);
        }
    }
}
