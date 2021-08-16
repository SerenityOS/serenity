/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.ec;

import sun.security.util.math.IntegerFieldModuloP;
import sun.security.util.math.ImmutableIntegerModuloP;
import sun.security.util.math.IntegerModuloP;
import sun.security.util.math.MutableIntegerModuloP;
import sun.security.util.math.SmallValue;
import sun.security.util.math.intpoly.IntegerPolynomial25519;
import sun.security.util.math.intpoly.IntegerPolynomial448;

import java.math.BigInteger;
import java.security.ProviderException;
import java.security.SecureRandom;

public class XECOperations {

    private final XECParameters params;
    private final IntegerFieldModuloP field;
    private final ImmutableIntegerModuloP zero;
    private final ImmutableIntegerModuloP one;
    private final SmallValue a24;
    private final ImmutableIntegerModuloP basePoint;

    public XECOperations(XECParameters c) {
        this.params = c;

        BigInteger p = params.getP();
        this.field = getIntegerFieldModulo(p);
        this.zero = field.getElement(BigInteger.ZERO).fixed();
        this.one = field.get1().fixed();
        this.a24 = field.getSmallValue(params.getA24());
        this.basePoint = field.getElement(
            BigInteger.valueOf(c.getBasePoint()));
    }

    public XECParameters getParameters() {
        return params;
    }

    public byte[] generatePrivate(SecureRandom random) {
        byte[] result = new byte[this.params.getBytes()];
        random.nextBytes(result);
        return result;
    }

    /**
     * Compute a public key from an encoded private key. This method will
     * modify the supplied array in order to prune it.
     */
    public BigInteger computePublic(byte[] k) {
        pruneK(k);
        return pointMultiply(k, this.basePoint).asBigInteger();
    }

    /**
     *
     * Multiply an encoded scalar with a point as a BigInteger and return an
     * encoded point. The array k holding the scalar will be pruned by
     * modifying it in place.
     *
     * @param k an encoded scalar
     * @param u the u-coordinate of a point as a BigInteger
     * @return the encoded product
     */
    public byte[] encodedPointMultiply(byte[] k, BigInteger u) {
        pruneK(k);
        ImmutableIntegerModuloP elemU = field.getElement(u);
        return pointMultiply(k, elemU).asByteArray(params.getBytes());
    }

    /**
     *
     * Multiply an encoded scalar with an encoded point and return an encoded
     * point. The array k holding the scalar will be pruned by
     * modifying it in place.
     *
     * @param k an encoded scalar
     * @param u an encoded point
     * @return the encoded product
     */
    public byte[] encodedPointMultiply(byte[] k, byte[] u) {
        pruneK(k);
        ImmutableIntegerModuloP elemU = decodeU(u);
        return pointMultiply(k, elemU).asByteArray(params.getBytes());
    }

    /**
     * Return the field element corresponding to an encoded u-coordinate.
     * This method prunes u by modifying it in place.
     *
     * @param u
     * @param bits
     * @return
     */
    private ImmutableIntegerModuloP decodeU(byte[] u, int bits) {

        maskHighOrder(u, bits);

        return field.getElement(u);
    }

    /**
     * Mask off the high order bits of an encoded integer in an array. The
     * array is modified in place.
     *
     * @param arr an array containing an encoded integer
     * @param bits the number of bits to keep
     * @return the number, in range [1,8], of bits kept in the highest byte
     */
    private static byte maskHighOrder(byte[] arr, int bits) {

        int lastByteIndex = arr.length - 1;
        byte bitsMod8 = (byte) (bits % 8);
        byte highBits = bitsMod8 == 0 ? 8 : bitsMod8;
        byte msbMaskOff = (byte) ((1 << highBits) - 1);
        arr[lastByteIndex] &= msbMaskOff;

        return highBits;
    }

    /**
     * Prune an encoded scalar value by modifying it in place. The extra
     * high-order bits are masked off, the highest valid bit it set, and the
     * number is rounded down to a multiple of the cofactor.
     *
     * @param k an encoded scalar value
     * @param bits the number of bits in the scalar
     * @param logCofactor the base-2 logarithm of the cofactor
     */
    private static void pruneK(byte[] k, int bits, int logCofactor) {

        int lastByteIndex = k.length - 1;

        // mask off unused high-order bits
        byte highBits = maskHighOrder(k, bits);

        // set the highest bit
        byte msbMaskOn = (byte) (1 << (highBits - 1));
        k[lastByteIndex] |= msbMaskOn;

        // round down to a multiple of the cofactor
        byte lsbMaskOff = (byte) (0xFF << logCofactor);
        k[0] &= lsbMaskOff;
    }

    private void pruneK(byte[] k) {
        pruneK(k, params.getBits(), params.getLogCofactor());
    }

    private ImmutableIntegerModuloP decodeU(byte [] u) {
        return decodeU(u, params.getBits());
    }

    // Constant-time conditional swap
    private static void cswap(int swap, MutableIntegerModuloP x1,
        MutableIntegerModuloP x2) {

        x1.conditionalSwapWith(x2, swap);
    }

    private static IntegerFieldModuloP getIntegerFieldModulo(BigInteger p) {

        if (p.equals(IntegerPolynomial25519.MODULUS)) {
            return new IntegerPolynomial25519();
        }
        else if (p.equals(IntegerPolynomial448.MODULUS)) {
            return new IntegerPolynomial448();
        }

        throw new ProviderException("Unsupported prime: " + p.toString());
    }

    private int bitAt(byte[] arr, int index) {
        int byteIndex = index / 8;
        int bitIndex = index % 8;
        return (arr[byteIndex] & (1 << bitIndex)) >> bitIndex;
    }

    /*
     * Constant-time Montgomery ladder that computes k*u and returns the
     * result as a field element.
     */
    private IntegerModuloP pointMultiply(byte[] k,
                                         ImmutableIntegerModuloP u) {

        ImmutableIntegerModuloP x_1 = u;
        MutableIntegerModuloP x_2 = this.one.mutable();
        MutableIntegerModuloP z_2 = this.zero.mutable();
        MutableIntegerModuloP x_3 = u.mutable();
        MutableIntegerModuloP z_3 = this.one.mutable();
        int swap = 0;

        // Variables below are reused to avoid unnecessary allocation
        // They will be assigned in the loop, so initial value doesn't matter
        MutableIntegerModuloP m1 = this.zero.mutable();
        MutableIntegerModuloP DA = this.zero.mutable();
        MutableIntegerModuloP E = this.zero.mutable();
        MutableIntegerModuloP a24_times_E = this.zero.mutable();

        // Comments describe the equivalent operations from RFC 7748
        // In comments, A(m1) means the variable m1 holds the value A
        for (int t = params.getBits() - 1; t >= 0; t--) {
            int k_t = bitAt(k, t);
            swap = swap ^ k_t;
            cswap(swap, x_2, x_3);
            cswap(swap, z_2, z_3);
            swap = k_t;

            // A(m1) = x_2 + z_2
            m1.setValue(x_2).setSum(z_2);
            // D = x_3 - z_3
            // DA = D * A(m1)
            DA.setValue(x_3).setDifference(z_3).setProduct(m1);
            // AA(m1) = A(m1)^2
            m1.setSquare();
            // B(x_2) = x_2 - z_2
            x_2.setDifference(z_2);
            // C = x_3 + z_3
            // CB(x_3) = C * B(x_2)
            x_3.setSum(z_3).setProduct(x_2);
            // BB(x_2) = B^2
            x_2.setSquare();
            // E = AA(m1) - BB(x_2)
            E.setValue(m1).setDifference(x_2);
            // compute a24 * E using SmallValue
            a24_times_E.setValue(E);
            a24_times_E.setProduct(this.a24);

            // assign results to x_3, z_3, x_2, z_2
            // x_2 = AA(m1) * BB
            x_2.setProduct(m1);
            // z_2 = E * (AA(m1) + a24 * E)
            z_2.setValue(m1).setSum(a24_times_E).setProduct(E);
            // z_3 = x_1*(DA - CB(x_3))^2
            z_3.setValue(DA).setDifference(x_3).setSquare().setProduct(x_1);
            // x_3 = (CB(x_3) + DA)^2
            x_3.setSum(DA).setSquare();
        }

        cswap(swap, x_2, x_3);
        cswap(swap, z_2, z_3);

        // return (x_2 * z_2^(p - 2))
        return x_2.setProduct(z_2.multiplicativeInverse());
    }
}
