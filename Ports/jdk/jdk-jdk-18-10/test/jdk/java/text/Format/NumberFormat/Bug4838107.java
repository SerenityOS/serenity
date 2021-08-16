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

/*
 * @test
 * @bug 4838107 8008577
 * @summary Confirm that DecimalFormat can format a number with negative exponent number correctly.
 * @library /java/text/testlib
 * @run main/othervm -Djava.locale.providers=COMPAT,SPI Bug4838107
 */

import java.math.*;
import java.util.*;
import java.text.*;

public class Bug4838107 extends IntlTest {

    static DecimalFormat df;
    static DecimalFormatSymbols dfs;
    static boolean err = false;

    static public void main(String[] args) {
        Locale defaultLoc = Locale.getDefault();
        Locale.setDefault(Locale.US);

        /**
         * This bug is about exponential formatting. But I added test cases for:
         *   - Double and BigDecimal numbers which don't have exponent parts.
         *   - Long and BigInteger numbers which don't support exponential
         *     notation.
         * because there are few test cases for suffix and prefix.
         * And also, I added test cases to guarantee further formatting and
         * parsing using the same DecimalFormat instance will not change the
         * Number's value anymore.
         */

        test_double();
        test_long();
        test_BigDecimal();
        test_BigInteger();

        Locale.setDefault(defaultLoc);

        if (err) {
            throw new RuntimeException("Wrong format with DecimalFormat");
        }
    }

    static void test_double() {
        df = new DecimalFormat();
        dfs = df.getDecimalFormatSymbols();

        /* Test with default pattern */
        test(1234D,    "1,234");
        test(0.1234,  "0.123");     // rounded
        test(-1234D,   "-1,234");
        test(-0.1234, "-0.123");    // rounded

        test(Double.POSITIVE_INFINITY, "\u221e");
        test(Double.NEGATIVE_INFINITY, "-\u221e");
        test(Double.NaN, "\ufffd"); // without prefix and suffix
        test(0.0,  "0");
        test(-0.0, "-0");   // with the minus sign

        /* Specify a pattern and the minus sign. */
        prepareFormatter("<P>#.###E00<S>", 'm');
        test(1234D,    "<P>1.234E03<S>");
        test(0.1234,  "<P>1.234Em01<S>");
        test(-1234D,   "m<P>1.234E03<S>");
        test(-0.1234, "m<P>1.234Em01<S>");

        prepareFormatter("<P>#.###E00<S>;#.###E00", 'm');
        test(1234D,    "<P>1.234E03<S>");
        test(0.1234,  "<P>1.234Em01<S>");
        test(-1234D,   "1.234E03");
        test(-0.1234, "1.234Em01");

        prepareFormatter("#.###E00;<P>#.###E00<S>", 'm');
        test(1234D,    "1.234E03");
        test(0.1234,  "1.234Em01");
        test(-1234D,   "<P>1.234E03<S>");
        test(-0.1234, "<P>1.234Em01<S>");

        prepareFormatter("<P>#.###E00<S>;<p>-#.###E00<s>", 'm');
        test(1234D,    "<P>1.234E03<S>");
        test(0.1234,  "<P>1.234Em01<S>");
        test(-1234D,   "<p>m1.234E03<s>");
        test(-0.1234, "<p>m1.234Em01<s>");

        test(Double.POSITIVE_INFINITY, "<P>\u221e<S>");
        test(Double.NEGATIVE_INFINITY, "<p>m\u221e<s>");
        test(Double.NaN, "\ufffd"); // without prefix and suffix
        test(0.0,  "<P>0E00<S>");
        test(-0.0, "<p>m0E00<s>");  // with the minus sign
    }

    static void test_BigDecimal() {
        df = new DecimalFormat();
        dfs = df.getDecimalFormatSymbols();

        /* Test with default pattern */
        test(new BigDecimal("123456789012345678901234567890"),
             "123,456,789,012,345,678,901,234,567,890");
        test(new BigDecimal("0.000000000123456789012345678901234567890"),
             "0");
        test(new BigDecimal("-123456789012345678901234567890"),
             "-123,456,789,012,345,678,901,234,567,890");
        test(new BigDecimal("-0.000000000123456789012345678901234567890"),
              "-0");

        test(new BigDecimal("0"), "0");
        test(new BigDecimal("-0"), "0");

        /* Specify a pattern and the minus sign. */
        prepareFormatter("<P>#.####################E00<S>;<p>-#.####################E00<s>", 'm');
        test(new BigDecimal("123456789012345678901234567890"),
             "<P>1.23456789012345678901E29<S>");
        test(new BigDecimal("0.000000000123456789012345678901234567890"),
             "<P>1.23456789012345678901Em10<S>");
        test(new BigDecimal("-123456789012345678901234567890"),
             "<p>m1.23456789012345678901E29<s>");
        test(new BigDecimal("-0.000000000123456789012345678901234567890"),
              "<p>m1.23456789012345678901Em10<s>");

        test(new BigDecimal("0"), "<P>0E00<S>");
        test(new BigDecimal("-0"), "<P>0E00<S>");
    }

    static void test_long() {
        df = new DecimalFormat();
        dfs = df.getDecimalFormatSymbols();

        /* Test with default pattern */
        test(123456789L,  "123,456,789");
        test(-123456789L, "-123,456,789");

        test(0L, "0");
        test(-0L, "0");

        /* Specify a pattern and the minus sign. */
        prepareFormatter("<P>#,###<S>;<p>-#,###<s>", 'm');
        test(123456789L,  "<P>123,456,789<S>");
        test(-123456789L, "<p>m123,456,789<s>");

        test(0L, "<P>0<S>");
        test(-0L, "<P>0<S>");
    }

    static void test_BigInteger() {
        df = new DecimalFormat();
        dfs = df.getDecimalFormatSymbols();

        /* Test with default pattern */
        test(new BigInteger("123456789012345678901234567890"),
             "123,456,789,012,345,678,901,234,567,890");
        test(new BigInteger("-123456789012345678901234567890"),
             "-123,456,789,012,345,678,901,234,567,890");

        test(new BigInteger("0"), "0");
        test(new BigInteger("-0"), "0");

        /* Specify a pattern and the minus sign. */
        prepareFormatter("<P>#,###<S>;<p>-#,###<s>", 'm');
        test(new BigInteger("123456789012345678901234567890"),
             "<P>123,456,789,012,345,678,901,234,567,890<S>");
        test(new BigInteger("-123456789012345678901234567890"),
             "<p>m123,456,789,012,345,678,901,234,567,890<s>");

        test(new BigInteger("0"), "<P>0<S>");
        test(new BigInteger("-0"), "<P>0<S>");
    }

    static void prepareFormatter(String pattern, char minusSign) {
        dfs = df.getDecimalFormatSymbols();
        df.applyPattern(pattern);
        dfs.setMinusSign(minusSign);
        df.setDecimalFormatSymbols(dfs);
    }

    static void test(Number num, String str) {
        String formatted = df.format(num);
        if (!formatted.equals(str)) {
            err = true;
            System.err.println("    DecimalFormat format(" +
                               num.getClass().getName() +
                               ") error: \n\tnumber: " + num +
                               "\n\tminus sign: " + dfs.getMinusSign() +
                               "\n\tgot:        " + formatted +
                               "\n\texpected:   " + str);
            return;
        }

        if (num instanceof BigDecimal || num instanceof BigInteger) {
            df.setParseBigDecimal(true);
        }
        Number parsed1 = null, parsed2 = null;
        try {
            parsed1 = df.parse(formatted);
            formatted = df.format(parsed1);
            parsed2 = df.parse(formatted);
            if (!parsed1.equals(parsed2)) {
                err = true;
                System.err.println("    DecimalFormat roundtrip parse(" +
                                   num.getClass().getName() +
                                   ") error: \n\toriginal number:  " + str +
                                   "\n\tparsed number:    " + parsed1 +
                                   "  (" + parsed1.getClass().getName() + ")" +
                                   "\n\tformatted number: " + formatted +
                                   "\n\tre-parsed number: " + parsed2 +
                                   "  (" + parsed2.getClass().getName() + ")" +
                                   "\n\tminus sign: " + dfs.getMinusSign());
            }
        }
        catch (Exception e) {
            err = true;
            System.err.println("    DecimalFormat parse(" +
                               num.getClass().getName() +
                               ") threw an Exception:  " + e.getMessage() +
                               "\n\toriginal number:  " + str +
                               "\n\tparsed number   : " + parsed1 +
                               "  (" + parsed1.getClass().getName() + ")" +
                               "\n\tformatted number: " + formatted +
                               "\n\tre-parsed number: " + parsed2 +
                               "  (" + parsed2.getClass().getName() + ")" +
                               "\n\tminus sign: " + dfs.getMinusSign());
        }
    }
}
