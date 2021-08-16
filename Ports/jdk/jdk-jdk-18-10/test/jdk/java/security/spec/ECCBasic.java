/*
 * Copyright (c) 2003, 2007, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 4794996
 * @summary Ensure basic functionality of these new ECC classes.
 * @author Valerie Peng
 */
import java.math.BigInteger;
import java.util.Arrays;
import java.security.*;
import java.security.spec.*;

public class ECCBasic {
    private static final BigInteger ZERO = BigInteger.ZERO;
    private static final BigInteger ONE = BigInteger.ONE;
    private static final BigInteger TEN = BigInteger.TEN;
    private static final ECFieldFp FP = new ECFieldFp(TEN);
    private static final int F2M_M = 4;
    private static final int[] F2M_KS;
    static {
        F2M_KS = new int[1];
        F2M_KS[0] = 1;
    }
    private static final BigInteger F2M_RP = BigInteger.valueOf(19);
    private static final ECFieldF2m F2M = new ECFieldF2m(F2M_M, F2M_KS);
    private static final EllipticCurve CURVE = new EllipticCurve
        (new ECFieldFp(TEN), ONE, ONE);
    private static final ECPoint POINT = new ECPoint(ONE, TEN);
    private static final BigInteger ORDER = ONE;
    private static final int COFACTOR = 3;
    private static final ECParameterSpec PARAMS = new ECParameterSpec
        (CURVE, POINT, ORDER, COFACTOR);
    private static final ECGenParameterSpec GENPARAMS =
        new ECGenParameterSpec("prime192v1");
    private static final ECPrivateKeySpec PRIV_KEY = new ECPrivateKeySpec
        (TEN, PARAMS);
    private static final ECPublicKeySpec PUB_KEY = new ECPublicKeySpec
        (POINT, PARAMS);

    private static void testECFieldFp() throws Exception {
        System.out.println("Testing ECFieldFp(BigInteger)");
        try {
            new ECFieldFp(ZERO);
            throw new Exception("...should throw IAE");
        } catch (IllegalArgumentException iae) {
            System.out.println("...expected IAE thrown");
        }
        try {
            new ECFieldFp(null);
            throw new Exception("...should throw NPE");
        } catch (NullPointerException npe) {
            System.out.println("...expected NPE thrown");
        }
        if (TEN.equals(FP.getP()) == false) {
            throw new Exception("...error in getP()");
        }
        if (FP.getFieldSize() != TEN.bitLength()) {
            throw new Exception("...error in getFieldSize()");
        }
    }
    private static void testECFieldF2m() throws Exception {
        // invalid parameters
        int mBad = 0;
        int[] ksBad;
        for (int i=0; i<7; i++) {
            System.out.print("Testing ECFieldF2m");
            try {
                switch (i) {
                case 0:
                    System.out.println("(int)");
                    new ECFieldF2m(mBad);
                    break;
                case 1:
                    System.out.println("(int, BigInteger)#1");
                    new ECFieldF2m(mBad, F2M_RP);
                    break;
                case 2:
                    System.out.println("(int, BigInteger)#2");
                    new ECFieldF2m(mBad, F2M_RP);
                    break;
                case 3:
                    System.out.println("(int, int[])#1");
                    new ECFieldF2m(mBad, F2M_KS);
                    break;
                case 4:
                    ksBad = new int[2];
                    System.out.println("(int, int[])#2");
                    new ECFieldF2m(F2M_M, ksBad);
                    break;
                case 5:
                    ksBad = new int[1];
                    ksBad[0] = 5;
                    System.out.println("(int, int[])#3");
                    new ECFieldF2m(F2M_M, ksBad);
                    break;
                case 6:
                    ksBad = new int[3];
                    ksBad[0] = 1;
                    ksBad[1] = 2;
                    ksBad[2] = 3;
                    System.out.println("(int, int[])#4");
                    new ECFieldF2m(F2M_M, ksBad);
                    break;
                }
                throw new Exception("...should throw IAE");
            } catch (IllegalArgumentException iae) {
                System.out.println("...expected IAE thrown");
            }
        }
        for (int i=0; i<2; i++) {
            System.out.print("Testing ECFieldF2m");
            try {
                switch (i) {
                case 0:
                    System.out.println("(int, BigInteger)");
                    new ECFieldF2m(F2M_M, (BigInteger) null);
                    break;
                case 1:
                    System.out.println("(int, int[])#1");
                    new ECFieldF2m(F2M_M, (int[]) null);
                    break;
                }
                throw new Exception("...should throw NPE");
            } catch (NullPointerException npe) {
                System.out.println("...expected NPE thrown");
            }
        }
        if (F2M_RP.compareTo(F2M.getReductionPolynomial()) != 0) {
            throw new Exception("...error in getReductionPolynomial()");
        }
        ECFieldF2m field = new ECFieldF2m(F2M_M, F2M_RP);
        if (!(Arrays.equals(F2M_KS,
                            field.getMidTermsOfReductionPolynomial()))) {
            throw new Exception("...error in getMidTermsOfReductionPolynomial()");
        }
        if (field.getFieldSize() != F2M_M) {
            throw new Exception("...error in getFieldSize()");
        }
    }
    private static void testECParameterSpec() throws Exception {
        System.out.println("Testing ECParameterSpec(...)");
        for (int i = 0; i < 2; i++) {
            try {
                switch (i) {
                case 0:
                    System.out.println("with zero order");
                    new ECParameterSpec(CURVE, POINT, ZERO, COFACTOR);
                    break;
                case 1:
                    System.out.println("with negative cofactor");
                    new ECParameterSpec(CURVE, POINT, ORDER, -1);
                    break;
                }
                throw new Exception("...should throw IAE");
            } catch (IllegalArgumentException iae) {
                System.out.println("...expected IAE thrown");
            }
        }
        for (int i = 0; i < 3; i++) {
            try {
                switch (i) {
                case 0:
                    System.out.println("with null curve");
                    new ECParameterSpec(null, POINT, ORDER, COFACTOR);
                    break;
                case 1:
                    System.out.println("with null generator");
                    new ECParameterSpec(CURVE, null, ORDER, COFACTOR);
                    break;
                case 2:
                    System.out.println("with null order");
                    new ECParameterSpec(CURVE, POINT, null, COFACTOR);
                    break;
                }
                throw new Exception("...should throw NPE");
            } catch (NullPointerException npe) {
                System.out.println("...expected NPE thrown");
            }
        }
        if (!(CURVE.equals(PARAMS.getCurve()))) {
            throw new Exception("...error in getCurve()");
        }
        if (!(POINT.equals(PARAMS.getGenerator()))) {
            throw new Exception("...error in getGenerator()");
        }
        if (!(ORDER.equals(PARAMS.getOrder()))) {
            throw new Exception("...error in getOrder()");
        }
        if (COFACTOR != PARAMS.getCofactor()) {
            throw new Exception("...error in getCofactor()");
        }
    }
    private static void testECGenParameterSpec() throws Exception {
        System.out.println("Testing ECGenParameterSpec(String)");
        try {
            new ECGenParameterSpec(null);
            throw new Exception("...should throw NPE");
        } catch (NullPointerException npe) {
            System.out.println("...expected NPE thrown");
        }
        if (!("prime192v1".equals(GENPARAMS.getName()))) {
            throw new Exception("...error in getName()");
        }
    }
    private static void testECPoint() throws Exception {
        System.out.println("Testing ECParameterSpec(...)");
        for (int i = 0; i < 2; i++) {
            try {
                switch (i) {
                case 0:
                    System.out.println("with null x-coordinate");
                    new ECPoint(null, TEN);
                    break;
                case 1:
                    System.out.println("with null y-coordinate");
                    new ECPoint(ONE, null);
                    break;
                }
                throw new Exception("...should throw NPE");
            } catch (NullPointerException npe) {
                System.out.println("...expected NPE thrown");
            }
        }
        if (!(ONE.equals(POINT.getAffineX()))) {
            throw new Exception("...error in getAffineX()");
        }
        if (!(TEN.equals(POINT.getAffineY()))) {
            throw new Exception("...error in getAffineY()");
        }
    }
    private static void testECPublicKeySpec() throws Exception {
        System.out.println("Testing ECPublicKeySpec(...)");
        for (int i = 0; i < 2; i++) {
            try {
                switch (i) {
                case 0:
                    System.out.println("with null public value");
                    new ECPublicKeySpec(null, PARAMS);
                    break;
                case 1:
                    System.out.println("with null params");
                    new ECPublicKeySpec(POINT, null);
                    break;
                }
                throw new Exception("...should throw NPE");
            } catch (NullPointerException npe) {
                System.out.println("...expected NPE thrown");
            }
        }
        if (!(POINT.equals(PUB_KEY.getW()))) {
            throw new Exception("...error in getW()");
        }
        if (!(PARAMS.equals(PUB_KEY.getParams()))) {
            throw new Exception("...error in getParams()");
        }
    }
    private static void testECPrivateKeySpec() throws Exception {
        System.out.println("Testing ECPrivateKeySpec(...)");
        for (int i = 0; i < 2; i++) {
            try {
                switch (i) {
                case 0:
                    System.out.println("with null private value");
                    new ECPrivateKeySpec(null, PARAMS);
                    break;
                case 1:
                    System.out.println("with null param");
                    new ECPrivateKeySpec(ONE, null);
                    break;
                }
                throw new Exception("...should throw NPE");
            } catch (NullPointerException npe) {
                System.out.println("...expected NPE thrown");
            }
        }
        if (!(TEN.equals(PRIV_KEY.getS()))) {
            throw new Exception("...error in getS()");
        }
        if (!(PARAMS.equals(PRIV_KEY.getParams()))) {
            throw new Exception("...error in getParams()");
        }
    }
    private static void testEllipticCurve() throws Exception {
        System.out.println("Testing EllipticCurve(...)");
        for (int j = 0; j < 2; j++) {
            BigInteger a = ONE;
            BigInteger b = ONE;
            if (j == 0) { // test a out of range
                System.out.println("with a's value out of range");
                a = BigInteger.valueOf(20);
            } else { // test b out of range
                System.out.println("with b's value out of range");
                b = BigInteger.valueOf(20);
            }
            System.out.println(">> over ECFieldFp");
            for (int i = 0; i < 3; i++) {
                try {
                    switch (i) {
                    case 0:
                        new EllipticCurve(FP, a, b);
                        break;
                    case 1:
                        new EllipticCurve(FP, a, b, null);
                        break;
                    case 2:
                        new EllipticCurve(FP, a, b, new byte[8]);
                        break;
                    }
                    throw new Exception("...should throw IAE");
                } catch (IllegalArgumentException iae) {
                    System.out.println("...expected IAE thrown for #" + (i+1));
                }
            }
            System.out.println(">> over ECFieldF2m");
            for (int i = 0; i < 3; i++) {
                try {
                    switch (i) {
                    case 0:
                        new EllipticCurve(F2M, a, b);
                        break;
                    case 1:
                        new EllipticCurve(F2M, a, b, null);
                        break;
                    case 2:
                        new EllipticCurve(F2M, a, b, new byte[8]);
                        break;
                    }
                    throw new Exception("...should throw IAE");
                } catch (IllegalArgumentException iae) {
                    System.out.println("...expected IAE thrown for #" + (i+1));
                }
            }
        }
        for (int i = 0; i < 6; i++) {
            try {
                switch (i) {
                case 0:
                    new EllipticCurve(null, ONE, TEN);
                    break;
                case 1:
                    new EllipticCurve(FP, null, TEN);
                    break;
                case 2:
                    new EllipticCurve(FP, ONE, null);
                    break;
                case 3:
                    new EllipticCurve(null, ONE, TEN, null);
                    break;
                case 4:
                    new EllipticCurve(FP, null, TEN, null);
                    break;
                case 5:
                    new EllipticCurve(FP, ONE, null, null);
                    break;
                }
                throw new Exception("...should throw NPE");
            } catch (NullPointerException npe) {
                System.out.println("...expected NPE thrown");
            }
        }
        if (!(FP.equals(CURVE.getField()))) {
            throw new Exception("...error in getField()");
        }
        if (!(ONE.equals(CURVE.getA()))) {
            throw new Exception("...error in getA()");
        }
        if (!(ONE.equals(CURVE.getB()))) {
            throw new Exception("...error in getB()");
        }
        if (!(CURVE.equals(new EllipticCurve(FP, ONE, ONE, null)))) {
            throw new Exception("...error in equals()");
        }
    }
    private static void testAllHashCode() throws Exception {
        // make sure no unexpected exception when hashCode() is called.
        FP.hashCode();
        F2M.hashCode();
        GENPARAMS.hashCode();
        PARAMS.hashCode();
        POINT.hashCode();
        PRIV_KEY.hashCode();
        PUB_KEY.hashCode();
        CURVE.hashCode();
    }
    public static void main(String[] argv) throws Exception {
        testECFieldFp();
        testECFieldF2m();
        testECParameterSpec();
        testECGenParameterSpec();
        testECPoint();
        testECPrivateKeySpec();
        testECPublicKeySpec();
        testEllipticCurve();
        testAllHashCode();
        System.out.println("All Test Passed");
    }
}
