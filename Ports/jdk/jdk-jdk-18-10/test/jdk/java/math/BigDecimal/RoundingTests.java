/*
 * Copyright (c) 2005, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6334849
 * @summary Tests of dropping digits near the scale threshold
 * @author Joseph D. Darcy
 */
import java.math.*;
public class RoundingTests {
    private static int roundingTests() {
        int failures = 0;
        BigDecimal bd1 = BigDecimal.valueOf(11, Integer.MIN_VALUE);
        BigDecimal bd2 = null;
        MathContext mc = new MathContext(1);
        try {
                bd2 = bd1.round(mc); // should overflow here
                failures++;
                System.err.printf("Did not get expected overflow rounding %s to %d digits, got %s%n",
                                   bd1, mc.getPrecision(), bd2);
        } catch(ArithmeticException e) {
            ; // expected
        }
        return failures;
    }

    public static void main(String argv[]) {
        int failures = 0;

        failures += roundingTests();

        if (failures > 0) {
            System.err.println("Encountered " + failures +
                               " failures while testing rounding.");
            throw new RuntimeException();
        }
    }
}
