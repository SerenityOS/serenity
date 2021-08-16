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
 * @test
 * @bug 4916097
 * @summary Some exponent over/undeflow tests for the pow method
 * @author Joseph D. Darcy
 */

import java.math.*;

public class PowTests {
    static int zeroAndOneTests() {
        int failures = 0;

        BigDecimal[][] testCases = {
            {BigDecimal.valueOf(0, Integer.MAX_VALUE),  new BigDecimal(0),              BigDecimal.valueOf(1, 0)},
            {BigDecimal.valueOf(0, Integer.MAX_VALUE),  new BigDecimal(1),              BigDecimal.valueOf(0, Integer.MAX_VALUE)},
            {BigDecimal.valueOf(0, Integer.MAX_VALUE),  new BigDecimal(2),              BigDecimal.valueOf(0, Integer.MAX_VALUE)},
            {BigDecimal.valueOf(0, Integer.MAX_VALUE),  new BigDecimal(999999999),      BigDecimal.valueOf(0, Integer.MAX_VALUE)},

            {BigDecimal.valueOf(0, Integer.MIN_VALUE),  new BigDecimal(0),              BigDecimal.valueOf(1, 0)},
            {BigDecimal.valueOf(0, Integer.MIN_VALUE),  new BigDecimal(1),              BigDecimal.valueOf(0, Integer.MIN_VALUE)},
            {BigDecimal.valueOf(0, Integer.MIN_VALUE),  new BigDecimal(2),              BigDecimal.valueOf(0, Integer.MIN_VALUE)},
            {BigDecimal.valueOf(0, Integer.MIN_VALUE),  new BigDecimal(999999999),      BigDecimal.valueOf(0, Integer.MIN_VALUE)},

            {BigDecimal.valueOf(1, Integer.MAX_VALUE),  new BigDecimal(0),              BigDecimal.valueOf(1, 0)},
            {BigDecimal.valueOf(1, Integer.MAX_VALUE),  new BigDecimal(1),              BigDecimal.valueOf(1, Integer.MAX_VALUE)},
            {BigDecimal.valueOf(1, Integer.MAX_VALUE),  new BigDecimal(2),              null}, // overflow
            {BigDecimal.valueOf(1, Integer.MAX_VALUE),  new BigDecimal(999999999),      null}, // overflow

            {BigDecimal.valueOf(1, Integer.MIN_VALUE),  new BigDecimal(0),              BigDecimal.valueOf(1, 0)},
            {BigDecimal.valueOf(1, Integer.MIN_VALUE),  new BigDecimal(1),              BigDecimal.valueOf(1, Integer.MIN_VALUE)},
            {BigDecimal.valueOf(1, Integer.MIN_VALUE),  new BigDecimal(2),              null}, // underflow
            {BigDecimal.valueOf(1, Integer.MIN_VALUE),  new BigDecimal(999999999),      null}, // underflow
        };

        for(BigDecimal[] testCase: testCases) {
            int exponent = testCase[1].intValueExact();
            BigDecimal result;

            try{
                result = testCase[0].pow(exponent);
                if (!result.equals(testCase[2]) ) {
                    failures++;
                    System.err.println("Unexpected result while raising " +
                                       testCase[0] +
                                       " to the " + exponent + " power; expected " +
                                       testCase[2] + ", got " + result + ".");
                }
            } catch (ArithmeticException e) {
                if (testCase[2] != null) {
                    failures++;
                    System.err.println("Unexpected exception while raising " + testCase[0] +
                                       " to the " + exponent + " power.");

                }
            }
        }

        return failures;
    }

    public static void main(String argv[]) {
        int failures = 0;

        failures += zeroAndOneTests();

        if (failures > 0) {
            throw new RuntimeException("Incurred " + failures +
                                       " failures while testing pow methods.");
        }
    }

}
