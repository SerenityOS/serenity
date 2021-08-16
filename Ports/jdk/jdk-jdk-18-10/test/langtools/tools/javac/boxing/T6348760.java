/*
 * Copyright (c) 2005, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     6348760
 * @summary crash: java.lang.AssertionError at com.sun.tools.javac.comp.Lower.abstractLval(Lower.java:1853)
 * @author  Peter von der Ah\u00e9
 * @run main/othervm T6348760
 */

public class T6348760<T> {
    T value;
    static int n;

    T6348760(T value) {
        this.value = value;
    }

    static void testByte(T6348760<Byte> i, T6348760<Byte> j, T6348760<Boolean> a) {
        i.value++;
        i.value--;
        ++i.value;
        --i.value;
        n = +i.value;
        n = -i.value;
        n = ~i.value;
        n = i.value * j.value;
        n = i.value / j.value;
        n = i.value % j.value;
        n = i.value + j.value;
        n = i.value - j.value;
        n = i.value << j.value;
        n = i.value >> j.value;
        n = i.value >>> j.value;
        n = i.value & j.value;
        n = i.value ^ j.value;
        n = i.value | j.value;
        n *= j.value;
        n /= j.value;
        n %= j.value;
        n += j.value;
        n -= j.value;
        n <<= j.value;
        n >>= j.value;
        n >>>= j.value;
        n &= j.value;
        n ^= j.value;
        n |= j.value;
        a.value = i.value < j.value;
        a.value = i.value > j.value;
        a.value = i.value <= j.value;
        a.value = i.value >= j.value;
        a.value = i.value == j.value;
        a.value = i.value != j.value;
    }

    static void testShort(T6348760<Short> i, T6348760<Short> j, T6348760<Boolean> a) {
        i.value++;
        i.value--;
        ++i.value;
        --i.value;
        n = +i.value;
        n = -i.value;
        n = ~i.value;
        n = i.value * j.value;
        n = i.value / j.value;
        n = i.value % j.value;
        n = i.value + j.value;
        n = i.value - j.value;
        n = i.value << j.value;
        n = i.value >> j.value;
        n = i.value >>> j.value;
        n = i.value & j.value;
        n = i.value ^ j.value;
        n = i.value | j.value;
        n *= j.value;
        n /= j.value;
        n %= j.value;
        n += j.value;
        n -= j.value;
        n <<= j.value;
        n >>= j.value;
        n >>>= j.value;
        n &= j.value;
        n ^= j.value;
        n |= j.value;
        a.value = i.value < j.value;
        a.value = i.value > j.value;
        a.value = i.value <= j.value;
        a.value = i.value >= j.value;
        a.value = i.value == j.value;
        a.value = i.value != j.value;
    }

    static void testInteger(T6348760<Integer> i, T6348760<Integer> j, T6348760<Boolean> a) {
        i.value++;
        i.value--;
        ++i.value;
        --i.value;
        i.value = +i.value;
        i.value = -i.value;
        i.value = ~i.value;
        i.value = i.value * j.value;
        i.value = i.value / j.value;
        i.value = i.value % j.value;
        i.value = i.value + j.value;
        i.value = i.value - j.value;
        i.value = i.value << j.value;
        i.value = i.value >> j.value;
        i.value = i.value >>> j.value;
        i.value = i.value & j.value;
        i.value = i.value ^ j.value;
        i.value = i.value | j.value;
        i.value *= j.value;
        i.value /= j.value;
        i.value %= j.value;
        i.value += j.value;
        i.value -= j.value;
        i.value <<= j.value;
        i.value >>= j.value;
        i.value >>>= j.value;
        i.value &= j.value;
        i.value ^= j.value;
        i.value |= j.value;
        a.value = i.value < j.value;
        a.value = i.value > j.value;
        a.value = i.value <= j.value;
        a.value = i.value >= j.value;
        a.value = i.value == j.value;
        a.value = i.value != j.value;
    }

    static void testLong(T6348760<Long> i, T6348760<Long> j, T6348760<Boolean> a) {
        i.value++;
        i.value--;
        ++i.value;
        --i.value;
        i.value = +i.value;
        i.value = -i.value;
        i.value = ~i.value;
        i.value = i.value * j.value;
        i.value = i.value / j.value;
        i.value = i.value % j.value;
        i.value = i.value + j.value;
        i.value = i.value - j.value;
        i.value = i.value << j.value;
        i.value = i.value >> j.value;
        i.value = i.value >>> j.value;
        i.value = i.value & j.value;
        i.value = i.value ^ j.value;
        i.value = i.value | j.value;
        i.value *= j.value;
        i.value /= j.value;
        i.value %= j.value;
        i.value += j.value;
        i.value -= j.value;
        i.value <<= j.value;
        i.value >>= j.value;
        i.value >>>= j.value;
        i.value &= j.value;
        i.value ^= j.value;
        i.value |= j.value;
        a.value = i.value < j.value;
        a.value = i.value > j.value;
        a.value = i.value <= j.value;
        a.value = i.value >= j.value;
        a.value = i.value == j.value;
        a.value = i.value != j.value;
    }

    static void testCharacter(T6348760<Character> i, T6348760<Character> j, T6348760<Boolean> a) {
        i.value++;
        i.value--;
        ++i.value;
        --i.value;
        n = +i.value;
        n = -i.value;
        n = ~i.value;
        n = i.value * j.value;
        n = i.value / j.value;
        n = i.value % j.value;
        n = i.value + j.value;
        n = i.value - j.value;
        n = i.value << j.value;
        n = i.value >> j.value;
        n = i.value >>> j.value;
        n = i.value & j.value;
        n = i.value ^ j.value;
        n = i.value | j.value;
        n *= j.value;
        n /= j.value;
        n %= j.value;
        n += j.value;
        n -= j.value;
        n <<= j.value;
        n >>= j.value;
        n >>>= j.value;
        n &= j.value;
        n ^= j.value;
        n |= j.value;
        a.value = i.value < j.value;
        a.value = i.value > j.value;
        a.value = i.value <= j.value;
        a.value = i.value >= j.value;
        a.value = i.value == j.value;
        a.value = i.value != j.value;
    }

    static void testFloat(T6348760<Float> i, T6348760<Float> j, T6348760<Boolean> a) {
        i.value++;
        i.value--;
        ++i.value;
        --i.value;
        i.value = +i.value;
        i.value = -i.value;
        // i.value = ~i.value;
        i.value = i.value * j.value;
        i.value = i.value / j.value;
        i.value = i.value % j.value;
        i.value = i.value + j.value;
        i.value = i.value - j.value;
        // i.value = i.value << j.value;
        // i.value = i.value >> j.value;
        // i.value = i.value >>> j.value;
        // i.value = i.value & j.value;
        // i.value = i.value ^ j.value;
        // i.value = i.value | j.value;
        i.value *= j.value;
        i.value /= j.value;
        i.value %= j.value;
        i.value += j.value;
        i.value -= j.value;
        // i.value <<= j.value;
        // i.value >>= j.value;
        // i.value >>>= j.value;
        // i.value &= j.value;
        // i.value ^= j.value;
        // i.value |= j.value;
        a.value = i.value < j.value;
        a.value = i.value > j.value;
        a.value = i.value <= j.value;
        a.value = i.value >= j.value;
        a.value = i.value == j.value;
        a.value = i.value != j.value;
    }

    static void testDouble(T6348760<Double> i, T6348760<Double> j, T6348760<Boolean> a) {
        i.value++;
        i.value--;
        ++i.value;
        --i.value;
        i.value = +i.value;
        i.value = -i.value;
        // i.value = ~i.value;
        i.value = i.value * j.value;
        i.value = i.value / j.value;
        i.value = i.value % j.value;
        i.value = i.value + j.value;
        i.value = i.value - j.value;
        // i.value = i.value << j.value;
        // i.value = i.value >> j.value;
        // i.value = i.value >>> j.value;
        // i.value = i.value & j.value;
        // i.value = i.value ^ j.value;
        // i.value = i.value | j.value;
        i.value *= j.value;
        i.value /= j.value;
        i.value %= j.value;
        i.value += j.value;
        i.value -= j.value;
        // i.value <<= j.value;
        // i.value >>= j.value;
        // i.value >>>= j.value;
        // i.value &= j.value;
        // i.value ^= j.value;
        // i.value |= j.value;
        a.value = i.value < j.value;
        a.value = i.value > j.value;
        a.value = i.value <= j.value;
        a.value = i.value >= j.value;
        a.value = i.value == j.value;
        a.value = i.value != j.value;
    }

    static void testBoolean(T6348760<Boolean> i, T6348760<Boolean> j, T6348760<Boolean> a) {
        // i.value++;
        // i.value--;
        // ++i.value;
        // --i.value;
        // i.value = +i.value;
        // i.value = -i.value;
        // i.value = ~i.value;
        // i.value = i.value * j.value;
        // i.value = i.value / j.value;
        // i.value = i.value % j.value;
        // i.value = i.value + j.value;
        // i.value = i.value - j.value;
        // i.value = i.value << j.value;
        // i.value = i.value >> j.value;
        // i.value = i.value >>> j.value;
        i.value = i.value & j.value;
        i.value = i.value ^ j.value;
        i.value = i.value | j.value;
        // i.value *= j.value;
        // i.value /= j.value;
        // i.value %= j.value;
        // i.value += j.value;
        // i.value -= j.value;
        // i.value <<= j.value;
        // i.value >>= j.value;
        // i.value >>>= j.value;
        i.value &= j.value;
        i.value ^= j.value;
        i.value |= j.value;
        // a.value = i.value < j.value;
        // a.value = i.value > j.value;
        // a.value = i.value <= j.value;
        // a.value = i.value >= j.value;
        a.value = i.value == j.value;
        a.value = i.value != j.value;
        a.value = !j.value;
        a.value = i.value && j.value;
        a.value = i.value || j.value;
    }


    public static void main(String... args) {
        testByte     (new T6348760<Byte>((byte)42),
                      new T6348760<Byte>((byte)42),
                      new T6348760<Boolean>(true));
        testShort    (new T6348760<Short>((short)42),
                      new T6348760<Short>((short)42),
                      new T6348760<Boolean>(true));
        testInteger  (new T6348760<Integer>(42),
                      new T6348760<Integer>(42),
                      new T6348760<Boolean>(true));
        testLong     (new T6348760<Long>(42L),
                      new T6348760<Long>(42L),
                      new T6348760<Boolean>(true));
        testCharacter(new T6348760<Character>('*'),
                      new T6348760<Character>('*'),
                      new T6348760<Boolean>(true));
        testFloat    (new T6348760<Float>(42.0F),
                      new T6348760<Float>(42.0F),
                      new T6348760<Boolean>(true));
        testDouble   (new T6348760<Double>(42.0D),
                      new T6348760<Double>(42.0D),
                      new T6348760<Boolean>(true));
        testBoolean  (new T6348760<Boolean>(true),
                      new T6348760<Boolean>(true),
                      new T6348760<Boolean>(true));
    }
}
