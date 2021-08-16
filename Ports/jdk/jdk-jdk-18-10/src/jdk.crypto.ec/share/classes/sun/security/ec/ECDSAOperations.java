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

package sun.security.ec;

import sun.security.ec.point.*;
import sun.security.util.ArrayUtil;
import sun.security.util.math.*;
import static sun.security.ec.ECOperations.IntermediateValueException;

import java.security.ProviderException;
import java.security.spec.*;
import java.util.Arrays;
import java.util.Optional;

public class ECDSAOperations {

    public static class Seed {
        private final byte[] seedValue;

        public Seed(byte[] seedValue) {
            this.seedValue = seedValue;
        }

        public byte[] getSeedValue() {
            return seedValue;
        }
    }

    public static class Nonce {
        private final byte[] nonceValue;

        public Nonce(byte[] nonceValue) {
            this.nonceValue = nonceValue;
        }

        public byte[] getNonceValue() {
            return nonceValue;
        }
    }

    private final ECOperations ecOps;
    private final AffinePoint basePoint;

    public ECDSAOperations(ECOperations ecOps, ECPoint basePoint) {
        this.ecOps = ecOps;
        this.basePoint = toAffinePoint(basePoint, ecOps.getField());
    }

    public ECOperations getEcOperations() {
        return ecOps;
    }

    public AffinePoint basePointMultiply(byte[] scalar) {
        return ecOps.multiply(basePoint, scalar).asAffine();
    }

    public static AffinePoint toAffinePoint(ECPoint point,
        IntegerFieldModuloP field) {

        ImmutableIntegerModuloP affineX = field.getElement(point.getAffineX());
        ImmutableIntegerModuloP affineY = field.getElement(point.getAffineY());
        return new AffinePoint(affineX, affineY);
    }

    public static
    Optional<ECDSAOperations> forParameters(ECParameterSpec ecParams) {
        Optional<ECOperations> curveOps =
            ECOperations.forParameters(ecParams);
        return curveOps.map(
            ops -> new ECDSAOperations(ops, ecParams.getGenerator())
        );
    }

    /**
     *
     * Sign a digest using the provided private key and seed.
     * IMPORTANT: The private key is a scalar represented using a
     * little-endian byte array. This is backwards from the conventional
     * representation in ECDSA. The routines that produce and consume this
     * value uses little-endian, so this deviation from convention removes
     * the requirement to swap the byte order. The returned signature is in
     * the conventional byte order.
     *
     * @param privateKey the private key scalar as a little-endian byte array
     * @param digest the digest to be signed
     * @param seed the seed that will be used to produce the nonce. This object
     *             should contain an array that is at least 64 bits longer than
     *             the number of bits required to represent the group order.
     * @return the ECDSA signature value
     * @throws IntermediateValueException if the signature cannot be produced
     *      due to an unacceptable intermediate or final value. If this
     *      exception is thrown, then the caller should discard the nonnce and
     *      try again with an entirely new nonce value.
     */
    public byte[] signDigest(byte[] privateKey, byte[] digest, Seed seed)
        throws IntermediateValueException {

        byte[] nonceArr = ecOps.seedToScalar(seed.getSeedValue());

        Nonce nonce = new Nonce(nonceArr);
        return signDigest(privateKey, digest, nonce);
    }

    /**
     *
     * Sign a digest using the provided private key and nonce.
     * IMPORTANT: The private key and nonce are scalars represented by a
     * little-endian byte array. This is backwards from the conventional
     * representation in ECDSA. The routines that produce and consume these
     * values use little-endian, so this deviation from convention removes
     * the requirement to swap the byte order. The returned signature is in
     * the conventional byte order.
     *
     * @param privateKey the private key scalar as a little-endian byte array
     * @param digest the digest to be signed
     * @param nonce the nonce object containing a little-endian scalar value.
     * @return the ECDSA signature value
     * @throws IntermediateValueException if the signature cannot be produced
     *      due to an unacceptable intermediate or final value. If this
     *      exception is thrown, then the caller should discard the nonnce and
     *      try again with an entirely new nonce value.
     */
    public byte[] signDigest(byte[] privateKey, byte[] digest, Nonce nonce)
        throws IntermediateValueException {

        IntegerFieldModuloP orderField = ecOps.getOrderField();
        int orderBits = orderField.getSize().bitLength();
        if (orderBits % 8 != 0 && orderBits < digest.length * 8) {
            // This implementation does not support truncating digests to
            // a length that is not a multiple of 8.
            throw new ProviderException("Invalid digest length");
        }

        byte[] k = nonce.getNonceValue();
        // check nonce length
        int length = (orderField.getSize().bitLength() + 7) / 8;
        if (k.length != length) {
            throw new ProviderException("Incorrect nonce length");
        }

        MutablePoint R = ecOps.multiply(basePoint, k);
        IntegerModuloP r = R.asAffine().getX();
        // put r into the correct field by fully reducing to an array
        byte[] temp = new byte[length];
        r = b2a(r, orderField, temp);
        byte[] result = new byte[2 * length];
        ArrayUtil.reverse(temp);
        System.arraycopy(temp, 0, result, 0, length);
        // compare r to 0
        if (ECOperations.allZero(temp)) {
            throw new IntermediateValueException();
        }

        IntegerModuloP dU = orderField.getElement(privateKey);
        int lengthE = Math.min(length, digest.length);
        byte[] E = new byte[lengthE];
        System.arraycopy(digest, 0, E, 0, lengthE);
        ArrayUtil.reverse(E);
        IntegerModuloP e = orderField.getElement(E);
        IntegerModuloP kElem = orderField.getElement(k);
        IntegerModuloP kInv = kElem.multiplicativeInverse();
        MutableIntegerModuloP s = r.mutable();
        s.setProduct(dU).setSum(e).setProduct(kInv);
        // store s in result
        s.asByteArray(temp);
        ArrayUtil.reverse(temp);
        System.arraycopy(temp, 0, result, length, length);
        // compare s to 0
        if (ECOperations.allZero(temp)) {
            throw new IntermediateValueException();
        }

        return result;

    }
    public boolean verifySignedDigest(byte[] digest, byte[] sig, ECPoint pp) {

        IntegerFieldModuloP field = ecOps.getField();
        IntegerFieldModuloP orderField = ecOps.getOrderField();
        int length = (orderField.getSize().bitLength() + 7) / 8;

        byte[] r;
        byte[] s;

        int encodeLength = sig.length / 2;
        if (sig.length %2 != 0 || encodeLength > length) {
            return false;
        } else if (encodeLength == length) {
            r = Arrays.copyOf(sig, length);
            s = Arrays.copyOfRange(sig, length, length * 2);
        } else {
            r = new byte[length];
            s = new byte[length];
            System.arraycopy(sig, 0, r, length - encodeLength, encodeLength);
            System.arraycopy(sig, encodeLength, s, length - encodeLength, encodeLength);
        }

        ArrayUtil.reverse(r);
        ArrayUtil.reverse(s);
        IntegerModuloP ri = orderField.getElement(r);
        IntegerModuloP si = orderField.getElement(s);
        // z
        int lengthE = Math.min(length, digest.length);
        byte[] E = new byte[lengthE];
        System.arraycopy(digest, 0, E, 0, lengthE);
        ArrayUtil.reverse(E);
        IntegerModuloP e = orderField.getElement(E);

        IntegerModuloP sInv = si.multiplicativeInverse();
        ImmutableIntegerModuloP u1 = e.multiply(sInv);
        ImmutableIntegerModuloP u2 = ri.multiply(sInv);

        AffinePoint pub = new AffinePoint(field.getElement(pp.getAffineX()),
                field.getElement(pp.getAffineY()));

        byte[] temp1 = new byte[length];
        b2a(u1, orderField, temp1);

        byte[] temp2 = new byte[length];
        b2a(u2, orderField, temp2);

        MutablePoint p1 = ecOps.multiply(basePoint, temp1);
        MutablePoint p2 = ecOps.multiply(pub, temp2);

        ecOps.setSum(p1, p2.asAffine());
        IntegerModuloP result = p1.asAffine().getX();
        result = result.additiveInverse().add(ri);

        b2a(result, orderField, temp1);
        return ECOperations.allZero(temp1);
    }

    public static ImmutableIntegerModuloP b2a(IntegerModuloP b,
            IntegerFieldModuloP orderField, byte[] temp1) {
        b.asByteArray(temp1);
        ImmutableIntegerModuloP b2 = orderField.getElement(temp1);
        b2.asByteArray(temp1);
        return b2;
    }
}
