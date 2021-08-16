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
 * @bug 8200698
 * @summary Tests that exceptions are thrown for ops which would overflow
 * @requires (sun.arch.data.model == "64" & os.maxMemory >= 4g)
 * @run testng/othervm -Xmx4g LargeValueExceptions
 */
import java.math.BigInteger;
import static java.math.BigInteger.ONE;
import org.testng.annotations.Test;

//
// The intent of this test is to probe the boundaries between overflow and
// non-overflow, principally for multiplication and squaring, specifically
// the largest values which should not overflow and the smallest values which
// should. The transition values used are not necessarily at the exact
// boundaries but should be "close." Quite a few different values were used
// experimentally before settling on the ones in this test. For multiplication
// and squaring all cases are exercised: definite overflow and non-overflow
// which can be detected "up front," and "indefinite" overflow, i.e., overflow
// which cannot be detected up front so further calculations are required.
//
// Testing negative values is unnecessary. For both multiplication and squaring
// the paths lead to the Toom-Cook algorithm where the signum is used only to
// determine the sign of the result and not in the intermediate calculations.
// This is also true for exponentiation.
//
// @Test annotations with optional element "enabled" set to "false" should
// succeed when "enabled" is set to "true" but they take too to run in the
// course of the typical regression test execution scenario.
//
public class LargeValueExceptions {
    // BigInteger.MAX_MAG_LENGTH
    private static final int MAX_INTS = 1 << 26;

    // Number of bits corresponding to MAX_INTS
    private static final long MAX_BITS = (0xffffffffL & MAX_INTS) << 5L;

    // Half BigInteger.MAX_MAG_LENGTH
    private static final int MAX_INTS_HALF = MAX_INTS / 2;

    // --- squaring ---

    // Largest no overflow determined by examining data lengths alone.
    @Test(enabled=false)
    public void squareNoOverflow() {
        BigInteger x = ONE.shiftLeft(16*MAX_INTS - 1).subtract(ONE);
        BigInteger y = x.multiply(x);
    }

    // Smallest no overflow determined by extra calculations.
    @Test(enabled=false)
    public void squareIndefiniteOverflowSuccess() {
        BigInteger x = ONE.shiftLeft(16*MAX_INTS - 1);
        BigInteger y = x.multiply(x);
    }

    // Largest overflow detected by extra calculations.
    @Test(expectedExceptions=ArithmeticException.class,enabled=false)
    public void squareIndefiniteOverflowFailure() {
        BigInteger x = ONE.shiftLeft(16*MAX_INTS).subtract(ONE);
        BigInteger y = x.multiply(x);
    }

    // Smallest overflow detected by examining data lengths alone.
    @Test(expectedExceptions=ArithmeticException.class)
    public void squareDefiniteOverflow() {
        BigInteger x = ONE.shiftLeft(16*MAX_INTS);
        BigInteger y = x.multiply(x);
    }

    // --- multiplication ---

    // Largest no overflow determined by examining data lengths alone.
    @Test(enabled=false)
    public void multiplyNoOverflow() {
        final int halfMaxBits = MAX_INTS_HALF << 5;

        BigInteger x = ONE.shiftLeft(halfMaxBits).subtract(ONE);
        BigInteger y = ONE.shiftLeft(halfMaxBits - 1).subtract(ONE);
        BigInteger z = x.multiply(y);
    }

    // Smallest no overflow determined by extra calculations.
    @Test(enabled=false)
    public void multiplyIndefiniteOverflowSuccess() {
        BigInteger x = ONE.shiftLeft((int)(MAX_BITS/2) - 1);
        long m = MAX_BITS - x.bitLength();

        BigInteger y = ONE.shiftLeft((int)(MAX_BITS/2) - 1);
        long n = MAX_BITS - y.bitLength();

        if (m + n != MAX_BITS) {
            throw new RuntimeException("Unexpected leading zero sum");
        }

        BigInteger z = x.multiply(y);
    }

    // Largest overflow detected by extra calculations.
    @Test(expectedExceptions=ArithmeticException.class,enabled=false)
    public void multiplyIndefiniteOverflowFailure() {
        BigInteger x = ONE.shiftLeft((int)(MAX_BITS/2)).subtract(ONE);
        long m = MAX_BITS - x.bitLength();

        BigInteger y = ONE.shiftLeft((int)(MAX_BITS/2)).subtract(ONE);
        long n = MAX_BITS - y.bitLength();

        if (m + n != MAX_BITS) {
            throw new RuntimeException("Unexpected leading zero sum");
        }

        BigInteger z = x.multiply(y);
    }

    // Smallest overflow detected by examining data lengths alone.
    @Test(expectedExceptions=ArithmeticException.class)
    public void multiplyDefiniteOverflow() {
        // multiply by 4 as MAX_INTS_HALF refers to ints
        byte[] xmag = new byte[4*MAX_INTS_HALF];
        xmag[0] = (byte)0xff;
        BigInteger x = new BigInteger(1, xmag);

        byte[] ymag = new byte[4*MAX_INTS_HALF + 1];
        ymag[0] = (byte)0xff;
        BigInteger y = new BigInteger(1, ymag);

        BigInteger z = x.multiply(y);
    }

    // --- exponentiation ---

    @Test(expectedExceptions=ArithmeticException.class)
    public void powOverflow() {
        BigInteger.TEN.pow(Integer.MAX_VALUE);
    }

    @Test(expectedExceptions=ArithmeticException.class)
    public void powOverflow1() {
        int shift = 20;
        int exponent = 1 << shift;
        BigInteger x = ONE.shiftLeft((int)(MAX_BITS / exponent));
        BigInteger y = x.pow(exponent);
    }

    @Test(expectedExceptions=ArithmeticException.class)
    public void powOverflow2() {
        int shift = 20;
        int exponent = 1 << shift;
        BigInteger x = ONE.shiftLeft((int)(MAX_BITS / exponent)).add(ONE);
        BigInteger y = x.pow(exponent);
    }

    @Test(expectedExceptions=ArithmeticException.class,enabled=false)
    public void powOverflow3() {
        int shift = 20;
        int exponent = 1 << shift;
        BigInteger x = ONE.shiftLeft((int)(MAX_BITS / exponent)).subtract(ONE);
        BigInteger y = x.pow(exponent);
    }

    @Test(enabled=false)
    public void powOverflow4() {
        int shift = 20;
        int exponent = 1 << shift;
        BigInteger x = ONE.shiftLeft((int)(MAX_BITS / exponent - 1)).add(ONE);
        BigInteger y = x.pow(exponent);
    }
}
