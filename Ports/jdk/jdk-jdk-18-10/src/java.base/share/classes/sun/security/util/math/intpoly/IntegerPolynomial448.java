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
 * An IntegerFieldModuloP designed for use with the Curve448.
 * The representation uses 16 signed long values.
 */

public class IntegerPolynomial448 extends IntegerPolynomial {

    private static final int POWER = 448;
    private static final int NUM_LIMBS = 16;
    private static final int BITS_PER_LIMB = 28;
    public static final BigInteger MODULUS
        = TWO.pow(POWER).subtract(TWO.pow(POWER / 2))
            .subtract(BigInteger.valueOf(1));

    public IntegerPolynomial448() {
        super(BITS_PER_LIMB, NUM_LIMBS, 1, MODULUS);
    }

    @Override
    protected void reduceIn(long[] limbs, long v, int i) {
        limbs[i - 8] += v;
        limbs[i - 16] += v;
    }

    @Override
    protected void finalCarryReduceLast(long[] limbs) {
        long carry = limbs[numLimbs - 1] >> bitsPerLimb;
        limbs[numLimbs - 1] -= carry << bitsPerLimb;
        reduceIn(limbs, carry, numLimbs);
    }

    @Override
    protected void reduce(long[] a) {

        // carry(14, 2)
        long carry14 = carryValue(a[14]);
        a[14] -= (carry14 << BITS_PER_LIMB);
        a[15] += carry14;

        long carry15 = carryValue(a[15]);
        a[15] -= (carry15 << BITS_PER_LIMB);

        // reduce(0, 1)
        a[0] += carry15;
        a[8] += carry15;

        // carry(0, 15)
        carry(a, 0, 15);
    }

    @Override
    protected void mult(long[] a, long[] b, long[] r) {

        // Use grade-school multiplication into primitives to avoid the
        // temporary array allocation. This is equivalent to the following
        // code:
        //  long[] c = new long[2 * NUM_LIMBS - 1];
        //  for(int i = 0; i < NUM_LIMBS; i++) {
        //      for(int j - 0; j < NUM_LIMBS; j++) {
        //          c[i + j] += a[i] * b[j]
        //      }
        //  }

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
        long c10 = (a[0] * b[10]) + (a[1] * b[9]) + (a[2] * b[8]) + (a[3] * b[7]) + (a[4] * b[6]) + (a[5] * b[5]) + (a[6] * b[4]) + (a[7] * b[3]) + (a[8] * b[2]) + (a[9] * b[1]) + (a[10] * b[0]);
        long c11 = (a[0] * b[11]) + (a[1] * b[10]) + (a[2] * b[9]) + (a[3] * b[8]) + (a[4] * b[7]) + (a[5] * b[6]) + (a[6] * b[5]) + (a[7] * b[4]) + (a[8] * b[3]) + (a[9] * b[2]) + (a[10] * b[1]) + (a[11] * b[0]);
        long c12 = (a[0] * b[12]) + (a[1] * b[11]) + (a[2] * b[10]) + (a[3] * b[9]) + (a[4] * b[8]) + (a[5] * b[7]) + (a[6] * b[6]) + (a[7] * b[5]) + (a[8] * b[4]) + (a[9] * b[3]) + (a[10] * b[2]) + (a[11] * b[1]) + (a[12] * b[0]);
        long c13 = (a[0] * b[13]) + (a[1] * b[12]) + (a[2] * b[11]) + (a[3] * b[10]) + (a[4] * b[9]) + (a[5] * b[8]) + (a[6] * b[7]) + (a[7] * b[6]) + (a[8] * b[5]) + (a[9] * b[4]) + (a[10] * b[3]) + (a[11] * b[2]) + (a[12] * b[1]) + (a[13] * b[0]);
        long c14 = (a[0] * b[14]) + (a[1] * b[13]) + (a[2] * b[12]) + (a[3] * b[11]) + (a[4] * b[10]) + (a[5] * b[9]) + (a[6] * b[8]) + (a[7] * b[7]) + (a[8] * b[6]) + (a[9] * b[5]) + (a[10] * b[4]) + (a[11] * b[3]) + (a[12] * b[2]) + (a[13] * b[1]) + (a[14] * b[0]);
        long c15 = (a[0] * b[15]) + (a[1] * b[14]) + (a[2] * b[13]) + (a[3] * b[12]) + (a[4] * b[11]) + (a[5] * b[10]) + (a[6] * b[9]) + (a[7] * b[8]) + (a[8] * b[7]) + (a[9] * b[6]) + (a[10] * b[5]) + (a[11] * b[4]) + (a[12] * b[3]) + (a[13] * b[2]) + (a[14] * b[1]) + (a[15] * b[0]);
        long c16 = (a[1] * b[15]) + (a[2] * b[14]) + (a[3] * b[13]) + (a[4] * b[12]) + (a[5] * b[11]) + (a[6] * b[10]) + (a[7] * b[9]) + (a[8] * b[8]) + (a[9] * b[7]) + (a[10] * b[6]) + (a[11] * b[5]) + (a[12] * b[4]) + (a[13] * b[3]) + (a[14] * b[2]) + (a[15] * b[1]);
        long c17 = (a[2] * b[15]) + (a[3] * b[14]) + (a[4] * b[13]) + (a[5] * b[12]) + (a[6] * b[11]) + (a[7] * b[10]) + (a[8] * b[9]) + (a[9] * b[8]) + (a[10] * b[7]) + (a[11] * b[6]) + (a[12] * b[5]) + (a[13] * b[4]) + (a[14] * b[3]) + (a[15] * b[2]);
        long c18 = (a[3] * b[15]) + (a[4] * b[14]) + (a[5] * b[13]) + (a[6] * b[12]) + (a[7] * b[11]) + (a[8] * b[10]) + (a[9] * b[9]) + (a[10] * b[8]) + (a[11] * b[7]) + (a[12] * b[6]) + (a[13] * b[5]) + (a[14] * b[4]) + (a[15] * b[3]);
        long c19 = (a[4] * b[15]) + (a[5] * b[14]) + (a[6] * b[13]) + (a[7] * b[12]) + (a[8] * b[11]) + (a[9] * b[10]) + (a[10] * b[9]) + (a[11] * b[8]) + (a[12] * b[7]) + (a[13] * b[6]) + (a[14] * b[5]) + (a[15] * b[4]);
        long c20 = (a[5] * b[15]) + (a[6] * b[14]) + (a[7] * b[13]) + (a[8] * b[12]) + (a[9] * b[11]) + (a[10] * b[10]) + (a[11] * b[9]) + (a[12] * b[8]) + (a[13] * b[7]) + (a[14] * b[6]) + (a[15] * b[5]);
        long c21 = (a[6] * b[15]) + (a[7] * b[14]) + (a[8] * b[13]) + (a[9] * b[12]) + (a[10] * b[11]) + (a[11] * b[10]) + (a[12] * b[9]) + (a[13] * b[8]) + (a[14] * b[7]) + (a[15] * b[6]);
        long c22 = (a[7] * b[15]) + (a[8] * b[14]) + (a[9] * b[13]) + (a[10] * b[12]) + (a[11] * b[11]) + (a[12] * b[10]) + (a[13] * b[9]) + (a[14] * b[8]) + (a[15] * b[7]);
        long c23 = (a[8] * b[15]) + (a[9] * b[14]) + (a[10] * b[13]) + (a[11] * b[12]) + (a[12] * b[11]) + (a[13] * b[10]) + (a[14] * b[9]) + (a[15] * b[8]);
        long c24 = (a[9] * b[15]) + (a[10] * b[14]) + (a[11] * b[13]) + (a[12] * b[12]) + (a[13] * b[11]) + (a[14] * b[10]) + (a[15] * b[9]);
        long c25 = (a[10] * b[15]) + (a[11] * b[14]) + (a[12] * b[13]) + (a[13] * b[12]) + (a[14] * b[11]) + (a[15] * b[10]);
        long c26 = (a[11] * b[15]) + (a[12] * b[14]) + (a[13] * b[13]) + (a[14] * b[12]) + (a[15] * b[11]);
        long c27 = (a[12] * b[15]) + (a[13] * b[14]) + (a[14] * b[13]) + (a[15] * b[12]);
        long c28 = (a[13] * b[15]) + (a[14] * b[14]) + (a[15] * b[13]);
        long c29 = (a[14] * b[15]) + (a[15] * b[14]);
        long c30 = (a[15] * b[15]);

        carryReduce(r, c0, c1, c2, c3, c4, c5, c6, c7, c8, c9, c10, c11, c12,
            c13, c14, c15, c16, c17, c18, c19, c20, c21, c22, c23, c24, c25,
            c26, c27, c28, c29, c30);
    }

    private void carryReduce(long[] r, long c0, long c1, long c2, long c3,
                             long c4, long c5, long c6, long c7, long c8,
                             long c9, long c10, long c11, long c12, long c13,
                             long c14, long c15, long c16, long c17, long c18,
                             long c19, long c20, long c21, long c22, long c23,
                             long c24, long c25, long c26, long c27, long c28,
                             long c29, long c30) {

        // reduce(8, 7)
        c8 += c24;
        c16 += c24;

        c9 += c25;
        c17 += c25;

        c10 += c26;
        c18 += c26;

        c11 += c27;
        c19 += c27;

        c12 += c28;
        c20 += c28;

        c13 += c29;
        c21 += c29;

        c14 += c30;
        c22 += c30;

        // reduce(4, 4)
        r[4] = c4 + c20;
        r[12] = c12 + c20;

        r[5] = c5 + c21;
        r[13] = c13 + c21;

        r[6] = c6 + c22;
        c14 += c22;

        r[7] = c7 + c23;
        c15 += c23;

        //carry(14, 2)
        long carry14 = carryValue(c14);
        r[14] = c14 - (carry14 << BITS_PER_LIMB);
        c15 += carry14;

        long carry15 = carryValue(c15);
        r[15] = c15 - (carry15 << BITS_PER_LIMB);
        c16 += carry15;

        // reduce(0, 4)
        r[0] = c0 + c16;
        r[8] = c8 + c16;

        r[1] = c1 + c17;
        r[9] = c9 + c17;

        r[2] =  c2 + c18;
        r[10] = c10 + c18;

        r[3] = c3 + c19;
        r[11] = c11 + c19;

        // carry(0, 15)
        carry(r, 0, 15);
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
        long c10 = a[5] * a[5] + 2 * (a[0] * a[10] + a[1] * a[9] + a[2] * a[8] + a[3] * a[7] + a[4] * a[6]);
        long c11 = 2 * (a[0] * a[11] + a[1] * a[10] + a[2] * a[9] + a[3] * a[8] + a[4] * a[7] + a[5] * a[6]);
        long c12 = a[6] * a[6] + 2 * (a[0] * a[12] + a[1] * a[11] + a[2] * a[10] + a[3] * a[9] + a[4] * a[8] + a[5] * a[7]);
        long c13 = 2 * (a[0] * a[13] + a[1] * a[12] + a[2] * a[11] + a[3] * a[10] + a[4] * a[9] + a[5] * a[8] + a[6] * a[7]);
        long c14 = a[7] * a[7] + 2 * (a[0] * a[14] + a[1] * a[13] + a[2] * a[12] + a[3] * a[11] + a[4] * a[10] + a[5] * a[9] + a[6] * a[8]);
        long c15 = 2 * (a[0] * a[15] + a[1] * a[14] + a[2] * a[13] + a[3] * a[12] + a[4] * a[11] + a[5] * a[10] + a[6] * a[9] + a[7] * a[8]);
        long c16 = a[8] * a[8] + 2 * (a[1] * a[15] + a[2] * a[14] + a[3] * a[13] + a[4] * a[12] + a[5] * a[11] + a[6] * a[10] + a[7] * a[9]);
        long c17 = 2 * (a[2] * a[15] + a[3] * a[14] + a[4] * a[13] + a[5] * a[12] + a[6] * a[11] + a[7] * a[10] + a[8] * a[9]);
        long c18 = a[9] * a[9] + 2 * (a[3] * a[15] + a[4] * a[14] + a[5] * a[13] + a[6] * a[12] + a[7] * a[11] + a[8] * a[10]);
        long c19 = 2 * (a[4] * a[15] + a[5] * a[14] + a[6] * a[13] + a[7] * a[12] + a[8] * a[11] + a[9] * a[10]);
        long c20 = a[10] * a[10] + 2 * (a[5] * a[15] + a[6] * a[14] + a[7] * a[13] + a[8] * a[12] + a[9] * a[11]);
        long c21 = 2 * (a[6] * a[15] + a[7] * a[14] + a[8] * a[13] + a[9] * a[12] + a[10] * a[11]);
        long c22 = a[11] * a[11] + 2 * (a[7] * a[15] + a[8] * a[14] + a[9] * a[13] + a[10] * a[12]);
        long c23 = 2 * (a[8] * a[15] + a[9] * a[14] + a[10] * a[13] + a[11] * a[12]);
        long c24 = a[12] * a[12] + 2 * (a[9] * a[15] + a[10] * a[14] + a[11] * a[13]);
        long c25 = 2 * (a[10] * a[15] + a[11] * a[14] + a[12] * a[13]);
        long c26 = a[13] * a[13] + 2 * (a[11] * a[15] + a[12] * a[14]);
        long c27 = 2 * (a[12] * a[15] + a[13] * a[14]);
        long c28 = a[14] * a[14] + 2 * a[13] * a[15];
        long c29 = 2 * a[14] * a[15];
        long c30 = a[15] * a[15];

        carryReduce(r, c0, c1, c2, c3, c4, c5, c6, c7, c8, c9, c10, c11, c12,
            c13, c14, c15, c16, c17, c18, c19, c20, c21, c22, c23, c24, c25,
            c26, c27, c28, c29, c30);

    }


}
