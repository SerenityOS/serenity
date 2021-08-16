/*
 * Copyright (c) 2006, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6473768
 * @summary Tests of BigDecimal.compareTo
 * @author Joseph D. Darcy
 */
import java.math.*;
import static java.math.BigDecimal.*;

public class CompareToTests {
    private static int compareToTests() {
        int failures = 0;

        final BigDecimal MINUS_ONE = BigDecimal.ONE.negate();

        // First operand, second operand, expected compareTo result
        BigDecimal [][] testCases = {
            // Basics
            {valueOf(0),        valueOf(0),     ZERO},
            {valueOf(0),        valueOf(1),     MINUS_ONE},
            {valueOf(1),        valueOf(2),     MINUS_ONE},
            {valueOf(2),        valueOf(1),     ONE},
            {valueOf(10),       valueOf(10),    ZERO},

            // Significands would compare differently than scaled value
            {valueOf(2,1),      valueOf(2),     MINUS_ONE},
            {valueOf(2,-1),     valueOf(2),     ONE},
            {valueOf(1,1),      valueOf(2),     MINUS_ONE},
            {valueOf(1,-1),     valueOf(2),     ONE},
            {valueOf(5,-1),     valueOf(2),     ONE},

            // Boundary and near boundary values
            {valueOf(Long.MAX_VALUE),            valueOf(Long.MAX_VALUE), ZERO},
            {valueOf(Long.MAX_VALUE).negate(),   valueOf(Long.MAX_VALUE), MINUS_ONE},

            {valueOf(Long.MAX_VALUE-1),          valueOf(Long.MAX_VALUE), MINUS_ONE},
            {valueOf(Long.MAX_VALUE-1).negate(), valueOf(Long.MAX_VALUE), MINUS_ONE},

            {valueOf(Long.MIN_VALUE),            valueOf(Long.MAX_VALUE), MINUS_ONE},
            {valueOf(Long.MIN_VALUE).negate(),   valueOf(Long.MAX_VALUE), ONE},

            {valueOf(Long.MIN_VALUE+1),          valueOf(Long.MAX_VALUE), MINUS_ONE},
            {valueOf(Long.MIN_VALUE+1).negate(), valueOf(Long.MAX_VALUE), ZERO},

            {valueOf(Long.MAX_VALUE),            valueOf(Long.MIN_VALUE), ONE},
            {valueOf(Long.MAX_VALUE).negate(),   valueOf(Long.MIN_VALUE), ONE},

            {valueOf(Long.MAX_VALUE-1),          valueOf(Long.MIN_VALUE), ONE},
            {valueOf(Long.MAX_VALUE-1).negate(), valueOf(Long.MIN_VALUE), ONE},

            {valueOf(Long.MIN_VALUE),            valueOf(Long.MIN_VALUE), ZERO},
            {valueOf(Long.MIN_VALUE).negate(),   valueOf(Long.MIN_VALUE), ONE},

            {valueOf(Long.MIN_VALUE+1),          valueOf(Long.MIN_VALUE), ONE},
            {valueOf(Long.MIN_VALUE+1).negate(), valueOf(Long.MIN_VALUE), ONE},
        };

        for (BigDecimal[] testCase : testCases) {
            BigDecimal a = testCase[0];
            BigDecimal a_negate = a.negate();
            BigDecimal b = testCase[1];
            BigDecimal b_negate = b.negate();
            int expected = testCase[2].intValue();

            failures += compareToTest(a,        b,         expected);
            failures += compareToTest(a_negate, b_negate, -expected);
        }


        return failures;
    }

    private static int compareToTest(BigDecimal a, BigDecimal b, int expected) {
        int result = a.compareTo(b);
        int failed = (result==expected) ? 0 : 1;
        if (failed == 1) {
            System.err.println("(" + a + ").compareTo(" + b + ") => " + result +
                               "\n\tExpected " + expected);
        }
        return failed;
    }

    public static void main(String argv[]) {
        int failures = 0;

        failures += compareToTests();

        if (failures > 0) {
            throw new RuntimeException("Incurred " + failures +
                                       " failures while testing exact compareTo.");
        }
    }
}
