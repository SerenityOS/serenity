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
 * @bug 8196399 8202537
 * @summary  test Formatter if any ArithmeticException is thrown while
 * formatting a number in the locale which does not use any
 * grouping, but specifies a grouping separator.
 * @library provider
 * @build provider/module-info provider/test.NumberFormatProviderImpl
 * @run main/othervm -Djava.locale.providers=SPI,COMPAT NoGroupingUsed
 */

import java.util.Formatter;
import java.util.Locale;

public class NoGroupingUsed {

    public static void main(String[] args) {
        Locale locale = new Locale("xx", "YY");
        String number = "1234567";
        String formatString = "%,d";

        try {
            testGrouping(locale, formatString, number);
        } catch (ArithmeticException ex) {
            throw new RuntimeException("[FAILED: ArithmeticException occurred"
                    + " while formatting the number: " + number + ", with"
                    + " format string: " + formatString + ", in locale: "
                    + locale, ex);
        }
    }

    private static void testGrouping(Locale locale, String formatString, String number) {
        // test using String.format
        String result = String.format(locale, formatString, Integer.parseInt(number));
        if (!number.equals(result)) {
            throw new RuntimeException("[FAILED: Incorrect formatting"
                    + " of number: " + number + " using String.format with format"
                    + " string: " + formatString + " in locale: " + locale
                    + ". Actual: " + result + ", Expected: " + number + "]");
        }

        // test using Formatter's format
        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb, locale);
        formatter.format(formatString, Integer.parseInt(number));
        if (!number.equals(sb.toString())) {
            throw new RuntimeException("[FAILED: Incorrect formatting"
                    + " of number: " + number + "using Formatter.format with"
                    + " format string: " + formatString + " in locale: " + locale
                    + ". Actual: " + sb.toString() + ", Expected: " + number + "]");
        }
    }
}
