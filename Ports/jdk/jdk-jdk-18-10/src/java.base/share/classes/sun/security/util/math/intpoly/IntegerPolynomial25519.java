/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package sun.security.util.math.intpoly;

import java.math.BigInteger;

/**
 * An IntegerFieldModuloP designed for use with the Curve25519.
 * The representation uses 10 signed long values.
 */

public class IntegerPolynomial25519 extends IntegerPolynomial {

    private static final int POWER = 255;
    private static final int SUBTRAHEND = 19;
    private static final int NUM_LIMBS = 10;
    private static final int BITS_PER_LIMB = 26;
    public static final BigInteger MODULUS
        = TWO.pow(POWER).subtract(BigInteger.valueOf(SUBTRAHEND));

    // BITS_PER_LIMB does not divide POWER, so reduction is a bit complicated
    // The constants below help split up values during reduction
    private static final int BIT_OFFSET = NUM_LIMBS * BITS_PER_LIMB - POWER;
    private static final int LIMB_MASK = -1 >>> (64 - BITS_PER_LIMB);
    private static final int RIGHT_BIT_OFFSET = BITS_PER_LIMB - BIT_OFFSET;

    public IntegerPolynomial25519() {
        super(BITS_PER_LIMB, NUM_LIMBS, 1, MODULUS);
    }

    @Override
    protected void reduceIn(long[] limbs, long v, int i) {
        long t0 = 19 * v;
        limbs[i - 10] += (t0 << 5) & LIMB_MASK;
        limbs[i - 9] += t0 >> 21;
    }

    @Override
    protected void finalCarryReduceLast(long[] limbs) {

        long reducedValue = limbs[numLimbs - 1] >> RIGHT_BIT_OFFSET;
        limbs[numLimbs - 1] -= reducedValue << RIGHT_BIT_OFFSET;
        limbs[0] += reducedValue * SUBTRAHEND;
    }

    @Override
    protected void reduce(long[] a) {

        // carry(8, 2)
        long carry8 = carryValue(a[8]);
        a[8] -= (carry8 << BITS_PER_LIMB);
        a[9] += carry8;

        long carry9 = carryValue(a[9]);
        a[9] -= (carry9 << BITS_PER_LIMB);

        // reduce(0, 1)
        long reducedValue10 = (carry9 * SUBTRAHEND);
        a[0] += ((reducedValue10 << BIT_OFFSET) & LIMB_MASK);
        a[1] += reducedValue10 >> RIGHT_BIT_OFFSET;

        // carry(0, 9)
        carry(a, 0, 9);
    }

    @Override
    protected void mult(long[] a, long[] b, long[] r) {
        long c0 = (a[0] * b[0]);
        long c1 = (a[0] * b[1]) + (a[1] * b[0]);
        long c2 = (a[0] * b[2]) + (a[1] * b[1]) + (a[2] * b[0]);
        long c3 = (a[0] * b[3]) + (a[1] * b[2]) + (a[2] * b[1]) + (a[3] * b[0]);
        long c4 = (a[0] * b[4]) + (a[1] * b[3]) + (a[2] * b[2]) + (a[3] * b[1]) + (a[4] * b[0]);
        long c5 = (a[0] * b[5]) + (a[1] * b[4]) + (a[2] * b[3]) + (a[3] * b[2]) + (a[4] * b[1]) + (a[5] * b[0]);
        long c6 = (a[0] * b[6]) + (a[1] * b[5]) + (a[2] * b[4]) + (a[3] * b[3]) + (a[4] * b[2]) + (a[5] * b[1]) + (a[6] * b[0]);
        long c7 = (a[0] * b[7]) + (a[1] * b[6]) + (a[2] * b[5]) + (a[3] * b[4]) + (a[4] * b[3]) + (a[5] * b[2]) + (a[6] * b[1]) + (a[7] * b[0]);
        long c8 = (a[0] * b[8]) + (a[1] * b[7]) + (a[2] * b[6]) + (a[3] * b[5]) + (a[4] * b[4]) + (a[5] * b[3]) + (a[6] * b[2]) + (a[7] * b[1]) + (a[8] * b[0]);
        long c9 = (a[0] * b[9]) + (a[1] * b[8]) + (a[2] * b[7]) + (a[3] * b[6]) + (a[4] * b[5]) + (a[5] * b[4]) + (a[6] * b[3]) + (a[7] * b[2]) + (a[8] * b[1]) + (a[9] * b[0]);
        long c10 = (a[1] * b[9]) + (a[2] * b[8]) + (a[3] * b[7]) + (a[4] * b[6]) + (a[5] * b[5]) + (a[6] * b[4]) + (a[7] * b[3]) + (a[8] * b[2]) + (a[9] * b[1]);
        long c11 = (a[2] * b[9]) + (a[3] * b[8]) + (a[4] * b[7]) + (a[5] * b[6]) + (a[6] * b[5]) + (a[7] * b[4]) + (a[8] * b[3]) + (a[9] * b[2]);
        long c12 = (a[3] * b[9]) + (a[4] * b[8]) + (a[5] * b[7]) + (a[6] * b[6]) + (a[7] * b[5]) + (a[8] * b[4]) + (a[9] * b[3]);
        long c13 = (a[4] * b[9]) + (a[5] * b[8]) + (a[6] * b[7]) + (a[7] * b[6]) + (a[8] * b[5]) + (a[9] * b[4]);
        long c14 = (a[5] * b[9]) + (a[6] * b[8]) + (a[7] * b[7]) + (a[8] * b[6]) + (a[9] * b[5]);
        long c15 = (a[6] * b[9]) + (a[7] * b[8]) + (a[8] * b[7]) + (a[9] * b[6]);
        long c16 = (a[7] * b[9]) + (a[8] * b[8]) + (a[9] * b[7]);
        long c17 = (a[8] * b[9]) + (a[9] * b[8]);
        long c18 = a[9] * b[9];

        carryReduce(r, c0, c1, c2, c3, c4, c5, c6, c7, c8,
            c9, c10, c11, c12, c13, c14, c15, c16, c17, c18);

    }

    private void carryReduce(long[] r, long c0, long c1, long c2,
                             long c3, long c4, long c5, long c6,
                             long c7, long c8, long c9, long c10,
                             long c11, long c12, long c13, long c14,
                             long c15, long c16, long c17, long c18) {
        // reduce(7,2)
        long reducedValue17 = (c17 * SUBTRAHEND);
        c7 += (reducedValue17 << BIT_OFFSET) & LIMB_MASK;
        c8 += reducedValue17 >> RIGHT_BIT_OFFSET;

        long reducedValue18 = (c18 * SUBTRAHEND);
        c8 += (reducedValue18 << BIT_OFFSET) & LIMB_MASK;
        c9 += reducedValue18 >> RIGHT_BIT_OFFSET;

        // carry(8,2)
        long carry8 = carryValue(c8);
        r[8] = c8 - (carry8 << BITS_PER_LIMB);
        c9 += carry8;

        long carry9 = carryValue(c9);
        r[9] = c9 - (carry9 << BITS_PER_LIMB);
        c10 += carry9;

        // reduce(0,7)
        long reducedValue10 = (c10 * SUBTRAHEND);
        r[0] = c0 + ((reducedValue10 << BIT_OFFSET) & LIMB_MASK);
        c1 += reducedValue10 >> RIGHT_BIT_OFFSET;

        long reducedValue11 = (c11 * SUBTRAHEND);
        r[1] = c1 + ((reducedValue11 << BIT_OFFSET) & LIMB_MASK);
        c2 += reducedValue11 >> RIGHT_BIT_OFFSET;

        long reducedValue12 = (c12 * SUBTRAHEND);
        r[2] = c2 + ((reducedValue12 << BIT_OFFSET) & LIMB_MASK);
        c3 += reducedValue12 >> RIGHT_BIT_OFFSET;

        long reducedValue13 = (c13 * SUBTRAHEND);
        r[3] = c3 + ((reducedValue13 << BIT_OFFSET) & LIMB_MASK);
        c4 += reducedValue13 >> RIGHT_BIT_OFFSET;

        long reducedValue14 = (c14 * SUBTRAHEND);
        r[4] = c4 + ((reducedValue14 << BIT_OFFSET) & LIMB_MASK);
        c5 += reducedValue14 >> RIGHT_BIT_OFFSET;

        long reducedValue15 = (c15 * SUBTRAHEND);
        r[5] = c5 + ((reducedValue15 << BIT_OFFSET) & LIMB_MASK);
        c6 += reducedValue15 >> RIGHT_BIT_OFFSET;

        long reducedValue16 = (c16 * SUBTRAHEND);
        r[6] = c6 + ((reducedValue16 << BIT_OFFSET) & LIMB_MASK);
        r[7] = c7 + (reducedValue16 >> RIGHT_BIT_OFFSET);

        // carry(0,9)
        carry(r, 0, 9);
    }
    @Override
    protected void square(long[] a, long[] r) {

        // Use grade-school multiplication with a simple squaring optimization.
        // Multiply into primitives to avoid the temporary array allocation.
        // This is equivalent to the following code:
        //  long[] c = new long[2 * NUM_LIMBS - 1];
        //  for(int i = 0; i < NUM_LIMBS; i++) {
        //      c[2 * i] = a[i] * a[i];
        //      for(int j = i + 1; j < NUM_LIMBS; j++) {
        //          c[i + j] += 2 * a[i] * a[j]
        //      }
        //  }

        long c0 = a[0] * a[0];
        long c1 = 2 * a[0] * a[1];
        long c2 = a[1] * a[1] + 2 * a[0] * a[2];
        long c3 = 2 * (a[0] * a[3] + a[1] * a[2]);
        long c4 = a[2] * a[2] + 2 * (a[0] * a[4] + a[1] * a[3]);
        long c5 = 2 * (a[0] * a[5] + a[1] * a[4] + a[2] * a[3]);
        long c6 = a[3] * a[3] + 2 * (a[0] * a[6] + a[1] * a[5] + a[2] * a[4]);
        long c7 = 2 * (a[0] * a[7] + a[1] * a[6] + a[2] * a[5] + a[3] * a[4]);
        long c8 = a[4] * a[4] + 2 * (a[0] * a[8] + a[1] * a[7] + a[2] * a[6] + a[3] * a[5]);
        long c9 = 2 * (a[0] * a[9] + a[1] * a[8] + a[2] * a[7] + a[3] * a[6] + a[4] * a[5]);
        long c10 = a[5] * a[5] + 2 * (a[1] * a[9] + a[2] * a[8] + a[3] * a[7] + a[4] * a[6]);
        long c11 = 2 * (a[2] * a[9] + a[3] * a[8] + a[4] * a[7] + a[5] * a[6]);
        long c12 = a[6] * a[6] + 2 * (a[3] * a[9] + a[4] * a[8] + a[5] * a[7]);
        long c13 = 2 * (a[4] * a[9] + a[5] * a[8] + a[6] * a[7]);
        long c14 = a[7] * a[7] + 2 * (a[5] * a[9] + a[6] * a[8]);
        long c15 = 2 * (a[6] * a[9] + a[7] * a[8]);
        long c16 = a[8] * a[8] + 2 * a[7] * a[9];
        long c17 = 2 * a[8] * a[9];
        long c18 = a[9] * a[9];

        carryReduce(r, c0, c1, c2, c3, c4, c5, c6, c7, c8,
            c9, c10, c11, c12, c13, c14, c15, c16, c17, c18);
    }


}
