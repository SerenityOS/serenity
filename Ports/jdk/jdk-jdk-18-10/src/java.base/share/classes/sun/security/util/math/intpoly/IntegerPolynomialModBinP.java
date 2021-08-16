/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
import java.nio.ByteBuffer;

/**
 * The field of integers modulo a binomial prime. This is a general-purpose
 * field implementation, that is much slower than more specialized classes
 * like IntegerPolynomial25519. It is suitable when only a small number of
 * arithmetic operations are required in some field. For example, this class
 * can be used for operations on scalars/exponents in signature operations.
 *
 * This class may only be used for primes of the form 2^a + b.
 */

public class IntegerPolynomialModBinP extends IntegerPolynomial {

    private final long[] reduceLimbs;
    private final int bitOffset;
    private final int limbMask;
    private final int rightBitOffset;
    private final int power;

    public IntegerPolynomialModBinP(int bitsPerLimb,
                                    int numLimbs,
                                    int power,
                                    BigInteger subtrahend) {
        super(bitsPerLimb, numLimbs, 1,
            BigInteger.valueOf(2).pow(power).subtract(subtrahend));

        boolean negate = false;
        if (subtrahend.compareTo(BigInteger.ZERO) < 0) {
            negate = true;
            subtrahend = subtrahend.negate();
        }
        int reduceLimbsLength = subtrahend.bitLength() / bitsPerLimb + 1;
        reduceLimbs = new long[reduceLimbsLength];
        ImmutableElement reduceElem = getElement(subtrahend);
        if (negate) {
            reduceElem = reduceElem.additiveInverse();
        }
        System.arraycopy(reduceElem.limbs, 0, reduceLimbs, 0,
            reduceLimbs.length);

        // begin test code
        System.out.println("reduce limbs:");
        for (int i = 0; i < reduceLimbs.length; i++) {
            System.out.println(i + ":" + reduceLimbs[i]);
        }
        // end test code

        this.power = power;
        this.bitOffset = numLimbs * bitsPerLimb - power;
        this.limbMask = -1 >>> (64 - bitsPerLimb);
        this.rightBitOffset = bitsPerLimb - bitOffset;
    }

    @Override
    protected void finalCarryReduceLast(long[] limbs) {

        int extraBits = bitsPerLimb * numLimbs - power;
        int highBits = bitsPerLimb - extraBits;
        long c = limbs[numLimbs - 1] >> highBits;
        limbs[numLimbs - 1] -= c << highBits;
        for (int j = 0; j < reduceLimbs.length; j++) {
            int reduceBits = power + extraBits - j * bitsPerLimb;
            modReduceInBits(limbs, numLimbs, reduceBits, c * reduceLimbs[j]);
        }
    }


    /**
     * Allow more general (and slower) input conversion that takes a large
     * value and reduces it.
     */
    @Override
    public ImmutableElement getElement(byte[] v, int offset, int length,
                                       byte highByte) {

        long[] result = new long[numLimbs];
        int numHighBits = 32 - Integer.numberOfLeadingZeros(highByte);
        int numBits = 8 * length + numHighBits;
        int requiredLimbs = (numBits + bitsPerLimb - 1) / bitsPerLimb;
        if (requiredLimbs > numLimbs) {
            long[] temp = new long[requiredLimbs];
            encode(v, offset, length, highByte, temp);
            // encode does a full carry/reduce
            System.arraycopy(temp, 0, result, 0, result.length);
        } else {
            encode(v, offset, length, highByte, result);
        }

        return new ImmutableElement(result, 0);
    }

    /**
     * Multiply a and b, and store the result in c. Requires that
     * a.length == b.length == numLimbs and c.length >= 2 * numLimbs - 1.
     * It is allowed for a and b to be the same array.
     */
    private void multOnly(long[] a, long[] b, long[] c) {
        for (int i = 0; i < numLimbs; i++) {
            for (int j = 0; j < numLimbs; j++) {
                c[i + j] += a[i] * b[j];
            }
        }
    }

    @Override
    protected void mult(long[] a, long[] b, long[] r) {

        long[] c = new long[2 * numLimbs];
        multOnly(a, b, c);
        carryReduce(c, r);
    }

    private void modReduceInBits(long[] limbs, int index, int bits, long x) {

        if (bits % bitsPerLimb == 0) {
            int pos = bits / bitsPerLimb;
            limbs[index - pos] += x;
        }
        else {
            int secondPos = bits / (bitsPerLimb);
            int bitOffset = (secondPos + 1) * bitsPerLimb - bits;
            int rightBitOffset = bitsPerLimb - bitOffset;
            limbs[index - (secondPos + 1)] += (x << bitOffset) & limbMask;
            limbs[index - secondPos] += x >> rightBitOffset;
        }
    }

    protected void reduceIn(long[] c, long v, int i) {

        for (int j = 0; j < reduceLimbs.length; j++) {
            modReduceInBits(c, i, power - bitsPerLimb * j, reduceLimbs[j] * v);
        }
    }

    private void carryReduce(long[] c, long[] r) {

        // full carry to prevent overflow during reduce
        carry(c);
        // Reduce in from all high positions
        for (int i = c.length - 1; i >= numLimbs; i--) {
            reduceIn(c, c[i], i);
            c[i] = 0;
        }
        // carry on lower positions that possibly carries out one position
        carry(c, 0, numLimbs);
        // reduce in a single position
        reduceIn(c, c[numLimbs], numLimbs);
        c[numLimbs] = 0;
        // final carry
        carry(c, 0, numLimbs - 1);
        System.arraycopy(c, 0, r, 0, r.length);
    }

    @Override
    protected void reduce(long[] a) {
        // TODO: optimize this
        long[] c = new long[a.length + 2];
        System.arraycopy(a, 0, c, 0, a.length);
        carryReduce(c, a);
    }

    @Override
    protected void square(long[] a, long[] r) {

        long[] c = new long[2 * numLimbs];
        for (int i = 0; i < numLimbs; i++) {
            c[2 * i] += a[i] * a[i];
            for (int j = i + 1; j < numLimbs; j++) {
                c[i + j] += 2 * a[i] * a[j];
            }
        }

        carryReduce(c, r);

    }

    /**
     * The field of integers modulo the order of the Curve25519 subgroup
     */
    public static class Curve25519OrderField extends IntegerPolynomialModBinP {

        public Curve25519OrderField() {
            super(26, 10, 252,
                new BigInteger("-27742317777372353535851937790883648493"));
        }
    }

    /**
     * The field of integers modulo the order of the Curve448 subgroup
     */
    public static class Curve448OrderField extends IntegerPolynomialModBinP {

        public Curve448OrderField() {
            super(28, 16, 446,
                new BigInteger("138180668098951153520073867485154268803366" +
                    "92474882178609894547503885"));
        }
    }
}
