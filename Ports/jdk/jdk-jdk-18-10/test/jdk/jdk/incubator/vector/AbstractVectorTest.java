/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.Integer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.IntFunction;
import java.util.function.IntUnaryOperator;
import java.util.stream.Stream;
import java.util.stream.Collectors;

import org.testng.Assert;

public class AbstractVectorTest {

    static final Random RAND = new Random(Integer.getInteger("jdk.incubator.vector.test.random-seed", 1337));

    interface ToBoolF {
        boolean apply(int i);
    }

    static boolean[] fill_boolean(int s , ToBoolF f) {
        return fill_boolean(new boolean[s], f);
    }

    static boolean[] fill_boolean(boolean[] a, ToBoolF f) {
        for (int i = 0; i < a.length; i++) {
            a[i] = f.apply(i);
        }
        return a;
    }

    static <R> IntFunction<R> withToString(String s, IntFunction<R> f) {
        return new IntFunction<R>() {
            @Override
            public R apply(int v) {
                return f.apply(v);
            }

            @Override
            public String toString() {
                return s;
            }
        };
    }

    static <R> BiFunction<Integer,Integer,R> withToStringBi(String s, BiFunction<Integer,Integer,R> f) {
        return new BiFunction<Integer,Integer,R>() {
            @Override
            public R apply(Integer v, Integer u) {
                return f.apply(v, u);
            }

            @Override
            public String toString() {
                return s;
            }
        };
    }

    static final List<IntFunction<boolean[]>> BOOL_ARRAY_GENERATORS = List.of(
            withToString("boolean[i % 2]", (int s) -> {
                return fill_boolean(s,
                            i -> ((i % 2) == 0));
            }),
            withToString("boolean[i % 5]", (int s) -> {
                return fill_boolean(s,
                            i -> ((i % 5) == 0));
            })
    );

    interface IntOp {
        int apply(int i);
    }

    static int[] fillInts(int s , IntOp f) {
        return fillInts(new int[s], f);
    }

    static int[] fillInts(int[] a, IntOp f) {
        for (int i = 0; i < a.length; i++) {
            a[i] = f.apply(i);
        }
        return a;
    }

    static final List<IntFunction<boolean[]>> BOOLEAN_MASK_GENERATORS = List.of(
            withToString("mask[i % 2]", (int l) -> {
                boolean[] a = new boolean[l];
                for (int i = 0; i < l; i++) {
                    a[i] = (i % 2 == 0);
                }
                return a;
            }),
            withToString("mask[true]", (int l) -> {
                boolean[] a = new boolean[l];
                Arrays.fill(a, true);
                return a;
            }),
            withToString("mask[false]", boolean[]::new)
    );

    static final List<List<IntFunction<boolean[]>>>
        BOOLEAN_MASK_COMPARE_GENERATOR_PAIRS =
            Stream.of(BOOLEAN_MASK_GENERATORS.get(0)).
                flatMap(fa -> BOOLEAN_MASK_GENERATORS.stream().skip(1).map(
                                      fb -> List.of(fa, fb))).collect(Collectors.toList());

    static final List<BiFunction<Integer,Integer,int[]>> INT_SHUFFLE_GENERATORS = List.of(
            withToStringBi("shuffle[random]",
                    (Integer l, Integer m) -> RAND.ints(l, 0, m).toArray())
    );

    interface RangeIntOp {
        int apply(int i, int min, int max);
    }

    static int[] fillRangeInts(int s, int min, int max, RangeIntOp f) {
        return fillRangeInts(new int[s], min, max, f);
    }

    static int[] fillRangeInts(int[] a, int min, int max, RangeIntOp f) {
        for (int i = 0; i < a.length; i++) {
            a[i] = f.apply(i, min, max);
        }
        return a;
    }

    static final List<List<BiFunction<Integer, Integer, int[]>>>
       INT_SHUFFLE_COMPARE_GENERATOR_PAIRS = List.of(
           List.of(
               withToStringBi("shuffle[i]", (Integer l, Integer m) -> {
                   return fillRangeInts(l, 0, m,  (i, _min, _max) -> (i % _max));
               }),
               withToStringBi("shuffle[random]", (Integer l, Integer m) -> {
                   return RAND.ints(l, 0, m).toArray();
               })
           ),
           List.of(
               withToStringBi("shuffle[i]", (Integer l, Integer m) -> {
                   return fillRangeInts(l, 0, m,  (i, _min, _max) -> (i % _max));
               }),
               withToStringBi("shuffle[random]", (Integer l, Integer m) -> {
                   return RAND.ints(l, 0, m).toArray();
               })
           )
    );

    static final List<BiFunction<Integer,Integer,int[]>> INT_INDEX_GENERATORS = List.of(
            withToStringBi("index[random]",
                    (Integer l, Integer m) -> RAND.ints(l, 0, m).toArray())
    );

    static boolean isIndexOutOfBoundsForMask(boolean[] mask, int offset, int length) {
        return isIndexOutOfBoundsForMask(mask, offset, length, 1);
    }

    static boolean isIndexOutOfBoundsForMask(boolean[] mask, int offset, int length, int eSize) {
        for (int i = 0; i < mask.length; i++) {
            int index = i * eSize + offset;
            if (mask[i]) {
                if (index < 0 || index > length - eSize) {
                    return true;
                }
            }
        }
        return false;
    }

    static boolean isIndexOutOfBounds(int size, int offset, int length) {
        int upperBound = offset + size;
        return upperBound < size || upperBound > length;
    }

    public static int[] expectedShuffle(int length, IntUnaryOperator fn) {
        int [] a = new int[length];
        for (int i = 0; i < length; i++) {
            int elem = fn.applyAsInt(i);
            int wrapElem = Math.floorMod(elem, length);
            if (elem != wrapElem) {
                elem = wrapElem - length;
            }
            a[i] = elem;
        }
        return a;
    }

    interface FBooleanBinOp {
        boolean apply(boolean a, boolean b);
    }

    static void assertArraysEquals(boolean[] r, boolean[] a, boolean[] b, FBooleanBinOp f) {
        int i = 0;
        try {
            for (; i < a.length; i++) {
                Assert.assertEquals(r[i], f.apply(a[i], b[i]));
            }
        } catch (AssertionError e) {
            Assert.assertEquals(r[i], f.apply(a[i], b[i]), "(" + a[i] + ", " + b[i] + ") at index #" + i);
        }
    }
}
