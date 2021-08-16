/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @library /test/lib
 * @build jdk.test.lib.RandomFactory
 * @run main BitTwiddle
 * @bug     4495754 8078672
 * @summary Basic test for int bit twiddling (use -Dseed=X to set PRNG seed)
 * @author  Josh Bloch
 * @key randomness
 */

import java.util.Random;
import jdk.test.lib.RandomFactory;
import static java.lang.Integer.*;

public class BitTwiddle {
    private static final int N = 1000; // # of repetitions per test

    public static void main(String args[]) {
        Random rnd = RandomFactory.getRandom();

        if (highestOneBit(0) != 0)
            throw new RuntimeException("a");
        if (highestOneBit(-1) != MIN_VALUE)
            throw new RuntimeException("b");
        if (highestOneBit(1) != 1)
            throw new RuntimeException("c");

        if (lowestOneBit(0) != 0)
            throw new RuntimeException("d");
        if (lowestOneBit(-1) != 1)
            throw new RuntimeException("e");
        if (lowestOneBit(MIN_VALUE) != MIN_VALUE)
            throw new RuntimeException("f");

        for (int i = 0; i < N; i++) {
            int x = rnd.nextInt();

            String expected = new StringBuilder()
                .append(leftpad(toBinaryString(x), 32))
                .reverse().toString();

            String actual = leftpad(toBinaryString(reverse(x)), 32);

            if (!expected.equals(actual)) {
                throw new RuntimeException("reverse: \n" +
                        expected + " \n" + actual);
            }
        }

        for (int i = 0; i < N; i++) {
            int x = rnd.nextInt();
            if (highestOneBit(x) != reverse(lowestOneBit(reverse(x))))
                throw new RuntimeException("g: " + toHexString(x));
        }

        if (numberOfLeadingZeros(0) != SIZE)
            throw new RuntimeException("h");
        if (numberOfLeadingZeros(-1) != 0)
            throw new RuntimeException("i");
        if (numberOfLeadingZeros(1) != (SIZE - 1))
            throw new RuntimeException("j");
        if (numberOfLeadingZeros(Integer.MAX_VALUE) != 1)
            throw new RuntimeException("lzmax");

        if (numberOfTrailingZeros(0) != SIZE)
            throw new RuntimeException("k");
        if (numberOfTrailingZeros(1) != 0)
            throw new RuntimeException("l");
        if (numberOfTrailingZeros(MIN_VALUE) != (SIZE - 1))
            throw new RuntimeException("m");

        for (int i = 0; i < N; i++) {
            int x = rnd.nextInt();
            if (numberOfLeadingZeros(x) != numberOfTrailingZeros(reverse(x)))
                throw new RuntimeException("n: " + toHexString(x));
        }
        for (int i = 1, r = SIZE - 1; i != 0; i <<= 1, r--) {
            if (numberOfLeadingZeros(i) != r ||
                numberOfTrailingZeros(i) != (SIZE - r - 1) ||
                numberOfLeadingZeros(i) != numberOfTrailingZeros(reverse(i))) {
                throw new RuntimeException("lzx: " + toHexString(i));
            }
        }

        if (bitCount(0) != 0)
                throw new RuntimeException("o");

        for (int i = 0; i < SIZE; i++) {
            int pow2 = 1 << i;
            if (bitCount(pow2) != 1)
                throw new RuntimeException("p: " + i);
            if (bitCount(pow2 -1) != i)
                throw new RuntimeException("q: " + i);
        }

        for (int i = 0; i < N; i++) {
            int x = rnd.nextInt();
            if (bitCount(x) != bitCount(reverse(x)))
                throw new RuntimeException("r: " + toHexString(x));
        }

        for (int i = 0; i < N; i++) {
            int x = rnd.nextInt();
            int dist = rnd.nextInt();
            if (bitCount(x) != bitCount(rotateRight(x, dist)))
                throw new RuntimeException("s: " + toHexString(x) +
                                           toHexString(dist));
            if (bitCount(x) != bitCount(rotateLeft(x, dist)))
                throw new RuntimeException("t: " + toHexString(x) +
                                           toHexString(dist));
            if (rotateRight(x, dist) != rotateLeft(x, -dist))
                throw new RuntimeException("u: " + toHexString(x) +
                                           toHexString(dist));
            if (rotateRight(x, -dist) != rotateLeft(x, dist))
                throw new RuntimeException("v: " + toHexString(x) +
                                           toHexString(dist));
        }

        if (signum(0) != 0 || signum(1) != 1 || signum(-1) != -1
            || signum(MIN_VALUE) != -1 || signum(MAX_VALUE) != 1)
            throw new RuntimeException("w");

        for (int i = 0; i < N; i++) {
            int x = rnd.nextInt();
            int sign = (x < 0 ? -1 : (x == 0 ? 0 : 1));
            if (signum(x) != sign)
                throw new RuntimeException("x: " + toHexString(x));
        }

        if(reverseBytes(0xaabbccdd) != 0xddccbbaa)
            throw new RuntimeException("y");

        for (int i = 0; i < N; i++) {
            int x = rnd.nextInt();
            if (bitCount(x) != bitCount(reverseBytes(x)))
                throw new RuntimeException("z: " + toHexString(x));
        }
    }

    private static final String ZEROS = "0".repeat(32);
    private static String leftpad(String s, int width) {
        return ZEROS.substring(0, width - s.length()) + s;
    }
}
