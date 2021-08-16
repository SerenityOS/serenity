/*
 * Copyright (c) 2011, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6513074
 * @summary Confirm that JIS X 0213 characters are processed in line-breaking properly.
 */

import java.text.*;
import java.util.*;

public class Bug6513074 {

    private static final String[][] source = {
        {"\ufa30\ufa31 \ufa69\ufa6a",
         "JIS X 0213 compatibility additions (\\uFA30-\\uFA6A)"},
    };

    private static final String[] expected_line = {
        "\ufa30/\ufa31 /\ufa69/\ufa6a/",
    };

    private static final String[] expected_word = {
        "\ufa30\ufa31/ /\ufa69\ufa6a/",
    };

    private static final String[] expected_char = {
        "\ufa30/\ufa31/ /\ufa69/\ufa6a/",
    };


    private static boolean err = false;

    public static void main(String[] args) {
        Locale defaultLocale = Locale.getDefault();
        if (defaultLocale.getLanguage().equals("th")) {
            Locale.setDefault(Locale.JAPAN);
            test6513074();
            Locale.setDefault(defaultLocale);
        } else {
            test6513074();
        }

        if (err) {
            throw new RuntimeException("Failed: Incorrect Text-breaking.");
        }
    }


    private static void test6513074() {
        BreakIterator bi = BreakIterator.getLineInstance(Locale.JAPAN);
        for (int i = 0; i < source.length; i++) {
            testBreakIterator(bi, "Line", source[i][0], expected_line[i], source[i][1]);
        }

        bi = BreakIterator.getWordInstance(Locale.JAPAN);
        for (int i = 0; i < source.length; i++) {
            testBreakIterator(bi, "Word", source[i][0], expected_word[i], source[i][1]);
        }

        bi = BreakIterator.getCharacterInstance(Locale.JAPAN);
        for (int i = 0; i < source.length; i++) {
            testBreakIterator(bi, "Character", source[i][0], expected_char[i], source[i][1]);
        }
    }

    private static void testBreakIterator(BreakIterator bi,
                                          String type,
                                          String source,
                                          String expected,
                                          String description) {
        bi.setText(source);
        int start = bi.first();
        int end = bi.next();
        StringBuilder sb =  new StringBuilder();

        for (; end != BreakIterator.DONE; start = end, end = bi.next()) {
            sb.append(source.substring(start,end));
            sb.append('/');
        }

        if (!expected.equals(sb.toString())) {
            System.err.println("Failed: Incorrect " + type + "-breaking for " +
                description +
                "\n\tExpected: " + toString(expected) +
                "\n\tGot:      " + toString(sb.toString()));
            err = true;
        }
    }

    private static String toString(String s) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < s.length(); i++) {
            sb.append("  0x" + Integer.toHexString(s.charAt(i)));
        }

        return sb.toString();
    }

}
