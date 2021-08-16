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
 * @bug 4944439
 * @summary Confirm that numbers where all digits after the decimal separator are 0
 * and which are between Long.MIN_VALUE and Long.MAX_VALUE are returned as Long(not double).
 */

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.util.Locale;

public class Bug4944439 {

    static boolean err = false;
    static DecimalFormat df;

    public static void main(String[] args) throws Exception {

        Locale defaultLoc = Locale.getDefault();
        Locale.setDefault(Locale.US);

        df = new DecimalFormat();
        String s = "-9223372036854775809";      // Long.MIN_VALUE-1
        check_Double(s);

        test(Long.MIN_VALUE, Long.MIN_VALUE+10);
        test(-10, 10);
        test(Long.MAX_VALUE-10, Long.MAX_VALUE-1);

        s = "9223372036854775807.00";   // Long.MAX_VALUE
        check_Long(s);
        s = "9223372036854775808";      // Long.MAX_VALUE+1
        check_Double(s);

        s = "-0.0";
        check_Double(s);
        s = "0.0";
        check_Long(s);

        Locale.setDefault(defaultLoc);

        if (err) {
            throw new RuntimeException("Wrong parsing with DecimalFormat");
        }
    }

    private static void test(long from, long to) throws Exception {
        for (long l = from; l <= to; l++) {
            check_Long(Long.toString(l) + ".00");
        }
    }

    private static void check_Long(String s) throws Exception {
        Number number = df.parse(s);
        if (!(number instanceof Long)) {
            err = true;
            System.err.println("Failed: DecimalFormat.parse(\"" + s +
                "\") should return a Long, but returned a " +
                number.getClass().getName());
        }

        int index = s.indexOf('.');
        Long l = Long.valueOf(s.substring(0, index));
        if (!l.equals(number)) {
            err = true;
            System.err.println("Failed: DecimalFormat.parse(" + s +
                ") should return a Long(" + l + "), but returned " + number);
        }
    }

    private static void check_Double(String s) throws Exception {
        Number number = df.parse(s);
        if (!(number instanceof Double)) {
            err = true;
            System.err.println("Failed: DecimalFormat.parse(\"" + s +
                "\") should return a Double, but returned a " +
                number.getClass().getName());
        }

        Double d = Double.valueOf(s);
        if (!d.equals(number)) {
            err = true;
            System.err.println("Failed: DecimalFormat.parse(" + s +
                ") should return a Double(" + d + "), but returned " + number);
        }
    }
}
