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
 * @bug 5037596
 * @summary Verify bitwise conversion works for non-canonical NaN values
 * @library ../Math
 * @build DoubleConsts
 * @run main BitwiseConversion
 * @author Joseph D. Darcy
 */

import static java.lang.Double.*;

public class BitwiseConversion {
    static int testNanCase(long x) {
        int errors  = 0;
        // Strip out sign and exponent bits
        long y = x & DoubleConsts.SIGNIF_BIT_MASK;

        double values[] = {
            longBitsToDouble(DoubleConsts.EXP_BIT_MASK | y),
            longBitsToDouble(DoubleConsts.SIGN_BIT_MASK | DoubleConsts.EXP_BIT_MASK | y)
        };

        for(double value: values) {
            if (!isNaN(value)) {
                throw new RuntimeException("Invalid input " + y +
                                           "yielded non-NaN" + value);
            }
            long converted = doubleToLongBits(value);
            if (converted != 0x7ff8000000000000L) {
                errors++;
                System.err.format("Non-canoncial NaN bits returned: %x%n",
                                  converted);
            }
        }
        return errors;
    }

    public static void main(String... argv) {
        int errors = 0;

        for (int i = 0; i < DoubleConsts.SIGNIFICAND_WIDTH-1; i++) {
            errors += testNanCase(1L<<i);
        }

        if (doubleToLongBits(Double.POSITIVE_INFINITY)
                != 0x7ff0000000000000L) {
            errors++;
            System.err.println("Bad conversion for +infinity.");
        }

        if (doubleToLongBits(Double.NEGATIVE_INFINITY)
                != 0xfff0000000000000L) {
            errors++;
            System.err.println("Bad conversion for -infinity.");
        }

        if (errors > 0)
            throw new RuntimeException();
    }
}
