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

import sun.security.ec.point.*;
import sun.security.util.math.*;
import sun.security.util.math.intpoly.*;

import java.math.BigInteger;
import java.security.ProviderException;
import java.security.spec.ECFieldFp;
import java.security.spec.ECParameterSpec;
import java.security.spec.EllipticCurve;
import java.util.Map;
import java.util.Optional;

/*
 * Elliptic curve point arithmetic for prime-order curves where a=-3.
 * Formulas are derived from "Complete addition formulas for prime order
 * elliptic curves" by Renes, Costello, and Batina.
 */

public class ECOperations {

    /*
     * An exception indicating a problem with an intermediate value produced
     * by some part of the computation. For example, the signing operation
     * will throw this exception to indicate that the r or s value is 0, and
     * that the signing operation should be tried again with a different nonce.
     */
    static class IntermediateValueException extends Exception {
        private static final long serialVersionUID = 1;
    }

    static final Map<BigInteger, IntegerFieldModuloP> fields = Map.of(
        IntegerPolynomialP256.MODULUS, new IntegerPolynomialP256(),
        IntegerPolynomialP384.MODULUS, new IntegerPolynomialP384(),
        IntegerPolynomialP521.MODULUS, new IntegerPolynomialP521()
    );

    static final Map<BigInteger, IntegerFieldModuloP> orderFields = Map.of(
        P256OrderField.MODULUS, new P256OrderField(),
        P384OrderField.MODULUS, new P384OrderField(),
        P521OrderField.MODULUS, new P521OrderField()
    );

    public static Optional<ECOperations> forParameters(ECParameterSpec params) {

        EllipticCurve curve = params.getCurve();
        if (!(curve.getField() instanceof ECFieldFp)) {
            return Optional.empty();
        }
        ECFieldFp primeField = (ECFieldFp) curve.getField();

        BigInteger three = BigInteger.valueOf(3);
        if (!primeField.getP().subtract(curve.getA()).equals(three)) {
            return Optional.empty();
        }
        IntegerFieldModuloP field = fields.get(primeField.getP());
        if (field == null) {
            return Optional.empty();
        }

        IntegerFieldModuloP orderField = orderFields.get(params.getOrder());
        if (orderField == null) {
            return Optional.empty();
        }

        ImmutableIntegerModuloP b = field.getElement(curve.getB());
        ECOperations ecOps = new ECOperations(b, orderField);
        return Optional.of(ecOps);
    }

    final ImmutableIntegerModuloP b;
    final SmallValue one;
    final SmallValue two;
    final SmallValue three;
    final SmallValue four;
    final ProjectivePoint.Immutable neutral;
    private final IntegerFieldModuloP orderField;

    public ECOperations(IntegerModuloP b, IntegerFieldModuloP orderField) {
        this.b = b.fixed();
        this.orderField = orderField;

        this.one = b.getField().getSmallValue(1);
        this.two = b.getField().getSmallValue(2);
        this.three = b.getField().getSmallValue(3);
        this.four = b.getField().getSmallValue(4);

        IntegerFieldModuloP field = b.getField();
        this.neutral = new ProjectivePoint.Immutable(field.get0(),
            field.get1(), field.get0());
    }

    public IntegerFieldModuloP getField() {
        return b.getField();
    }
    public IntegerFieldModuloP getOrderField() {
        return orderField;
    }

    protected ProjectivePoint.Immutable getNeutral() {
        return neutral;
    }

    public boolean isNeutral(Point p) {
        ProjectivePoint<?> pp = (ProjectivePoint<?>) p;

        IntegerModuloP z = pp.getZ();

        IntegerFieldModuloP field = z.getField();
        int byteLength = (field.getSize().bitLength() + 7) / 8;
        byte[] zBytes = z.asByteArray(byteLength);
        return allZero(zBytes);
    }

    byte[] seedToScalar(byte[] seedBytes)
        throws IntermediateValueException {

        // Produce a nonce from the seed using FIPS 186-4,section B.5.1:
        // Per-Message Secret Number Generation Using Extra Random Bits
        // or
        // Produce a scalar from the seed using FIPS 186-4, section B.4.1:
        // Key Pair Generation Using Extra Random Bits

        // To keep the implementation simple, sample in the range [0,n)
        // and throw IntermediateValueException in the (unlikely) event
        // that the result is 0.

        // Get 64 extra bits and reduce in to the nonce
        int seedBits = orderField.getSize().bitLength() + 64;
        if (seedBytes.length * 8 < seedBits) {
            throw new ProviderException("Incorrect seed length: " +
            seedBytes.length * 8 + " < " + seedBits);
        }

        // input conversion only works on byte boundaries
        // clear high-order bits of last byte so they don't influence nonce
        int lastByteBits = seedBits % 8;
        if (lastByteBits != 0) {
            int lastByteIndex = seedBits / 8;
            byte mask = (byte) (0xFF >>> (8 - lastByteBits));
            seedBytes[lastByteIndex] &= mask;
        }

        int seedLength = (seedBits + 7) / 8;
        IntegerModuloP scalarElem =
            orderField.getElement(seedBytes, 0, seedLength, (byte) 0);
        int scalarLength = (orderField.getSize().bitLength() + 7) / 8;
        byte[] scalarArr = new byte[scalarLength];
        scalarElem.asByteArray(scalarArr);
        if (ECOperations.allZero(scalarArr)) {
            throw new IntermediateValueException();
        }
        return scalarArr;
    }

    /*
     * Compare all values in the array to 0 without branching on any value
     *
     */
    public static boolean allZero(byte[] arr) {
        byte acc = 0;
        for (int i = 0; i < arr.length; i++) {
            acc |= arr[i];
        }
        return acc == 0;
    }

    /*
     * 4-bit branchless array lookup for projective points.
     */
    private void lookup4(ProjectivePoint.Immutable[] arr, int index,
        ProjectivePoint.Mutable result, IntegerModuloP zero) {

        for (int i = 0; i < 16; i++) {
            int xor = index ^ i;
            int bit3 = (xor & 0x8) >>> 3;
            int bit2 = (xor & 0x4) >>> 2;
            int bit1 = (xor & 0x2) >>> 1;
            int bit0 = (xor & 0x1);
            int inverse = bit0 | bit1 | bit2 | bit3;
            int set = 1 - inverse;

            ProjectivePoint.Immutable pi = arr[i];
            result.conditionalSet(pi, set);
        }
    }

    private void double4(ProjectivePoint.Mutable p, MutableIntegerModuloP t0,
        MutableIntegerModuloP t1, MutableIntegerModuloP t2,
        MutableIntegerModuloP t3, MutableIntegerModuloP t4) {

        for (int i = 0; i < 4; i++) {
            setDouble(p, t0, t1, t2, t3, t4);
        }
    }

    /**
     * Multiply an affine point by a scalar and return the result as a mutable
     * point.
     *
     * @param affineP the point
     * @param s the scalar as a little-endian array
     * @return the product
     */
    public MutablePoint multiply(AffinePoint affineP, byte[] s) {

        // 4-bit windowed multiply with branchless lookup.
        // The mixed addition is faster, so it is used to construct the array
        // at the beginning of the operation.

        IntegerFieldModuloP field = affineP.getX().getField();
        ImmutableIntegerModuloP zero = field.get0();
        // temporaries
        MutableIntegerModuloP t0 = zero.mutable();
        MutableIntegerModuloP t1 = zero.mutable();
        MutableIntegerModuloP t2 = zero.mutable();
        MutableIntegerModuloP t3 = zero.mutable();
        MutableIntegerModuloP t4 = zero.mutable();

        ProjectivePoint.Mutable result = new ProjectivePoint.Mutable(field);
        result.getY().setValue(field.get1().mutable());

        ProjectivePoint.Immutable[] pointMultiples =
            new ProjectivePoint.Immutable[16];
        // 0P is neutral---same as initial result value
        pointMultiples[0] = result.fixed();

        ProjectivePoint.Mutable ps = new ProjectivePoint.Mutable(field);
        ps.setValue(affineP);
        // 1P = P
        pointMultiples[1] = ps.fixed();

        // the rest are calculated using mixed point addition
        for (int i = 2; i < 16; i++) {
            setSum(ps, affineP, t0, t1, t2, t3, t4);
            pointMultiples[i] = ps.fixed();
        }

        ProjectivePoint.Mutable lookupResult = ps.mutable();

        for (int i = s.length - 1; i >= 0; i--) {

            double4(result, t0, t1, t2, t3, t4);

            int high = (0xFF & s[i]) >>> 4;
            lookup4(pointMultiples, high, lookupResult, zero);
            setSum(result, lookupResult, t0, t1, t2, t3, t4);

            double4(result, t0, t1, t2, t3, t4);

            int low = 0xF & s[i];
            lookup4(pointMultiples, low, lookupResult, zero);
            setSum(result, lookupResult, t0, t1, t2, t3, t4);
        }

        return result;

    }

    /*
     * Point double
     */
    private void setDouble(ProjectivePoint.Mutable p, MutableIntegerModuloP t0,
        MutableIntegerModuloP t1, MutableIntegerModuloP t2,
        MutableIntegerModuloP t3, MutableIntegerModuloP t4) {

        t0.setValue(p.getX()).setSquare();
        t1.setValue(p.getY()).setSquare();
        t2.setValue(p.getZ()).setSquare();
        t3.setValue(p.getX()).setProduct(p.getY());
        t4.setValue(p.getY()).setProduct(p.getZ());

        t3.setSum(t3);
        p.getZ().setProduct(p.getX());

        p.getZ().setProduct(two);

        p.getY().setValue(t2).setProduct(b);
        p.getY().setDifference(p.getZ());

        p.getX().setValue(p.getY()).setProduct(two);
        p.getY().setSum(p.getX());
        p.getY().setReduced();
        p.getX().setValue(t1).setDifference(p.getY());

        p.getY().setSum(t1);
        p.getY().setProduct(p.getX());
        p.getX().setProduct(t3);

        t3.setValue(t2).setProduct(two);
        t2.setSum(t3);
        p.getZ().setProduct(b);

        t2.setReduced();
        p.getZ().setDifference(t2);
        p.getZ().setDifference(t0);
        t3.setValue(p.getZ()).setProduct(two);
        p.getZ().setReduced();
        p.getZ().setSum(t3);
        t0.setProduct(three);

        t0.setDifference(t2);
        t0.setProduct(p.getZ());
        p.getY().setSum(t0);

        t4.setSum(t4);
        p.getZ().setProduct(t4);

        p.getX().setDifference(p.getZ());
        p.getZ().setValue(t4).setProduct(t1);

        p.getZ().setProduct(four);

    }

    /*
     * Mixed point addition. This method constructs new temporaries each time
     * it is called. For better efficiency, the method that reuses temporaries
     * should be used if more than one sum will be computed.
     */
    public void setSum(MutablePoint p, AffinePoint p2) {

        IntegerModuloP zero = p.getField().get0();
        MutableIntegerModuloP t0 = zero.mutable();
        MutableIntegerModuloP t1 = zero.mutable();
        MutableIntegerModuloP t2 = zero.mutable();
        MutableIntegerModuloP t3 = zero.mutable();
        MutableIntegerModuloP t4 = zero.mutable();
        setSum((ProjectivePoint.Mutable) p, p2, t0, t1, t2, t3, t4);

    }

    /*
     * Mixed point addition
     */
    private void setSum(ProjectivePoint.Mutable p, AffinePoint p2,
        MutableIntegerModuloP t0, MutableIntegerModuloP t1,
        MutableIntegerModuloP t2, MutableIntegerModuloP t3,
        MutableIntegerModuloP t4) {

        t0.setValue(p.getX()).setProduct(p2.getX());
        t1.setValue(p.getY()).setProduct(p2.getY());
        t3.setValue(p2.getX()).setSum(p2.getY());
        t4.setValue(p.getX()).setSum(p.getY());
        p.getX().setReduced();
        t3.setProduct(t4);
        t4.setValue(t0).setSum(t1);

        t3.setDifference(t4);
        t4.setValue(p2.getY()).setProduct(p.getZ());
        t4.setSum(p.getY());

        p.getY().setValue(p2.getX()).setProduct(p.getZ());
        p.getY().setSum(p.getX());
        t2.setValue(p.getZ());
        p.getZ().setProduct(b);

        p.getX().setValue(p.getY()).setDifference(p.getZ());
        p.getX().setReduced();
        p.getZ().setValue(p.getX()).setProduct(two);
        p.getX().setSum(p.getZ());

        p.getZ().setValue(t1).setDifference(p.getX());
        p.getX().setSum(t1);
        p.getY().setProduct(b);

        t1.setValue(t2).setProduct(two);
        t2.setSum(t1);
        t2.setReduced();
        p.getY().setDifference(t2);

        p.getY().setDifference(t0);
        p.getY().setReduced();
        t1.setValue(p.getY()).setProduct(two);
        p.getY().setSum(t1);

        t1.setValue(t0).setProduct(two);
        t0.setSum(t1);
        t0.setDifference(t2);

        t1.setValue(t4).setProduct(p.getY());
        t2.setValue(t0).setProduct(p.getY());
        p.getY().setValue(p.getX()).setProduct(p.getZ());

        p.getY().setSum(t2);
        p.getX().setProduct(t3);
        p.getX().setDifference(t1);

        p.getZ().setProduct(t4);
        t1.setValue(t3).setProduct(t0);
        p.getZ().setSum(t1);

    }

    /*
     * Projective point addition
     */
    private void setSum(ProjectivePoint.Mutable p, ProjectivePoint.Mutable p2,
        MutableIntegerModuloP t0, MutableIntegerModuloP t1,
        MutableIntegerModuloP t2, MutableIntegerModuloP t3,
        MutableIntegerModuloP t4) {

        t0.setValue(p.getX()).setProduct(p2.getX());
        t1.setValue(p.getY()).setProduct(p2.getY());
        t2.setValue(p.getZ()).setProduct(p2.getZ());

        t3.setValue(p.getX()).setSum(p.getY());
        t4.setValue(p2.getX()).setSum(p2.getY());
        t3.setProduct(t4);

        t4.setValue(t0).setSum(t1);
        t3.setDifference(t4);
        t4.setValue(p.getY()).setSum(p.getZ());

        p.getY().setValue(p2.getY()).setSum(p2.getZ());
        t4.setProduct(p.getY());
        p.getY().setValue(t1).setSum(t2);

        t4.setDifference(p.getY());
        p.getX().setSum(p.getZ());
        p.getY().setValue(p2.getX()).setSum(p2.getZ());

        p.getX().setProduct(p.getY());
        p.getY().setValue(t0).setSum(t2);
        p.getY().setAdditiveInverse().setSum(p.getX());
        p.getY().setReduced();

        p.getZ().setValue(t2).setProduct(b);
        p.getX().setValue(p.getY()).setDifference(p.getZ());
        p.getZ().setValue(p.getX()).setProduct(two);

        p.getX().setSum(p.getZ());
        p.getX().setReduced();
        p.getZ().setValue(t1).setDifference(p.getX());
        p.getX().setSum(t1);

        p.getY().setProduct(b);
        t1.setValue(t2).setSum(t2);
        t2.setSum(t1);
        t2.setReduced();

        p.getY().setDifference(t2);
        p.getY().setDifference(t0);
        p.getY().setReduced();
        t1.setValue(p.getY()).setSum(p.getY());

        p.getY().setSum(t1);
        t1.setValue(t0).setProduct(two);
        t0.setSum(t1);

        t0.setDifference(t2);
        t1.setValue(t4).setProduct(p.getY());
        t2.setValue(t0).setProduct(p.getY());

        p.getY().setValue(p.getX()).setProduct(p.getZ());
        p.getY().setSum(t2);
        p.getX().setProduct(t3);

        p.getX().setDifference(t1);
        p.getZ().setProduct(t4);
        t1.setValue(t3).setProduct(t0);

        p.getZ().setSum(t1);

    }
}

