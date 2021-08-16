/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8028267
 * @summary Unit tests for the com.sun.tools.javac.util.Bits class.
 * @modules jdk.compiler/com.sun.tools.javac.util
 * @run main BitsTest
 */

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.sun.tools.javac.util.Bits;

public class BitsTest {

    final static int[] samples = { 0, 1, 7, 16, 19, 31, 32, 33, 63, 64 };
    final static int LENGTH = samples[samples.length - 1] + 50;

    public static void main(String... args) throws Exception {

        testIncl();
        testInclRange();
        testDup();
        testClear();
        testExcl();
        testExcludeFrom();
        testBinOps();
        testNextBit();

    }


    // Test Bits.incl
    static void testIncl() {
        for (int a : samples) {
            for (int b : samples) {
                Bits bits = new Bits();
                bits.incl(a);
                if (a != b)
                    bits.incl(b);
                for (int i = 0; i < LENGTH; i++)
                    assert bits.isMember(i) == (i == a || i == b);
            }
        }
    }


    // Test Bits.excl
    static void testExcl() {
        for (int a : samples) {
            for (int b : samples) {
                Bits bits = new Bits();
                bits.inclRange(0, LENGTH);
                bits.excl(a);
                if (a != b)
                    bits.excl(b);
                for (int i = 0; i < LENGTH; i++)
                    assert !bits.isMember(i) == (i == a || i == b);
            }
        }
    }


    // Test Bits.inclRange with various ranges.
    static void testInclRange() {
        for (int i = 0; i < samples.length; i++) {
            for (int j = i; j < samples.length; j++)
                testInclRangeHelper(samples[i], samples[j]);
        }
    }


    // Tests Bits.inclRange for the given range.
    static void testInclRangeHelper(int from, int to) {
        Bits bits = new Bits();
        bits.inclRange(from, to);
        for (int i = 0; i < LENGTH; i++)
            assert bits.isMember(i) == (from <= i && i < to);
    }


    // Test Bits.dup
    static void testDup() {
        Bits bits = sampleBits();
        Bits dupBits = bits.dup();
        assertEquals(LENGTH, bits, dupBits);
    }


    // Make sure Bits.clear clears all bits.
    static void testClear() {
        Bits bits = sampleBits();
        bits.clear();
        for (int i = 0; i < LENGTH; i++)
            assert !bits.isMember(i);
    }


    // Test Bits.excludeFrom
    static void testExcludeFrom() {
        Bits bits = sampleBits();

        int half = samples.length / 2;
        Set<Integer> expected = new HashSet<Integer>();
        for (int i : Arrays.copyOf(samples, half))
            expected.add(i);

        bits.excludeFrom(samples[half]);

        for (int i = 0; i < LENGTH; i++)
            assert bits.isMember(i) == expected.contains(i);
    }


    // Test andSet, orSet, diffSet, xorSet
    static void testBinOps() {
        int[] a = { 1630757163, -592623705 };
        int[] b = { 1062404889, 1969380693 };

        int[] or   = { a[0] | b[0],  a[1] | b[1] };
        int[] and  = { a[0] & b[0],  a[1] & b[1] };
        int[] xor  = { a[0] ^ b[0],  a[1] ^ b[1] };
        int[] diff = { a[0] & ~b[0], a[1] & ~b[1] };

        assertEquals(64, fromInts(a).orSet  (fromInts(b)), fromInts(or));
        assertEquals(64, fromInts(a).andSet (fromInts(b)), fromInts(and));
        assertEquals(64, fromInts(a).xorSet (fromInts(b)), fromInts(xor));
        assertEquals(64, fromInts(a).diffSet(fromInts(b)), fromInts(diff));

    }


    // Create a Bits-instance based on bits in 'ints' argument.
    static Bits fromInts(int[] ints) {
        Bits bits = new Bits();
        for (int bit = 0; bit < ints.length * 32; bit++)
            if ((ints[bit / 32] & (1 << (bit % 32))) != 0)
                bits.incl(bit);
        return bits;
    }


    // Asserts that two Bits-instances are equal up to first 'len' bits.
    static void assertEquals(int len, Bits a, Bits b) {
        for (int i = 0; i < len; i++)
            assert a.isMember(i) == b.isMember(i);
    }


    // Test Bits.nextBit
    static void testNextBit() {
        Bits bits = sampleBits();

        int index = 0;
        for (int bit = 0; bit < LENGTH; bit++) {

            int expected;

            // Passed last sample index?
            if (index < samples.length) {
                expected = samples[index];
                if (bit == samples[index])
                    index++;
            } else {
                expected = -1;
            }

            assert bits.nextBit(bit) == expected;
        }
    }


    // Convenience method: Generate a Bits-instance based on samples.
    static Bits sampleBits() {
        Bits bits = new Bits();
        for (int i : samples)
            bits.incl(i);
        return bits;
    }

}
