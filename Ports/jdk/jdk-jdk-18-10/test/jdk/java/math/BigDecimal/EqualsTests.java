/*
 * Copyright (c) 2009, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 1234567
 * @summary Test BigDecimal.equals() method.
 * @author xlu
 */

import java.math.*;
import static java.math.BigDecimal.*;

public class EqualsTests {

    public static void main(String argv[]) {
        int failures = 0;

        BigDecimal[][] testValues = {
            // The even index is supposed to return true for equals call and
            // the odd index is supposed to return false, i.e. not equal.
            {ZERO, ZERO},
            {ONE, TEN},

            {valueOf(Integer.MAX_VALUE), valueOf(Integer.MAX_VALUE)},
            {valueOf(Long.MAX_VALUE), valueOf(-Long.MAX_VALUE)},

            {valueOf(12345678), valueOf(12345678)},
            {valueOf(123456789), valueOf(123456788)},

            {new BigDecimal("123456789123456789123"),
             new BigDecimal(new BigInteger("123456789123456789123"))},
            {new BigDecimal("123456789123456789123"),
             new BigDecimal(new BigInteger("123456789123456789124"))},

            {valueOf(Long.MIN_VALUE), new BigDecimal("-9223372036854775808")},
            {new BigDecimal("9223372036854775808"), valueOf(Long.MAX_VALUE)},

            {valueOf(Math.round(Math.pow(2, 10))), new BigDecimal("1024")},
            {new BigDecimal("1020"), valueOf(Math.pow(2, 11))},

            {new BigDecimal(BigInteger.valueOf(2).pow(65)),
             new BigDecimal("36893488147419103232")},
            {new BigDecimal("36893488147419103231.81"),
             new BigDecimal("36893488147419103231.811"),
            }
        };

        boolean expected = Boolean.TRUE;
        for (BigDecimal[] testValuePair : testValues) {
            failures += equalsTest(testValuePair[0], testValuePair[1], expected);
            expected = !expected;
        }

        if (failures > 0) {
            throw new RuntimeException("Inccured " + failures +
                                       " failures while testing equals.");
        }
    }

    private static int equalsTest(BigDecimal l, BigDecimal r, boolean expected) {
        boolean result = l.equals(r);
        int failed = (result == expected) ? 0 : 1;

        if (failed == 1) {
            System.err.println(l + " .equals(" + r + ") => " + result +
                               "\n\tExpected " + expected);
        }
        return failed;
    }
}
