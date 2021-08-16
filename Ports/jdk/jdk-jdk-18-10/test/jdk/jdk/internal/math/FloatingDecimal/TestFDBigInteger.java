/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.math.BigInteger;
import java.util.Random;
import jdk.internal.math.FDBigInteger;

/**
 * @test
 * @bug 7032154
 * @summary unit testys of FDBigInteger
 * @modules java.base/jdk.internal.math
 * @author Dmitry Nadezhin
 */
public class TestFDBigInteger {

    private static final int MAX_P5 = 413;
    private static final int MAX_P2 = 65;
    private static final long LONG_SIGN_MASK = (1L << 63);
    private static final BigInteger FIVE = BigInteger.valueOf(5);
    private static final FDBigInteger MUTABLE_ZERO = FDBigInteger.valueOfPow52(0, 0).leftInplaceSub(FDBigInteger.valueOfPow52(0, 0));
    private static final FDBigInteger IMMUTABLE_ZERO = FDBigInteger.valueOfPow52(0, 0).leftInplaceSub(FDBigInteger.valueOfPow52(0, 0));
    private static final FDBigInteger IMMUTABLE_MILLION = genMillion1();
    private static final FDBigInteger IMMUTABLE_BILLION = genBillion1();
    private static final FDBigInteger IMMUTABLE_TEN18 = genTen18();

    static {
        IMMUTABLE_ZERO.makeImmutable();
        IMMUTABLE_MILLION.makeImmutable();
        IMMUTABLE_BILLION.makeImmutable();
        IMMUTABLE_TEN18.makeImmutable();
    }

    private static FDBigInteger mutable(String hex, int offset) {
        char[] chars = new BigInteger(hex, 16).toString().toCharArray();
        return new FDBigInteger(0, chars, 0, chars.length).multByPow52(0, offset * 32);
    }

    private static FDBigInteger immutable(String hex, int offset) {
        FDBigInteger fd = mutable(hex, offset);
        fd.makeImmutable();
        return fd;
    }

    private static BigInteger biPow52(int p5, int p2) {
        return FIVE.pow(p5).shiftLeft(p2);
    }

    // data.length == 1, nWords == 1, offset == 0
    private static FDBigInteger genMillion1() {
        return FDBigInteger.valueOfPow52(6, 0).leftShift(6);
    }

    // data.length == 2, nWords == 1, offset == 0
    private static FDBigInteger genMillion2() {
        return FDBigInteger.valueOfMulPow52(1000000L, 0, 0);
    }

    // data.length == 1, nWords == 1, offset == 0
    private static FDBigInteger genBillion1() {
        return FDBigInteger.valueOfPow52(9, 0).leftShift(9);
    }

    // data.length == 2, nWords == 2, offset == 0
    private static FDBigInteger genTen18() {
        return FDBigInteger.valueOfPow52(18, 0).leftShift(18);
    }

    private static void check(BigInteger expected, FDBigInteger actual, String message) throws Exception {
        if (!expected.equals(actual.toBigInteger())) {
            throw new Exception(message + " result " + actual.toHexString() + " expected " + expected.toString(16));
        }
    }

    private static void testValueOfPow52(int p5, int p2) throws Exception {
        check(biPow52(p5, p2), FDBigInteger.valueOfPow52(p5, p2),
                "valueOfPow52(" + p5 + "," + p2 + ")");
    }

    private static void testValueOfPow52() throws Exception {
        for (int p5 = 0; p5 <= MAX_P5; p5++) {
            for (int p2 = 0; p2 <= MAX_P2; p2++) {
                testValueOfPow52(p5, p2);
            }
        }
    }

    private static void testValueOfMulPow52(long value, int p5, int p2) throws Exception {
        BigInteger bi = BigInteger.valueOf(value & ~LONG_SIGN_MASK);
        if (value < 0) {
            bi = bi.setBit(63);
        }
        check(biPow52(p5, p2).multiply(bi), FDBigInteger.valueOfMulPow52(value, p5, p2),
                "valueOfMulPow52(" + Long.toHexString(value) + "." + p5 + "," + p2 + ")");
    }

    private static void testValueOfMulPow52(long value, int p5) throws Exception {
        testValueOfMulPow52(value, p5, 0);
        testValueOfMulPow52(value, p5, 1);
        testValueOfMulPow52(value, p5, 30);
        testValueOfMulPow52(value, p5, 31);
        testValueOfMulPow52(value, p5, 33);
        testValueOfMulPow52(value, p5, 63);
    }

    private static void testValueOfMulPow52() throws Exception {
        for (int p5 = 0; p5 <= MAX_P5; p5++) {
            testValueOfMulPow52(0xFFFFFFFFL, p5);
            testValueOfMulPow52(0x123456789AL, p5);
            testValueOfMulPow52(0x7FFFFFFFFFFFFFFFL, p5);
            testValueOfMulPow52(0xFFFFFFFFFFF54321L, p5);
        }
    }

    private static void testLeftShift(FDBigInteger t, int shift, boolean isImmutable) throws Exception {
        BigInteger bt = t.toBigInteger();
        FDBigInteger r = t.leftShift(shift);
        if ((bt.signum() == 0 || shift == 0 || !isImmutable) && r != t) {
            throw new Exception("leftShift doesn't reuse its argument");
        }
        if (isImmutable) {
            check(bt, t, "leftShift corrupts its argument");
        }
        check(bt.shiftLeft(shift), r, "leftShift returns wrong result");
    }

    private static void testLeftShift() throws Exception {
        testLeftShift(IMMUTABLE_ZERO, 0, true);
        testLeftShift(IMMUTABLE_ZERO, 10, true);
        testLeftShift(MUTABLE_ZERO, 0, false);
        testLeftShift(MUTABLE_ZERO, 10, false);

        testLeftShift(IMMUTABLE_MILLION, 0, true);
        testLeftShift(IMMUTABLE_MILLION, 1, true);
        testLeftShift(IMMUTABLE_MILLION, 12, true);
        testLeftShift(IMMUTABLE_MILLION, 13, true);
        testLeftShift(IMMUTABLE_MILLION, 32, true);
        testLeftShift(IMMUTABLE_MILLION, 33, true);
        testLeftShift(IMMUTABLE_MILLION, 44, true);
        testLeftShift(IMMUTABLE_MILLION, 45, true);

        testLeftShift(genMillion1(), 0, false);
        testLeftShift(genMillion1(), 1, false);
        testLeftShift(genMillion1(), 12, false);
        testLeftShift(genMillion1(), 13, false);
        testLeftShift(genMillion1(), 25, false);
        testLeftShift(genMillion1(), 26, false);
        testLeftShift(genMillion1(), 32, false);
        testLeftShift(genMillion1(), 33, false);
        testLeftShift(genMillion1(), 44, false);
        testLeftShift(genMillion1(), 45, false);

        testLeftShift(genMillion2(), 0, false);
        testLeftShift(genMillion2(), 1, false);
        testLeftShift(genMillion2(), 12, false);
        testLeftShift(genMillion2(), 13, false);
        testLeftShift(genMillion2(), 25, false);
        testLeftShift(genMillion2(), 26, false);
        testLeftShift(genMillion2(), 32, false);
        testLeftShift(genMillion2(), 33, false);
        testLeftShift(genMillion2(), 44, false);
        testLeftShift(genMillion2(), 45, false);
    }

    private static void testQuoRemIteration(FDBigInteger t, FDBigInteger s) throws Exception {
        BigInteger bt = t.toBigInteger();
        BigInteger bs = s.toBigInteger();
        int q = t.quoRemIteration(s);
        BigInteger[] qr = bt.divideAndRemainder(bs);
        if (!BigInteger.valueOf(q).equals(qr[0])) {
            throw new Exception("quoRemIteration returns incorrect quo");
        }
        check(qr[1].multiply(BigInteger.TEN), t, "quoRemIteration returns incorrect rem");
    }

    private static void testQuoRemIteration() throws Exception {
        // IMMUTABLE_TEN18 == 0de0b6b3a7640000
        // q = 0
        testQuoRemIteration(mutable("00000001", 0), IMMUTABLE_TEN18);
        testQuoRemIteration(mutable("00000001", 1), IMMUTABLE_TEN18);
        testQuoRemIteration(mutable("0de0b6b2", 1), IMMUTABLE_TEN18);
        // q = 1 -> q = 0
        testQuoRemIteration(mutable("0de0b6b3", 1), IMMUTABLE_TEN18);
        testQuoRemIteration(mutable("0de0b6b3a763FFFF", 0), IMMUTABLE_TEN18);
        // q = 1
        testQuoRemIteration(mutable("0de0b6b3a7640000", 0), IMMUTABLE_TEN18);
        testQuoRemIteration(mutable("0de0b6b3FFFFFFFF", 0), IMMUTABLE_TEN18);
        testQuoRemIteration(mutable("8ac72304", 1), IMMUTABLE_TEN18);
        testQuoRemIteration(mutable("0de0b6b400000000", 0), IMMUTABLE_TEN18);
        testQuoRemIteration(mutable("8ac72305", 1), IMMUTABLE_TEN18);
        // q = 18
        testQuoRemIteration(mutable("FFFFFFFF", 1), IMMUTABLE_TEN18);
    }

    private static void testCmp(FDBigInteger t, FDBigInteger o) throws Exception {
        BigInteger bt = t.toBigInteger();
        BigInteger bo = o.toBigInteger();
        int cmp = t.cmp(o);
        int bcmp = bt.compareTo(bo);
        if (bcmp != cmp) {
            throw new Exception("cmp returns " + cmp + " expected " + bcmp);
        }
        check(bt, t, "cmp corrupts this");
        check(bo, o, "cmp corrupts other");
        if (o.cmp(t) != -cmp) {
            throw new Exception("asymmetrical cmp");
        }
        check(bt, t, "cmp corrupts this");
        check(bo, o, "cmp corrupts other");
    }

    private static void testCmp() throws Exception {
        testCmp(mutable("FFFFFFFF", 0), mutable("100000000", 0));
        testCmp(mutable("FFFFFFFF", 0), mutable("1", 1));
        testCmp(mutable("5", 0), mutable("6", 0));
        testCmp(mutable("5", 0), mutable("5", 0));
        testCmp(mutable("5000000001", 0), mutable("500000001", 0));
        testCmp(mutable("5000000001", 0), mutable("6", 1));
        testCmp(mutable("5000000001", 0), mutable("5", 1));
        testCmp(mutable("5000000000", 0), mutable("5", 1));
    }

    private static void testCmpPow52(FDBigInteger t, int p5, int p2) throws Exception {
        FDBigInteger o = FDBigInteger.valueOfPow52(p5, p2);
        BigInteger bt = t.toBigInteger();
        BigInteger bo = biPow52(p5, p2);
        int cmp = t.cmp(o);
        int bcmp = bt.compareTo(bo);
        if (bcmp != cmp) {
            throw new Exception("cmpPow52 returns " + cmp + " expected " + bcmp);
        }
        check(bt, t, "cmpPow52 corrupts this");
        check(bo, o, "cmpPow5 corrupts other");
    }

    private static void testCmpPow52() throws Exception {
        testCmpPow52(mutable("00000002", 1), 0, 31);
        testCmpPow52(mutable("00000002", 1), 0, 32);
        testCmpPow52(mutable("00000002", 1), 0, 33);
        testCmpPow52(mutable("00000002", 1), 0, 34);
        testCmpPow52(mutable("00000002", 1), 0, 64);
        testCmpPow52(mutable("00000003", 1), 0, 32);
        testCmpPow52(mutable("00000003", 1), 0, 33);
        testCmpPow52(mutable("00000003", 1), 0, 34);
    }

    private static void testAddAndCmp(FDBigInteger t, FDBigInteger x, FDBigInteger y) throws Exception {
        BigInteger bt = t.toBigInteger();
        BigInteger bx = x.toBigInteger();
        BigInteger by = y.toBigInteger();
        int cmp = t.addAndCmp(x, y);
        int bcmp = bt.compareTo(bx.add(by));
        if (bcmp != cmp) {
            throw new Exception("addAndCmp returns " + cmp + " expected " + bcmp);
        }
        check(bt, t, "addAndCmp corrupts this");
        check(bx, x, "addAndCmp corrupts x");
        check(by, y, "addAndCmp corrupts y");
    }

    private static void testAddAndCmp() throws Exception {
        testAddAndCmp(MUTABLE_ZERO, MUTABLE_ZERO, MUTABLE_ZERO);
        testAddAndCmp(mutable("00000001", 0), MUTABLE_ZERO, MUTABLE_ZERO);
        testAddAndCmp(mutable("00000001", 0), mutable("00000001", 0), MUTABLE_ZERO);
        testAddAndCmp(mutable("00000001", 0), MUTABLE_ZERO, mutable("00000001", 0));
        testAddAndCmp(mutable("00000001", 0), mutable("00000002", 0), MUTABLE_ZERO);
        testAddAndCmp(mutable("00000001", 0), MUTABLE_ZERO, mutable("00000002", 0));
        testAddAndCmp(mutable("00000001", 2), mutable("FFFFFFFF", 0), mutable("FFFFFFFF", 0));
        testAddAndCmp(mutable("00000001", 0), mutable("00000001", 1), mutable("00000001", 0));

        testAddAndCmp(mutable("00000001", 2), mutable("0F0F0F0F80000000", 1), mutable("F0F0F0F080000000", 1));
        testAddAndCmp(mutable("00000001", 2), mutable("0F0F0F0E80000000", 1), mutable("F0F0F0F080000000", 1));

        testAddAndCmp(mutable("00000002", 1), mutable("0000000180000000", 1), mutable("0000000280000000", 1));
        testAddAndCmp(mutable("00000003", 1), mutable("0000000180000000", 1), mutable("0000000280000000", 1));
        testAddAndCmp(mutable("00000004", 1), mutable("0000000180000000", 1), mutable("0000000280000000", 1));
        testAddAndCmp(mutable("00000005", 1), mutable("0000000180000000", 1), mutable("0000000280000000", 1));

        testAddAndCmp(mutable("00000001", 2), mutable("8000000000000000", 0), mutable("8000000000000000", 0));
        testAddAndCmp(mutable("00000001", 2), mutable("8000000000000000", 0), mutable("8000000000000001", 0));
        testAddAndCmp(mutable("00000002", 2), mutable("8000000000000000", 0), mutable("8000000000000000", 0));
        testAddAndCmp(mutable("00000003", 2), mutable("8000000000000000", 0), mutable("8000000000000000", 0));
    }

    private static void testMultBy10(FDBigInteger t, boolean isImmutable) throws Exception {
        BigInteger bt = t.toBigInteger();
        FDBigInteger r = t.multBy10();
        if ((bt.signum() == 0 || !isImmutable) && r != t) {
            throw new Exception("multBy10 of doesn't reuse its argument");
        }
        if (isImmutable) {
            check(bt, t, "multBy10 corrupts its argument");
        }
        check(bt.multiply(BigInteger.TEN), r, "multBy10 returns wrong result");
    }

    private static void testMultBy10() throws Exception {
        for (int p5 = 0; p5 <= MAX_P5; p5++) {
            for (int p2 = 0; p2 <= MAX_P2; p2++) {
                // This strange way of creating a value ensures that it is mutable.
                FDBigInteger value = FDBigInteger.valueOfPow52(0, 0).multByPow52(p5, p2);
                testMultBy10(value, false);
                value.makeImmutable();
                testMultBy10(value, true);
            }
        }
    }

    private static void testMultByPow52(FDBigInteger t, int p5, int p2) throws Exception {
        BigInteger bt = t.toBigInteger();
        FDBigInteger r = t.multByPow52(p5, p2);
        if (bt.signum() == 0 && r != t) {
            throw new Exception("multByPow52 of doesn't reuse its argument");
        }
        check(bt.multiply(biPow52(p5, p2)), r, "multByPow52 returns wrong result");
    }

    private static void testMultByPow52() throws Exception {
        for (int p5 = 0; p5 <= MAX_P5; p5++) {
            for (int p2 = 0; p2 <= MAX_P2; p2++) {
                // This strange way of creating a value ensures that it is mutable.
                FDBigInteger value = FDBigInteger.valueOfPow52(0, 0).multByPow52(p5, p2);
                testMultByPow52(value, p5, p2);
            }
        }
    }

    private static void testLeftInplaceSub(FDBigInteger left, FDBigInteger right, boolean isImmutable) throws Exception {
        BigInteger biLeft = left.toBigInteger();
        BigInteger biRight = right.toBigInteger();
        FDBigInteger diff = left.leftInplaceSub(right);
        if (!isImmutable && diff != left) {
            throw new Exception("leftInplaceSub of doesn't reuse its argument");
        }
        if (isImmutable) {
            check(biLeft, left, "leftInplaceSub corrupts its left immutable argument");
        }
        check(biRight, right, "leftInplaceSub corrupts its right argument");
        check(biLeft.subtract(biRight), diff, "leftInplaceSub returns wrong result");
    }

    private static void testLeftInplaceSub() throws Exception {
        for (int p5 = 0; p5 <= MAX_P5; p5++) {
            for (int p2 = 0; p2 <= MAX_P2; p2++) {
//                for (int p5r = 0; p5r <= p5; p5r += 10) {
//                    for (int p2r = 0; p2r <= p2; p2r += 10) {
                for (int p5r = 0; p5r <= p5; p5r++) {
                    for (int p2r = 0; p2r <= p2; p2r++) {
                        // This strange way of creating a value ensures that it is mutable.
                        FDBigInteger left = FDBigInteger.valueOfPow52(0, 0).multByPow52(p5, p2);
                        FDBigInteger right = FDBigInteger.valueOfPow52(0, 0).multByPow52(p5r, p2r);
                        testLeftInplaceSub(left, right, false);
                        left = FDBigInteger.valueOfPow52(0, 0).multByPow52(p5, p2);
                        left.makeImmutable();
                        testLeftInplaceSub(left, right, true);
                    }
                }
            }
        }
    }

    private static void testRightInplaceSub(FDBigInteger left, FDBigInteger right, boolean isImmutable) throws Exception {
        BigInteger biLeft = left.toBigInteger();
        BigInteger biRight = right.toBigInteger();
        FDBigInteger diff = left.rightInplaceSub(right);
        if (!isImmutable && diff != right) {
            throw new Exception("rightInplaceSub of doesn't reuse its argument");
        }
        check(biLeft, left, "leftInplaceSub corrupts its left argument");
        if (isImmutable) {
            check(biRight, right, "leftInplaceSub corrupts its right immutable argument");
        }
        try {
            check(biLeft.subtract(biRight), diff, "rightInplaceSub returns wrong result");
        } catch (Exception e) {
            System.out.println(biLeft+" - "+biRight+" = "+biLeft.subtract(biRight));
            throw e;
        }
    }

    private static void testRightInplaceSub() throws Exception {
        for (int p5 = 0; p5 <= MAX_P5; p5++) {
            for (int p2 = 0; p2 <= MAX_P2; p2++) {
//                for (int p5r = 0; p5r <= p5; p5r += 10) {
//                    for (int p2r = 0; p2r <= p2; p2r += 10) {
                for (int p5r = 0; p5r <= p5; p5r++) {
                    for (int p2r = 0; p2r <= p2; p2r++) {
                        // This strange way of creating a value ensures that it is mutable.
                        FDBigInteger left = FDBigInteger.valueOfPow52(0, 0).multByPow52(p5, p2);
                        FDBigInteger right = FDBigInteger.valueOfPow52(0, 0).multByPow52(p5r, p2r);
                        testRightInplaceSub(left, right, false);
                        right = FDBigInteger.valueOfPow52(0, 0).multByPow52(p5r, p2r);
                        right.makeImmutable();
                        testRightInplaceSub(left, right, true);
                    }
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        testValueOfPow52();
        testValueOfMulPow52();
        testLeftShift();
        testQuoRemIteration();
        testCmp();
        testCmpPow52();
        testAddAndCmp();
        // Uncomment the following for more comprehensize but slow testing.
        // testLeftInplaceSub();
        // testMultBy10();
        // testMultByPow52();
        // testRightInplaceSub();
    }
}
