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
package sun.security.ec.ed;

import sun.security.ec.point.AffinePoint;
import sun.security.ec.point.Point;
import sun.security.util.ArrayUtil;
import sun.security.util.math.IntegerFieldModuloP;
import sun.security.util.math.IntegerModuloP;
import sun.security.util.math.MutableIntegerModuloP;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.security.spec.EdDSAParameterSpec;
import java.security.spec.EdECPoint;
import java.util.Arrays;
import java.util.function.Function;

/*
 * A class containing the operations of the EdDSA signature scheme. The
 * parameters include an object that performs the elliptic curve point
 * arithmetic, and EdDSAOperations uses this object to construct the signing
 * and verification operations.
 */
public class EdDSAOperations {

    private final EdDSAParameters params;

    public EdDSAOperations(EdDSAParameters params)
        throws NoSuchAlgorithmException {

        this.params = params;
    }

    public EdDSAParameters getParameters() {
        return params;
    }

    public byte[] generatePrivate(SecureRandom random) {
        byte[] result = new byte[params.getKeyLength()];
        random.nextBytes(result);
        return result;
    }

    public EdECPoint computePublic(byte[] privateKey) {
        byte[] privateKeyHash = params.digest(privateKey);
        int byteLength = privateKeyHash.length / 2;
        byte[] s = Arrays.copyOf(privateKeyHash, byteLength);
        prune(s);
        IntegerModuloP fieldS = params.getOrderField().getElement(s);
        fieldS.asByteArray(s);
        Point A = params.getEdOperations().basePointMultiply(s);
        return asEdECPoint(A.asAffine());
    }

    private static EdECPoint asEdECPoint(AffinePoint p) {
        return new EdECPoint(p.getX().asBigInteger().testBit(0),
            p.getY().asBigInteger());
    }

    public byte[] sign(EdDSAParameterSpec sigParams, byte[] privateKey,
                       byte[] message) {

        byte[] privateKeyHash = params.digest(privateKey);

        int byteLength = privateKeyHash.length / 2;
        byte[] s = Arrays.copyOf(privateKeyHash, byteLength);
        prune(s);
        IntegerModuloP sElem = params.getOrderField().getElement(s);
        sElem.asByteArray(s);
        Point A = params.getEdOperations().basePointMultiply(s);
        byte[] prefix = Arrays.copyOfRange(privateKeyHash,
            privateKeyHash.length / 2, privateKeyHash.length);
        byte[] dom = params.dom(sigParams);
        byte[] r = params.digest(dom, prefix, message);

        // reduce r modulo the order
        IntegerModuloP fieldR = params.getOrderField().getElement(r);
        r = new byte[params.getKeyLength()];
        fieldR.asByteArray(r);

        Point R = params.getEdOperations().basePointMultiply(r);

        byte[] encodedR = encode(byteLength, R);
        byte[] encodedA = encode(byteLength, A);
        byte[] k = params.digest(dom, encodedR, encodedA, message);

        // S computation is in group-order field
        IntegerFieldModuloP subField = params.getOrderField();
        IntegerModuloP kElem = subField.getElement(k);
        IntegerModuloP rElem = subField.getElement(r);
        MutableIntegerModuloP S = kElem.mutable().setProduct(sElem);
        S.setSum(rElem);
        // need to be reduced before output conversion
        S.setReduced();
        byte[] sArr = S.asByteArray(byteLength);
        byte[] rArr = encode(byteLength, R);

        byte[] result = new byte[byteLength * 2];
        System.arraycopy(rArr, 0, result, 0, byteLength);
        System.arraycopy(sArr, 0, result, byteLength, byteLength);
        return result;
    }

    public boolean verify(EdDSAParameterSpec sigParams, AffinePoint affineA,
                          byte[] publicKey, byte[] message, byte[] signature)
        throws SignatureException {

        if (signature == null) {
            throw new SignatureException("signature was null");
        }
        byte[] encR = Arrays.copyOf(signature, signature.length / 2);
        byte[] encS = Arrays.copyOfRange(signature, signature.length / 2,
            signature.length);

        // reject s if it is too large
        ArrayUtil.reverse(encS);
        BigInteger bigS = new BigInteger(1, encS);
        if (bigS.compareTo(params.getOrderField().getSize()) >= 0) {
            throw new SignatureException("s is too large");
        }
        ArrayUtil.reverse(encS);

        byte[] dom = params.dom(sigParams);
        AffinePoint affineR = decodeAffinePoint(SignatureException::new, encR);
        byte[] k = params.digest(dom, encR, publicKey, message);
        // reduce k to improve performance of multiply
        IntegerFieldModuloP subField = params.getOrderField();
        IntegerModuloP kElem = subField.getElement(k);
        k = kElem.asByteArray(k.length / 2);

        Point pointR = params.getEdOperations().of(affineR);
        Point pointA = params.getEdOperations().of(affineA);

        EdECOperations edOps = params.getEdOperations();
        Point lhs = edOps.basePointMultiply(encS);
        Point rhs = edOps.setSum(edOps.setProduct(pointA.mutable(), k),
            pointR.mutable());

        return lhs.affineEquals(rhs);
    }

    public boolean verify(EdDSAParameterSpec sigParams, byte[] publicKey,
                          byte[] message, byte[] signature)
        throws InvalidKeyException, SignatureException {

        AffinePoint affineA = decodeAffinePoint(InvalidKeyException::new,
            publicKey);
        return verify(sigParams, affineA, publicKey, message, signature);
    }

    public
    <T extends Throwable>
    AffinePoint decodeAffinePoint(Function<String, T> exception, byte[] arr)
    throws T {

        if (arr.length != params.getKeyLength()) {
            throw exception.apply("incorrect length");
        }

        arr = arr.clone();
        int xLSB = (0xFF & arr[arr.length - 1]) >>> 7;
        arr[arr.length - 1] &= 0x7F;
        int yLength = (params.getBits() + 7) >> 3;
        IntegerModuloP y =
            params.getField().getElement(arr, 0, yLength, (byte) 0);
        // reject non-canonical y values
        ArrayUtil.reverse(arr);
        BigInteger bigY = new BigInteger(1, arr);
        if (bigY.compareTo(params.getField().getSize()) >= 0) {
            throw exception.apply("y value is too large");
        }
        return params.getEdOperations().decodeAffinePoint(exception, xLSB, y);
    }

    public
    <T extends Throwable>
    AffinePoint decodeAffinePoint(Function<String, T> exception,
                                  EdECPoint point)
        throws T {

        // reject non-canonical y values
        if (point.getY().compareTo(params.getField().getSize()) >= 0) {
            throw exception.apply("y value is too large");
        }

        int xLSB = point.isXOdd() ? 1 : 0;
        IntegerModuloP y = params.getField().getElement(point.getY());
        return params.getEdOperations().decodeAffinePoint(exception, xLSB, y);
    }

    /**
     * Mask off the high order bits of an encoded integer in an array. The
     * array is modified in place.
     *
     * @param arr an array containing an encoded integer
     * @param bits the number of bits to keep
     * @return the number, in range [0,8], of bits kept in the highest byte
     */
    private static int maskHighOrder(byte[] arr, int bits) {

        int lastByteIndex = arr.length - 1;
        int bitsDiff = arr.length * 8 - bits;
        int highBits = 8 - bitsDiff;
        byte msbMaskOff = (byte) ((1 << highBits) - 1);
        arr[lastByteIndex] &= msbMaskOff;

        return highBits;
    }

    /**
     * Prune an encoded scalar value by modifying it in place. The extra
     * high-order bits are masked off, the highest valid bit it set, and the
     * number is rounded down to a multiple of the co-factor.
     *
     * @param k an encoded scalar value
     * @param bits the number of bits in the scalar
     * @param logCofactor the base-2 logarithm of the co-factor
     */
    private static void prune(byte[] k, int bits, int logCofactor) {

        int lastByteIndex = k.length - 1;

        // mask off unused high-order bits
        int highBits = maskHighOrder(k, bits);

        // set the highest bit
        if (highBits == 0) {
            k[lastByteIndex - 1] |= 0x80;
        } else {
            byte msbMaskOn = (byte) (1 << (highBits - 1));
            k[lastByteIndex] |= msbMaskOn;
        }

        // round down to a multiple of the co-factor
        byte lsbMaskOff = (byte) (0xFF << logCofactor);
        k[0] &= lsbMaskOff;
    }

    void prune(byte[] arr) {
        prune(arr, params.getBits(), params.getLogCofactor());
    }

    private static byte[] encode(int length, Point p) {
        return encode(length, p.asAffine());
    }

    private static byte[] encode(int length, AffinePoint p) {
        byte[] result = p.getY().asByteArray(length);
        int xLSB = p.getX().asByteArray(1)[0] & 0x01;
        result[result.length - 1] |= (xLSB << 7);
        return result;
    }
}
