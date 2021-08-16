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
 * @bug 4018937
 * @summary Confirm that DecimalFormat.parse() parses BigDecimal and BigInteger as expected.
 */

import java.math.*;
import java.text.*;
import java.util.*;

public class BigDecimalCompatibilityTest {

    static boolean err = false;

    static final String[] input_data = {
        "0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000",
        "1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890"
    };
    static final String[] exponents = {
        "E-100", "E100", "E-900", "E900", ""
    };
    static final int[] multipliers = {
        -1, 1, -100, 100, -9999, 9999
    };

    public static void main(String[] args) throws Exception {
        Locale loc = Locale.getDefault();
        Locale.setDefault(Locale.US);

        testBigDecimal();
        testBigInteger();

        Locale.setDefault(loc);

        if (err) {
            throw new RuntimeException("Error: Unexpected value");
        }
    }

    static private void testBigDecimal() {
        DecimalFormat df = new DecimalFormat();
        df.setParseBigDecimal(true);
        df.setMaximumFractionDigits(Integer.MAX_VALUE);

        for (int i = 0; i < input_data.length; i++) {
            for (int j = 0; j < input_data.length; j++) {
                for (int k = 0; k < input_data.length; k++) {
                    for (int l = 0; l < input_data.length; l++) {
                        for (int m = 0; m < exponents.length; m++) {
                            String s = input_data[i] + input_data[j] + '.' +
                                       input_data[k] + input_data[l] +
                                       exponents[m];
                            for (int n = 0; n < multipliers.length; n++) {
                                test(df, s, multipliers[n]);
                                test(df, '-'+s, multipliers[n]);
                            }
                        }
                    }
                }
            }
        }
    }

    static private void testBigInteger() {
        DecimalFormat df = new DecimalFormat();
        df.setParseBigDecimal(true);
        df.setMaximumFractionDigits(Integer.MAX_VALUE);

        for (int i = 0; i < input_data.length; i++) {
            for (int j = 0; j < input_data.length; j++) {
                String s = input_data[i] + input_data[j];
                for (int k = 0; k < multipliers.length; k++) {
                    test(df, s, multipliers[k]);
                    test(df, '-'+s, multipliers[k]);
                }
            }
        }
    }

    static void test(DecimalFormat df, String s, int multiplier) {
        df.setMultiplier(multiplier);

        Number num = null;
        try {
            num = df.parse(s);
        }
        catch (ParseException e) {
            err = true;
            System.err.println("Failed: Exception occurred: " + e.getMessage());
            return;
        }

        BigDecimal bd = new BigDecimal(s);
        try {
           bd = bd.divide(new BigDecimal(multiplier));
        }
        catch (ArithmeticException e) {
           bd = bd.divide(new BigDecimal(multiplier), RoundingMode.HALF_EVEN);
        }
        check(num, bd, multiplier);
    }

    static void check(Number got, BigDecimal expected, int multiplier) {
        if (!got.equals(expected)) {
            err = true;
            System.err.println("Failed: got:" + got +
                               ", expected: " + expected +
                               ", multiplier=" + multiplier);
        }
    }
}
