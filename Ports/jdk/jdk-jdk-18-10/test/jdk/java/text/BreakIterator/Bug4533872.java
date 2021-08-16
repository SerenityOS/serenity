/*
 * Copyright (c) 2003, 2016, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 4533872 4640853
 * @library /java/text/testlib
 * @summary Unit tests for supplementary character support (JSR-204) and Unicode 4.0 support
 */

import java.text.BreakIterator;
import java.util.Locale;

public class Bug4533872 extends IntlTest {

    public static void main(String[] args) throws Exception {
        new Bug4533872().run(args);
    }

    static final String[] given = {
      /* Lu Nd    Lu     Ll    */
        "XYZ12345 ABCDE  abcde",
      /* Nd Lo          Nd  Lu Po    Lu   Ll    */
        "123\uD800\uDC00345 ABC\uFF61XYZ  abc",
      /* Nd Lo          Nd  Lu Po          Lu   Ll    */
        "123\uD800\uDC00345 ABC\uD800\uDD00XYZ  abc",
      /* Lu Ll Cs    Ll Cs    Lu Lo          Lu  */
        "ABCabc\uDC00xyz\uD800ABC\uD800\uDC00XYZ",
    };

    // Golden data for TestNext(), TestBoundar() and TestPrintEach*ward()
    static final String[][] expected = {
        {"XYZ12345", " ", "ABCDE", "  ", "abcde"},
        {"123\uD800\uDC00345", " ", "ABC", "\uFF61", "XYZ", "  ", "abc"},
        {"123\uD800\uDC00345", " ", "ABC", "\uD800\uDD00", "XYZ", "  ", "abc"},
        {"ABCabc", "\uDC00", "xyz", "\uD800", "ABC\uD800\uDC00XYZ"},
    };

    BreakIterator iter;
    int start, end, current;

    /*
     * Test for next(int n)
     */
    void TestNext() {
        iter = BreakIterator.getWordInstance(Locale.US);

        for (int i = 0; i < given.length; i++) {
            iter.setText(given[i]);
            start = iter.first();
            int j = expected[i].length - 1;
            start = iter.next(j);
            end = iter.next();

            if (!expected[i][j].equals(given[i].substring(start, end))) {
                errln("Word break failure: printEachForward() expected:<" +
                      expected[i][j] + ">, got:<" +
                      given[i].substring(start, end) +
                      "> start=" + start + "  end=" + end);
            }
        }
    }

    /*
     * Test for isBoundary(int n)
     */
    void TestIsBoundary() {
        iter = BreakIterator.getWordInstance(Locale.US);

        for (int i = 0; i < given.length; i++) {
            iter.setText(given[i]);

            start = iter.first();
            end = iter.next();

            while (end < given[i].length()) {
                if (!iter.isBoundary(end)) {
                    errln("Word break failure: isBoundary() This should be a boundary. Index=" +
                          end + " for " + given[i]);
                }
                end = iter.next();
            }
        }
    }


    /*
     * The followig test cases were made based on examples in BreakIterator's
     * API Doc.
     */

    /*
     * Test mainly for next() and current()
     */
    void TestPrintEachForward() {
        iter = BreakIterator.getWordInstance(Locale.US);

        for (int i = 0; i < given.length; i++) {
            iter.setText(given[i]);
            start = iter.first();

            // Check current()'s return value - should be same as first()'s.
            current = iter.current();
            if (start != current) {
                errln("Word break failure: printEachForward() Unexpected current value: current()=" +
                      current + ", expected(=first())=" + start);
            }

            int j = 0;
            for (end = iter.next();
                 end != BreakIterator.DONE;
                 start = end, end = iter.next(), j++) {

                // Check current()'s return value - should be same as next()'s.
                current = iter.current();
                if (end != current) {
                    errln("Word break failure: printEachForward() Unexpected current value: current()=" +
                          current + ", expected(=next())=" + end);
                }

                if (!expected[i][j].equals(given[i].substring(start, end))) {
                    errln("Word break failure: printEachForward() expected:<" +
                          expected[i][j] + ">, got:<" +
                          given[i].substring(start, end) +
                          "> start=" + start + "  end=" + end);
                }
            }
        }
    }

    /*
     * Test mainly for previous() and current()
     */
    void TestPrintEachBackward() {
        iter = BreakIterator.getWordInstance(Locale.US);

        for (int i = 0; i < given.length; i++) {
            iter.setText(given[i]);
            end = iter.last();

            // Check current()'s return value - should be same as last()'s.
            current = iter.current();
            if (end != current) {
                errln("Word break failure: printEachBackward() Unexpected current value: current()=" +
                      current + ", expected(=last())=" + end);
            }

            int j;
            for (start = iter.previous(), j = expected[i].length-1;
                 start != BreakIterator.DONE;
                 end = start, start = iter.previous(), j--) {

                // Check current()'s return value - should be same as previous()'s.
                current = iter.current();
                if (start != current) {
                    errln("Word break failure: printEachBackward() Unexpected current value: current()=" +
                          current + ", expected(=previous())=" + start);
                }

                if (!expected[i][j].equals(given[i].substring(start, end))) {
                    errln("Word break failure: printEachBackward() expected:<" +
                          expected[i][j] + ">, got:<" +
                          given[i].substring(start, end) +
                          "> start=" + start + "  end=" + end);
                }
            }
        }
    }

    /*
     * Test mainly for following() and previous()
     */
    void TestPrintAt_1() {
        iter = BreakIterator.getWordInstance(Locale.US);

        int[][] index = {
            {2, 8, 10, 15, 17},
            {1, 8, 10, 12, 15, 17, 20},
            {3, 8, 10, 13, 16, 18, 20},
            {4, 6,  9, 10, 16},
        };

        for (int i = 0; i < given.length; i++) {
            iter.setText(given[i]);
            for (int j = index[i].length-1; j >= 0; j--) {
                end = iter.following(index[i][j]);
                start = iter.previous();

                if (!expected[i][j].equals(given[i].substring(start, end))) {
                    errln("Word break failure: printAt_1() expected:<" +
                          expected[i][j] + ">, got:<" +
                          given[i].substring(start, end) +
                          "> start=" + start + "  end=" + end);
                }
            }
        }
    }

    /*
     * Test mainly for preceding() and next()
     */
    void TestPrintAt_2() {
        iter = BreakIterator.getWordInstance(Locale.US);

        int[][] index = {
            {2, 9, 10, 15, 17},
            {1, 9, 10, 13, 16, 18, 20},
            {4, 9, 10, 13, 16, 18, 20},
            {6, 7, 10, 11, 15},
        };

        for (int i = 0; i < given.length; i++) {
            iter.setText(given[i]);

            // Check preceding(0)'s return value - should equals BreakIterator.DONE.
            if (iter.preceding(0) != BreakIterator.DONE) {
                 errln("Word break failure: printAt_2() expected:-1(BreakIterator.DONE), got:" +
                       iter.preceding(0));
            }

            for (int j = 0; j < index[i].length; j++) {
                start = iter.preceding(index[i][j]);
                end = iter.next();

                if (!expected[i][j].equals(given[i].substring(start, end))) {
                    errln("Word break failure: printAt_2() expected:<" +
                          expected[i][j] + ">, got:<" +
                          given[i].substring(start, end) +
                          "> start=" + start + "  end=" + end);
                }
            }

            // Check next()'s return value - should equals BreakIterator.DONE.
            end = iter.last();
            start = iter.next();
            if (start != BreakIterator.DONE) {
                 errln("Word break failure: printAt_2() expected:-1(BreakIterator.DONE), got:" + start);
            }
        }
    }
}
