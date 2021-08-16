/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6910473 8021204 8021203 9005933 8074460 8078672
 * @summary Test range of BigInteger values (use -Dseed=X to set PRNG seed)
 * @library /test/lib
 * @requires (sun.arch.data.model == "64" & os.maxMemory >= 10g)
 * @run main/timeout=180/othervm -Xmx8g -XX:+CompactStrings SymmetricRangeTests
 * @author Dmitry Nadezhin
 * @key randomness
 */
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.math.BigInteger;
import java.util.Random;
import jdk.test.lib.RandomFactory;

public class SymmetricRangeTests {

    private static final BigInteger MAX_VALUE = makeMaxValue();
    private static final BigInteger MIN_VALUE = MAX_VALUE.negate();

    private static BigInteger makeMaxValue() {
        byte[] ba = new byte[1 << 28];
        Arrays.fill(ba, (byte) 0xFF);
        ba[0] = (byte) 0x7F;
        return new BigInteger(ba);
    }

    private static void check(String msg, BigInteger actual, BigInteger expected) {
        if (!actual.equals(expected)) {
            throw new RuntimeException(msg + ".bitLength()=" + actual.bitLength());
        }
    }

    private static void check(String msg, double actual, double expected) {
        if (actual != expected) {
            throw new RuntimeException(msg + "=" + actual);
        }
    }

    private static void check(String msg, float actual, float expected) {
        if (actual != expected) {
            throw new RuntimeException(msg + "=" + actual);
        }
    }

    private static void check(String msg, long actual, long expected) {
        if (actual != expected) {
            throw new RuntimeException(msg + "=" + actual);
        }
    }

    private static void check(String msg, int actual, int expected) {
        if (actual != expected) {
            throw new RuntimeException(msg + "=" + actual);
        }
    }

    private static void testOverflowInMakePositive() {
        System.out.println("Testing overflow in BigInteger.makePositive");
        byte[] ba = new byte[Integer.MAX_VALUE - 2];
        ba[0] = (byte) 0x80;
        try {
            BigInteger actual = new BigInteger(ba);
            throw new RuntimeException("new BigInteger(ba).bitLength()=" + actual.bitLength());
        } catch (ArithmeticException e) {
            // expected
        }
    }

    private static void testBug8021204() {
        System.out.println("Testing Bug 8021204");
        StringBuilder sb = new StringBuilder();
        sb.append('1');
        for (int i = 0; i < (1 << 30) - 1; i++) {
            sb.append('0');
        }
        sb.append('1');
        String s = sb.toString();
        sb = null;
        try {
            BigInteger actual = new BigInteger(s, 16);
            throw new RuntimeException("new BigInteger(\"1000...001\").bitLength()=" + actual.bitLength());
        } catch (ArithmeticException e) {
            // expected
        }
    }

    private static void testOverflowInBitSieve() {
        System.out.println("Testing overflow in BitSieve.sieveSingle");
        int bitLength = (5 << 27) - 1;
        try {
            Random random = RandomFactory.getRandom();
            BigInteger actual = new BigInteger(bitLength, 0, random);
            throw new RuntimeException("new BigInteger(bitLength, 0, null).bitLength()=" + actual.bitLength());
        } catch (ArithmeticException e) {
            // expected
        }
        try {
            BigInteger bi = BigInteger.ONE.shiftLeft(bitLength - 1).subtract(BigInteger.ONE);
            BigInteger actual = bi.nextProbablePrime();
            throw new RuntimeException("bi.nextActualPrime().bitLength()=" + actual.bitLength());
        } catch (ArithmeticException e) {
            // expected
        }
    }

    private static void testAdd() {
        System.out.println("Testing BigInteger.add");
        try {
            BigInteger actual = MAX_VALUE.add(BigInteger.ONE);
            throw new RuntimeException("BigInteger.MAX_VALUE.add(BigInteger.ONE).bitLength()=" + actual.bitLength());
        } catch (ArithmeticException e) {
            // expected
        }
    }

    private static void testSubtract() {
        System.out.println("Testing BigInteger.subtract");
        try {
            BigInteger actual = MIN_VALUE.subtract(BigInteger.ONE);
            throw new RuntimeException("BigInteger.MIN_VALUE.subtract(BigInteger.ONE).bitLength()=" + actual.bitLength());
        } catch (ArithmeticException e) {
            // expected
        }
    }

    private static void testMultiply() {
        System.out.println("Testing BigInteger.multiply");
        int py = 2000;
        int px = Integer.MAX_VALUE - py;
        BigInteger x = BigInteger.ONE.shiftLeft(px);
        BigInteger y = BigInteger.ONE.shiftLeft(py);
        try {
            BigInteger actual = x.multiply(y);
            throw new RuntimeException("(1 << " + px + " ) * (1 << " + py + ").bitLength()=" + actual.bitLength());
        } catch (ArithmeticException e) {
            // expected
        }
    }

    private static void testDivide() {
        System.out.println("Testing BigInteger.divide");
        check("BigInteger.MIN_VALUE.divide(BigInteger.valueOf(-1))",
                MIN_VALUE.divide(BigInteger.valueOf(-1)), MAX_VALUE);
        check("BigInteger.MIN_VALUE.divide(BigInteger.ONE)",
                MIN_VALUE.divide(BigInteger.ONE), MIN_VALUE);
    }

    private static void testDivideAndRemainder(String msg, BigInteger dividend, BigInteger divisor,
            BigInteger expectedQuotent, BigInteger expectedRemainder) {
        BigInteger[] qr = dividend.divideAndRemainder(divisor);
        check(msg + "[0]", qr[0], expectedQuotent);
        check(msg + "[1]", qr[1], expectedRemainder);
    }

    private static void testDivideAndRemainder() {
        System.out.println("Testing BigInteger.divideAndRemainder");
        testDivideAndRemainder("BigInteger.MIN_VALUE.divideAndRemainder(BigInteger.valueOf(-1))",
                MIN_VALUE, BigInteger.valueOf(-1),
                MAX_VALUE,
                BigInteger.ZERO);
    }

    private static void testBug9005933() {
        System.out.println("Testing Bug 9005933");
        int dividendPow = 2147483646;
        int divisorPow = 1568;
        BigInteger dividend = BigInteger.ONE.shiftLeft(dividendPow);
        BigInteger divisor = BigInteger.ONE.shiftLeft(divisorPow);
        testDivideAndRemainder("(1 << " + dividendPow + ").divideAndRemainder(1 << " + divisorPow + ")",
                dividend, divisor,
                BigInteger.ONE.shiftLeft(dividendPow - divisorPow),
                BigInteger.ZERO);
    }

    private static void testRemainder() {
        System.out.println("Testing BigInteger.remainder");
        check("BigInteger.MIN_VALUE.remainder(BigInteger.valueOf(-1))",
                MIN_VALUE.remainder(BigInteger.valueOf(-1)), BigInteger.ZERO);
    }

    private static void testPow() {
        System.out.println("Testing BigInteger.pow");
        check("BigInteger.MIN_VALUE.pow(1)",
                MIN_VALUE.pow(1), MIN_VALUE);
        try {
            BigInteger actual = BigInteger.valueOf(4).pow(Integer.MAX_VALUE);
            throw new RuntimeException("BigInteger.valueOf(4).pow(Integer.MAX_VALUE).bitLength()=" + actual.bitLength());
        } catch (ArithmeticException e) {
            // expected
        }
    }

    private static void testGcd() {
        System.out.println("Testing BigInteger.gcd");
        check("BigInteger.MIN_VALUE.gcd(BigInteger.MIN_VALUE)",
                MIN_VALUE.gcd(MIN_VALUE), MAX_VALUE);
        check("BigInteger.MIN_VALUE.gcd(BigInteger.ZERO)",
                MIN_VALUE.gcd(BigInteger.ZERO), MAX_VALUE);
        check("BigInteger.ZERO.gcd(MIN_VALUE)",
                BigInteger.ZERO.gcd(MIN_VALUE), MAX_VALUE);
    }

    private static void testAbs() {
        System.out.println("Testing BigInteger.abs");
        check("BigInteger.MIN_VALUE.abs()",
                MIN_VALUE.abs(), MAX_VALUE);
        check("BigInteger.MAX_VALUE.abs()",
                MAX_VALUE.abs(), MAX_VALUE);
    }

    private static void testNegate() {
        System.out.println("Testing BigInteger.negate");
        check("BigInteger.MIN_VALUE.negate()",
                MIN_VALUE.negate(), MAX_VALUE);
        check("BigInteger.MAX_VALUE.negate()",
                MAX_VALUE.negate(), MIN_VALUE);
    }

    private static void testMod() {
        System.out.println("Testing BigInteger.mod");
        check("BigInteger.MIN_VALUE.mod(BigInteger.MAX_VALUE)",
                MIN_VALUE.mod(MAX_VALUE), BigInteger.ZERO);
        check("BigInteger.MAX_VALUE.mod(BigInteger.MAX_VALUE)",
                MIN_VALUE.mod(MAX_VALUE), BigInteger.ZERO);
    }

    private static void testModPow() {
        System.out.println("Testing BigInteger.modPow");
        BigInteger x = BigInteger.valueOf(3);
        BigInteger m = BigInteger.valueOf(-4).subtract(MIN_VALUE);
        check("BigInteger.valueOf(3).modPow(BigInteger.ONE, m)",
                x.modPow(BigInteger.ONE, m), x);
    }

    // slow test
    private static void testModInverse() {
        System.out.println("Testing BigInteger.modInverse");
        check("BigInteger.MIN_VALUE.modInverse(BigInteger.MAX_VALUE)",
                MIN_VALUE.modInverse(MAX_VALUE), MAX_VALUE.subtract(BigInteger.ONE));
    }

    private static void testShiftLeft() {
        System.out.println("Testing BigInteger.shiftLeft");
        try {
            BigInteger actual = MIN_VALUE.shiftLeft(1);
            throw new RuntimeException("BigInteger.MIN_VALUE.shiftLeft(1).bitLength()=" + actual.bitLength());
        } catch (ArithmeticException e) {
            // expected
        }
        try {
            BigInteger actual = MAX_VALUE.shiftLeft(1);
            throw new RuntimeException("BigInteger.MAX_VALUE.shiftLeft(1).bitLength()=" + actual.bitLength());
        } catch (ArithmeticException e) {
            // expected
        }
    }

    private static void testShiftRight() {
        System.out.println("Testing BigInteger.shiftRight");
        try {
            BigInteger actual = MIN_VALUE.shiftRight(-1);
            throw new RuntimeException("BigInteger.MIN_VALUE.shiftRight(-1).bitLength()=" + actual.bitLength());
        } catch (ArithmeticException e) {
            // expected
        }
        try {
            BigInteger actual = MAX_VALUE.shiftRight(-1);
            throw new RuntimeException("BigInteger.MAX_VALUE.shiftRight(-1).bitLength()=" + actual.bitLength());
        } catch (ArithmeticException e) {
            // expected
        }
    }

    private static void testAnd() {
        System.out.println("Testing BigInteger.and");
        check("BigInteger.MIN_VALUE.and(BigInteger.MIN_VALUE)",
                MIN_VALUE.and(MIN_VALUE), MIN_VALUE);
        check("BigInteger.MAX_VALUE.and(BigInteger.MAX_VALUE)",
                MAX_VALUE.and(MAX_VALUE), MAX_VALUE);
        check("BigInteger.MIN_VALUE.and(BigInteger.MAX_VALUE)",
                MIN_VALUE.and(MAX_VALUE), BigInteger.ONE);
        try {
            BigInteger actual = MIN_VALUE.and(BigInteger.valueOf(-2));
            throw new RuntimeException("BigInteger.MIN_VALUE.and(-2)).bitLength()=" + actual.bitLength());
        } catch (ArithmeticException e) {
            // expected
        }
    }

    private static void testOr() {
        System.out.println("Testing BigInteger.or");
        check("BigInteger.MIN_VALUE.or(BigInteger.MIN_VALUE)",
                MIN_VALUE.or(MIN_VALUE), MIN_VALUE);
        check("BigInteger.MAX_VALUE.or(BigInteger.MAX_VALUE)",
                MAX_VALUE.or(MAX_VALUE), MAX_VALUE);
        check("BigInteger.MIN_VALUE.and(BigInteger.MAX_VALUE)",
                MIN_VALUE.or(MAX_VALUE), BigInteger.valueOf(-1));
    }

    private static void testXor() {
        System.out.println("Testing BigInteger.xor");
        check("BigInteger.MIN_VALUE.xor(BigInteger.MIN_VALUE)",
                MIN_VALUE.xor(MIN_VALUE), BigInteger.ZERO);
        check("BigInteger.MAX_VALUE.xor(BigInteger.MAX_VALUE)",
                MAX_VALUE.xor(MAX_VALUE), BigInteger.ZERO);
        check("BigInteger.MIN_VALUE.xor(BigInteger.MAX_VALUE)",
                MIN_VALUE.xor(MAX_VALUE), BigInteger.valueOf(-2));
        try {
            BigInteger actual = MIN_VALUE.xor(BigInteger.ONE);
            throw new RuntimeException("BigInteger.MIN_VALUE.xor(BigInteger.ONE)).bitLength()=" + actual.bitLength());
        } catch (ArithmeticException e) {
            // expected
        }
    }

    private static void testNot() {
        System.out.println("Testing BigInteger.not");
        check("BigInteger.MIN_VALUE.not()",
                MIN_VALUE.not(), MAX_VALUE.subtract(BigInteger.ONE));
        try {
            BigInteger actual = MAX_VALUE.not();
            throw new RuntimeException("BigInteger.MAX_VALUE.not()).bitLength()=" + actual.bitLength());
        } catch (ArithmeticException e) {
            // expected
        }
    }

    private static void testSetBit() {
        System.out.println("Testing BigInteger.setBit");
        check("BigInteger.MIN_VALUE.setBit(" + Integer.MAX_VALUE + ")",
                MIN_VALUE.setBit(Integer.MAX_VALUE), MIN_VALUE);
        try {
            BigInteger actual = MAX_VALUE.setBit(Integer.MAX_VALUE);
            throw new RuntimeException("BigInteger.MAX_VALUE.setBit(" + Integer.MAX_VALUE + ").bitLength()=" + actual.bitLength());
        } catch (ArithmeticException e) {
            // expected
        }
    }

    private static void testClearBit() {
        System.out.println("Testing BigInteger.clearBit");
        check("BigInteger.MAX_VALUE.clearBit(" + Integer.MAX_VALUE + ")",
                MAX_VALUE.clearBit(Integer.MAX_VALUE), MAX_VALUE);
        try {
            BigInteger actual = MIN_VALUE.clearBit(Integer.MAX_VALUE);
            throw new RuntimeException("BigInteger.MIN_VALUE.clearBit(" + Integer.MAX_VALUE + ").bitLength()=" + actual.bitLength());
        } catch (ArithmeticException e) {
            // expected
        }
        try {
            BigInteger actual = MIN_VALUE.clearBit(0);
            throw new RuntimeException("BigInteger.MIN_VALUE.clearBit(0).bitLength()=" + actual.bitLength());
        } catch (ArithmeticException e) {
            // expected
        }
    }

    private static void testFlipBit() {
        System.out.println("Testing BigInteger.flipBit");
        try {
            BigInteger actual = MIN_VALUE.flipBit(Integer.MAX_VALUE);
            throw new RuntimeException("BigInteger.MIN_VALUE.flipBit(" + Integer.MAX_VALUE + ").bitLength()=" + actual.bitLength());
        } catch (ArithmeticException e) {
            // expected
        }
        try {
            BigInteger actual = MIN_VALUE.flipBit(0);
            throw new RuntimeException("BigInteger.MIN_VALUE.flipBit(0).bitLength()=" + actual.bitLength());
        } catch (ArithmeticException e) {
            // expected
        }
        try {
            BigInteger actual = MAX_VALUE.flipBit(Integer.MAX_VALUE);
            throw new RuntimeException("BigInteger.MAX_VALUE.flipBit(" + Integer.MAX_VALUE + ").bitLength()=" + actual.bitLength());
        } catch (ArithmeticException e) {
            // expected
        }
    }

    private static void testGetLowestSetBit() {
        System.out.println("Testing BigInteger.getLowestSetBit");
        check("BigInteger.MIN_VALUE.getLowestSetBit()",
                MIN_VALUE.getLowestSetBit(), 0);
        check("BigInteger.MAX_VALUE.getLowestSetBit()",
                MAX_VALUE.getLowestSetBit(), 0);
    }

    private static void testBitLength() {
        System.out.println("Testing BigInteger.bitLength");
        check("BigInteger.MIN_NEXT.bitLength()",
                MIN_VALUE.bitLength(), Integer.MAX_VALUE);
        check("BigInteger.MAX_VALUE.bitLength()",
                MAX_VALUE.bitLength(), Integer.MAX_VALUE);
    }

    private static void testBitCount() {
        System.out.println("Testing BigInteger.bitCount");
        check("BigInteger.MIN_VALUE.bitCount()",
                MIN_VALUE.bitCount(), Integer.MAX_VALUE - 1);
        check("BigInteger.MAX_VALUE.bitCount()",
                MAX_VALUE.bitCount(), Integer.MAX_VALUE);
    }

    private static void testToString(String msg, int radix, BigInteger bi, int length, String startsWith, char c) {
        String s = bi.toString(radix);
        if (s.length() != length) {
            throw new RuntimeException(msg + ".length=" + s.length());
        }
        if (!s.startsWith(startsWith)) {
            throw new RuntimeException(msg + "[0]=" + s.substring(0, startsWith.length()));
        }
        for (int i = startsWith.length(); i < s.length(); i++) {
            if (s.charAt(i) != c) {
                throw new RuntimeException(msg + "[" + i + "]='" + s.charAt(i) + "'");
            }
        }
    }

    private static void testToString() {
        System.out.println("Testing BigInteger.toString");
        testToString("BigInteger.MIN_VALUE.toString(16)=", 16,
                BigInteger.valueOf(-1).shiftLeft(Integer.MAX_VALUE - 1),
                (1 << 29) + 1, "-4", '0');
    }

    private static void testToByteArrayWithConstructor(String msg, BigInteger bi, int length, byte msb, byte b, byte lsb) {
        byte[] ba = bi.toByteArray();
        if (ba.length != length) {
            throw new RuntimeException(msg + ".length=" + ba.length);
        }
        if (ba[0] != msb) {
            throw new RuntimeException(msg + "[0]=" + ba[0]);
        }
        for (int i = 1; i < ba.length - 1; i++) {
            if (ba[i] != b) {
                throw new RuntimeException(msg + "[" + i + "]=" + ba[i]);
            }
        }
        if (ba[ba.length - 1] != lsb) {
            throw new RuntimeException(msg + "[" + (ba.length - 1) + "]=" + ba[ba.length - 1]);
        }
        BigInteger actual = new BigInteger(ba);
        if (!actual.equals(bi)) {
            throw new RuntimeException(msg + ".bitLength()=" + actual.bitLength());
        }
    }

    private static void testToByteArrayWithConstructor() {
        System.out.println("Testing BigInteger.toByteArray with constructor");
        testToByteArrayWithConstructor("BigInteger.MIN_VALUE.toByteArray()",
                MIN_VALUE, (1 << 28), (byte) 0x80, (byte) 0x00, (byte) 0x01);
        testToByteArrayWithConstructor("BigInteger.MAX_VALUE.toByteArray()",
                MAX_VALUE, (1 << 28), (byte) 0x7f, (byte) 0xff, (byte) 0xff);

        byte[] ba = new byte[1 << 28];
        ba[0] = (byte) 0x80;
        try {
            BigInteger actual = new BigInteger(-1, ba);
            throw new RuntimeException("new BigInteger(-1, ba).bitLength()=" + actual.bitLength());
        } catch (ArithmeticException e) {
            // expected
        }
        try {
            BigInteger actual = new BigInteger(1, ba);
            throw new RuntimeException("new BigInteger(1, ba).bitLength()=" + actual.bitLength());
        } catch (ArithmeticException e) {
            // expected
        }
    }

    private static void testIntValue() {
        System.out.println("Testing BigInteger.intValue");
        check("BigInteger.MIN_VALUE.intValue()",
                MIN_VALUE.intValue(), 1);
        check("BigInteger.MAX_VALUE.floatValue()",
                MAX_VALUE.intValue(), -1);
    }

    private static void testLongValue() {
        System.out.println("Testing BigInteger.longValue");
        check("BigInteger.MIN_VALUE.longValue()",
                MIN_VALUE.longValue(), 1L);
        check("BigInteger.MAX_VALUE.longValue()",
                MAX_VALUE.longValue(), -1L);
    }

    private static void testFloatValue() {
        System.out.println("Testing BigInteger.floatValue, Bug 8021203");
        check("BigInteger.MIN_VALUE_.floatValue()",
                MIN_VALUE.floatValue(), Float.NEGATIVE_INFINITY);
        check("BigInteger.MAX_VALUE.floatValue()",
                MAX_VALUE.floatValue(), Float.POSITIVE_INFINITY);
    }

    private static void testDoubleValue() {
        System.out.println("Testing BigInteger.doubleValue, Bug 8021203");
        check("BigInteger.MIN_VALUE.doubleValue()",
                MIN_VALUE.doubleValue(), Double.NEGATIVE_INFINITY);
        check("BigInteger.MAX_VALUE.doubleValue()",
                MAX_VALUE.doubleValue(), Double.POSITIVE_INFINITY);
    }

    private static void testSerialization(String msg, BigInteger bi) {
        try {
            ByteArrayOutputStream baOut = new ByteArrayOutputStream((1 << 28) + 1000);
            ObjectOutputStream out = new ObjectOutputStream(baOut);
            out.writeObject(bi);
            out.close();
            out = null;
            byte[] ba = baOut.toByteArray();
            baOut = null;
            ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(ba));
            BigInteger actual = (BigInteger) in.readObject();
            if (!actual.equals(bi)) {
                throw new RuntimeException(msg + ".bitLength()=" + actual.bitLength());
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(msg + " raised exception ", e);
        }
    }

    private static void testSerialization() {
        System.out.println("Testing BigInteger serialization");
        testSerialization("BigInteger.MIN_VALUE.intValue()",
                MIN_VALUE);
        testSerialization("BigInteger.MAX_VALUE.floatValue()",
                MAX_VALUE);
    }

    private static void testLongValueExact() {
        System.out.println("Testing BigInteger.longValueExact");
        try {
            long actual = MIN_VALUE.longValueExact();
            throw new RuntimeException("BigInteger.MIN_VALUE.longValueExact()= " + actual);
        } catch (ArithmeticException e) {
            // excpected
        }
        try {
            long actual = MAX_VALUE.longValueExact();
            throw new RuntimeException("BigInteger.MAX_VALUE.longValueExact()= " + actual);
        } catch (ArithmeticException e) {
            // excpected
        }
    }

    private static void testIntValueExact() {
        System.out.println("Testing BigInteger.intValueExact");
        try {
            long actual = MIN_VALUE.intValueExact();
            throw new RuntimeException("BigInteger.MIN_VALUE.intValueExact()= " + actual);
        } catch (ArithmeticException e) {
            // excpected
        }
        try {
            long actual = MAX_VALUE.intValueExact();
            throw new RuntimeException("BigInteger.MAX_VALUE.intValueExact()= " + actual);
        } catch (ArithmeticException e) {
            // excpected
        }
    }

    private static void testShortValueExact() {
        System.out.println("Testing BigInteger.shortValueExact");
        try {
            long actual = MIN_VALUE.shortValueExact();
            throw new RuntimeException("BigInteger.MIN_VALUE.shortValueExact()= " + actual);
        } catch (ArithmeticException e) {
            // excpected
        }
        try {
            long actual = MAX_VALUE.shortValueExact();
            throw new RuntimeException("BigInteger.MAX_VALUE.shortValueExact()= " + actual);
        } catch (ArithmeticException e) {
            // excpected
        }
    }

    private static void testByteValueExact() {
        System.out.println("Testing BigInteger.byteValueExact");
        try {
            long actual = MIN_VALUE.byteValueExact();
            throw new RuntimeException("BigInteger.MIN_VALUE.byteValueExact()= " + actual);
        } catch (ArithmeticException e) {
            // excpected
        }
        try {
            long actual = MAX_VALUE.byteValueExact();
            throw new RuntimeException("BigInteger.MAX_VALUE.byteValueExact()= " + actual);
        } catch (ArithmeticException e) {
            // excpected
        }
    }

    public static void main(String... args) {
        testOverflowInMakePositive();
        testBug8021204();
        testOverflowInBitSieve();
        testAdd();
        testSubtract();
        testMultiply();
        testDivide();
        testDivideAndRemainder();
        testBug9005933();
        testRemainder();
        testPow();
        testGcd();
        testAbs();
        testNegate();
        testMod();
        testModPow();
//        testModInverse();
        testShiftLeft();
        testShiftRight();
        testAnd();
        testOr();
        testXor();
        testNot();
        testSetBit();
        testClearBit();
        testFlipBit();
        testGetLowestSetBit();
        testBitLength();
        testBitCount();
        testToString();
        testToByteArrayWithConstructor();
        testIntValue();
        testLongValue();
        testFloatValue();
        testDoubleValue();
        testSerialization();
        testLongValueExact();
        testIntValueExact();
        testShortValueExact();
        testByteValueExact();
    }
}
