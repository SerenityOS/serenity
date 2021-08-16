/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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
import jdk.incubator.vector.*;
import jdk.internal.vm.annotation.ForceInline;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.testng.annotations.DataProvider;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.List;
import java.util.function.IntFunction;
import jdk.incubator.vector.VectorShape;
import jdk.incubator.vector.VectorSpecies;

/**
 * @test
 * @modules jdk.incubator.vector
 * @modules java.base/jdk.internal.vm.annotation
 * @run testng/othervm --add-opens jdk.incubator.vector/jdk.incubator.vector=ALL-UNNAMED
 *      -XX:-TieredCompilation VectorReshapeTests
 */

@Test
public class VectorReshapeTests {
    static final int INVOC_COUNT = Integer.getInteger("jdk.incubator.vector.test.loop-iterations", 100);
    static final int NUM_ITER = 200 * INVOC_COUNT;

    static final VectorShape S_Max_BIT = getMaxBit();

    static final VectorSpecies<Integer> ispec64 = IntVector.SPECIES_64;
    static final VectorSpecies<Float> fspec64 = FloatVector.SPECIES_64;
    static final VectorSpecies<Long> lspec64 = LongVector.SPECIES_64;
    static final VectorSpecies<Double> dspec64 = DoubleVector.SPECIES_64;
    static final VectorSpecies<Byte> bspec64 = ByteVector.SPECIES_64;
    static final VectorSpecies<Short> sspec64 = ShortVector.SPECIES_64;

    static final VectorSpecies<Integer> ispec128 = IntVector.SPECIES_128;
    static final VectorSpecies<Float> fspec128 = FloatVector.SPECIES_128;
    static final VectorSpecies<Long> lspec128 = LongVector.SPECIES_128;
    static final VectorSpecies<Double> dspec128 = DoubleVector.SPECIES_128;
    static final VectorSpecies<Byte> bspec128 = ByteVector.SPECIES_128;
    static final VectorSpecies<Short> sspec128 = ShortVector.SPECIES_128;

    static final VectorSpecies<Integer> ispec256 = IntVector.SPECIES_256;
    static final VectorSpecies<Float> fspec256 = FloatVector.SPECIES_256;
    static final VectorSpecies<Long> lspec256 = LongVector.SPECIES_256;
    static final VectorSpecies<Double> dspec256 = DoubleVector.SPECIES_256;
    static final VectorSpecies<Byte> bspec256 = ByteVector.SPECIES_256;
    static final VectorSpecies<Short> sspec256 = ShortVector.SPECIES_256;

    static final VectorSpecies<Integer> ispec512 = IntVector.SPECIES_512;
    static final VectorSpecies<Float> fspec512 = FloatVector.SPECIES_512;
    static final VectorSpecies<Long> lspec512 = LongVector.SPECIES_512;
    static final VectorSpecies<Double> dspec512 = DoubleVector.SPECIES_512;
    static final VectorSpecies<Byte> bspec512 = ByteVector.SPECIES_512;
    static final VectorSpecies<Short> sspec512 = ShortVector.SPECIES_512;

    static final VectorSpecies<Integer> ispecMax = IntVector.SPECIES_MAX;
    static final VectorSpecies<Float> fspecMax = FloatVector.SPECIES_MAX;
    static final VectorSpecies<Long> lspecMax = LongVector.SPECIES_MAX;
    static final VectorSpecies<Double> dspecMax = DoubleVector.SPECIES_MAX;
    static final VectorSpecies<Byte> bspecMax = ByteVector.SPECIES_MAX;
    static final VectorSpecies<Short> sspecMax = ShortVector.SPECIES_MAX;

    static VectorShape getMaxBit() {
        return VectorShape.S_Max_BIT;
    }

    static <T> IntFunction<T> withToString(String s, IntFunction<T> f) {
        return new IntFunction<T>() {
            @Override
            public T apply(int v) {
                return f.apply(v);
            }

            @Override
            public String toString() {
                return s;
            }
        };
    }

    interface ToByteF {
        byte apply(int i);
    }

    static byte[] fill_byte(int s , ToByteF f) {
        return fill_byte(new byte[s], f);
    }

    static byte[] fill_byte(byte[] a, ToByteF f) {
        for (int i = 0; i < a.length; i++) {
            a[i] = f.apply(i);
        }
        return a;
    }

    interface ToBoolF {
        boolean apply(int i);
    }

    static boolean[] fill_bool(int s , ToBoolF f) {
        return fill_bool(new boolean[s], f);
    }

    static boolean[] fill_bool(boolean[] a, ToBoolF f) {
        for (int i = 0; i < a.length; i++) {
            a[i] = f.apply(i);
        }
        return a;
    }

    interface ToShortF {
        short apply(int i);
    }

    static short[] fill_short(int s , ToShortF f) {
        return fill_short(new short[s], f);
    }

    static short[] fill_short(short[] a, ToShortF f) {
        for (int i = 0; i < a.length; i++) {
            a[i] = f.apply(i);
        }
        return a;
    }

    interface ToIntF {
        int apply(int i);
    }

    static int[] fill_int(int s , ToIntF f) {
        return fill_int(new int[s], f);
    }

    static int[] fill_int(int[] a, ToIntF f) {
        for (int i = 0; i < a.length; i++) {
            a[i] = f.apply(i);
        }
        return a;
    }

    interface ToLongF {
        long apply(int i);
    }

    static long[] fill_long(int s , ToLongF f) {
        return fill_long(new long[s], f);
    }

    static long[] fill_long(long[] a, ToLongF f) {
        for (int i = 0; i < a.length; i++) {
            a[i] = f.apply(i);
        }
        return a;
    }

    interface ToFloatF {
        float apply(int i);
    }

    static float[] fill_float(int s , ToFloatF f) {
        return fill_float(new float[s], f);
    }

    static float[] fill_float(float[] a, ToFloatF f) {
        for (int i = 0; i < a.length; i++) {
            a[i] = f.apply(i);
        }
        return a;
    }

    interface ToDoubleF {
        double apply(int i);
    }

    static double[] fill_double(int s , ToDoubleF f) {
        return fill_double(new double[s], f);
    }

    static double[] fill_double(double[] a, ToDoubleF f) {
        for (int i = 0; i < a.length; i++) {
            a[i] = f.apply(i);
        }
        return a;
    }

    static final List<IntFunction<byte[]>> BYTE_GENERATORS = List.of(
            withToString("byte(i)", (int s) -> {
                return fill_byte(s, i -> (byte)(i+1));
            })
    );

    @DataProvider
    public Object[][] byteUnaryOpProvider() {
        return BYTE_GENERATORS.stream().
                map(f -> new Object[]{f}).
                toArray(Object[][]::new);
    }

    static final List<IntFunction<boolean[]>> BOOL_GENERATORS = List.of(
        withToString("boolean(i%3)", (int s) -> {
            return fill_bool(s, i -> i % 3 == 0);
        })
    );

    @DataProvider
    public Object[][] booleanUnaryOpProvider() {
        return BOOL_GENERATORS.stream().
                map(f -> new Object[]{f}).
                toArray(Object[][]::new);
    }

    static final List<IntFunction<short[]>> SHORT_GENERATORS = List.of(
            withToString("short(i)", (int s) -> {
                return fill_short(s, i -> (short)(i*100+1));
            })
    );

    @DataProvider
    public Object[][] shortUnaryOpProvider() {
        return SHORT_GENERATORS.stream().
                map(f -> new Object[]{f}).
                toArray(Object[][]::new);
    }

    static final List<IntFunction<int[]>> INT_GENERATORS = List.of(
            withToString("int(i)", (int s) -> {
                return fill_int(s, i -> (int)(i^((i&1)-1)));
            })
    );

    @DataProvider
    public Object[][] intUnaryOpProvider() {
        return INT_GENERATORS.stream().
                map(f -> new Object[]{f}).
                toArray(Object[][]::new);
    }

    static final List<IntFunction<long[]>> LONG_GENERATORS = List.of(
            withToString("long(i)", (int s) -> {
                return fill_long(s, i -> (long)(i^((i&1)-1)));
            })
    );

    @DataProvider
    public Object[][] longUnaryOpProvider() {
        return LONG_GENERATORS.stream().
                map(f -> new Object[]{f}).
                toArray(Object[][]::new);
    }

    static final List<IntFunction<float[]>> FLOAT_GENERATORS = List.of(
            withToString("float(i)", (int s) -> {
                return fill_float(s, i -> (float)(i * 10 + 0.1));
            })
    );

    @DataProvider
    public Object[][] floatUnaryOpProvider() {
        return FLOAT_GENERATORS.stream().
                map(f -> new Object[]{f}).
                toArray(Object[][]::new);
    }

    static final List<IntFunction<double[]>> DOUBLE_GENERATORS = List.of(
            withToString("double(i)", (int s) -> {
                return fill_double(s, i -> (double)(i * 10 + 0.1));
            })
    );

    @DataProvider
    public Object[][] doubleUnaryOpProvider() {
        return DOUBLE_GENERATORS.stream().
                map(f -> new Object[]{f}).
                toArray(Object[][]::new);
    }

    static int partLimit(VectorSpecies<?> a, VectorSpecies<?> b, boolean lanewise) {
        int partLimit = a.partLimit(b, lanewise);
        // Check it:
        int parts = (partLimit >= 0 ? Math.max(1, partLimit) : -partLimit);
        int asize = a.vectorByteSize(), bsize = b.vectorByteSize();
        if (lanewise) {
            asize *= b.elementSize();
            asize /= a.elementSize();
        }
        int larger = Math.max(asize, bsize);
        int smaller = Math.min(asize, bsize);
        assert(parts == larger / smaller) : Arrays.asList(partLimit, parts, a+":"+asize, b+":"+bsize);
        if (asize > bsize) assert(partLimit > 0);
        else if (asize < bsize) assert(partLimit < 0);
        else  assert(partLimit == 0);
        return partLimit;
    }

    @ForceInline
    static <E>
    void testVectorReshape(VectorSpecies<E> a, VectorSpecies<E> b, byte[] input, byte[] output) {
        testVectorReshape(a, b, input, output, false);
        testVectorReshapeLanewise(a, b, input, output);
    }
    @ForceInline
    static <E>
    void testVectorReshapeLanewise(VectorSpecies<E> a, VectorSpecies<E> b, byte[] input, byte[] output) {
        testVectorReshape(a, b, input, output, true);
    }
    @ForceInline
    static <E>
    void testVectorReshape(VectorSpecies<E> a, VectorSpecies<E> b, byte[] input, byte[] output, boolean lanewise) {
        Class<?> atype = a.elementType(), btype = b.elementType();
        Vector<E> av = a.fromByteArray(input, 0, ByteOrder.nativeOrder());
        int partLimit = partLimit(a, b, lanewise);
        int block = Math.min(a.vectorByteSize(), b.vectorByteSize());
        if (false)
            System.out.println("testing "+a+"->"+b+
                               (lanewise?" (lanewise)":" (reinterpret)")+
                               ", partLimit=" + partLimit +
                               ", block=" + block);
        byte[] expected;
        int origin;
        if (partLimit > 0) {
            for (int part = 0; part < partLimit; part++) {
                Vector<E> bv = (lanewise
                                ? av.castShape(b, part)
                                : av.reinterpretShape(b, part));
                bv.intoByteArray(output, 0, ByteOrder.nativeOrder());
                // expansion: slice some of the input
                origin = part * block;
                expected = Arrays.copyOfRange(input, origin, origin + block);
                if (lanewise) {
                    expected = castByteArrayData(expected, atype, btype);
                }
                checkPartialResult(a, b, input, output, expected,
                                   lanewise, part, origin);
            }
        } else if (partLimit < 0) {
            for (int part = 0; part > partLimit; part--) {
                Vector<E> bv = (lanewise
                                ? av.castShape(b, part)
                                : av.reinterpretShape(b, part));
                bv.intoByteArray(output, 0, ByteOrder.nativeOrder());
                // contraction: unslice the input into part of the output
                byte[] logical = input;
                if (lanewise) {
                    logical = castByteArrayData(input, atype, btype);
                }
                assert(logical.length == block);
                expected = new byte[output.length];
                origin = -part * block;
                System.arraycopy(logical, 0, expected, origin, block);
                checkPartialResult(a, b, input, output, expected,
                                   lanewise, part, origin);
            }
        } else {
            int part = 0;
            Vector<E> bv = (lanewise
                            ? av.castShape(b, part)
                            : av.reinterpretShape(b, part));
            bv.intoByteArray(output, 0, ByteOrder.nativeOrder());
            // in-place copy, no resize
            expected = input;
            origin = 0;
            if (lanewise) {
                expected = castByteArrayData(expected, atype, btype);
            }
            checkPartialResult(a, b, input, output, expected,
                               lanewise, part, origin);
        }
    }

    static
    void checkPartialResult(VectorSpecies<?> a, VectorSpecies<?> b,
                            byte[] input, byte[] output, byte[] expected,
                            boolean lanewise, int part, int origin) {
        if (Arrays.equals(expected, output)) {
            return;
        }
        int partLimit = partLimit(a, b, lanewise);
        int block;
        if (!lanewise)
            block = Math.min(a.vectorByteSize(), b.vectorByteSize());
        else if (partLimit >= 0)
            block = a.vectorByteSize() / Math.max(1, partLimit);
        else
            block = b.vectorByteSize() / -partLimit;
        System.out.println("input:  "+Arrays.toString(input));
        System.out.println("Failing with "+a+"->"+b+
                           (lanewise?" (lanewise)":" (reinterpret)")+
                           ", partLimit=" + partLimit +
                           ", block=" + block +
                           ", part=" + part +
                           ", origin=" + origin);
        System.out.println("expect: "+Arrays.toString(expected));
        System.out.println("output: "+Arrays.toString(output));
        Assert.assertEquals(output, expected);
    }

    @Test(dataProvider = "byteUnaryOpProvider")
    static void testReshapeByte(IntFunction<byte[]> fa) {
        byte[] bin64 = fa.apply(64/Byte.SIZE);
        byte[] bin128 = fa.apply(128/Byte.SIZE);
        byte[] bin256 = fa.apply(256/Byte.SIZE);
        byte[] bin512 = fa.apply(512/Byte.SIZE);
        byte[] binMax = fa.apply(S_Max_BIT.vectorBitSize()/Byte.SIZE);
        byte[] bout64 = new byte[bin64.length];
        byte[] bout128 = new byte[bin128.length];
        byte[] bout256 = new byte[bin256.length];
        byte[] bout512 = new byte[bin512.length];
        byte[] boutMax = new byte[binMax.length];

        for (int i = 0; i < NUM_ITER; i++) {
            testVectorReshape(bspec64, bspec64, bin64, bout64);
            testVectorReshape(bspec64, bspec128, bin64, bout128);
            testVectorReshape(bspec64, bspec256, bin64, bout256);
            testVectorReshape(bspec64, bspec512, bin64, bout512);
            testVectorReshape(bspec64, bspecMax, bin64, boutMax);

            testVectorReshape(bspec128, bspec64, bin128, bout64);
            testVectorReshape(bspec128, bspec128, bin128, bout128);
            testVectorReshape(bspec128, bspec256, bin128, bout256);
            testVectorReshape(bspec128, bspec512, bin128, bout512);
            testVectorReshape(bspec128, bspecMax, bin128, boutMax);

            testVectorReshape(bspec256, bspec64, bin256, bout64);
            testVectorReshape(bspec256, bspec128, bin256, bout128);
            testVectorReshape(bspec256, bspec256, bin256, bout256);
            testVectorReshape(bspec256, bspec512, bin256, bout512);
            testVectorReshape(bspec256, bspecMax, bin256, boutMax);

            testVectorReshape(bspec512, bspec64, bin512, bout64);
            testVectorReshape(bspec512, bspec128, bin512, bout128);
            testVectorReshape(bspec512, bspec256, bin512, bout256);
            testVectorReshape(bspec512, bspec512, bin512, bout512);
            testVectorReshape(bspec512, bspecMax, bin512, boutMax);

            testVectorReshape(bspecMax, bspec64, binMax, bout64);
            testVectorReshape(bspecMax, bspec128, binMax, bout128);
            testVectorReshape(bspecMax, bspec256, binMax, bout256);
            testVectorReshape(bspecMax, bspec512, binMax, bout512);
            testVectorReshape(bspecMax, bspecMax, binMax, boutMax);
        }
    }

    @Test(dataProvider = "byteUnaryOpProvider")
    static void testReshapeShort(IntFunction<byte[]> fa) {
        byte[] bin64 = fa.apply(64/Byte.SIZE);
        byte[] bin128 = fa.apply(128/Byte.SIZE);
        byte[] bin256 = fa.apply(256/Byte.SIZE);
        byte[] bin512 = fa.apply(512/Byte.SIZE);
        byte[] binMax = fa.apply(S_Max_BIT.vectorBitSize()/Byte.SIZE);
        byte[] bout64 = new byte[bin64.length];
        byte[] bout128 = new byte[bin128.length];
        byte[] bout256 = new byte[bin256.length];
        byte[] bout512 = new byte[bin512.length];
        byte[] boutMax = new byte[binMax.length];

        for (int i = 0; i < NUM_ITER; i++) {
            testVectorReshape(sspec64, sspec64, bin64, bout64);
            testVectorReshape(sspec64, sspec128, bin64, bout128);
            testVectorReshape(sspec64, sspec256, bin64, bout256);
            testVectorReshape(sspec64, sspec512, bin64, bout512);
            testVectorReshape(sspec64, sspecMax, bin64, boutMax);

            testVectorReshape(sspec128, sspec64, bin128, bout64);
            testVectorReshape(sspec128, sspec128, bin128, bout128);
            testVectorReshape(sspec128, sspec256, bin128, bout256);
            testVectorReshape(sspec128, sspec512, bin128, bout512);
            testVectorReshape(sspec128, sspecMax, bin128, boutMax);

            testVectorReshape(sspec256, sspec64, bin256, bout64);
            testVectorReshape(sspec256, sspec128, bin256, bout128);
            testVectorReshape(sspec256, sspec256, bin256, bout256);
            testVectorReshape(sspec256, sspec512, bin256, bout512);
            testVectorReshape(sspec256, sspecMax, bin256, boutMax);

            testVectorReshape(sspec512, sspec64, bin512, bout64);
            testVectorReshape(sspec512, sspec128, bin512, bout128);
            testVectorReshape(sspec512, sspec256, bin512, bout256);
            testVectorReshape(sspec512, sspec512, bin512, bout512);
            testVectorReshape(sspec512, sspecMax, bin512, boutMax);

            testVectorReshape(sspecMax, sspec64, binMax, bout64);
            testVectorReshape(sspecMax, sspec128, binMax, bout128);
            testVectorReshape(sspecMax, sspec256, binMax, bout256);
            testVectorReshape(sspecMax, sspec512, binMax, bout512);
            testVectorReshape(sspecMax, sspecMax, binMax, boutMax);
        }
    }

    @Test(dataProvider = "byteUnaryOpProvider")
    static void testReshapeInt(IntFunction<byte[]> fa) {
        byte[] bin64 = fa.apply(64/Byte.SIZE);
        byte[] bin128 = fa.apply(128/Byte.SIZE);
        byte[] bin256 = fa.apply(256/Byte.SIZE);
        byte[] bin512 = fa.apply(512/Byte.SIZE);
        byte[] binMax = fa.apply(S_Max_BIT.vectorBitSize()/Byte.SIZE);
        byte[] bout64 = new byte[bin64.length];
        byte[] bout128 = new byte[bin128.length];
        byte[] bout256 = new byte[bin256.length];
        byte[] bout512 = new byte[bin512.length];
        byte[] boutMax = new byte[binMax.length];

        for (int i = 0; i < NUM_ITER; i++) {
            testVectorReshape(ispec64, ispec64, bin64, bout64);
            testVectorReshape(ispec64, ispec128, bin64, bout128);
            testVectorReshape(ispec64, ispec256, bin64, bout256);
            testVectorReshape(ispec64, ispec512, bin64, bout512);
            testVectorReshape(ispec64, ispecMax, bin64, boutMax);

            testVectorReshape(ispec128, ispec64, bin128, bout64);
            testVectorReshape(ispec128, ispec128, bin128, bout128);
            testVectorReshape(ispec128, ispec256, bin128, bout256);
            testVectorReshape(ispec128, ispec512, bin128, bout512);
            testVectorReshape(ispec128, ispecMax, bin128, boutMax);

            testVectorReshape(ispec256, ispec64, bin256, bout64);
            testVectorReshape(ispec256, ispec128, bin256, bout128);
            testVectorReshape(ispec256, ispec256, bin256, bout256);
            testVectorReshape(ispec256, ispec512, bin256, bout512);
            testVectorReshape(ispec256, ispecMax, bin256, boutMax);

            testVectorReshape(ispec512, ispec64, bin512, bout64);
            testVectorReshape(ispec512, ispec128, bin512, bout128);
            testVectorReshape(ispec512, ispec256, bin512, bout256);
            testVectorReshape(ispec512, ispec512, bin512, bout512);
            testVectorReshape(ispec512, ispecMax, bin512, boutMax);

            testVectorReshape(ispecMax, ispec64, binMax, bout64);
            testVectorReshape(ispecMax, ispec128, binMax, bout128);
            testVectorReshape(ispecMax, ispec256, binMax, bout256);
            testVectorReshape(ispecMax, ispec512, binMax, bout512);
            testVectorReshape(ispecMax, ispecMax, binMax, boutMax);
        }
    }

    @Test(dataProvider = "byteUnaryOpProvider")
    static void testReshapeLong(IntFunction<byte[]> fa) {
        byte[] bin64 = fa.apply(64/Byte.SIZE);
        byte[] bin128 = fa.apply(128/Byte.SIZE);
        byte[] bin256 = fa.apply(256/Byte.SIZE);
        byte[] bin512 = fa.apply(512/Byte.SIZE);
        byte[] binMax = fa.apply(S_Max_BIT.vectorBitSize()/Byte.SIZE);
        byte[] bout64 = new byte[bin64.length];
        byte[] bout128 = new byte[bin128.length];
        byte[] bout256 = new byte[bin256.length];
        byte[] bout512 = new byte[bin512.length];
        byte[] boutMax = new byte[binMax.length];

        for (int i = 0; i < NUM_ITER; i++) {
            testVectorReshape(lspec64, lspec64, bin64, bout64);
            testVectorReshape(lspec64, lspec128, bin64, bout128);
            testVectorReshape(lspec64, lspec256, bin64, bout256);
            testVectorReshape(lspec64, lspec512, bin64, bout512);
            testVectorReshape(lspec64, lspecMax, bin64, boutMax);

            testVectorReshape(lspec128, lspec64, bin128, bout64);
            testVectorReshape(lspec128, lspec128, bin128, bout128);
            testVectorReshape(lspec128, lspec256, bin128, bout256);
            testVectorReshape(lspec128, lspec512, bin128, bout512);
            testVectorReshape(lspec128, lspecMax, bin128, boutMax);

            testVectorReshape(lspec256, lspec64, bin256, bout64);
            testVectorReshape(lspec256, lspec128, bin256, bout128);
            testVectorReshape(lspec256, lspec256, bin256, bout256);
            testVectorReshape(lspec256, lspec512, bin256, bout512);
            testVectorReshape(lspec256, lspecMax, bin256, boutMax);

            testVectorReshape(lspec512, lspec64, bin512, bout64);
            testVectorReshape(lspec512, lspec128, bin512, bout128);
            testVectorReshape(lspec512, lspec256, bin512, bout256);
            testVectorReshape(lspec512, lspec512, bin512, bout512);
            testVectorReshape(lspec512, lspecMax, bin512, boutMax);

            testVectorReshape(lspecMax, lspec64, binMax, bout64);
            testVectorReshape(lspecMax, lspec128, binMax, bout128);
            testVectorReshape(lspecMax, lspec256, binMax, bout256);
            testVectorReshape(lspecMax, lspec512, binMax, bout512);
            testVectorReshape(lspecMax, lspecMax, binMax, boutMax);
        }
    }

    @Test(dataProvider = "byteUnaryOpProvider")
    static void testReshapeFloat(IntFunction<byte[]> fa) {
        byte[] bin64 = fa.apply(64/Byte.SIZE);
        byte[] bin128 = fa.apply(128/Byte.SIZE);
        byte[] bin256 = fa.apply(256/Byte.SIZE);
        byte[] bin512 = fa.apply(512/Byte.SIZE);
        byte[] binMax = fa.apply(S_Max_BIT.vectorBitSize()/Byte.SIZE);
        byte[] bout64 = new byte[bin64.length];
        byte[] bout128 = new byte[bin128.length];
        byte[] bout256 = new byte[bin256.length];
        byte[] bout512 = new byte[bin512.length];
        byte[] boutMax = new byte[binMax.length];

        for (int i = 0; i < NUM_ITER; i++) {
            testVectorReshape(fspec64, fspec64, bin64, bout64);
            testVectorReshape(fspec64, fspec128, bin64, bout128);
            testVectorReshape(fspec64, fspec256, bin64, bout256);
            testVectorReshape(fspec64, fspec512, bin64, bout512);
            testVectorReshape(fspec64, fspecMax, bin64, boutMax);

            testVectorReshape(fspec128, fspec64, bin128, bout64);
            testVectorReshape(fspec128, fspec128, bin128, bout128);
            testVectorReshape(fspec128, fspec256, bin128, bout256);
            testVectorReshape(fspec128, fspec512, bin128, bout512);
            testVectorReshape(fspec128, fspecMax, bin128, boutMax);

            testVectorReshape(fspec256, fspec64, bin256, bout64);
            testVectorReshape(fspec256, fspec128, bin256, bout128);
            testVectorReshape(fspec256, fspec256, bin256, bout256);
            testVectorReshape(fspec256, fspec512, bin256, bout512);
            testVectorReshape(fspec256, fspecMax, bin256, boutMax);

            testVectorReshape(fspec512, fspec64, bin512, bout64);
            testVectorReshape(fspec512, fspec128, bin512, bout128);
            testVectorReshape(fspec512, fspec256, bin512, bout256);
            testVectorReshape(fspec512, fspec512, bin512, bout512);
            testVectorReshape(fspec512, fspecMax, bin512, boutMax);

            testVectorReshape(fspecMax, fspec64, binMax, bout64);
            testVectorReshape(fspecMax, fspec128, binMax, bout128);
            testVectorReshape(fspecMax, fspec256, binMax, bout256);
            testVectorReshape(fspecMax, fspec512, binMax, bout512);
            testVectorReshape(fspecMax, fspecMax, binMax, boutMax);
        }
    }

    @Test(dataProvider = "byteUnaryOpProvider")
    static void testReshapeDouble(IntFunction<byte[]> fa) {
        byte[] bin64 = fa.apply(64/Byte.SIZE);
        byte[] bin128 = fa.apply(128/Byte.SIZE);
        byte[] bin256 = fa.apply(256/Byte.SIZE);
        byte[] bin512 = fa.apply(512/Byte.SIZE);
        byte[] binMax = fa.apply(S_Max_BIT.vectorBitSize()/Byte.SIZE);
        byte[] bout64 = new byte[bin64.length];
        byte[] bout128 = new byte[bin128.length];
        byte[] bout256 = new byte[bin256.length];
        byte[] bout512 = new byte[bin512.length];
        byte[] boutMax = new byte[binMax.length];

        for (int i = 0; i < NUM_ITER; i++) {
            testVectorReshape(dspec64, dspec64, bin64, bout64);
            testVectorReshape(dspec64, dspec128, bin64, bout128);
            testVectorReshape(dspec64, dspec256, bin64, bout256);
            testVectorReshape(dspec64, dspec512, bin64, bout512);
            testVectorReshape(dspec64, dspecMax, bin64, boutMax);

            testVectorReshape(dspec128, dspec64, bin128, bout64);
            testVectorReshape(dspec128, dspec128, bin128, bout128);
            testVectorReshape(dspec128, dspec256, bin128, bout256);
            testVectorReshape(dspec128, dspec512, bin128, bout512);
            testVectorReshape(dspec128, dspecMax, bin128, boutMax);

            testVectorReshape(dspec256, dspec64, bin256, bout64);
            testVectorReshape(dspec256, dspec128, bin256, bout128);
            testVectorReshape(dspec256, dspec256, bin256, bout256);
            testVectorReshape(dspec256, dspec512, bin256, bout512);
            testVectorReshape(dspec256, dspecMax, bin256, boutMax);

            testVectorReshape(dspec512, dspec64, bin512, bout64);
            testVectorReshape(dspec512, dspec128, bin512, bout128);
            testVectorReshape(dspec512, dspec256, bin512, bout256);
            testVectorReshape(dspec512, dspec512, bin512, bout512);
            testVectorReshape(dspec512, dspecMax, bin512, boutMax);

            testVectorReshape(dspecMax, dspec64, binMax, bout64);
            testVectorReshape(dspecMax, dspec128, binMax, bout128);
            testVectorReshape(dspecMax, dspec256, binMax, bout256);
            testVectorReshape(dspecMax, dspec512, binMax, bout512);
            testVectorReshape(dspecMax, dspecMax, binMax, boutMax);
        }
    }
    @ForceInline
    static <E,F>
    void testVectorRebracket(VectorSpecies<E> a, VectorSpecies<F> b, byte[] input, byte[] output) {
        testVectorRebracket(a, b, input, output, false);
        testVectorRebracketLanewise(a, b, input, output);
    }
    @ForceInline
    static <E,F>
    void testVectorRebracketLanewise(VectorSpecies<E> a, VectorSpecies<F> b, byte[] input, byte[] output) {
        testVectorRebracket(a, b, input, output, true);
    }
    @ForceInline
    static <E,F>
    void testVectorRebracket(VectorSpecies<E> a, VectorSpecies<F> b, byte[] input, byte[] output, boolean lanewise) {
        Class<?> atype = a.elementType(), btype = b.elementType();
        Vector<E> av = a.fromByteArray(input, 0, ByteOrder.nativeOrder());
        int partLimit = partLimit(a, b, lanewise);
        int block;
        assert(input.length == output.length);
        if (!lanewise)
            block = Math.min(a.vectorByteSize(), b.vectorByteSize());
        else if (partLimit >= 0)
            block = a.vectorByteSize() / Math.max(1, partLimit);
        else
            block = b.vectorByteSize() / -partLimit;
        if (lanewise) {
            if (atype == btype)  return;
            if (atype == float.class || atype == double.class)  return;
            if (btype == float.class || btype == double.class)  return;
        }
        if (false)
            System.out.println("testing "+a+"->"+b+
                               (lanewise?" (lanewise)":" (reinterpret)")+
                               ", partLimit=" + partLimit +
                               ", block=" + block);
        byte[] expected;
        int origin;
        if (partLimit > 0) {
            for (int part = 0; part < partLimit; part++) {
                Vector<F> bv = (lanewise
                                ? av.castShape(b, part)
                                : av.reinterpretShape(b, part));
                bv.intoByteArray(output, 0, ByteOrder.nativeOrder());
                // expansion: slice some of the input
                origin = part * block;
                expected = Arrays.copyOfRange(input, origin, origin + block);
                if (lanewise) {
                    expected = castByteArrayData(expected, atype, btype);
                }
                checkPartialResult(a, b, input, output, expected,
                                   lanewise, part, origin);
            }
        } else if (partLimit < 0) {
            for (int part = 0; part > partLimit; part--) {
                Vector<F> bv = (lanewise
                                ? av.castShape(b, part)
                                : av.reinterpretShape(b, part));
                bv.intoByteArray(output, 0, ByteOrder.nativeOrder());
                // contraction: unslice the input into part of the output
                byte[] logical = input;
                if (lanewise) {
                    logical = castByteArrayData(input, atype, btype);
                }
                assert(logical.length == block);
                expected = new byte[output.length];
                origin = -part * block;
                System.arraycopy(logical, 0, expected, origin, block);
                checkPartialResult(a, b, input, output, expected,
                                   lanewise, part, origin);
            }
        } else {
            int part = 0;
            Vector<F> bv = (lanewise
                            ? av.castShape(b, part)
                            : av.reinterpretShape(b, part));
            bv.intoByteArray(output, 0, ByteOrder.nativeOrder());
            // in-place copy, no resize
            expected = input;
            origin = 0;
            if (lanewise) {
                expected = castByteArrayData(expected, atype, btype);
            }
            checkPartialResult(a, b, input, output, expected,
                               lanewise, part, origin);
        }
    }

    static int decodeType(Class<?> type) {
        switch (type.getName().charAt(0)) {
        case 'b': return 1;
        case 's': return 2;
        case 'i': return 4;
        case 'l': return 8;
        case 'f': return -4;
        case 'd': return -8;
        }
        throw new AssertionError(type);
    }

    static byte[] castByteArrayData(byte[] data, Class<?> atype, Class<?> btype) {
        if (atype == btype)  return data;
        int asize = decodeType(atype), bsize = decodeType(btype);
        assert((asize | bsize) > 0);  // no float or double
        int count = data.length / asize;
        assert(data.length == count * asize);
        byte[] result = new byte[count * bsize];

        int minsize = Math.min(asize, bsize);
        int size_diff = bsize - asize;
        ByteOrder bo = ByteOrder.nativeOrder();
        int rp = 0, dp = 0;
        for (int i = 0; i < count; i++) {
            if (bo == ByteOrder.BIG_ENDIAN) {
                if (size_diff > 0) {
                    byte sign = (byte)(data[dp] >> 7); // sign extend
                    for (int j = 0; j < size_diff; j++) {
                        result[rp++] = sign;
                    }
                } else {
                    dp -= size_diff; // step forward if needed
                }
            }
            byte b = 0;
            for (int j = 0; j < minsize; j++) {
                b = data[dp++];
                result[rp++] = b;
            }
            if (bo == ByteOrder.LITTLE_ENDIAN) {
                if (size_diff > 0) {
                    byte sign = (byte)(b >> 7); // sign extend
                    for (int j = 0; j < size_diff; j++) {
                        result[rp++] = sign;
                    }
                } else {
                    dp -= size_diff; // step forward if needed
                }
            }
        }
        assert(dp == data.length);
        assert(rp == result.length);

        return result;
    }

    @Test(dataProvider = "byteUnaryOpProvider")
    static void testRebracket64(IntFunction<byte[]> fa) {
        byte[] barr = fa.apply(64/Byte.SIZE);
        byte[] bout = new byte[barr.length];
        for (int i = 0; i < NUM_ITER; i++) {
            testVectorRebracket(bspec64, bspec64, barr, bout);
            testVectorRebracket(bspec64, sspec64, barr, bout);
            testVectorRebracket(bspec64, ispec64, barr, bout);
            testVectorRebracket(bspec64, lspec64, barr, bout);
            testVectorRebracket(bspec64, fspec64, barr, bout);
            testVectorRebracket(bspec64, dspec64, barr, bout);

            testVectorRebracket(sspec64, bspec64, barr, bout);
            testVectorRebracket(sspec64, sspec64, barr, bout);
            testVectorRebracket(sspec64, ispec64, barr, bout);
            testVectorRebracket(sspec64, lspec64, barr, bout);
            testVectorRebracket(sspec64, fspec64, barr, bout);
            testVectorRebracket(sspec64, dspec64, barr, bout);

            testVectorRebracket(ispec64, bspec64, barr, bout);
            testVectorRebracket(ispec64, sspec64, barr, bout);
            testVectorRebracket(ispec64, ispec64, barr, bout);
            testVectorRebracket(ispec64, lspec64, barr, bout);
            testVectorRebracket(ispec64, fspec64, barr, bout);
            testVectorRebracket(ispec64, dspec64, barr, bout);

            testVectorRebracket(lspec64, bspec64, barr, bout);
            testVectorRebracket(lspec64, sspec64, barr, bout);
            testVectorRebracket(lspec64, ispec64, barr, bout);
            testVectorRebracket(lspec64, lspec64, barr, bout);
            testVectorRebracket(lspec64, fspec64, barr, bout);
            testVectorRebracket(lspec64, dspec64, barr, bout);

            testVectorRebracket(fspec64, bspec64, barr, bout);
            testVectorRebracket(fspec64, sspec64, barr, bout);
            testVectorRebracket(fspec64, ispec64, barr, bout);
            testVectorRebracket(fspec64, lspec64, barr, bout);
            testVectorRebracket(fspec64, fspec64, barr, bout);
            testVectorRebracket(fspec64, dspec64, barr, bout);

            testVectorRebracket(dspec64, bspec64, barr, bout);
            testVectorRebracket(dspec64, sspec64, barr, bout);
            testVectorRebracket(dspec64, ispec64, barr, bout);
            testVectorRebracket(dspec64, lspec64, barr, bout);
            testVectorRebracket(dspec64, fspec64, barr, bout);
            testVectorRebracket(dspec64, dspec64, barr, bout);
        }
    }

    @Test(dataProvider = "byteUnaryOpProvider")
    static void testRebracket128(IntFunction<byte[]> fa) {
        byte[] barr = fa.apply(128/Byte.SIZE);
        byte[] bout = new byte[barr.length];
        for (int i = 0; i < NUM_ITER; i++) {
            testVectorRebracket(bspec128, bspec128, barr, bout);
            testVectorRebracket(bspec128, sspec128, barr, bout);
            testVectorRebracket(bspec128, ispec128, barr, bout);
            testVectorRebracket(bspec128, lspec128, barr, bout);
            testVectorRebracket(bspec128, fspec128, barr, bout);
            testVectorRebracket(bspec128, dspec128, barr, bout);

            testVectorRebracket(sspec128, bspec128, barr, bout);
            testVectorRebracket(sspec128, sspec128, barr, bout);
            testVectorRebracket(sspec128, ispec128, barr, bout);
            testVectorRebracket(sspec128, lspec128, barr, bout);
            testVectorRebracket(sspec128, fspec128, barr, bout);
            testVectorRebracket(sspec128, dspec128, barr, bout);

            testVectorRebracket(ispec128, bspec128, barr, bout);
            testVectorRebracket(ispec128, sspec128, barr, bout);
            testVectorRebracket(ispec128, ispec128, barr, bout);
            testVectorRebracket(ispec128, lspec128, barr, bout);
            testVectorRebracket(ispec128, fspec128, barr, bout);
            testVectorRebracket(ispec128, dspec128, barr, bout);

            testVectorRebracket(lspec128, bspec128, barr, bout);
            testVectorRebracket(lspec128, sspec128, barr, bout);
            testVectorRebracket(lspec128, ispec128, barr, bout);
            testVectorRebracket(lspec128, lspec128, barr, bout);
            testVectorRebracket(lspec128, fspec128, barr, bout);
            testVectorRebracket(lspec128, dspec128, barr, bout);

            testVectorRebracket(fspec128, bspec128, barr, bout);
            testVectorRebracket(fspec128, sspec128, barr, bout);
            testVectorRebracket(fspec128, ispec128, barr, bout);
            testVectorRebracket(fspec128, lspec128, barr, bout);
            testVectorRebracket(fspec128, fspec128, barr, bout);
            testVectorRebracket(fspec128, dspec128, barr, bout);

            testVectorRebracket(dspec128, bspec128, barr, bout);
            testVectorRebracket(dspec128, sspec128, barr, bout);
            testVectorRebracket(dspec128, ispec128, barr, bout);
            testVectorRebracket(dspec128, lspec128, barr, bout);
            testVectorRebracket(dspec128, fspec128, barr, bout);
            testVectorRebracket(dspec128, dspec128, barr, bout);
        }
    }

    @Test(dataProvider = "byteUnaryOpProvider")
    static void testRebracket256(IntFunction<byte[]> fa) {
        byte[] barr = fa.apply(256/Byte.SIZE);
        byte[] bout = new byte[barr.length];
        for (int i = 0; i < NUM_ITER; i++) {
            testVectorRebracket(bspec256, bspec256, barr, bout);
            testVectorRebracket(bspec256, sspec256, barr, bout);
            testVectorRebracket(bspec256, ispec256, barr, bout);
            testVectorRebracket(bspec256, lspec256, barr, bout);
            testVectorRebracket(bspec256, fspec256, barr, bout);
            testVectorRebracket(bspec256, dspec256, barr, bout);

            testVectorRebracket(sspec256, bspec256, barr, bout);
            testVectorRebracket(sspec256, sspec256, barr, bout);
            testVectorRebracket(sspec256, ispec256, barr, bout);
            testVectorRebracket(sspec256, lspec256, barr, bout);
            testVectorRebracket(sspec256, fspec256, barr, bout);
            testVectorRebracket(sspec256, dspec256, barr, bout);

            testVectorRebracket(ispec256, bspec256, barr, bout);
            testVectorRebracket(ispec256, sspec256, barr, bout);
            testVectorRebracket(ispec256, ispec256, barr, bout);
            testVectorRebracket(ispec256, lspec256, barr, bout);
            testVectorRebracket(ispec256, fspec256, barr, bout);
            testVectorRebracket(ispec256, dspec256, barr, bout);

            testVectorRebracket(lspec256, bspec256, barr, bout);
            testVectorRebracket(lspec256, sspec256, barr, bout);
            testVectorRebracket(lspec256, ispec256, barr, bout);
            testVectorRebracket(lspec256, lspec256, barr, bout);
            testVectorRebracket(lspec256, fspec256, barr, bout);
            testVectorRebracket(lspec256, dspec256, barr, bout);

            testVectorRebracket(fspec256, bspec256, barr, bout);
            testVectorRebracket(fspec256, sspec256, barr, bout);
            testVectorRebracket(fspec256, ispec256, barr, bout);
            testVectorRebracket(fspec256, lspec256, barr, bout);
            testVectorRebracket(fspec256, fspec256, barr, bout);
            testVectorRebracket(fspec256, dspec256, barr, bout);

            testVectorRebracket(dspec256, bspec256, barr, bout);
            testVectorRebracket(dspec256, sspec256, barr, bout);
            testVectorRebracket(dspec256, ispec256, barr, bout);
            testVectorRebracket(dspec256, lspec256, barr, bout);
            testVectorRebracket(dspec256, fspec256, barr, bout);
            testVectorRebracket(dspec256, dspec256, barr, bout);
        }
    }

    @Test(dataProvider = "byteUnaryOpProvider")
    static void testRebracket512(IntFunction<byte[]> fa) {
        byte[] barr = fa.apply(512/Byte.SIZE);
        byte[] bout = new byte[barr.length];
        for (int i = 0; i < NUM_ITER; i++) {
            testVectorRebracket(bspec512, bspec512, barr, bout);
            testVectorRebracket(bspec512, sspec512, barr, bout);
            testVectorRebracket(bspec512, ispec512, barr, bout);
            testVectorRebracket(bspec512, lspec512, barr, bout);
            testVectorRebracket(bspec512, fspec512, barr, bout);
            testVectorRebracket(bspec512, dspec512, barr, bout);

            testVectorRebracket(sspec512, bspec512, barr, bout);
            testVectorRebracket(sspec512, sspec512, barr, bout);
            testVectorRebracket(sspec512, ispec512, barr, bout);
            testVectorRebracket(sspec512, lspec512, barr, bout);
            testVectorRebracket(sspec512, fspec512, barr, bout);
            testVectorRebracket(sspec512, dspec512, barr, bout);

            testVectorRebracket(ispec512, bspec512, barr, bout);
            testVectorRebracket(ispec512, sspec512, barr, bout);
            testVectorRebracket(ispec512, ispec512, barr, bout);
            testVectorRebracket(ispec512, lspec512, barr, bout);
            testVectorRebracket(ispec512, fspec512, barr, bout);
            testVectorRebracket(ispec512, dspec512, barr, bout);

            testVectorRebracket(lspec512, bspec512, barr, bout);
            testVectorRebracket(lspec512, sspec512, barr, bout);
            testVectorRebracket(lspec512, ispec512, barr, bout);
            testVectorRebracket(lspec512, lspec512, barr, bout);
            testVectorRebracket(lspec512, fspec512, barr, bout);
            testVectorRebracket(lspec512, dspec512, barr, bout);

            testVectorRebracket(fspec512, bspec512, barr, bout);
            testVectorRebracket(fspec512, sspec512, barr, bout);
            testVectorRebracket(fspec512, ispec512, barr, bout);
            testVectorRebracket(fspec512, lspec512, barr, bout);
            testVectorRebracket(fspec512, fspec512, barr, bout);
            testVectorRebracket(fspec512, dspec512, barr, bout);

            testVectorRebracket(dspec512, bspec512, barr, bout);
            testVectorRebracket(dspec512, sspec512, barr, bout);
            testVectorRebracket(dspec512, ispec512, barr, bout);
            testVectorRebracket(dspec512, lspec512, barr, bout);
            testVectorRebracket(dspec512, fspec512, barr, bout);
            testVectorRebracket(dspec512, dspec512, barr, bout);
        }
    }

    @Test(dataProvider = "byteUnaryOpProvider")
    static void testRebracketMax(IntFunction<byte[]> fa) {
        byte[] barr = fa.apply(S_Max_BIT.vectorBitSize()/Byte.SIZE);
        byte[] bout = new byte[barr.length];
        for (int i = 0; i < NUM_ITER; i++) {
            testVectorRebracket(bspecMax, bspecMax, barr, bout);
            testVectorRebracket(bspecMax, sspecMax, barr, bout);
            testVectorRebracket(bspecMax, ispecMax, barr, bout);
            testVectorRebracket(bspecMax, lspecMax, barr, bout);
            testVectorRebracket(bspecMax, fspecMax, barr, bout);
            testVectorRebracket(bspecMax, dspecMax, barr, bout);

            testVectorRebracket(sspecMax, bspecMax, barr, bout);
            testVectorRebracket(sspecMax, sspecMax, barr, bout);
            testVectorRebracket(sspecMax, ispecMax, barr, bout);
            testVectorRebracket(sspecMax, lspecMax, barr, bout);
            testVectorRebracket(sspecMax, fspecMax, barr, bout);
            testVectorRebracket(sspecMax, dspecMax, barr, bout);

            testVectorRebracket(ispecMax, bspecMax, barr, bout);
            testVectorRebracket(ispecMax, sspecMax, barr, bout);
            testVectorRebracket(ispecMax, ispecMax, barr, bout);
            testVectorRebracket(ispecMax, lspecMax, barr, bout);
            testVectorRebracket(ispecMax, fspecMax, barr, bout);
            testVectorRebracket(ispecMax, dspecMax, barr, bout);

            testVectorRebracket(lspecMax, bspecMax, barr, bout);
            testVectorRebracket(lspecMax, sspecMax, barr, bout);
            testVectorRebracket(lspecMax, ispecMax, barr, bout);
            testVectorRebracket(lspecMax, lspecMax, barr, bout);
            testVectorRebracket(lspecMax, fspecMax, barr, bout);
            testVectorRebracket(lspecMax, dspecMax, barr, bout);

            testVectorRebracket(fspecMax, bspecMax, barr, bout);
            testVectorRebracket(fspecMax, sspecMax, barr, bout);
            testVectorRebracket(fspecMax, ispecMax, barr, bout);
            testVectorRebracket(fspecMax, lspecMax, barr, bout);
            testVectorRebracket(fspecMax, fspecMax, barr, bout);
            testVectorRebracket(fspecMax, dspecMax, barr, bout);

            testVectorRebracket(dspecMax, bspecMax, barr, bout);
            testVectorRebracket(dspecMax, sspecMax, barr, bout);
            testVectorRebracket(dspecMax, ispecMax, barr, bout);
            testVectorRebracket(dspecMax, lspecMax, barr, bout);
            testVectorRebracket(dspecMax, fspecMax, barr, bout);
            testVectorRebracket(dspecMax, dspecMax, barr, bout);
        }
    }

    @ForceInline
    static
    void testVectorCastByteToFloat(VectorSpecies<Byte> a, VectorSpecies<Float> b, byte[] input, float[] output) {
        assert(input.length == a.length());
        assert(output.length == b.length());

        ByteVector av = ByteVector.fromArray(a, input, 0);
        FloatVector bv = (FloatVector) av.castShape(b, 0);
        bv.intoArray(output, 0);

        for (int i = 0; i < Math.min(input.length, output.length); i++) {
            Assert.assertEquals(output[i], (float)input[i]);
        }
        for(int i = input.length; i < output.length; i++) {
            Assert.assertEquals(output[i], (float)0);
        }
    }

    @ForceInline
    static
    void testVectorCastByteToFloatFail(VectorSpecies<Byte> a, VectorSpecies<Float> b, byte[] input) {
        assert(input.length == a.length());

        ByteVector av = ByteVector.fromArray(a, input, 0);
        try {
            av.castShape(b, 0);
            Assert.fail(String.format(
                    "Cast failed to throw ClassCastException for differing species lengths for %s and %s",
                    a, b));
        } catch (ClassCastException e) {
        }
    }

    @ForceInline
    static
    void testVectorCastShortToFloat(VectorSpecies<Short> a, VectorSpecies<Float> b, short[] input, float[] output) {
        assert(input.length == a.length());
        assert(output.length == b.length());

        ShortVector av = ShortVector.fromArray(a, input, 0);
        FloatVector bv = (FloatVector) av.castShape(b, 0);
        bv.intoArray(output, 0);

        for (int i = 0; i < Math.min(input.length, output.length); i++) {
            Assert.assertEquals(output[i], (float)input[i]);
        }
        for(int i = input.length; i < output.length; i++) {
            Assert.assertEquals(output[i], (float)0);
        }
    }

    @ForceInline
    static
    void testVectorCastShortToFloatFail(VectorSpecies<Short> a, VectorSpecies<Float> b, short[] input) {
        assert(input.length == a.length());

        ShortVector av = ShortVector.fromArray(a, input, 0);
        try {
            av.castShape(b, 0);
            Assert.fail(String.format(
                    "Cast failed to throw ClassCastException for differing species lengths for %s and %s",
                    a, b));
        } catch (ClassCastException e) {
        }
    }

    @ForceInline
    static
    void testVectorCastIntToFloat(VectorSpecies<Integer> a, VectorSpecies<Float> b, int[] input, float[] output) {
        assert(input.length == a.length());
        assert(output.length == b.length());

        IntVector av = IntVector.fromArray(a, input, 0);
        FloatVector bv = (FloatVector) av.castShape(b, 0);
        bv.intoArray(output, 0);

        for (int i = 0; i < Math.min(input.length, output.length); i++) {
            Assert.assertEquals(output[i], (float)input[i]);
        }
        for(int i = input.length; i < output.length; i++) {
            Assert.assertEquals(output[i], (float)0);
        }
    }

    @ForceInline
    static
    void testVectorCastIntToFloatFail(VectorSpecies<Integer> a, VectorSpecies<Float> b, int[] input) {
        assert(input.length == a.length());

        IntVector av = IntVector.fromArray(a, input, 0);
        try {
            av.castShape(b, 0);
            Assert.fail(String.format(
                    "Cast failed to throw ClassCastException for differing species lengths for %s and %s",
                    a, b));
        } catch (ClassCastException e) {
        }
    }

    @ForceInline
    static
    void testVectorCastLongToFloat(VectorSpecies<Long> a, VectorSpecies<Float> b, long[] input, float[] output) {
        assert(input.length == a.length());
        assert(output.length == b.length());

        LongVector av = LongVector.fromArray(a, input, 0);
        FloatVector bv = (FloatVector) av.castShape(b, 0);
        bv.intoArray(output, 0);

        for (int i = 0; i < Math.min(input.length, output.length); i++) {
            Assert.assertEquals(output[i], (float)input[i]);
        }
        for(int i = input.length; i < output.length; i++) {
            Assert.assertEquals(output[i], (float)0);
        }
    }

    @ForceInline
    static
    void testVectorCastLongToFloatFail(VectorSpecies<Long> a, VectorSpecies<Float> b, long[] input) {
        assert(input.length == a.length());

        LongVector av = LongVector.fromArray(a, input, 0);
        try {
            av.castShape(b, 0);
            Assert.fail(String.format(
                    "Cast failed to throw ClassCastException for differing species lengths for %s and %s",
                    a, b));
        } catch (ClassCastException e) {
        }
    }

    @ForceInline
    static
    void testVectorCastFloatToFloat(VectorSpecies<Float> a, VectorSpecies<Float> b, float[] input, float[] output) {
        assert(input.length == a.length());
        assert(output.length == b.length());

        FloatVector av = FloatVector.fromArray(a, input, 0);
        FloatVector bv = (FloatVector) av.castShape(b, 0);
        bv.intoArray(output, 0);

        for (int i = 0; i < Math.min(input.length, output.length); i++) {
            Assert.assertEquals(output[i], (float)input[i]);
        }
        for(int i = input.length; i < output.length; i++) {
            Assert.assertEquals(output[i], (float)0);
        }
    }

    @ForceInline
    static
    void testVectorCastFloatToFloatFail(VectorSpecies<Float> a, VectorSpecies<Float> b, float[] input) {
        assert(input.length == a.length());

        FloatVector av = FloatVector.fromArray(a, input, 0);
        try {
            av.castShape(b, 0);
            Assert.fail(String.format(
                    "Cast failed to throw ClassCastException for differing species lengths for %s and %s",
                    a, b));
        } catch (ClassCastException e) {
        }
    }

    @ForceInline
    static
    void testVectorCastDoubleToFloat(VectorSpecies<Double> a, VectorSpecies<Float> b, double[] input, float[] output) {
        assert(input.length == a.length());
        assert(output.length == b.length());

        DoubleVector av = DoubleVector.fromArray(a, input, 0);
        FloatVector bv = (FloatVector) av.castShape(b, 0);
        bv.intoArray(output, 0);

        for (int i = 0; i < Math.min(input.length, output.length); i++) {
            Assert.assertEquals(output[i], (float)input[i]);
        }
        for(int i = input.length; i < output.length; i++) {
            Assert.assertEquals(output[i], (float)0);
        }
    }

    @ForceInline
    static
    void testVectorCastDoubleToFloatFail(VectorSpecies<Double> a, VectorSpecies<Float> b, double[] input) {
        assert(input.length == a.length());

        DoubleVector av = DoubleVector.fromArray(a, input, 0);
        try {
            av.castShape(b, 0);
            Assert.fail(String.format(
                    "Cast failed to throw ClassCastException for differing species lengths for %s and %s",
                    a, b));
        } catch (ClassCastException e) {
        }
    }

    @ForceInline
    static
    void testVectorCastByteToByte(VectorSpecies<Byte> a, VectorSpecies<Byte> b, byte[] input, byte[] output) {
        assert(input.length == a.length());
        assert(output.length == b.length());

        ByteVector av = ByteVector.fromArray(a, input, 0);
        ByteVector bv = (ByteVector) av.castShape(b, 0);
        bv.intoArray(output, 0);

        for (int i = 0; i < Math.min(input.length, output.length); i++) {
            Assert.assertEquals(output[i], (byte)input[i]);
        }
        for(int i = input.length; i < output.length; i++) {
            Assert.assertEquals(output[i], (byte)0);
        }
    }

    @ForceInline
    static
    void testVectorCastByteToByteFail(VectorSpecies<Byte> a, VectorSpecies<Byte> b, byte[] input) {
        assert(input.length == a.length());

        ByteVector av = ByteVector.fromArray(a, input, 0);
        try {
            av.castShape(b, 0);
            Assert.fail(String.format(
                    "Cast failed to throw ClassCastException for differing species lengths for %s and %s",
                    a, b));
        } catch (ClassCastException e) {
        }
    }

    @ForceInline
    static
    void testVectorCastShortToByte(VectorSpecies<Short> a, VectorSpecies<Byte> b, short[] input, byte[] output) {
        assert(input.length == a.length());
        assert(output.length == b.length());

        ShortVector av = ShortVector.fromArray(a, input, 0);
        ByteVector bv = (ByteVector) av.castShape(b, 0);
        bv.intoArray(output, 0);

        for (int i = 0; i < Math.min(input.length, output.length); i++) {
            Assert.assertEquals(output[i], (byte)input[i]);
        }
        for(int i = input.length; i < output.length; i++) {
            Assert.assertEquals(output[i], (byte)0);
        }
    }

    @ForceInline
    static
    void testVectorCastShortToByteFail(VectorSpecies<Short> a, VectorSpecies<Byte> b, short[] input) {
        assert(input.length == a.length());

        ShortVector av = ShortVector.fromArray(a, input, 0);
        try {
            av.castShape(b, 0);
            Assert.fail(String.format(
                    "Cast failed to throw ClassCastException for differing species lengths for %s and %s",
                    a, b));
        } catch (ClassCastException e) {
        }
    }

    @ForceInline
    static
    void testVectorCastIntToByte(VectorSpecies<Integer> a, VectorSpecies<Byte> b, int[] input, byte[] output) {
        assert(input.length == a.length());
        assert(output.length == b.length());

        IntVector av = IntVector.fromArray(a, input, 0);
        ByteVector bv = (ByteVector) av.castShape(b, 0);
        bv.intoArray(output, 0);

        for (int i = 0; i < Math.min(input.length, output.length); i++) {
            Assert.assertEquals(output[i], (byte)input[i]);
        }
        for(int i = input.length; i < output.length; i++) {
            Assert.assertEquals(output[i], (byte)0);
        }
    }

    @ForceInline
    static
    void testVectorCastIntToByteFail(VectorSpecies<Integer> a, VectorSpecies<Byte> b, int[] input) {
        assert(input.length == a.length());

        IntVector av = IntVector.fromArray(a, input, 0);
        try {
            av.castShape(b, 0);
            Assert.fail(String.format(
                    "Cast failed to throw ClassCastException for differing species lengths for %s and %s",
                    a, b));
        } catch (ClassCastException e) {
        }
    }

    @ForceInline
    static
    void testVectorCastLongToByte(VectorSpecies<Long> a, VectorSpecies<Byte> b, long[] input, byte[] output) {
        assert(input.length == a.length());
        assert(output.length == b.length());

        LongVector av = LongVector.fromArray(a, input, 0);
        ByteVector bv = (ByteVector) av.castShape(b, 0);
        bv.intoArray(output, 0);

        for (int i = 0; i < Math.min(input.length, output.length); i++) {
            Assert.assertEquals(output[i], (byte)input[i]);
        }
        for(int i = input.length; i < output.length; i++) {
            Assert.assertEquals(output[i], (byte)0);
        }
    }

    @ForceInline
    static
    void testVectorCastLongToByteFail(VectorSpecies<Long> a, VectorSpecies<Byte> b, long[] input) {
        assert(input.length == a.length());

        LongVector av = LongVector.fromArray(a, input, 0);
        try {
            av.castShape(b, 0);
            Assert.fail(String.format(
                    "Cast failed to throw ClassCastException for differing species lengths for %s and %s",
                    a, b));
        } catch (ClassCastException e) {
        }
    }

    @ForceInline
    static
    void testVectorCastFloatToByte(VectorSpecies<Float> a, VectorSpecies<Byte> b, float[] input, byte[] output) {
        assert(input.length == a.length());
        assert(output.length == b.length());

        FloatVector av = FloatVector.fromArray(a, input, 0);
        ByteVector bv = (ByteVector) av.castShape(b, 0);
        bv.intoArray(output, 0);

        for (int i = 0; i < Math.min(input.length, output.length); i++) {
            Assert.assertEquals(output[i], (byte)input[i]);
        }
        for(int i = input.length; i < output.length; i++) {
            Assert.assertEquals(output[i], (byte)0);
        }
    }

    @ForceInline
    static
    void testVectorCastFloatToByteFail(VectorSpecies<Float> a, VectorSpecies<Byte> b, float[] input) {
        assert(input.length == a.length());

        FloatVector av = FloatVector.fromArray(a, input, 0);
        try {
            av.castShape(b, 0);
            Assert.fail(String.format(
                    "Cast failed to throw ClassCastException for differing species lengths for %s and %s",
                    a, b));
        } catch (ClassCastException e) {
        }
    }

    @ForceInline
    static
    void testVectorCastDoubleToByte(VectorSpecies<Double> a, VectorSpecies<Byte> b, double[] input, byte[] output) {
        assert(input.length == a.length());
        assert(output.length == b.length());

        DoubleVector av = DoubleVector.fromArray(a, input, 0);
        ByteVector bv = (ByteVector) av.castShape(b, 0);
        bv.intoArray(output, 0);

        for (int i = 0; i < Math.min(input.length, output.length); i++) {
            Assert.assertEquals(output[i], (byte)input[i]);
        }
        for(int i = input.length; i < output.length; i++) {
            Assert.assertEquals(output[i], (byte)0);
        }
    }

    @ForceInline
    static
    void testVectorCastDoubleToByteFail(VectorSpecies<Double> a, VectorSpecies<Byte> b, double[] input) {
        assert(input.length == a.length());

        DoubleVector av = DoubleVector.fromArray(a, input, 0);
        try {
            av.castShape(b, 0);
            Assert.fail(String.format(
                    "Cast failed to throw ClassCastException for differing species lengths for %s and %s",
                    a, b));
        } catch (ClassCastException e) {
        }
    }

    @ForceInline
    static
    void testVectorCastByteToShort(VectorSpecies<Byte> a, VectorSpecies<Short> b, byte[] input, short[] output) {
        assert(input.length == a.length());
        assert(output.length == b.length());

        ByteVector av = ByteVector.fromArray(a, input, 0);
        ShortVector bv = (ShortVector) av.castShape(b, 0);
        bv.intoArray(output, 0);

        for (int i = 0; i < Math.min(input.length, output.length); i++) {
            Assert.assertEquals(output[i], (short)input[i]);
        }
        for(int i = input.length; i < output.length; i++) {
            Assert.assertEquals(output[i], (short)0);
        }
    }

    @ForceInline
    static
    void testVectorCastByteToShortFail(VectorSpecies<Byte> a, VectorSpecies<Short> b, byte[] input) {
        assert(input.length == a.length());

        ByteVector av = ByteVector.fromArray(a, input, 0);
        try {
            av.castShape(b, 0);
            Assert.fail(String.format(
                    "Cast failed to throw ClassCastException for differing species lengths for %s and %s",
                    a, b));
        } catch (ClassCastException e) {
        }
    }

    @ForceInline
    static
    void testVectorCastShortToShort(VectorSpecies<Short> a, VectorSpecies<Short> b, short[] input, short[] output) {
        assert(input.length == a.length());
        assert(output.length == b.length());

        ShortVector av = ShortVector.fromArray(a, input, 0);
        ShortVector bv = (ShortVector) av.castShape(b, 0);
        bv.intoArray(output, 0);

        for (int i = 0; i < Math.min(input.length, output.length); i++) {
            Assert.assertEquals(output[i], (short)input[i]);
        }
        for(int i = input.length; i < output.length; i++) {
            Assert.assertEquals(output[i], (short)0);
        }
    }

    @ForceInline
    static
    void testVectorCastShortToShortFail(VectorSpecies<Short> a, VectorSpecies<Short> b, short[] input) {
        assert(input.length == a.length());

        ShortVector av = ShortVector.fromArray(a, input, 0);
        try {
            av.castShape(b, 0);
            Assert.fail(String.format(
                    "Cast failed to throw ClassCastException for differing species lengths for %s and %s",
                    a, b));
        } catch (ClassCastException e) {
        }
    }

    @ForceInline
    static
    void testVectorCastIntToShort(VectorSpecies<Integer> a, VectorSpecies<Short> b, int[] input, short[] output) {
        assert(input.length == a.length());
        assert(output.length == b.length());

        IntVector av = IntVector.fromArray(a, input, 0);
        ShortVector bv = (ShortVector) av.castShape(b, 0);
        bv.intoArray(output, 0);

        for (int i = 0; i < Math.min(input.length, output.length); i++) {
            Assert.assertEquals(output[i], (short)input[i]);
        }
        for(int i = input.length; i < output.length; i++) {
            Assert.assertEquals(output[i], (short)0);
        }
    }

    @ForceInline
    static
    void testVectorCastIntToShortFail(VectorSpecies<Integer> a, VectorSpecies<Short> b, int[] input) {
        assert(input.length == a.length());

        IntVector av = IntVector.fromArray(a, input, 0);
        try {
            av.castShape(b, 0);
            Assert.fail(String.format(
                    "Cast failed to throw ClassCastException for differing species lengths for %s and %s",
                    a, b));
        } catch (ClassCastException e) {
        }
    }

    @ForceInline
    static
    void testVectorCastLongToShort(VectorSpecies<Long> a, VectorSpecies<Short> b, long[] input, short[] output) {
        assert(input.length == a.length());
        assert(output.length == b.length());

        LongVector av = LongVector.fromArray(a, input, 0);
        ShortVector bv = (ShortVector) av.castShape(b, 0);
        bv.intoArray(output, 0);

        for (int i = 0; i < Math.min(input.length, output.length); i++) {
            Assert.assertEquals(output[i], (short)input[i]);
        }
        for(int i = input.length; i < output.length; i++) {
            Assert.assertEquals(output[i], (short)0);
        }
    }

    @ForceInline
    static
    void testVectorCastLongToShortFail(VectorSpecies<Long> a, VectorSpecies<Short> b, long[] input) {
        assert(input.length == a.length());

        LongVector av = LongVector.fromArray(a, input, 0);
        try {
            av.castShape(b, 0);
            Assert.fail(String.format(
                    "Cast failed to throw ClassCastException for differing species lengths for %s and %s",
                    a, b));
        } catch (ClassCastException e) {
        }
    }

    @ForceInline
    static
    void testVectorCastFloatToShort(VectorSpecies<Float> a, VectorSpecies<Short> b, float[] input, short[] output) {
        assert(input.length == a.length());
        assert(output.length == b.length());

        FloatVector av = FloatVector.fromArray(a, input, 0);
        ShortVector bv = (ShortVector) av.castShape(b, 0);
        bv.intoArray(output, 0);

        for (int i = 0; i < Math.min(input.length, output.length); i++) {
            Assert.assertEquals(output[i], (short)input[i]);
        }
        for(int i = input.length; i < output.length; i++) {
            Assert.assertEquals(output[i], (short)0);
        }
    }

    @ForceInline
    static
    void testVectorCastFloatToShortFail(VectorSpecies<Float> a, VectorSpecies<Short> b, float[] input) {
        assert(input.length == a.length());

        FloatVector av = FloatVector.fromArray(a, input, 0);
        try {
            av.castShape(b, 0);
            Assert.fail(String.format(
                    "Cast failed to throw ClassCastException for differing species lengths for %s and %s",
                    a, b));
        } catch (ClassCastException e) {
        }
    }

    @ForceInline
    static
    void testVectorCastDoubleToShort(VectorSpecies<Double> a, VectorSpecies<Short> b, double[] input, short[] output) {
        assert(input.length == a.length());
        assert(output.length == b.length());

        DoubleVector av = DoubleVector.fromArray(a, input, 0);
        ShortVector bv = (ShortVector) av.castShape(b, 0);
        bv.intoArray(output, 0);

        for (int i = 0; i < Math.min(input.length, output.length); i++) {
            Assert.assertEquals(output[i], (short)input[i]);
        }
        for(int i = input.length; i < output.length; i++) {
            Assert.assertEquals(output[i], (short)0);
        }
    }

    @ForceInline
    static
    void testVectorCastDoubleToShortFail(VectorSpecies<Double> a, VectorSpecies<Short> b, double[] input) {
        assert(input.length == a.length());

        DoubleVector av = DoubleVector.fromArray(a, input, 0);
        try {
            av.castShape(b, 0);
            Assert.fail(String.format(
                    "Cast failed to throw ClassCastException for differing species lengths for %s and %s",
                    a, b));
        } catch (ClassCastException e) {
        }
    }

    @ForceInline
    static
    void testVectorCastByteToInt(VectorSpecies<Byte> a, VectorSpecies<Integer> b, byte[] input, int[] output) {
        assert(input.length == a.length());
        assert(output.length == b.length());

        ByteVector av = ByteVector.fromArray(a, input, 0);
        IntVector bv = (IntVector) av.castShape(b, 0);
        bv.intoArray(output, 0);

        for (int i = 0; i < Math.min(input.length, output.length); i++) {
            Assert.assertEquals(output[i], (int)input[i]);
        }
        for(int i = input.length; i < output.length; i++) {
            Assert.assertEquals(output[i], (int)0);
        }
    }

    @ForceInline
    static
    void testVectorCastByteToIntFail(VectorSpecies<Byte> a, VectorSpecies<Integer> b, byte[] input) {
        assert(input.length == a.length());

        ByteVector av = ByteVector.fromArray(a, input, 0);
        try {
            av.castShape(b, 0);
            Assert.fail(String.format(
                    "Cast failed to throw ClassCastException for differing species lengths for %s and %s",
                    a, b));
        } catch (ClassCastException e) {
        }
    }

    @ForceInline
    static
    void testVectorCastShortToInt(VectorSpecies<Short> a, VectorSpecies<Integer> b, short[] input, int[] output) {
        assert(input.length == a.length());
        assert(output.length == b.length());

        ShortVector av = ShortVector.fromArray(a, input, 0);
        IntVector bv = (IntVector) av.castShape(b, 0);
        bv.intoArray(output, 0);

        for (int i = 0; i < Math.min(input.length, output.length); i++) {
            Assert.assertEquals(output[i], (int)input[i]);
        }
        for(int i = input.length; i < output.length; i++) {
            Assert.assertEquals(output[i], (int)0);
        }
    }

    @ForceInline
    static
    void testVectorCastShortToIntFail(VectorSpecies<Short> a, VectorSpecies<Integer> b, short[] input) {
        assert(input.length == a.length());

        ShortVector av = ShortVector.fromArray(a, input, 0);
        try {
            av.castShape(b, 0);
            Assert.fail(String.format(
                    "Cast failed to throw ClassCastException for differing species lengths for %s and %s",
                    a, b));
        } catch (ClassCastException e) {
        }
    }

    @ForceInline
    static
    void testVectorCastIntToInt(VectorSpecies<Integer> a, VectorSpecies<Integer> b, int[] input, int[] output) {
        assert(input.length == a.length());
        assert(output.length == b.length());

        IntVector av = IntVector.fromArray(a, input, 0);
        IntVector bv = (IntVector) av.castShape(b, 0);
        bv.intoArray(output, 0);

        for (int i = 0; i < Math.min(input.length, output.length); i++) {
            Assert.assertEquals(output[i], (int)input[i]);
        }
        for(int i = input.length; i < output.length; i++) {
            Assert.assertEquals(output[i], (int)0);
        }
    }

    @ForceInline
    static
    void testVectorCastIntToIntFail(VectorSpecies<Integer> a, VectorSpecies<Integer> b, int[] input) {
        assert(input.length == a.length());

        IntVector av = IntVector.fromArray(a, input, 0);
        try {
            av.castShape(b, 0);
            Assert.fail(String.format(
                    "Cast failed to throw ClassCastException for differing species lengths for %s and %s",
                    a, b));
        } catch (ClassCastException e) {
        }
    }

    @ForceInline
    static
    void testVectorCastLongToInt(VectorSpecies<Long> a, VectorSpecies<Integer> b, long[] input, int[] output) {
        assert(input.length == a.length());
        assert(output.length == b.length());

        LongVector av = LongVector.fromArray(a, input, 0);
        IntVector bv = (IntVector) av.castShape(b, 0);
        bv.intoArray(output, 0);

        for (int i = 0; i < Math.min(input.length, output.length); i++) {
            Assert.assertEquals(output[i], (int)input[i]);
        }
        for(int i = input.length; i < output.length; i++) {
            Assert.assertEquals(output[i], (int)0);
        }
    }

    @ForceInline
    static
    void testVectorCastLongToIntFail(VectorSpecies<Long> a, VectorSpecies<Integer> b, long[] input) {
        assert(input.length == a.length());

        LongVector av = LongVector.fromArray(a, input, 0);
        try {
            av.castShape(b, 0);
            Assert.fail(String.format(
                    "Cast failed to throw ClassCastException for differing species lengths for %s and %s",
                    a, b));
        } catch (ClassCastException e) {
        }
    }

    @ForceInline
    static
    void testVectorCastFloatToInt(VectorSpecies<Float> a, VectorSpecies<Integer> b, float[] input, int[] output) {
        assert(input.length == a.length());
        assert(output.length == b.length());

        FloatVector av = FloatVector.fromArray(a, input, 0);
        IntVector bv = (IntVector) av.castShape(b, 0);
        bv.intoArray(output, 0);

        for (int i = 0; i < Math.min(input.length, output.length); i++) {
            Assert.assertEquals(output[i], (int)input[i]);
        }
        for(int i = input.length; i < output.length; i++) {
            Assert.assertEquals(output[i], (int)0);
        }
    }

    @ForceInline
    static
    void testVectorCastFloatToIntFail(VectorSpecies<Float> a, VectorSpecies<Integer> b, float[] input) {
        assert(input.length == a.length());

        FloatVector av = FloatVector.fromArray(a, input, 0);
        try {
            av.castShape(b, 0);
            Assert.fail(String.format(
                    "Cast failed to throw ClassCastException for differing species lengths for %s and %s",
                    a, b));
        } catch (ClassCastException e) {
        }
    }

    @ForceInline
    static
    void testVectorCastDoubleToInt(VectorSpecies<Double> a, VectorSpecies<Integer> b, double[] input, int[] output) {
        assert(input.length == a.length());
        assert(output.length == b.length());

        DoubleVector av = DoubleVector.fromArray(a, input, 0);
        IntVector bv = (IntVector) av.castShape(b, 0);
        bv.intoArray(output, 0);

        for (int i = 0; i < Math.min(input.length, output.length); i++) {
            Assert.assertEquals(output[i], (int)input[i]);
        }
        for(int i = input.length; i < output.length; i++) {
            Assert.assertEquals(output[i], (int)0);
        }
    }

    @ForceInline
    static
    void testVectorCastDoubleToIntFail(VectorSpecies<Double> a, VectorSpecies<Integer> b, double[] input) {
        assert(input.length == a.length());

        DoubleVector av = DoubleVector.fromArray(a, input, 0);
        try {
            av.castShape(b, 0);
            Assert.fail(String.format(
                    "Cast failed to throw ClassCastException for differing species lengths for %s and %s",
                    a, b));
        } catch (ClassCastException e) {
        }
    }

    @ForceInline
    static
    void testVectorCastByteToLong(VectorSpecies<Byte> a, VectorSpecies<Long> b, byte[] input, long[] output) {
        assert(input.length == a.length());
        assert(output.length == b.length());

        ByteVector av = ByteVector.fromArray(a, input, 0);
        LongVector bv = (LongVector) av.castShape(b, 0);
        bv.intoArray(output, 0);

        for (int i = 0; i < Math.min(input.length, output.length); i++) {
            Assert.assertEquals(output[i], (long)input[i]);
        }
        for(int i = input.length; i < output.length; i++) {
            Assert.assertEquals(output[i], (long)0);
        }
    }

    @ForceInline
    static
    void testVectorCastByteToLongFail(VectorSpecies<Byte> a, VectorSpecies<Long> b, byte[] input) {
        assert(input.length == a.length());

        ByteVector av = ByteVector.fromArray(a, input, 0);
        try {
            av.castShape(b, 0);
            Assert.fail(String.format(
                    "Cast failed to throw ClassCastException for differing species lengths for %s and %s",
                    a, b));
        } catch (ClassCastException e) {
        }
    }

    @ForceInline
    static
    void testVectorCastShortToLong(VectorSpecies<Short> a, VectorSpecies<Long> b, short[] input, long[] output) {
        assert(input.length == a.length());
        assert(output.length == b.length());

        ShortVector av = ShortVector.fromArray(a, input, 0);
        LongVector bv = (LongVector) av.castShape(b, 0);
        bv.intoArray(output, 0);

        for (int i = 0; i < Math.min(input.length, output.length); i++) {
            Assert.assertEquals(output[i], (long)input[i]);
        }
        for(int i = input.length; i < output.length; i++) {
            Assert.assertEquals(output[i], (long)0);
        }
    }

    @ForceInline
    static
    void testVectorCastShortToLongFail(VectorSpecies<Short> a, VectorSpecies<Long> b, short[] input) {
        assert(input.length == a.length());

        ShortVector av = ShortVector.fromArray(a, input, 0);
        try {
            av.castShape(b, 0);
            Assert.fail(String.format(
                    "Cast failed to throw ClassCastException for differing species lengths for %s and %s",
                    a, b));
        } catch (ClassCastException e) {
        }
    }

    @ForceInline
    static
    void testVectorCastIntToLong(VectorSpecies<Integer> a, VectorSpecies<Long> b, int[] input, long[] output) {
        assert(input.length == a.length());
        assert(output.length == b.length());

        IntVector av = IntVector.fromArray(a, input, 0);
        LongVector bv = (LongVector) av.castShape(b, 0);
        bv.intoArray(output, 0);

        for (int i = 0; i < Math.min(input.length, output.length); i++) {
            Assert.assertEquals(output[i], (long)input[i]);
        }
        for(int i = input.length; i < output.length; i++) {
            Assert.assertEquals(output[i], (long)0);
        }
    }

    @ForceInline
    static
    void testVectorCastIntToLongFail(VectorSpecies<Integer> a, VectorSpecies<Long> b, int[] input) {
        assert(input.length == a.length());

        IntVector av = IntVector.fromArray(a, input, 0);
        try {
            av.castShape(b, 0);
            Assert.fail(String.format(
                    "Cast failed to throw ClassCastException for differing species lengths for %s and %s",
                    a, b));
        } catch (ClassCastException e) {
        }
    }

    @ForceInline
    static
    void testVectorCastLongToLong(VectorSpecies<Long> a, VectorSpecies<Long> b, long[] input, long[] output) {
        assert(input.length == a.length());
        assert(output.length == b.length());

        LongVector av = LongVector.fromArray(a, input, 0);
        LongVector bv = (LongVector) av.castShape(b, 0);
        bv.intoArray(output, 0);

        for (int i = 0; i < Math.min(input.length, output.length); i++) {
            Assert.assertEquals(output[i], (long)input[i]);
        }
        for(int i = input.length; i < output.length; i++) {
            Assert.assertEquals(output[i], (long)0);
        }
    }

    @ForceInline
    static
    void testVectorCastLongToLongFail(VectorSpecies<Long> a, VectorSpecies<Long> b, long[] input) {
        assert(input.length == a.length());

        LongVector av = LongVector.fromArray(a, input, 0);
        try {
            av.castShape(b, 0);
            Assert.fail(String.format(
                    "Cast failed to throw ClassCastException for differing species lengths for %s and %s",
                    a, b));
        } catch (ClassCastException e) {
        }
    }

    @ForceInline
    static
    void testVectorCastFloatToLong(VectorSpecies<Float> a, VectorSpecies<Long> b, float[] input, long[] output) {
        assert(input.length == a.length());
        assert(output.length == b.length());

        FloatVector av = FloatVector.fromArray(a, input, 0);
        LongVector bv = (LongVector) av.castShape(b, 0);
        bv.intoArray(output, 0);

        for (int i = 0; i < Math.min(input.length, output.length); i++) {
            Assert.assertEquals(output[i], (long)input[i]);
        }
        for(int i = input.length; i < output.length; i++) {
            Assert.assertEquals(output[i], (long)0);
        }
    }

    @ForceInline
    static
    void testVectorCastFloatToLongFail(VectorSpecies<Float> a, VectorSpecies<Long> b, float[] input) {
        assert(input.length == a.length());

        FloatVector av = FloatVector.fromArray(a, input, 0);
        try {
            av.castShape(b, 0);
            Assert.fail(String.format(
                    "Cast failed to throw ClassCastException for differing species lengths for %s and %s",
                    a, b));
        } catch (ClassCastException e) {
        }
    }

    @ForceInline
    static
    void testVectorCastDoubleToLong(VectorSpecies<Double> a, VectorSpecies<Long> b, double[] input, long[] output) {
        assert(input.length == a.length());
        assert(output.length == b.length());

        DoubleVector av = DoubleVector.fromArray(a, input, 0);
        LongVector bv = (LongVector) av.castShape(b, 0);
        bv.intoArray(output, 0);

        for (int i = 0; i < Math.min(input.length, output.length); i++) {
            Assert.assertEquals(output[i], (long)input[i]);
        }
        for(int i = input.length; i < output.length; i++) {
            Assert.assertEquals(output[i], (long)0);
        }
    }

    @ForceInline
    static
    void testVectorCastDoubleToLongFail(VectorSpecies<Double> a, VectorSpecies<Long> b, double[] input) {
        assert(input.length == a.length());

        DoubleVector av = DoubleVector.fromArray(a, input, 0);
        try {
            av.castShape(b, 0);
            Assert.fail(String.format(
                    "Cast failed to throw ClassCastException for differing species lengths for %s and %s",
                    a, b));
        } catch (ClassCastException e) {
        }
    }

    @ForceInline
    static
    void testVectorCastByteToDouble(VectorSpecies<Byte> a, VectorSpecies<Double> b, byte[] input, double[] output) {
        assert(input.length == a.length());
        assert(output.length == b.length());

        ByteVector av = ByteVector.fromArray(a, input, 0);
        DoubleVector bv = (DoubleVector) av.castShape(b, 0);
        bv.intoArray(output, 0);

        for (int i = 0; i < Math.min(input.length, output.length); i++) {
            Assert.assertEquals(output[i], (double)input[i]);
        }
        for(int i = input.length; i < output.length; i++) {
            Assert.assertEquals(output[i], (double)0);
        }
    }

    @ForceInline
    static
    void testVectorCastByteToDoubleFail(VectorSpecies<Byte> a, VectorSpecies<Double> b, byte[] input) {
        assert(input.length == a.length());

        ByteVector av = ByteVector.fromArray(a, input, 0);
        try {
            av.castShape(b, 0);
            Assert.fail(String.format(
                    "Cast failed to throw ClassCastException for differing species lengths for %s and %s",
                    a, b));
        } catch (ClassCastException e) {
        }
    }

    @ForceInline
    static
    void testVectorCastShortToDouble(VectorSpecies<Short> a, VectorSpecies<Double> b, short[] input, double[] output) {
        assert(input.length == a.length());
        assert(output.length == b.length());

        ShortVector av = ShortVector.fromArray(a, input, 0);
        DoubleVector bv = (DoubleVector) av.castShape(b, 0);
        bv.intoArray(output, 0);

        for (int i = 0; i < Math.min(input.length, output.length); i++) {
            Assert.assertEquals(output[i], (double)input[i]);
        }
        for(int i = input.length; i < output.length; i++) {
            Assert.assertEquals(output[i], (double)0);
        }
    }

    @ForceInline
    static
    void testVectorCastShortToDoubleFail(VectorSpecies<Short> a, VectorSpecies<Double> b, short[] input) {
        assert(input.length == a.length());

        ShortVector av = ShortVector.fromArray(a, input, 0);
        try {
            av.castShape(b, 0);
            Assert.fail(String.format(
                    "Cast failed to throw ClassCastException for differing species lengths for %s and %s",
                    a, b));
        } catch (ClassCastException e) {
        }
    }

    @ForceInline
    static
    void testVectorCastIntToDouble(VectorSpecies<Integer> a, VectorSpecies<Double> b, int[] input, double[] output) {
        assert(input.length == a.length());
        assert(output.length == b.length());

        IntVector av = IntVector.fromArray(a, input, 0);
        DoubleVector bv = (DoubleVector) av.castShape(b, 0);
        bv.intoArray(output, 0);

        for (int i = 0; i < Math.min(input.length, output.length); i++) {
            Assert.assertEquals(output[i], (double)input[i]);
        }
        for(int i = input.length; i < output.length; i++) {
            Assert.assertEquals(output[i], (double)0);
        }
    }

    @ForceInline
    static
    void testVectorCastIntToDoubleFail(VectorSpecies<Integer> a, VectorSpecies<Double> b, int[] input) {
        assert(input.length == a.length());

        IntVector av = IntVector.fromArray(a, input, 0);
        try {
            av.castShape(b, 0);
            Assert.fail(String.format(
                    "Cast failed to throw ClassCastException for differing species lengths for %s and %s",
                    a, b));
        } catch (ClassCastException e) {
        }
    }

    @ForceInline
    static
    void testVectorCastLongToDouble(VectorSpecies<Long> a, VectorSpecies<Double> b, long[] input, double[] output) {
        assert(input.length == a.length());
        assert(output.length == b.length());

        LongVector av = LongVector.fromArray(a, input, 0);
        DoubleVector bv = (DoubleVector) av.castShape(b, 0);
        bv.intoArray(output, 0);

        for (int i = 0; i < Math.min(input.length, output.length); i++) {
            Assert.assertEquals(output[i], (double)input[i]);
        }
        for(int i = input.length; i < output.length; i++) {
            Assert.assertEquals(output[i], (double)0);
        }
    }

    @ForceInline
    static
    void testVectorCastLongToDoubleFail(VectorSpecies<Long> a, VectorSpecies<Double> b, long[] input) {
        assert(input.length == a.length());

        LongVector av = LongVector.fromArray(a, input, 0);
        try {
            av.castShape(b, 0);
            Assert.fail(String.format(
                    "Cast failed to throw ClassCastException for differing species lengths for %s and %s",
                    a, b));
        } catch (ClassCastException e) {
        }
    }

    @ForceInline
    static
    void testVectorCastFloatToDouble(VectorSpecies<Float> a, VectorSpecies<Double> b, float[] input, double[] output) {
        assert(input.length == a.length());
        assert(output.length == b.length());

        FloatVector av = FloatVector.fromArray(a, input, 0);
        DoubleVector bv = (DoubleVector) av.castShape(b, 0);
        bv.intoArray(output, 0);

        for (int i = 0; i < Math.min(input.length, output.length); i++) {
            Assert.assertEquals(output[i], (double)input[i]);
        }
        for(int i = input.length; i < output.length; i++) {
            Assert.assertEquals(output[i], (double)0);
        }
    }

    @ForceInline
    static
    void testVectorCastFloatToDoubleFail(VectorSpecies<Float> a, VectorSpecies<Double> b, float[] input) {
        assert(input.length == a.length());

        FloatVector av = FloatVector.fromArray(a, input, 0);
        try {
            av.castShape(b, 0);
            Assert.fail(String.format(
                    "Cast failed to throw ClassCastException for differing species lengths for %s and %s",
                    a, b));
        } catch (ClassCastException e) {
        }
    }

    @ForceInline
    static
    void testVectorCastDoubleToDouble(VectorSpecies<Double> a, VectorSpecies<Double> b, double[] input, double[] output) {
        assert(input.length == a.length());
        assert(output.length == b.length());

        DoubleVector av = DoubleVector.fromArray(a, input, 0);
        DoubleVector bv = (DoubleVector) av.castShape(b, 0);
        bv.intoArray(output, 0);

        for (int i = 0; i < Math.min(input.length, output.length); i++) {
            Assert.assertEquals(output[i], (double)input[i]);
        }
        for(int i = input.length; i < output.length; i++) {
            Assert.assertEquals(output[i], (double)0);
        }
    }

    @ForceInline
    static
    void testVectorCastDoubleToDoubleFail(VectorSpecies<Double> a, VectorSpecies<Double> b, double[] input) {
        assert(input.length == a.length());

        DoubleVector av = DoubleVector.fromArray(a, input, 0);
        try {
            av.castShape(b, 0);
            Assert.fail(String.format(
                    "Cast failed to throw ClassCastException for differing species lengths for %s and %s",
                    a, b));
        } catch (ClassCastException e) {
        }
    }

    @Test(dataProvider = "byteUnaryOpProvider")
    static void testCastFromByte(IntFunction<byte[]> fa) {
        byte[] bin64 = fa.apply(bspec64.length());
        byte[] bin128 = fa.apply(bspec128.length());
        byte[] bin256 = fa.apply(bspec256.length());
        byte[] bin512 = fa.apply(bspec512.length());

        byte[] bout64 = new byte[bspec64.length()];
        byte[] bout128 = new byte[bspec128.length()];
        byte[] bout256 = new byte[bspec256.length()];
        byte[] bout512 = new byte[bspec512.length()];

        short[] sout64 = new short[sspec64.length()];
        short[] sout128 = new short[sspec128.length()];
        short[] sout256 = new short[sspec256.length()];
        short[] sout512 = new short[sspec512.length()];

        int[] iout64 = new int[ispec64.length()];
        int[] iout128 = new int[ispec128.length()];
        int[] iout256 = new int[ispec256.length()];
        int[] iout512 = new int[ispec512.length()];

        long[] lout64 = new long[lspec64.length()];
        long[] lout128 = new long[lspec128.length()];
        long[] lout256 = new long[lspec256.length()];
        long[] lout512 = new long[lspec512.length()];

        float[] fout64 = new float[fspec64.length()];
        float[] fout128 = new float[fspec128.length()];
        float[] fout256 = new float[fspec256.length()];
        float[] fout512 = new float[fspec512.length()];

        double[] dout64 = new double[dspec64.length()];
        double[] dout128 = new double[dspec128.length()];
        double[] dout256 = new double[dspec256.length()];
        double[] dout512 = new double[dspec512.length()];

        for (int i = 0; i < NUM_ITER; i++) {
            // B2B exact fit
            testVectorCastByteToByte(bspec64, bspec64, bin64, bout64);
            testVectorCastByteToByte(bspec128, bspec128, bin128, bout128);
            testVectorCastByteToByte(bspec256, bspec256, bin256, bout256);
            testVectorCastByteToByte(bspec512, bspec512, bin512, bout512);

            // B2B expansion
            testVectorCastByteToByte(bspec128, bspec64, bin128, bout64);
            testVectorCastByteToByte(bspec256, bspec128, bin256, bout128);
            testVectorCastByteToByte(bspec512, bspec256, bin512, bout256);

            testVectorCastByteToByte(bspec256, bspec64, bin256, bout64);
            testVectorCastByteToByte(bspec512, bspec128, bin512, bout128);

            testVectorCastByteToByte(bspec512, bspec64, bin512, bout64);

            // B2B contraction
            testVectorCastByteToByte(bspec64, bspec128, bin64, bout128);
            testVectorCastByteToByte(bspec128, bspec256, bin128, bout256);
            testVectorCastByteToByte(bspec256, bspec512, bin256, bout512);

            testVectorCastByteToByte(bspec64, bspec256, bin64, bout256);
            testVectorCastByteToByte(bspec128, bspec512, bin128, bout512);

            testVectorCastByteToByte(bspec64, bspec512, bin64, bout512);

            // B2S exact fit
            testVectorCastByteToShort(bspec64, sspec128, bin64, sout128);
            testVectorCastByteToShort(bspec128, sspec256, bin128, sout256);
            testVectorCastByteToShort(bspec256, sspec512, bin256, sout512);

            // B2S expansion
            testVectorCastByteToShort(bspec64, sspec64, bin64, sout64);
            testVectorCastByteToShort(bspec128, sspec128, bin128, sout128);
            testVectorCastByteToShort(bspec256, sspec256, bin256, sout256);
            testVectorCastByteToShort(bspec512, sspec512, bin512, sout512);

            testVectorCastByteToShort(bspec128, sspec64, bin128, sout64);
            testVectorCastByteToShort(bspec256, sspec128, bin256, sout128);
            testVectorCastByteToShort(bspec512, sspec256, bin512, sout256);

            testVectorCastByteToShort(bspec256, sspec64, bin256, sout64);
            testVectorCastByteToShort(bspec512, sspec128, bin512, sout128);

            testVectorCastByteToShort(bspec512, sspec64, bin512, sout64);

            // B2S contraction
            testVectorCastByteToShort(bspec64, sspec256, bin64, sout256);
            testVectorCastByteToShort(bspec128, sspec512, bin128, sout512);

            testVectorCastByteToShort(bspec64, sspec512, bin64, sout512);

            // B2I exact fit
            testVectorCastByteToInt(bspec64, ispec256, bin64, iout256);
            testVectorCastByteToInt(bspec128, ispec512, bin128, iout512);

            // B2L exact fit
            testVectorCastByteToLong(bspec64, lspec512, bin64, lout512);

            // B2F exact fit
            testVectorCastByteToFloat(bspec64, fspec256, bin64, fout256);
            testVectorCastByteToFloat(bspec128, fspec512, bin128, fout512);

            // B2D exact fit
            testVectorCastByteToDouble(bspec64, dspec512, bin64, dout512);

            // Previous failure tests.
            testVectorCastByteToByte(bspec64, bspec128, bin64, bout128);
            testVectorCastByteToByte(bspec64, bspec256, bin64, bout256);
            testVectorCastByteToByte(bspec64, bspec512, bin64, bout512);

            testVectorCastByteToByte(bspec128, bspec64, bin128, bout64);
            testVectorCastByteToByte(bspec128, bspec256, bin128, bout256);
            testVectorCastByteToByte(bspec128, bspec512, bin128, bout512);

            testVectorCastByteToByte(bspec256, bspec64, bin256, bout64);
            testVectorCastByteToByte(bspec256, bspec128, bin256, bout128);
            testVectorCastByteToByte(bspec256, bspec512, bin256, bout512);

            testVectorCastByteToByte(bspec512, bspec64, bin512, bout64);
            testVectorCastByteToByte(bspec512, bspec128, bin512, bout128);
            testVectorCastByteToByte(bspec512, bspec256, bin512, bout256);

            testVectorCastByteToShort(bspec64, sspec64, bin64, sout64);
            testVectorCastByteToShort(bspec64, sspec256, bin64, sout256);
            testVectorCastByteToShort(bspec64, sspec512, bin64, sout512);

            testVectorCastByteToShort(bspec128, sspec64, bin128, sout64);
            testVectorCastByteToShort(bspec128, sspec128, bin128, sout128);
            testVectorCastByteToShort(bspec128, sspec512, bin128, sout512);

            testVectorCastByteToShort(bspec256, sspec64, bin256, sout64);
            testVectorCastByteToShort(bspec256, sspec128, bin256, sout128);
            testVectorCastByteToShort(bspec256, sspec256, bin256, sout256);

            testVectorCastByteToShort(bspec512, sspec64, bin512, sout64);
            testVectorCastByteToShort(bspec512, sspec128, bin512, sout128);
            testVectorCastByteToShort(bspec512, sspec256, bin512, sout256);
            testVectorCastByteToShort(bspec512, sspec512, bin512, sout512);

            testVectorCastByteToInt(bspec64, ispec64, bin64, iout64);
            testVectorCastByteToInt(bspec64, ispec128, bin64, iout128);
            testVectorCastByteToInt(bspec64, ispec512, bin64, iout512);

            testVectorCastByteToInt(bspec128, ispec64, bin128, iout64);
            testVectorCastByteToInt(bspec128, ispec128, bin128, iout128);
            testVectorCastByteToInt(bspec128, ispec256, bin128, iout256);

            testVectorCastByteToInt(bspec256, ispec64, bin256, iout64);
            testVectorCastByteToInt(bspec256, ispec128, bin256, iout128);
            testVectorCastByteToInt(bspec256, ispec256, bin256, iout256);
            testVectorCastByteToInt(bspec256, ispec512, bin256, iout512);

            testVectorCastByteToInt(bspec512, ispec64, bin512, iout64);
            testVectorCastByteToInt(bspec512, ispec128, bin512, iout128);
            testVectorCastByteToInt(bspec512, ispec256, bin512, iout256);
            testVectorCastByteToInt(bspec512, ispec512, bin512, iout512);

            testVectorCastByteToLong(bspec64, lspec64, bin64, lout64);
            testVectorCastByteToLong(bspec64, lspec128, bin64, lout128);
            testVectorCastByteToLong(bspec64, lspec256, bin64, lout256);

            testVectorCastByteToLong(bspec128, lspec64, bin128, lout64);
            testVectorCastByteToLong(bspec128, lspec128, bin128, lout128);
            testVectorCastByteToLong(bspec128, lspec256, bin128, lout256);
            testVectorCastByteToLong(bspec128, lspec512, bin128, lout512);

            testVectorCastByteToLong(bspec256, lspec64, bin256, lout64);
            testVectorCastByteToLong(bspec256, lspec128, bin256, lout128);
            testVectorCastByteToLong(bspec256, lspec256, bin256, lout256);
            testVectorCastByteToLong(bspec256, lspec512, bin256, lout512);

            testVectorCastByteToLong(bspec512, lspec64, bin512, lout64);
            testVectorCastByteToLong(bspec512, lspec128, bin512, lout128);
            testVectorCastByteToLong(bspec512, lspec256, bin512, lout256);
            testVectorCastByteToLong(bspec512, lspec512, bin512, lout512);

            testVectorCastByteToFloat(bspec64, fspec64, bin64, fout64);
            testVectorCastByteToFloat(bspec64, fspec128, bin64, fout128);
            testVectorCastByteToFloat(bspec64, fspec512, bin64, fout512);

            testVectorCastByteToFloat(bspec128, fspec64, bin128, fout64);
            testVectorCastByteToFloat(bspec128, fspec128, bin128, fout128);
            testVectorCastByteToFloat(bspec128, fspec256, bin128, fout256);

            testVectorCastByteToFloat(bspec256, fspec64, bin256, fout64);
            testVectorCastByteToFloat(bspec256, fspec128, bin256, fout128);
            testVectorCastByteToFloat(bspec256, fspec256, bin256, fout256);
            testVectorCastByteToFloat(bspec256, fspec512, bin256, fout512);

            testVectorCastByteToFloat(bspec512, fspec64, bin512, fout64);
            testVectorCastByteToFloat(bspec512, fspec128, bin512, fout128);
            testVectorCastByteToFloat(bspec512, fspec256, bin512, fout256);
            testVectorCastByteToFloat(bspec512, fspec512, bin512, fout512);

            testVectorCastByteToDouble(bspec64, dspec64, bin64, dout64);
            testVectorCastByteToDouble(bspec64, dspec128, bin64, dout128);
            testVectorCastByteToDouble(bspec64, dspec256, bin64, dout256);

            testVectorCastByteToDouble(bspec128, dspec64, bin128, dout64);
            testVectorCastByteToDouble(bspec128, dspec128, bin128, dout128);
            testVectorCastByteToDouble(bspec128, dspec256, bin128, dout256);
            testVectorCastByteToDouble(bspec128, dspec512, bin128, dout512);

            testVectorCastByteToDouble(bspec256, dspec64, bin256, dout64);
            testVectorCastByteToDouble(bspec256, dspec128, bin256, dout128);
            testVectorCastByteToDouble(bspec256, dspec256, bin256, dout256);
            testVectorCastByteToDouble(bspec256, dspec512, bin256, dout512);

            testVectorCastByteToDouble(bspec512, dspec64, bin512, dout64);
            testVectorCastByteToDouble(bspec512, dspec128, bin512, dout128);
            testVectorCastByteToDouble(bspec512, dspec256, bin512, dout256);
            testVectorCastByteToDouble(bspec512, dspec512, bin512, dout512);
        }
    }

    @Test(dataProvider = "shortUnaryOpProvider")
    static void testCastFromShort(IntFunction<short[]> fa) {
        short[] sin64 = fa.apply(sspec64.length());
        short[] sin128 = fa.apply(sspec128.length());
        short[] sin256 = fa.apply(sspec256.length());
        short[] sin512 = fa.apply(sspec512.length());

        byte[] bout64 = new byte[bspec64.length()];
        byte[] bout128 = new byte[bspec128.length()];
        byte[] bout256 = new byte[bspec256.length()];

        short[] sout64 = new short[sspec64.length()];
        short[] sout128 = new short[sspec128.length()];
        short[] sout256 = new short[sspec256.length()];
        short[] sout512 = new short[sspec512.length()];

        int[] iout128 = new int[ispec128.length()];
        int[] iout256 = new int[ispec256.length()];
        int[] iout512 = new int[ispec512.length()];

        long[] lout256 = new long[lspec256.length()];
        long[] lout512 = new long[lspec512.length()];

        float[] fout128 = new float[fspec128.length()];
        float[] fout256 = new float[fspec256.length()];
        float[] fout512 = new float[fspec512.length()];

        double[] dout256 = new double[dspec256.length()];
        double[] dout512 = new double[dspec512.length()];

        for (int i = 0; i < NUM_ITER; i++) {
            testVectorCastShortToByte(sspec128, bspec64, sin128, bout64);
            testVectorCastShortToByte(sspec256, bspec128, sin256, bout128);
            testVectorCastShortToByte(sspec512, bspec256, sin512, bout256);

            testVectorCastShortToShort(sspec64, sspec64, sin64, sout64);
            testVectorCastShortToShort(sspec128, sspec128, sin128, sout128);
            testVectorCastShortToShort(sspec256, sspec256, sin256, sout256);
            testVectorCastShortToShort(sspec512, sspec512, sin512, sout512);

            testVectorCastShortToInt(sspec64, ispec128, sin64, iout128);
            testVectorCastShortToInt(sspec128, ispec256, sin128, iout256);
            testVectorCastShortToInt(sspec256, ispec512, sin256, iout512);

            testVectorCastShortToLong(sspec64, lspec256, sin64, lout256);
            testVectorCastShortToLong(sspec128, lspec512, sin128, lout512);

            testVectorCastShortToFloat(sspec64, fspec128, sin64, fout128);
            testVectorCastShortToFloat(sspec128, fspec256, sin128, fout256);
            testVectorCastShortToFloat(sspec256, fspec512, sin256, fout512);

            testVectorCastShortToDouble(sspec64, dspec256, sin64, dout256);
            testVectorCastShortToDouble(sspec128, dspec512, sin128, dout512);
        }
    }

    //@Test()
    static void testCastFromShortFail() {
        short[] sin64 = new short[sspec64.length()];
        short[] sin128 = new short[sspec128.length()];
        short[] sin256 = new short[sspec256.length()];
        short[] sin512 = new short[sspec512.length()];

        for (int i = 0; i < INVOC_COUNT; i++) {
            testVectorCastShortToByteFail(sspec64, bspec64, sin64);
            testVectorCastShortToByteFail(sspec64, bspec128, sin64);
            testVectorCastShortToByteFail(sspec64, bspec256, sin64);
            testVectorCastShortToByteFail(sspec64, bspec512, sin64);

            testVectorCastShortToByteFail(sspec128, bspec128, sin128);
            testVectorCastShortToByteFail(sspec128, bspec256, sin128);
            testVectorCastShortToByteFail(sspec128, bspec512, sin128);

            testVectorCastShortToByteFail(sspec256, bspec64, sin256);
            testVectorCastShortToByteFail(sspec256, bspec256, sin256);
            testVectorCastShortToByteFail(sspec256, bspec512, sin256);

            testVectorCastShortToByteFail(sspec512, bspec64, sin512);
            testVectorCastShortToByteFail(sspec512, bspec128, sin512);
            testVectorCastShortToByteFail(sspec512, bspec512, sin512);

            testVectorCastShortToShortFail(sspec64, sspec128, sin64);
            testVectorCastShortToShortFail(sspec64, sspec256, sin64);
            testVectorCastShortToShortFail(sspec64, sspec512, sin64);

            testVectorCastShortToShortFail(sspec128, sspec64, sin128);
            testVectorCastShortToShortFail(sspec128, sspec256, sin128);
            testVectorCastShortToShortFail(sspec128, sspec512, sin128);

            testVectorCastShortToShortFail(sspec256, sspec64, sin256);
            testVectorCastShortToShortFail(sspec256, sspec128, sin256);
            testVectorCastShortToShortFail(sspec256, sspec512, sin256);

            testVectorCastShortToShortFail(sspec512, sspec64, sin512);
            testVectorCastShortToShortFail(sspec512, sspec128, sin512);
            testVectorCastShortToShortFail(sspec512, sspec256, sin512);

            testVectorCastShortToIntFail(sspec64, ispec64, sin64);
            testVectorCastShortToIntFail(sspec64, ispec256, sin64);
            testVectorCastShortToIntFail(sspec64, ispec512, sin64);

            testVectorCastShortToIntFail(sspec128, ispec64, sin128);
            testVectorCastShortToIntFail(sspec128, ispec128, sin128);
            testVectorCastShortToIntFail(sspec128, ispec512, sin128);

            testVectorCastShortToIntFail(sspec256, ispec64, sin256);
            testVectorCastShortToIntFail(sspec256, ispec128, sin256);
            testVectorCastShortToIntFail(sspec256, ispec256, sin256);

            testVectorCastShortToIntFail(sspec512, ispec64, sin512);
            testVectorCastShortToIntFail(sspec512, ispec128, sin512);
            testVectorCastShortToIntFail(sspec512, ispec256, sin512);
            testVectorCastShortToIntFail(sspec512, ispec512, sin512);

            testVectorCastShortToLongFail(sspec64, lspec64, sin64);
            testVectorCastShortToLongFail(sspec64, lspec128, sin64);
            testVectorCastShortToLongFail(sspec64, lspec512, sin64);

            testVectorCastShortToLongFail(sspec128, lspec64, sin128);
            testVectorCastShortToLongFail(sspec128, lspec128, sin128);
            testVectorCastShortToLongFail(sspec128, lspec256, sin128);

            testVectorCastShortToLongFail(sspec256, lspec64, sin256);
            testVectorCastShortToLongFail(sspec256, lspec128, sin256);
            testVectorCastShortToLongFail(sspec256, lspec256, sin256);
            testVectorCastShortToLongFail(sspec256, lspec512, sin256);

            testVectorCastShortToLongFail(sspec512, lspec64, sin512);
            testVectorCastShortToLongFail(sspec512, lspec128, sin512);
            testVectorCastShortToLongFail(sspec512, lspec256, sin512);
            testVectorCastShortToLongFail(sspec512, lspec512, sin512);

            testVectorCastShortToFloatFail(sspec64, fspec64, sin64);
            testVectorCastShortToFloatFail(sspec64, fspec256, sin64);
            testVectorCastShortToFloatFail(sspec64, fspec512, sin64);

            testVectorCastShortToFloatFail(sspec128, fspec64, sin128);
            testVectorCastShortToFloatFail(sspec128, fspec128, sin128);
            testVectorCastShortToFloatFail(sspec128, fspec512, sin128);

            testVectorCastShortToFloatFail(sspec256, fspec64, sin256);
            testVectorCastShortToFloatFail(sspec256, fspec128, sin256);
            testVectorCastShortToFloatFail(sspec256, fspec256, sin256);

            testVectorCastShortToFloatFail(sspec512, fspec64, sin512);
            testVectorCastShortToFloatFail(sspec512, fspec128, sin512);
            testVectorCastShortToFloatFail(sspec512, fspec256, sin512);
            testVectorCastShortToFloatFail(sspec512, fspec512, sin512);

            testVectorCastShortToDoubleFail(sspec64, dspec64, sin64);
            testVectorCastShortToDoubleFail(sspec64, dspec128, sin64);
            testVectorCastShortToDoubleFail(sspec64, dspec512, sin64);

            testVectorCastShortToDoubleFail(sspec128, dspec64, sin128);
            testVectorCastShortToDoubleFail(sspec128, dspec128, sin128);
            testVectorCastShortToDoubleFail(sspec128, dspec256, sin128);

            testVectorCastShortToDoubleFail(sspec256, dspec64, sin256);
            testVectorCastShortToDoubleFail(sspec256, dspec128, sin256);
            testVectorCastShortToDoubleFail(sspec256, dspec256, sin256);
            testVectorCastShortToDoubleFail(sspec256, dspec512, sin256);

            testVectorCastShortToDoubleFail(sspec512, dspec64, sin512);
            testVectorCastShortToDoubleFail(sspec512, dspec128, sin512);
            testVectorCastShortToDoubleFail(sspec512, dspec256, sin512);
            testVectorCastShortToDoubleFail(sspec512, dspec512, sin512);
        }
    }

    @Test(dataProvider = "intUnaryOpProvider")
    static void testCastFromInt(IntFunction<int[]> fa) {
        int[] iin64 = fa.apply(ispec64.length());
        int[] iin128 = fa.apply(ispec128.length());
        int[] iin256 = fa.apply(ispec256.length());
        int[] iin512 = fa.apply(ispec512.length());

        byte[] bout64 = new byte[bspec64.length()];
        byte[] bout128 = new byte[bspec128.length()];

        short[] sout64 = new short[sspec64.length()];
        short[] sout128 = new short[sspec128.length()];
        short[] sout256 = new short[sspec256.length()];

        int[] iout64 = new int[ispec64.length()];
        int[] iout128 = new int[ispec128.length()];
        int[] iout256 = new int[ispec256.length()];
        int[] iout512 = new int[ispec512.length()];

        long[] lout128 = new long[lspec128.length()];
        long[] lout256 = new long[lspec256.length()];
        long[] lout512 = new long[lspec512.length()];

        float[] fout64 = new float[fspec64.length()];
        float[] fout128 = new float[fspec128.length()];
        float[] fout256 = new float[fspec256.length()];
        float[] fout512 = new float[fspec512.length()];

        double[] dout128 = new double[dspec128.length()];
        double[] dout256 = new double[dspec256.length()];
        double[] dout512 = new double[dspec512.length()];

        for (int i = 0; i < NUM_ITER; i++) {
            testVectorCastIntToByte(ispec256, bspec64, iin256, bout64);
            testVectorCastIntToByte(ispec512, bspec128, iin512, bout128);

            testVectorCastIntToShort(ispec128, sspec64, iin128, sout64);
            testVectorCastIntToShort(ispec256, sspec128, iin256, sout128);
            testVectorCastIntToShort(ispec512, sspec256, iin512, sout256);

            testVectorCastIntToInt(ispec64, ispec64, iin64, iout64);
            testVectorCastIntToInt(ispec128, ispec128, iin128, iout128);
            testVectorCastIntToInt(ispec256, ispec256, iin256, iout256);
            testVectorCastIntToInt(ispec512, ispec512, iin512, iout512);

            testVectorCastIntToLong(ispec64, lspec128, iin64, lout128);
            testVectorCastIntToLong(ispec128, lspec256, iin128, lout256);
            testVectorCastIntToLong(ispec256, lspec512, iin256, lout512);

            testVectorCastIntToFloat(ispec64, fspec64, iin64, fout64);
            testVectorCastIntToFloat(ispec128, fspec128, iin128, fout128);
            testVectorCastIntToFloat(ispec256, fspec256, iin256, fout256);
            testVectorCastIntToFloat(ispec512, fspec512, iin512, fout512);

            testVectorCastIntToDouble(ispec64, dspec128, iin64, dout128);
            testVectorCastIntToDouble(ispec128, dspec256, iin128, dout256);
            testVectorCastIntToDouble(ispec256, dspec512, iin256, dout512);
        }
    }

    //@Test
    static void testCastFromIntFail() {
        int[] iin64 = new int[ispec64.length()];
        int[] iin128 = new int[ispec128.length()];
        int[] iin256 = new int[ispec256.length()];
        int[] iin512 = new int[ispec512.length()];

        for (int i = 0; i < INVOC_COUNT; i++) {
            testVectorCastIntToByteFail(ispec64, bspec64, iin64);
            testVectorCastIntToByteFail(ispec64, bspec128, iin64);
            testVectorCastIntToByteFail(ispec64, bspec256, iin64);
            testVectorCastIntToByteFail(ispec64, bspec512, iin64);

            testVectorCastIntToByteFail(ispec128, bspec64, iin128);
            testVectorCastIntToByteFail(ispec128, bspec128, iin128);
            testVectorCastIntToByteFail(ispec128, bspec256, iin128);
            testVectorCastIntToByteFail(ispec128, bspec512, iin128);

            testVectorCastIntToByteFail(ispec256, bspec128, iin256);
            testVectorCastIntToByteFail(ispec256, bspec256, iin256);
            testVectorCastIntToByteFail(ispec256, bspec512, iin256);

            testVectorCastIntToByteFail(ispec512, bspec64, iin512);
            testVectorCastIntToByteFail(ispec512, bspec256, iin512);
            testVectorCastIntToByteFail(ispec512, bspec512, iin512);

            testVectorCastIntToShortFail(ispec64, sspec64, iin64);
            testVectorCastIntToShortFail(ispec64, sspec128, iin64);
            testVectorCastIntToShortFail(ispec64, sspec256, iin64);
            testVectorCastIntToShortFail(ispec64, sspec512, iin64);

            testVectorCastIntToShortFail(ispec128, sspec128, iin128);
            testVectorCastIntToShortFail(ispec128, sspec256, iin128);
            testVectorCastIntToShortFail(ispec128, sspec512, iin128);

            testVectorCastIntToShortFail(ispec256, sspec64, iin256);
            testVectorCastIntToShortFail(ispec256, sspec256, iin256);
            testVectorCastIntToShortFail(ispec256, sspec512, iin256);

            testVectorCastIntToShortFail(ispec512, sspec64, iin512);
            testVectorCastIntToShortFail(ispec512, sspec128, iin512);
            testVectorCastIntToShortFail(ispec512, sspec512, iin512);

            testVectorCastIntToIntFail(ispec64, ispec128, iin64);
            testVectorCastIntToIntFail(ispec64, ispec256, iin64);
            testVectorCastIntToIntFail(ispec64, ispec512, iin64);

            testVectorCastIntToIntFail(ispec128, ispec64, iin128);
            testVectorCastIntToIntFail(ispec128, ispec256, iin128);
            testVectorCastIntToIntFail(ispec128, ispec512, iin128);

            testVectorCastIntToIntFail(ispec256, ispec64, iin256);
            testVectorCastIntToIntFail(ispec256, ispec128, iin256);
            testVectorCastIntToIntFail(ispec256, ispec512, iin256);

            testVectorCastIntToIntFail(ispec512, ispec64, iin512);
            testVectorCastIntToIntFail(ispec512, ispec128, iin512);
            testVectorCastIntToIntFail(ispec512, ispec256, iin512);

            testVectorCastIntToLongFail(ispec64, lspec64, iin64);
            testVectorCastIntToLongFail(ispec64, lspec256, iin64);
            testVectorCastIntToLongFail(ispec64, lspec512, iin64);

            testVectorCastIntToLongFail(ispec128, lspec64, iin128);
            testVectorCastIntToLongFail(ispec128, lspec128, iin128);
            testVectorCastIntToLongFail(ispec128, lspec512, iin128);

            testVectorCastIntToLongFail(ispec256, lspec64, iin256);
            testVectorCastIntToLongFail(ispec256, lspec128, iin256);
            testVectorCastIntToLongFail(ispec256, lspec256, iin256);

            testVectorCastIntToLongFail(ispec512, lspec64, iin512);
            testVectorCastIntToLongFail(ispec512, lspec128, iin512);
            testVectorCastIntToLongFail(ispec512, lspec256, iin512);
            testVectorCastIntToLongFail(ispec512, lspec512, iin512);

            testVectorCastIntToFloatFail(ispec64, fspec128, iin64);
            testVectorCastIntToFloatFail(ispec64, fspec256, iin64);
            testVectorCastIntToFloatFail(ispec64, fspec512, iin64);

            testVectorCastIntToFloatFail(ispec128, fspec64, iin128);
            testVectorCastIntToFloatFail(ispec128, fspec256, iin128);
            testVectorCastIntToFloatFail(ispec128, fspec512, iin128);

            testVectorCastIntToFloatFail(ispec256, fspec64, iin256);
            testVectorCastIntToFloatFail(ispec256, fspec128, iin256);
            testVectorCastIntToFloatFail(ispec256, fspec512, iin256);

            testVectorCastIntToFloatFail(ispec512, fspec64, iin512);
            testVectorCastIntToFloatFail(ispec512, fspec128, iin512);
            testVectorCastIntToFloatFail(ispec512, fspec256, iin512);

            testVectorCastIntToDoubleFail(ispec64, dspec64, iin64);
            testVectorCastIntToDoubleFail(ispec64, dspec256, iin64);
            testVectorCastIntToDoubleFail(ispec64, dspec512, iin64);

            testVectorCastIntToDoubleFail(ispec128, dspec64, iin128);
            testVectorCastIntToDoubleFail(ispec128, dspec128, iin128);
            testVectorCastIntToDoubleFail(ispec128, dspec512, iin128);

            testVectorCastIntToDoubleFail(ispec256, dspec64, iin256);
            testVectorCastIntToDoubleFail(ispec256, dspec128, iin256);
            testVectorCastIntToDoubleFail(ispec256, dspec256, iin256);

            testVectorCastIntToDoubleFail(ispec512, dspec64, iin512);
            testVectorCastIntToDoubleFail(ispec512, dspec128, iin512);
            testVectorCastIntToDoubleFail(ispec512, dspec256, iin512);
            testVectorCastIntToDoubleFail(ispec512, dspec512, iin512);
        }
    }

    @Test(dataProvider = "longUnaryOpProvider")
    static void testCastFromLong(IntFunction<long[]> fa) {
        long[] lin64 = fa.apply(lspec64.length());
        long[] lin128 = fa.apply(lspec128.length());
        long[] lin256 = fa.apply(lspec256.length());
        long[] lin512 = fa.apply(lspec512.length());

        byte[] bout64 = new byte[bspec64.length()];

        short[] sout64 = new short[sspec64.length()];
        short[] sout128 = new short[sspec128.length()];

        int[] iout64 = new int[ispec64.length()];
        int[] iout128 = new int[ispec128.length()];
        int[] iout256 = new int[ispec256.length()];

        long[] lout64 = new long[lspec64.length()];
        long[] lout128 = new long[lspec128.length()];
        long[] lout256 = new long[lspec256.length()];
        long[] lout512 = new long[lspec512.length()];

        float[] fout64 = new float[fspec64.length()];
        float[] fout128 = new float[fspec128.length()];
        float[] fout256 = new float[fspec256.length()];

        double[] dout64 = new double[dspec64.length()];
        double[] dout128 = new double[dspec128.length()];
        double[] dout256 = new double[dspec256.length()];
        double[] dout512 = new double[dspec512.length()];

        for (int i = 0; i < NUM_ITER; i++) {
            testVectorCastLongToByte(lspec512, bspec64, lin512, bout64);

            testVectorCastLongToShort(lspec256, sspec64, lin256, sout64);
            testVectorCastLongToShort(lspec512, sspec128, lin512, sout128);

            testVectorCastLongToInt(lspec128, ispec64, lin128, iout64);
            testVectorCastLongToInt(lspec256, ispec128, lin256, iout128);
            testVectorCastLongToInt(lspec512, ispec256, lin512, iout256);

            testVectorCastLongToLong(lspec64, lspec64, lin64, lout64);
            testVectorCastLongToLong(lspec128, lspec128, lin128, lout128);
            testVectorCastLongToLong(lspec256, lspec256, lin256, lout256);
            testVectorCastLongToLong(lspec512, lspec512, lin512, lout512);

            testVectorCastLongToFloat(lspec128, fspec64, lin128, fout64);
            testVectorCastLongToFloat(lspec256, fspec128, lin256, fout128);
            testVectorCastLongToFloat(lspec512, fspec256, lin512, fout256);

            testVectorCastLongToDouble(lspec64, dspec64, lin64, dout64);
            testVectorCastLongToDouble(lspec128, dspec128, lin128, dout128);
            testVectorCastLongToDouble(lspec256, dspec256, lin256, dout256);
            testVectorCastLongToDouble(lspec512, dspec512, lin512, dout512);
        }
    }

    //@Test
    static void testCastFromLongFail() {
        long[] lin64 = new long[lspec64.length()];
        long[] lin128 = new long[lspec128.length()];
        long[] lin256 = new long[lspec256.length()];
        long[] lin512 = new long[lspec512.length()];

        for (int i = 0; i < INVOC_COUNT; i++) {
            testVectorCastLongToByteFail(lspec64, bspec64, lin64);
            testVectorCastLongToByteFail(lspec64, bspec128, lin64);
            testVectorCastLongToByteFail(lspec64, bspec256, lin64);
            testVectorCastLongToByteFail(lspec64, bspec512, lin64);

            testVectorCastLongToByteFail(lspec128, bspec64, lin128);
            testVectorCastLongToByteFail(lspec128, bspec128, lin128);
            testVectorCastLongToByteFail(lspec128, bspec256, lin128);
            testVectorCastLongToByteFail(lspec128, bspec512, lin128);

            testVectorCastLongToByteFail(lspec256, bspec64, lin256);
            testVectorCastLongToByteFail(lspec256, bspec128, lin256);
            testVectorCastLongToByteFail(lspec256, bspec256, lin256);
            testVectorCastLongToByteFail(lspec256, bspec512, lin256);

            testVectorCastLongToByteFail(lspec512, bspec128, lin512);
            testVectorCastLongToByteFail(lspec512, bspec256, lin512);
            testVectorCastLongToByteFail(lspec512, bspec512, lin512);

            testVectorCastLongToShortFail(lspec64, sspec64, lin64);
            testVectorCastLongToShortFail(lspec64, sspec128, lin64);
            testVectorCastLongToShortFail(lspec64, sspec256, lin64);
            testVectorCastLongToShortFail(lspec64, sspec512, lin64);

            testVectorCastLongToShortFail(lspec128, sspec64, lin128);
            testVectorCastLongToShortFail(lspec128, sspec128, lin128);
            testVectorCastLongToShortFail(lspec128, sspec256, lin128);
            testVectorCastLongToShortFail(lspec128, sspec512, lin128);

            testVectorCastLongToShortFail(lspec256, sspec128, lin256);
            testVectorCastLongToShortFail(lspec256, sspec256, lin256);
            testVectorCastLongToShortFail(lspec256, sspec512, lin256);

            testVectorCastLongToShortFail(lspec512, sspec64, lin512);
            testVectorCastLongToShortFail(lspec512, sspec256, lin512);
            testVectorCastLongToShortFail(lspec512, sspec512, lin512);

            testVectorCastLongToIntFail(lspec64, ispec64, lin64);
            testVectorCastLongToIntFail(lspec64, ispec128, lin64);
            testVectorCastLongToIntFail(lspec64, ispec256, lin64);
            testVectorCastLongToIntFail(lspec64, ispec512, lin64);

            testVectorCastLongToIntFail(lspec128, ispec128, lin128);
            testVectorCastLongToIntFail(lspec128, ispec256, lin128);
            testVectorCastLongToIntFail(lspec128, ispec512, lin128);

            testVectorCastLongToIntFail(lspec256, ispec64, lin256);
            testVectorCastLongToIntFail(lspec256, ispec256, lin256);
            testVectorCastLongToIntFail(lspec256, ispec512, lin256);

            testVectorCastLongToIntFail(lspec512, ispec64, lin512);
            testVectorCastLongToIntFail(lspec512, ispec128, lin512);
            testVectorCastLongToIntFail(lspec512, ispec512, lin512);

            testVectorCastLongToLongFail(lspec64, lspec128, lin64);
            testVectorCastLongToLongFail(lspec64, lspec256, lin64);
            testVectorCastLongToLongFail(lspec64, lspec512, lin64);

            testVectorCastLongToLongFail(lspec128, lspec64, lin128);
            testVectorCastLongToLongFail(lspec128, lspec256, lin128);
            testVectorCastLongToLongFail(lspec128, lspec512, lin128);

            testVectorCastLongToLongFail(lspec256, lspec64, lin256);
            testVectorCastLongToLongFail(lspec256, lspec128, lin256);
            testVectorCastLongToLongFail(lspec256, lspec512, lin256);

            testVectorCastLongToLongFail(lspec512, lspec64, lin512);
            testVectorCastLongToLongFail(lspec512, lspec128, lin512);
            testVectorCastLongToLongFail(lspec512, lspec256, lin512);

            testVectorCastLongToFloatFail(lspec64, fspec64, lin64);
            testVectorCastLongToFloatFail(lspec64, fspec128, lin64);
            testVectorCastLongToFloatFail(lspec64, fspec256, lin64);
            testVectorCastLongToFloatFail(lspec64, fspec512, lin64);

            testVectorCastLongToFloatFail(lspec128, fspec128, lin128);
            testVectorCastLongToFloatFail(lspec128, fspec256, lin128);
            testVectorCastLongToFloatFail(lspec128, fspec512, lin128);

            testVectorCastLongToFloatFail(lspec256, fspec64, lin256);
            testVectorCastLongToFloatFail(lspec256, fspec256, lin256);
            testVectorCastLongToFloatFail(lspec256, fspec512, lin256);

            testVectorCastLongToFloatFail(lspec512, fspec64, lin512);
            testVectorCastLongToFloatFail(lspec512, fspec128, lin512);
            testVectorCastLongToFloatFail(lspec512, fspec512, lin512);

            testVectorCastLongToDoubleFail(lspec64, dspec128, lin64);
            testVectorCastLongToDoubleFail(lspec64, dspec256, lin64);
            testVectorCastLongToDoubleFail(lspec64, dspec512, lin64);

            testVectorCastLongToDoubleFail(lspec128, dspec64, lin128);
            testVectorCastLongToDoubleFail(lspec128, dspec256, lin128);
            testVectorCastLongToDoubleFail(lspec128, dspec512, lin128);

            testVectorCastLongToDoubleFail(lspec256, dspec64, lin256);
            testVectorCastLongToDoubleFail(lspec256, dspec128, lin256);
            testVectorCastLongToDoubleFail(lspec256, dspec512, lin256);

            testVectorCastLongToDoubleFail(lspec512, dspec64, lin512);
            testVectorCastLongToDoubleFail(lspec512, dspec128, lin512);
            testVectorCastLongToDoubleFail(lspec512, dspec256, lin512);
        }
    }

    @Test(dataProvider = "floatUnaryOpProvider")
    static void testCastFromFloat(IntFunction<float[]> fa) {
        float[] fin64 = fa.apply(fspec64.length());
        float[] fin128 = fa.apply(fspec128.length());
        float[] fin256 = fa.apply(fspec256.length());
        float[] fin512 = fa.apply(fspec512.length());

        byte[] bout64 = new byte[bspec64.length()];
        byte[] bout128 = new byte[bspec128.length()];

        short[] sout64 = new short[sspec64.length()];
        short[] sout128 = new short[sspec128.length()];
        short[] sout256 = new short[sspec256.length()];

        int[] iout64 = new int[ispec64.length()];
        int[] iout128 = new int[ispec128.length()];
        int[] iout256 = new int[ispec256.length()];
        int[] iout512 = new int[ispec512.length()];

        long[] lout128 = new long[lspec128.length()];
        long[] lout256 = new long[lspec256.length()];
        long[] lout512 = new long[lspec512.length()];

        float[] fout64 = new float[fspec64.length()];
        float[] fout128 = new float[fspec128.length()];
        float[] fout256 = new float[fspec256.length()];
        float[] fout512 = new float[fspec512.length()];

        double[] dout128 = new double[dspec128.length()];
        double[] dout256 = new double[dspec256.length()];
        double[] dout512 = new double[dspec512.length()];

        for (int i = 0; i < NUM_ITER; i++) {
            testVectorCastFloatToByte(fspec256, bspec64, fin256, bout64);
            testVectorCastFloatToByte(fspec512, bspec128, fin512, bout128);

            testVectorCastFloatToShort(fspec128, sspec64, fin128, sout64);
            testVectorCastFloatToShort(fspec256, sspec128, fin256, sout128);
            testVectorCastFloatToShort(fspec512, sspec256, fin512, sout256);

            testVectorCastFloatToInt(fspec64, ispec64, fin64, iout64);
            testVectorCastFloatToInt(fspec128, ispec128, fin128, iout128);
            testVectorCastFloatToInt(fspec256, ispec256, fin256, iout256);
            testVectorCastFloatToInt(fspec512, ispec512, fin512, iout512);

            testVectorCastFloatToLong(fspec64, lspec128, fin64, lout128);
            testVectorCastFloatToLong(fspec128, lspec256, fin128, lout256);
            testVectorCastFloatToLong(fspec256, lspec512, fin256, lout512);

            testVectorCastFloatToFloat(fspec64, fspec64, fin64, fout64);
            testVectorCastFloatToFloat(fspec128, fspec128, fin128, fout128);
            testVectorCastFloatToFloat(fspec256, fspec256, fin256, fout256);
            testVectorCastFloatToFloat(fspec512, fspec512, fin512, fout512);

            testVectorCastFloatToDouble(fspec64, dspec128, fin64, dout128);
            testVectorCastFloatToDouble(fspec128, dspec256, fin128, dout256);
            testVectorCastFloatToDouble(fspec256, dspec512, fin256, dout512);
        }
    }

    //@Test
    static void testCastFromFloatFail() {
        float[] fin64 = new float[fspec64.length()];
        float[] fin128 = new float[fspec128.length()];
        float[] fin256 = new float[fspec256.length()];
        float[] fin512 = new float[fspec512.length()];

        for (int i = 0; i < INVOC_COUNT; i++) {
            testVectorCastFloatToByteFail(fspec64, bspec64, fin64);
            testVectorCastFloatToByteFail(fspec64, bspec128, fin64);
            testVectorCastFloatToByteFail(fspec64, bspec256, fin64);
            testVectorCastFloatToByteFail(fspec64, bspec512, fin64);

            testVectorCastFloatToByteFail(fspec128, bspec64, fin128);
            testVectorCastFloatToByteFail(fspec128, bspec128, fin128);
            testVectorCastFloatToByteFail(fspec128, bspec256, fin128);
            testVectorCastFloatToByteFail(fspec128, bspec512, fin128);

            testVectorCastFloatToByteFail(fspec256, bspec128, fin256);
            testVectorCastFloatToByteFail(fspec256, bspec256, fin256);
            testVectorCastFloatToByteFail(fspec256, bspec512, fin256);

            testVectorCastFloatToByteFail(fspec512, bspec64, fin512);
            testVectorCastFloatToByteFail(fspec512, bspec256, fin512);
            testVectorCastFloatToByteFail(fspec512, bspec512, fin512);

            testVectorCastFloatToShortFail(fspec64, sspec64, fin64);
            testVectorCastFloatToShortFail(fspec64, sspec128, fin64);
            testVectorCastFloatToShortFail(fspec64, sspec256, fin64);
            testVectorCastFloatToShortFail(fspec64, sspec512, fin64);

            testVectorCastFloatToShortFail(fspec128, sspec128, fin128);
            testVectorCastFloatToShortFail(fspec128, sspec256, fin128);
            testVectorCastFloatToShortFail(fspec128, sspec512, fin128);

            testVectorCastFloatToShortFail(fspec256, sspec64, fin256);
            testVectorCastFloatToShortFail(fspec256, sspec256, fin256);
            testVectorCastFloatToShortFail(fspec256, sspec512, fin256);

            testVectorCastFloatToShortFail(fspec512, sspec64, fin512);
            testVectorCastFloatToShortFail(fspec512, sspec128, fin512);
            testVectorCastFloatToShortFail(fspec512, sspec512, fin512);

            testVectorCastFloatToIntFail(fspec64, ispec128, fin64);
            testVectorCastFloatToIntFail(fspec64, ispec256, fin64);
            testVectorCastFloatToIntFail(fspec64, ispec512, fin64);

            testVectorCastFloatToIntFail(fspec128, ispec64, fin128);
            testVectorCastFloatToIntFail(fspec128, ispec256, fin128);
            testVectorCastFloatToIntFail(fspec128, ispec512, fin128);

            testVectorCastFloatToIntFail(fspec256, ispec64, fin256);
            testVectorCastFloatToIntFail(fspec256, ispec128, fin256);
            testVectorCastFloatToIntFail(fspec256, ispec512, fin256);

            testVectorCastFloatToIntFail(fspec512, ispec64, fin512);
            testVectorCastFloatToIntFail(fspec512, ispec128, fin512);
            testVectorCastFloatToIntFail(fspec512, ispec256, fin512);

            testVectorCastFloatToLongFail(fspec64, lspec64, fin64);
            testVectorCastFloatToLongFail(fspec64, lspec256, fin64);
            testVectorCastFloatToLongFail(fspec64, lspec512, fin64);

            testVectorCastFloatToLongFail(fspec128, lspec64, fin128);
            testVectorCastFloatToLongFail(fspec128, lspec128, fin128);
            testVectorCastFloatToLongFail(fspec128, lspec512, fin128);

            testVectorCastFloatToLongFail(fspec256, lspec64, fin256);
            testVectorCastFloatToLongFail(fspec256, lspec128, fin256);
            testVectorCastFloatToLongFail(fspec256, lspec256, fin256);

            testVectorCastFloatToLongFail(fspec512, lspec64, fin512);
            testVectorCastFloatToLongFail(fspec512, lspec128, fin512);
            testVectorCastFloatToLongFail(fspec512, lspec256, fin512);
            testVectorCastFloatToLongFail(fspec512, lspec512, fin512);

            testVectorCastFloatToFloatFail(fspec64, fspec128, fin64);
            testVectorCastFloatToFloatFail(fspec64, fspec256, fin64);
            testVectorCastFloatToFloatFail(fspec64, fspec512, fin64);

            testVectorCastFloatToFloatFail(fspec128, fspec64, fin128);
            testVectorCastFloatToFloatFail(fspec128, fspec256, fin128);
            testVectorCastFloatToFloatFail(fspec128, fspec512, fin128);

            testVectorCastFloatToFloatFail(fspec256, fspec64, fin256);
            testVectorCastFloatToFloatFail(fspec256, fspec128, fin256);
            testVectorCastFloatToFloatFail(fspec256, fspec512, fin256);

            testVectorCastFloatToFloatFail(fspec512, fspec64, fin512);
            testVectorCastFloatToFloatFail(fspec512, fspec128, fin512);
            testVectorCastFloatToFloatFail(fspec512, fspec256, fin512);

            testVectorCastFloatToDoubleFail(fspec64, dspec64, fin64);
            testVectorCastFloatToDoubleFail(fspec64, dspec256, fin64);
            testVectorCastFloatToDoubleFail(fspec64, dspec512, fin64);

            testVectorCastFloatToDoubleFail(fspec128, dspec64, fin128);
            testVectorCastFloatToDoubleFail(fspec128, dspec128, fin128);
            testVectorCastFloatToDoubleFail(fspec128, dspec512, fin128);

            testVectorCastFloatToDoubleFail(fspec256, dspec64, fin256);
            testVectorCastFloatToDoubleFail(fspec256, dspec128, fin256);
            testVectorCastFloatToDoubleFail(fspec256, dspec256, fin256);

            testVectorCastFloatToDoubleFail(fspec512, dspec64, fin512);
            testVectorCastFloatToDoubleFail(fspec512, dspec128, fin512);
            testVectorCastFloatToDoubleFail(fspec512, dspec256, fin512);
            testVectorCastFloatToDoubleFail(fspec512, dspec512, fin512);
        }
    }

    @Test(dataProvider = "doubleUnaryOpProvider")
    static void testCastFromDouble(IntFunction<double[]> fa) {
        double[] din64 = fa.apply(dspec64.length());
        double[] din128 = fa.apply(dspec128.length());
        double[] din256 = fa.apply(dspec256.length());
        double[] din512 = fa.apply(dspec512.length());

        byte[] bout64 = new byte[bspec64.length()];

        short[] sout64 = new short[sspec64.length()];
        short[] sout128 = new short[sspec128.length()];

        int[] iout64 = new int[ispec64.length()];
        int[] iout128 = new int[ispec128.length()];
        int[] iout256 = new int[ispec256.length()];

        long[] lout64 = new long[lspec64.length()];
        long[] lout128 = new long[lspec128.length()];
        long[] lout256 = new long[lspec256.length()];
        long[] lout512 = new long[lspec512.length()];

        float[] fout64 = new float[fspec64.length()];
        float[] fout128 = new float[fspec128.length()];
        float[] fout256 = new float[fspec256.length()];

        double[] dout64 = new double[dspec64.length()];
        double[] dout128 = new double[dspec128.length()];
        double[] dout256 = new double[dspec256.length()];
        double[] dout512 = new double[dspec512.length()];

        for (int i = 0; i < NUM_ITER; i++) {
            testVectorCastDoubleToByte(dspec512, bspec64, din512, bout64);

            testVectorCastDoubleToShort(dspec256, sspec64, din256, sout64);
            testVectorCastDoubleToShort(dspec512, sspec128, din512, sout128);

            testVectorCastDoubleToInt(dspec128, ispec64, din128, iout64);
            testVectorCastDoubleToInt(dspec256, ispec128, din256, iout128);
            testVectorCastDoubleToInt(dspec512, ispec256, din512, iout256);

            testVectorCastDoubleToLong(dspec64, lspec64, din64, lout64);
            testVectorCastDoubleToLong(dspec128, lspec128, din128, lout128);
            testVectorCastDoubleToLong(dspec256, lspec256, din256, lout256);
            testVectorCastDoubleToLong(dspec512, lspec512, din512, lout512);

            testVectorCastDoubleToFloat(dspec128, fspec64, din128, fout64);
            testVectorCastDoubleToFloat(dspec256, fspec128, din256, fout128);
            testVectorCastDoubleToFloat(dspec512, fspec256, din512, fout256);

            testVectorCastDoubleToDouble(dspec64, dspec64, din64, dout64);
            testVectorCastDoubleToDouble(dspec128, dspec128, din128, dout128);
            testVectorCastDoubleToDouble(dspec256, dspec256, din256, dout256);
            testVectorCastDoubleToDouble(dspec512, dspec512, din512, dout512);
        }
    }

    //@Test
    static void testCastFromDoubleFail() {
        double[] din64 = new double[dspec64.length()];
        double[] din128 = new double[dspec128.length()];
        double[] din256 = new double[dspec256.length()];
        double[] din512 = new double[dspec512.length()];

        for (int i = 0; i < INVOC_COUNT; i++) {
            testVectorCastDoubleToByteFail(dspec64, bspec64, din64);
            testVectorCastDoubleToByteFail(dspec64, bspec128, din64);
            testVectorCastDoubleToByteFail(dspec64, bspec256, din64);
            testVectorCastDoubleToByteFail(dspec64, bspec512, din64);

            testVectorCastDoubleToByteFail(dspec128, bspec64, din128);
            testVectorCastDoubleToByteFail(dspec128, bspec128, din128);
            testVectorCastDoubleToByteFail(dspec128, bspec256, din128);
            testVectorCastDoubleToByteFail(dspec128, bspec512, din128);

            testVectorCastDoubleToByteFail(dspec256, bspec64, din256);
            testVectorCastDoubleToByteFail(dspec256, bspec128, din256);
            testVectorCastDoubleToByteFail(dspec256, bspec256, din256);
            testVectorCastDoubleToByteFail(dspec256, bspec512, din256);

            testVectorCastDoubleToByteFail(dspec512, bspec128, din512);
            testVectorCastDoubleToByteFail(dspec512, bspec256, din512);
            testVectorCastDoubleToByteFail(dspec512, bspec512, din512);

            testVectorCastDoubleToShortFail(dspec64, sspec64, din64);
            testVectorCastDoubleToShortFail(dspec64, sspec128, din64);
            testVectorCastDoubleToShortFail(dspec64, sspec256, din64);
            testVectorCastDoubleToShortFail(dspec64, sspec512, din64);

            testVectorCastDoubleToShortFail(dspec128, sspec64, din128);
            testVectorCastDoubleToShortFail(dspec128, sspec128, din128);
            testVectorCastDoubleToShortFail(dspec128, sspec256, din128);
            testVectorCastDoubleToShortFail(dspec128, sspec512, din128);

            testVectorCastDoubleToShortFail(dspec256, sspec128, din256);
            testVectorCastDoubleToShortFail(dspec256, sspec256, din256);
            testVectorCastDoubleToShortFail(dspec256, sspec512, din256);

            testVectorCastDoubleToShortFail(dspec512, sspec64, din512);
            testVectorCastDoubleToShortFail(dspec512, sspec256, din512);
            testVectorCastDoubleToShortFail(dspec512, sspec512, din512);

            testVectorCastDoubleToIntFail(dspec64, ispec64, din64);
            testVectorCastDoubleToIntFail(dspec64, ispec128, din64);
            testVectorCastDoubleToIntFail(dspec64, ispec256, din64);
            testVectorCastDoubleToIntFail(dspec64, ispec512, din64);

            testVectorCastDoubleToIntFail(dspec128, ispec128, din128);
            testVectorCastDoubleToIntFail(dspec128, ispec256, din128);
            testVectorCastDoubleToIntFail(dspec128, ispec512, din128);

            testVectorCastDoubleToIntFail(dspec256, ispec64, din256);
            testVectorCastDoubleToIntFail(dspec256, ispec256, din256);
            testVectorCastDoubleToIntFail(dspec256, ispec512, din256);

            testVectorCastDoubleToIntFail(dspec512, ispec64, din512);
            testVectorCastDoubleToIntFail(dspec512, ispec128, din512);
            testVectorCastDoubleToIntFail(dspec512, ispec512, din512);

            testVectorCastDoubleToLongFail(dspec64, lspec128, din64);
            testVectorCastDoubleToLongFail(dspec64, lspec256, din64);
            testVectorCastDoubleToLongFail(dspec64, lspec512, din64);

            testVectorCastDoubleToLongFail(dspec128, lspec64, din128);
            testVectorCastDoubleToLongFail(dspec128, lspec256, din128);
            testVectorCastDoubleToLongFail(dspec128, lspec512, din128);

            testVectorCastDoubleToLongFail(dspec256, lspec64, din256);
            testVectorCastDoubleToLongFail(dspec256, lspec128, din256);
            testVectorCastDoubleToLongFail(dspec256, lspec512, din256);

            testVectorCastDoubleToLongFail(dspec512, lspec64, din512);
            testVectorCastDoubleToLongFail(dspec512, lspec128, din512);
            testVectorCastDoubleToLongFail(dspec512, lspec256, din512);

            testVectorCastDoubleToFloatFail(dspec64, fspec64, din64);
            testVectorCastDoubleToFloatFail(dspec64, fspec128, din64);
            testVectorCastDoubleToFloatFail(dspec64, fspec256, din64);
            testVectorCastDoubleToFloatFail(dspec64, fspec512, din64);

            testVectorCastDoubleToFloatFail(dspec128, fspec128, din128);
            testVectorCastDoubleToFloatFail(dspec128, fspec256, din128);
            testVectorCastDoubleToFloatFail(dspec128, fspec512, din128);

            testVectorCastDoubleToFloatFail(dspec256, fspec64, din256);
            testVectorCastDoubleToFloatFail(dspec256, fspec256, din256);
            testVectorCastDoubleToFloatFail(dspec256, fspec512, din256);

            testVectorCastDoubleToFloatFail(dspec512, fspec64, din512);
            testVectorCastDoubleToFloatFail(dspec512, fspec128, din512);
            testVectorCastDoubleToFloatFail(dspec512, fspec512, din512);

            testVectorCastDoubleToDoubleFail(dspec64, dspec128, din64);
            testVectorCastDoubleToDoubleFail(dspec64, dspec256, din64);
            testVectorCastDoubleToDoubleFail(dspec64, dspec512, din64);

            testVectorCastDoubleToDoubleFail(dspec128, dspec64, din128);
            testVectorCastDoubleToDoubleFail(dspec128, dspec256, din128);
            testVectorCastDoubleToDoubleFail(dspec128, dspec512, din128);

            testVectorCastDoubleToDoubleFail(dspec256, dspec64, din256);
            testVectorCastDoubleToDoubleFail(dspec256, dspec128, din256);
            testVectorCastDoubleToDoubleFail(dspec256, dspec512, din256);

            testVectorCastDoubleToDoubleFail(dspec512, dspec64, din512);
            testVectorCastDoubleToDoubleFail(dspec512, dspec128, din512);
            testVectorCastDoubleToDoubleFail(dspec512, dspec256, din512);
        }
    }

    static
    void testVectorCastByteMaxToByte(VectorSpecies<Byte> a, VectorSpecies<Byte> b,
                                          byte[] input, byte[] output) {
        if (S_Max_BIT.vectorBitSize() == b.length() * Byte.SIZE) {
            testVectorCastByteToByte(a, b, input, output);
        } else {
            testVectorCastByteToByteFail(a, b, input);
        }
    }

    static
    void testVectorCastByteMaxToShort(VectorSpecies<Byte> a, VectorSpecies<Short> b,
                                           byte[] input, short[] output) {
        if (S_Max_BIT.vectorBitSize() == b.length() * Byte.SIZE) {
            testVectorCastByteToShort(a, b, input, output);
        } else {
            testVectorCastByteToShortFail(a, b, input);
        }
    }

    static
    void testVectorCastByteMaxToInt(VectorSpecies<Byte> a, VectorSpecies<Integer> b,
                                         byte[] input, int[] output) {
        if (S_Max_BIT.vectorBitSize() == b.length() * Byte.SIZE) {
            testVectorCastByteToInt(a, b, input, output);
        } else {
            testVectorCastByteToIntFail(a, b, input);
        }
    }

    static
    void testVectorCastByteMaxToLong(VectorSpecies<Byte> a, VectorSpecies<Long> b,
                                          byte[] input, long[] output) {
        if (S_Max_BIT.vectorBitSize() == b.length() * Byte.SIZE) {
            testVectorCastByteToLong(a, b, input, output);
        } else {
            testVectorCastByteToLongFail(a, b, input);
        }
    }

    static
    void testVectorCastByteMaxToFloat(VectorSpecies<Byte> a, VectorSpecies<Float> b,
                                           byte[] input, float[] output) {
        if (S_Max_BIT.vectorBitSize() == b.length() * Byte.SIZE) {
            testVectorCastByteToFloat(a, b, input, output);
        } else {
            testVectorCastByteToFloatFail(a, b, input);
        }
    }

    static
    void testVectorCastByteMaxToDouble(VectorSpecies<Byte> a, VectorSpecies<Double> b,
                                            byte[] input, double[] output) {
        if (S_Max_BIT.vectorBitSize() == b.length() * Byte.SIZE) {
            testVectorCastByteToDouble(a, b, input, output);
        } else {
            testVectorCastByteToDoubleFail(a, b, input);
        }
    }

    static
    void testVectorCastShortMaxToByte(VectorSpecies<Short> a, VectorSpecies<Byte> b,
                                           short[] input, byte[] output) {
        if (S_Max_BIT.vectorBitSize() == b.length() * Short.SIZE) {
            testVectorCastShortToByte(a, b, input, output);
        } else {
            testVectorCastShortToByteFail(a, b, input);
        }
    }

    static
    void testVectorCastShortMaxToShort(VectorSpecies<Short> a, VectorSpecies<Short> b,
                                            short[] input, short[] output) {
        if (S_Max_BIT.vectorBitSize() == b.length() * Short.SIZE) {
            testVectorCastShortToShort(a, b, input, output);
        } else {
            testVectorCastShortToShortFail(a, b, input);
        }
    }

    static
    void testVectorCastShortMaxToInt(VectorSpecies<Short> a, VectorSpecies<Integer> b,
                                          short[] input, int[] output) {
        if (S_Max_BIT.vectorBitSize() == b.length() * Short.SIZE) {
            testVectorCastShortToInt(a, b, input, output);
        } else {
            testVectorCastShortToIntFail(a, b, input);
        }
    }

    static
    void testVectorCastShortMaxToLong(VectorSpecies<Short> a, VectorSpecies<Long> b,
                                           short[] input, long[] output) {
        if (S_Max_BIT.vectorBitSize() == b.length() * Short.SIZE) {
            testVectorCastShortToLong(a, b, input, output);
        } else {
            testVectorCastShortToLongFail(a, b, input);
        }
    }

    static
    void testVectorCastShortMaxToFloat(VectorSpecies<Short> a, VectorSpecies<Float> b,
                                            short[] input, float[] output) {
        if (S_Max_BIT.vectorBitSize() == b.length() * Short.SIZE) {
            testVectorCastShortToFloat(a, b, input, output);
        } else {
            testVectorCastShortToFloatFail(a, b, input);
        }
    }

    static
    void testVectorCastShortMaxToDouble(VectorSpecies<Short> a, VectorSpecies<Double> b,
                                             short[] input, double[] output) {
        if (S_Max_BIT.vectorBitSize() == b.length() * Short.SIZE) {
            testVectorCastShortToDouble(a, b, input, output);
        } else {
            testVectorCastShortToDoubleFail(a, b, input);
        }
    }

    static
    void testVectorCastIntMaxToByte(VectorSpecies<Integer> a, VectorSpecies<Byte> b,
                                         int[] input, byte[] output) {
        if (S_Max_BIT.vectorBitSize() == b.length() * Integer.SIZE) {
            testVectorCastIntToByte(a, b, input, output);
        } else {
            testVectorCastIntToByteFail(a, b, input);
        }
    }

    static
    void testVectorCastIntMaxToShort(VectorSpecies<Integer> a, VectorSpecies<Short> b,
                                          int[] input, short[] output) {
        if (S_Max_BIT.vectorBitSize() == b.length() * Integer.SIZE) {
            testVectorCastIntToShort(a, b, input, output);
        } else {
            testVectorCastIntToShortFail(a, b, input);
        }
    }

    static
    void testVectorCastIntMaxToInt(VectorSpecies<Integer> a, VectorSpecies<Integer> b,
                                        int[] input, int[] output) {
        if (S_Max_BIT.vectorBitSize() == b.length() * Integer.SIZE) {
            testVectorCastIntToInt(a, b, input, output);
        } else {
            testVectorCastIntToIntFail(a, b, input);
        }
    }

    static
    void testVectorCastIntMaxToLong(VectorSpecies<Integer> a, VectorSpecies<Long> b,
                                         int[] input, long[] output) {
        if (S_Max_BIT.vectorBitSize() == b.length() * Integer.SIZE) {
            testVectorCastIntToLong(a, b, input, output);
        } else {
            testVectorCastIntToLongFail(a, b, input);
        }
    }

    static
    void testVectorCastIntMaxToFloat(VectorSpecies<Integer> a, VectorSpecies<Float> b,
                                          int[] input, float[] output) {
        if (S_Max_BIT.vectorBitSize() == b.length() * Integer.SIZE) {
            testVectorCastIntToFloat(a, b, input, output);
        } else {
            testVectorCastIntToFloatFail(a, b, input);
        }
    }

    static
    void testVectorCastIntMaxToDouble(VectorSpecies<Integer> a, VectorSpecies<Double> b,
                                           int[] input, double[] output) {
        if (S_Max_BIT.vectorBitSize() == b.length() * Integer.SIZE) {
            testVectorCastIntToDouble(a, b, input, output);
        } else {
            testVectorCastIntToDoubleFail(a, b, input);
        }
    }

    static
    void testVectorCastLongMaxToByte(VectorSpecies<Long> a, VectorSpecies<Byte> b,
                                          long[] input, byte[] output) {
        if (S_Max_BIT.vectorBitSize() == b.length() * Long.SIZE) {
            testVectorCastLongToByte(a, b, input, output);
        } else {
            testVectorCastLongToByteFail(a, b, input);
        }
    }

    static
    void testVectorCastLongMaxToShort(VectorSpecies<Long> a, VectorSpecies<Short> b,
                                           long[] input, short[] output) {
        if (S_Max_BIT.vectorBitSize() == b.length() * Long.SIZE) {
            testVectorCastLongToShort(a, b, input, output);
        } else {
            testVectorCastLongToShortFail(a, b, input);
        }
    }

    static
    void testVectorCastLongMaxToInt(VectorSpecies<Long> a, VectorSpecies<Integer> b,
                                         long[] input, int[] output) {
        if (S_Max_BIT.vectorBitSize() == b.length() * Long.SIZE) {
            testVectorCastLongToInt(a, b, input, output);
        } else {
            testVectorCastLongToIntFail(a, b, input);
        }
    }

    static
    void testVectorCastLongMaxToLong(VectorSpecies<Long> a, VectorSpecies<Long> b,
                                          long[] input, long[] output) {
        if (S_Max_BIT.vectorBitSize() == b.length() * Long.SIZE) {
            testVectorCastLongToLong(a, b, input, output);
        } else {
            testVectorCastLongToLongFail(a, b, input);
        }
    }

    static
    void testVectorCastLongMaxToFloat(VectorSpecies<Long> a, VectorSpecies<Float> b,
                                           long[] input, float[] output) {
        if (S_Max_BIT.vectorBitSize() == b.length() * Long.SIZE) {
            testVectorCastLongToFloat(a, b, input, output);
        } else {
            testVectorCastLongToFloatFail(a, b, input);
        }
    }

    static
    void testVectorCastLongMaxToDouble(VectorSpecies<Long> a, VectorSpecies<Double> b,
                                            long[] input, double[] output) {
        if (S_Max_BIT.vectorBitSize() == b.length() * Long.SIZE) {
            testVectorCastLongToDouble(a, b, input, output);
        } else {
            testVectorCastLongToDoubleFail(a, b, input);
        }
    }

    static
    void testVectorCastFloatMaxToByte(VectorSpecies<Float> a, VectorSpecies<Byte> b,
                                           float[] input, byte[] output) {
        if (S_Max_BIT.vectorBitSize() == b.length() * Float.SIZE) {
            testVectorCastFloatToByte(a, b, input, output);
        } else {
            testVectorCastFloatToByteFail(a, b, input);
        }
    }

    static
    void testVectorCastFloatMaxToShort(VectorSpecies<Float> a, VectorSpecies<Short> b,
                                            float[] input, short[] output) {
        if (S_Max_BIT.vectorBitSize() == b.length() * Float.SIZE) {
            testVectorCastFloatToShort(a, b, input, output);
        } else {
            testVectorCastFloatToShortFail(a, b, input);
        }
    }

    static
    void testVectorCastFloatMaxToInt(VectorSpecies<Float> a, VectorSpecies<Integer> b,
                                          float[] input, int[] output) {
        if (S_Max_BIT.vectorBitSize() == b.length() * Float.SIZE) {
            testVectorCastFloatToInt(a, b, input, output);
        } else {
            testVectorCastFloatToIntFail(a, b, input);
        }
    }

    static
    void testVectorCastFloatMaxToLong(VectorSpecies<Float> a, VectorSpecies<Long> b,
                                           float[] input, long[] output) {
        if (S_Max_BIT.vectorBitSize() == b.length() * Float.SIZE) {
            testVectorCastFloatToLong(a, b, input, output);
        } else {
            testVectorCastFloatToLongFail(a, b, input);
        }
    }

    static
    void testVectorCastFloatMaxToFloat(VectorSpecies<Float> a, VectorSpecies<Float> b,
                                            float[] input, float[] output) {
        if (S_Max_BIT.vectorBitSize() == b.length() * Float.SIZE) {
            testVectorCastFloatToFloat(a, b, input, output);
        } else {
            testVectorCastFloatToFloatFail(a, b, input);
        }
    }

    static
    void testVectorCastFloatMaxToDouble(VectorSpecies<Float> a, VectorSpecies<Double> b,
                                             float[] input, double[] output) {
        if (S_Max_BIT.vectorBitSize() == b.length() * Float.SIZE) {
            testVectorCastFloatToDouble(a, b, input, output);
        } else {
            testVectorCastFloatToDoubleFail(a, b, input);
        }
    }

    static
    void testVectorCastDoubleMaxToByte(VectorSpecies<Double> a, VectorSpecies<Byte> b,
                                            double[] input, byte[] output) {
        if (S_Max_BIT.vectorBitSize() == b.length() * Double.SIZE) {
            testVectorCastDoubleToByte(a, b, input, output);
        } else {
            testVectorCastDoubleToByteFail(a, b, input);
        }
    }

    static
    void testVectorCastDoubleMaxToShort(VectorSpecies<Double> a, VectorSpecies<Short> b,
                                             double[] input, short[] output) {
        if (S_Max_BIT.vectorBitSize() == b.length() * Double.SIZE) {
            testVectorCastDoubleToShort(a, b, input, output);
        } else {
            testVectorCastDoubleToShortFail(a, b, input);
        }
    }

    static
    void testVectorCastDoubleMaxToInt(VectorSpecies<Double> a, VectorSpecies<Integer> b,
                                           double[] input, int[] output) {
        if (S_Max_BIT.vectorBitSize() == b.length() * Double.SIZE) {
            testVectorCastDoubleToInt(a, b, input, output);
        } else {
            testVectorCastDoubleToIntFail(a, b, input);
        }
    }

    static
    void testVectorCastDoubleMaxToLong(VectorSpecies<Double> a, VectorSpecies<Long> b,
                                            double[] input, long[] output) {
        if (S_Max_BIT.vectorBitSize() == b.length() * Double.SIZE) {
            testVectorCastDoubleToLong(a, b, input, output);
        } else {
            testVectorCastDoubleToLongFail(a, b, input);
        }
    }

    static
    void testVectorCastDoubleMaxToFloat(VectorSpecies<Double> a, VectorSpecies<Float> b,
                                             double[] input, float[] output) {
        if (S_Max_BIT.vectorBitSize() == b.length() * Double.SIZE) {
            testVectorCastDoubleToFloat(a, b, input, output);
        } else {
            testVectorCastDoubleToFloatFail(a, b, input);
        }
    }

    static
    void testVectorCastDoubleMaxToDouble(VectorSpecies<Double> a, VectorSpecies<Double> b,
                                              double[] input, double[] output) {
        if (S_Max_BIT.vectorBitSize() == b.length() * Double.SIZE) {
            testVectorCastDoubleToDouble(a, b, input, output);
        } else {
            testVectorCastDoubleToDoubleFail(a, b, input);
        }
    }

    //@Test(dataProvider = "byteUnaryOpProvider")
    static void testCastFromByteMax(IntFunction<byte[]> fa) {
        byte[] binMax = fa.apply(bspecMax.length());

        byte[] bout64 = new byte[bspec64.length()];
        byte[] bout128 = new byte[bspec128.length()];
        byte[] bout256 = new byte[bspec256.length()];
        byte[] bout512 = new byte[bspec512.length()];
        byte[] boutMax = new byte[bspecMax.length()];

        short[] sout64 = new short[sspec64.length()];
        short[] sout128 = new short[sspec128.length()];
        short[] sout256 = new short[sspec256.length()];
        short[] sout512 = new short[sspec512.length()];
        short[] soutMax = new short[sspecMax.length()];

        int[] iout64 = new int[ispec64.length()];
        int[] iout128 = new int[ispec128.length()];
        int[] iout256 = new int[ispec256.length()];
        int[] iout512 = new int[ispec512.length()];
        int[] ioutMax = new int[ispecMax.length()];

        long[] lout64 = new long[lspec64.length()];
        long[] lout128 = new long[lspec128.length()];
        long[] lout256 = new long[lspec256.length()];
        long[] lout512 = new long[lspec512.length()];
        long[] loutMax = new long[lspecMax.length()];

        float[] fout64 = new float[fspec64.length()];
        float[] fout128 = new float[fspec128.length()];
        float[] fout256 = new float[fspec256.length()];
        float[] fout512 = new float[fspec512.length()];
        float[] foutMax = new float[fspecMax.length()];

        double[] dout64 = new double[dspec64.length()];
        double[] dout128 = new double[dspec128.length()];
        double[] dout256 = new double[dspec256.length()];
        double[] dout512 = new double[dspec512.length()];
        double[] doutMax = new double[dspecMax.length()];

        for (int i = 0; i < NUM_ITER; i++) {
            testVectorCastByteMaxToByte(bspecMax, bspec64, binMax, bout64);
            testVectorCastByteMaxToByte(bspecMax, bspec128, binMax, bout128);
            testVectorCastByteMaxToByte(bspecMax, bspec256, binMax, bout256);
            testVectorCastByteMaxToByte(bspecMax, bspec512, binMax, bout512);
            testVectorCastByteMaxToByte(bspecMax, bspecMax, binMax, boutMax);

            testVectorCastByteMaxToShort(bspecMax, sspec64, binMax, sout64);
            testVectorCastByteMaxToShort(bspecMax, sspec128, binMax, sout128);
            testVectorCastByteMaxToShort(bspecMax, sspec256, binMax, sout256);
            testVectorCastByteMaxToShort(bspecMax, sspec512, binMax, sout512);
            testVectorCastByteMaxToShort(bspecMax, sspecMax, binMax, soutMax);

            testVectorCastByteMaxToInt(bspecMax, ispec64, binMax, iout64);
            testVectorCastByteMaxToInt(bspecMax, ispec128, binMax, iout128);
            testVectorCastByteMaxToInt(bspecMax, ispec256, binMax, iout256);
            testVectorCastByteMaxToInt(bspecMax, ispec512, binMax, iout512);
            testVectorCastByteMaxToInt(bspecMax, ispecMax, binMax, ioutMax);

            testVectorCastByteMaxToLong(bspecMax, lspec64, binMax, lout64);
            testVectorCastByteMaxToLong(bspecMax, lspec128, binMax, lout128);
            testVectorCastByteMaxToLong(bspecMax, lspec256, binMax, lout256);
            testVectorCastByteMaxToLong(bspecMax, lspec512, binMax, lout512);
            testVectorCastByteMaxToLong(bspecMax, lspecMax, binMax, loutMax);

            testVectorCastByteMaxToFloat(bspecMax, fspec64, binMax, fout64);
            testVectorCastByteMaxToFloat(bspecMax, fspec128, binMax, fout128);
            testVectorCastByteMaxToFloat(bspecMax, fspec256, binMax, fout256);
            testVectorCastByteMaxToFloat(bspecMax, fspec512, binMax, fout512);
            testVectorCastByteMaxToFloat(bspecMax, fspecMax, binMax, foutMax);

            testVectorCastByteMaxToDouble(bspecMax, dspec64, binMax, dout64);
            testVectorCastByteMaxToDouble(bspecMax, dspec128, binMax, dout128);
            testVectorCastByteMaxToDouble(bspecMax, dspec256, binMax, dout256);
            testVectorCastByteMaxToDouble(bspecMax, dspec512, binMax, dout512);
            testVectorCastByteMaxToDouble(bspecMax, dspecMax, binMax, doutMax);
        }
    }

    //@Test(dataProvider = "shortUnaryOpProvider")
    static void testCastFromShortMax(IntFunction<short[]> fa) {
        short[] sinMax = fa.apply(sspecMax.length());

        byte[] bout64 = new byte[bspec64.length()];
        byte[] bout128 = new byte[bspec128.length()];
        byte[] bout256 = new byte[bspec256.length()];
        byte[] bout512 = new byte[bspec512.length()];
        byte[] boutMax = new byte[bspecMax.length()];

        short[] sout64 = new short[sspec64.length()];
        short[] sout128 = new short[sspec128.length()];
        short[] sout256 = new short[sspec256.length()];
        short[] sout512 = new short[sspec512.length()];
        short[] soutMax = new short[sspecMax.length()];

        int[] iout64 = new int[ispec64.length()];
        int[] iout128 = new int[ispec128.length()];
        int[] iout256 = new int[ispec256.length()];
        int[] iout512 = new int[ispec512.length()];
        int[] ioutMax = new int[ispecMax.length()];

        long[] lout64 = new long[lspec64.length()];
        long[] lout128 = new long[lspec128.length()];
        long[] lout256 = new long[lspec256.length()];
        long[] lout512 = new long[lspec512.length()];
        long[] loutMax = new long[lspecMax.length()];

        float[] fout64 = new float[fspec64.length()];
        float[] fout128 = new float[fspec128.length()];
        float[] fout256 = new float[fspec256.length()];
        float[] fout512 = new float[fspec512.length()];
        float[] foutMax = new float[fspecMax.length()];

        double[] dout64 = new double[dspec64.length()];
        double[] dout128 = new double[dspec128.length()];
        double[] dout256 = new double[dspec256.length()];
        double[] dout512 = new double[dspec512.length()];
        double[] doutMax = new double[dspecMax.length()];

        for (int i = 0; i < NUM_ITER; i++) {
            testVectorCastShortMaxToByte(sspecMax, bspec64, sinMax, bout64);
            testVectorCastShortMaxToByte(sspecMax, bspec128, sinMax, bout128);
            testVectorCastShortMaxToByte(sspecMax, bspec256, sinMax, bout256);
            testVectorCastShortMaxToByte(sspecMax, bspec512, sinMax, bout512);
            testVectorCastShortMaxToByte(sspecMax, bspecMax, sinMax, boutMax);

            testVectorCastShortMaxToShort(sspecMax, sspec64, sinMax, sout64);
            testVectorCastShortMaxToShort(sspecMax, sspec128, sinMax, sout128);
            testVectorCastShortMaxToShort(sspecMax, sspec256, sinMax, sout256);
            testVectorCastShortMaxToShort(sspecMax, sspec512, sinMax, sout512);
            testVectorCastShortMaxToShort(sspecMax, sspecMax, sinMax, soutMax);

            testVectorCastShortMaxToInt(sspecMax, ispec64, sinMax, iout64);
            testVectorCastShortMaxToInt(sspecMax, ispec128, sinMax, iout128);
            testVectorCastShortMaxToInt(sspecMax, ispec256, sinMax, iout256);
            testVectorCastShortMaxToInt(sspecMax, ispec512, sinMax, iout512);
            testVectorCastShortMaxToInt(sspecMax, ispecMax, sinMax, ioutMax);

            testVectorCastShortMaxToLong(sspecMax, lspec64, sinMax, lout64);
            testVectorCastShortMaxToLong(sspecMax, lspec128, sinMax, lout128);
            testVectorCastShortMaxToLong(sspecMax, lspec256, sinMax, lout256);
            testVectorCastShortMaxToLong(sspecMax, lspec512, sinMax, lout512);
            testVectorCastShortMaxToLong(sspecMax, lspecMax, sinMax, loutMax);

            testVectorCastShortMaxToFloat(sspecMax, fspec64, sinMax, fout64);
            testVectorCastShortMaxToFloat(sspecMax, fspec128, sinMax, fout128);
            testVectorCastShortMaxToFloat(sspecMax, fspec256, sinMax, fout256);
            testVectorCastShortMaxToFloat(sspecMax, fspec512, sinMax, fout512);
            testVectorCastShortMaxToFloat(sspecMax, fspecMax, sinMax, foutMax);

            testVectorCastShortMaxToDouble(sspecMax, dspec64, sinMax, dout64);
            testVectorCastShortMaxToDouble(sspecMax, dspec128, sinMax, dout128);
            testVectorCastShortMaxToDouble(sspecMax, dspec256, sinMax, dout256);
            testVectorCastShortMaxToDouble(sspecMax, dspec512, sinMax, dout512);
            testVectorCastShortMaxToDouble(sspecMax, dspecMax, sinMax, doutMax);
        }
    }

    //@Test(dataProvider = "intUnaryOpProvider")
    static void testCastFromIntMax(IntFunction<int[]> fa) {
        int[] iinMax = fa.apply(ispecMax.length());

        byte[] bout64 = new byte[bspec64.length()];
        byte[] bout128 = new byte[bspec128.length()];
        byte[] bout256 = new byte[bspec256.length()];
        byte[] bout512 = new byte[bspec512.length()];
        byte[] boutMax = new byte[bspecMax.length()];

        short[] sout64 = new short[sspec64.length()];
        short[] sout128 = new short[sspec128.length()];
        short[] sout256 = new short[sspec256.length()];
        short[] sout512 = new short[sspec512.length()];
        short[] soutMax = new short[sspecMax.length()];

        int[] iout64 = new int[ispec64.length()];
        int[] iout128 = new int[ispec128.length()];
        int[] iout256 = new int[ispec256.length()];
        int[] iout512 = new int[ispec512.length()];
        int[] ioutMax = new int[ispecMax.length()];

        long[] lout64 = new long[lspec64.length()];
        long[] lout128 = new long[lspec128.length()];
        long[] lout256 = new long[lspec256.length()];
        long[] lout512 = new long[lspec512.length()];
        long[] loutMax = new long[lspecMax.length()];

        float[] fout64 = new float[fspec64.length()];
        float[] fout128 = new float[fspec128.length()];
        float[] fout256 = new float[fspec256.length()];
        float[] fout512 = new float[fspec512.length()];
        float[] foutMax = new float[fspecMax.length()];

        double[] dout64 = new double[dspec64.length()];
        double[] dout128 = new double[dspec128.length()];
        double[] dout256 = new double[dspec256.length()];
        double[] dout512 = new double[dspec512.length()];
        double[] doutMax = new double[dspecMax.length()];

        for (int i = 0; i < NUM_ITER; i++) {
            testVectorCastIntMaxToByte(ispecMax, bspec64, iinMax, bout64);
            testVectorCastIntMaxToByte(ispecMax, bspec128, iinMax, bout128);
            testVectorCastIntMaxToByte(ispecMax, bspec256, iinMax, bout256);
            testVectorCastIntMaxToByte(ispecMax, bspec512, iinMax, bout512);
            testVectorCastIntMaxToByte(ispecMax, bspecMax, iinMax, boutMax);

            testVectorCastIntMaxToShort(ispecMax, sspec64, iinMax, sout64);
            testVectorCastIntMaxToShort(ispecMax, sspec128, iinMax, sout128);
            testVectorCastIntMaxToShort(ispecMax, sspec256, iinMax, sout256);
            testVectorCastIntMaxToShort(ispecMax, sspec512, iinMax, sout512);
            testVectorCastIntMaxToShort(ispecMax, sspecMax, iinMax, soutMax);

            testVectorCastIntMaxToInt(ispecMax, ispec64, iinMax, iout64);
            testVectorCastIntMaxToInt(ispecMax, ispec128, iinMax, iout128);
            testVectorCastIntMaxToInt(ispecMax, ispec256, iinMax, iout256);
            testVectorCastIntMaxToInt(ispecMax, ispec512, iinMax, iout512);
            testVectorCastIntMaxToInt(ispecMax, ispecMax, iinMax, ioutMax);

            testVectorCastIntMaxToLong(ispecMax, lspec64, iinMax, lout64);
            testVectorCastIntMaxToLong(ispecMax, lspec128, iinMax, lout128);
            testVectorCastIntMaxToLong(ispecMax, lspec256, iinMax, lout256);
            testVectorCastIntMaxToLong(ispecMax, lspec512, iinMax, lout512);
            testVectorCastIntMaxToLong(ispecMax, lspecMax, iinMax, loutMax);

            testVectorCastIntMaxToFloat(ispecMax, fspec64, iinMax, fout64);
            testVectorCastIntMaxToFloat(ispecMax, fspec128, iinMax, fout128);
            testVectorCastIntMaxToFloat(ispecMax, fspec256, iinMax, fout256);
            testVectorCastIntMaxToFloat(ispecMax, fspec512, iinMax, fout512);
            testVectorCastIntMaxToFloat(ispecMax, fspecMax, iinMax, foutMax);

            testVectorCastIntMaxToDouble(ispecMax, dspec64, iinMax, dout64);
            testVectorCastIntMaxToDouble(ispecMax, dspec128, iinMax, dout128);
            testVectorCastIntMaxToDouble(ispecMax, dspec256, iinMax, dout256);
            testVectorCastIntMaxToDouble(ispecMax, dspec512, iinMax, dout512);
            testVectorCastIntMaxToDouble(ispecMax, dspecMax, iinMax, doutMax);
        }
    }

    //@Test(dataProvider = "longUnaryOpProvider")
    static void testCastFromLongMax(IntFunction<long[]> fa) {
        long[] linMax = fa.apply(lspecMax.length());

        byte[] bout64 = new byte[bspec64.length()];
        byte[] bout128 = new byte[bspec128.length()];
        byte[] bout256 = new byte[bspec256.length()];
        byte[] bout512 = new byte[bspec512.length()];
        byte[] boutMax = new byte[bspecMax.length()];

        short[] sout64 = new short[sspec64.length()];
        short[] sout128 = new short[sspec128.length()];
        short[] sout256 = new short[sspec256.length()];
        short[] sout512 = new short[sspec512.length()];
        short[] soutMax = new short[sspecMax.length()];

        int[] iout64 = new int[ispec64.length()];
        int[] iout128 = new int[ispec128.length()];
        int[] iout256 = new int[ispec256.length()];
        int[] iout512 = new int[ispec512.length()];
        int[] ioutMax = new int[ispecMax.length()];

        long[] lout64 = new long[lspec64.length()];
        long[] lout128 = new long[lspec128.length()];
        long[] lout256 = new long[lspec256.length()];
        long[] lout512 = new long[lspec512.length()];
        long[] loutMax = new long[lspecMax.length()];

        float[] fout64 = new float[fspec64.length()];
        float[] fout128 = new float[fspec128.length()];
        float[] fout256 = new float[fspec256.length()];
        float[] fout512 = new float[fspec512.length()];
        float[] foutMax = new float[fspecMax.length()];

        double[] dout64 = new double[dspec64.length()];
        double[] dout128 = new double[dspec128.length()];
        double[] dout256 = new double[dspec256.length()];
        double[] dout512 = new double[dspec512.length()];
        double[] doutMax = new double[dspecMax.length()];

        for (int i = 0; i < NUM_ITER; i++) {
            testVectorCastLongMaxToByte(lspecMax, bspec64, linMax, bout64);
            testVectorCastLongMaxToByte(lspecMax, bspec128, linMax, bout128);
            testVectorCastLongMaxToByte(lspecMax, bspec256, linMax, bout256);
            testVectorCastLongMaxToByte(lspecMax, bspec512, linMax, bout512);
            testVectorCastLongMaxToByte(lspecMax, bspecMax, linMax, boutMax);

            testVectorCastLongMaxToShort(lspecMax, sspec64, linMax, sout64);
            testVectorCastLongMaxToShort(lspecMax, sspec128, linMax, sout128);
            testVectorCastLongMaxToShort(lspecMax, sspec256, linMax, sout256);
            testVectorCastLongMaxToShort(lspecMax, sspec512, linMax, sout512);
            testVectorCastLongMaxToShort(lspecMax, sspecMax, linMax, soutMax);

            testVectorCastLongMaxToInt(lspecMax, ispec64, linMax, iout64);
            testVectorCastLongMaxToInt(lspecMax, ispec128, linMax, iout128);
            testVectorCastLongMaxToInt(lspecMax, ispec256, linMax, iout256);
            testVectorCastLongMaxToInt(lspecMax, ispec512, linMax, iout512);
            testVectorCastLongMaxToInt(lspecMax, ispecMax, linMax, ioutMax);

            testVectorCastLongMaxToLong(lspecMax, lspec64, linMax, lout64);
            testVectorCastLongMaxToLong(lspecMax, lspec128, linMax, lout128);
            testVectorCastLongMaxToLong(lspecMax, lspec256, linMax, lout256);
            testVectorCastLongMaxToLong(lspecMax, lspec512, linMax, lout512);
            testVectorCastLongMaxToLong(lspecMax, lspecMax, linMax, loutMax);

            testVectorCastLongMaxToFloat(lspecMax, fspec64, linMax, fout64);
            testVectorCastLongMaxToFloat(lspecMax, fspec128, linMax, fout128);
            testVectorCastLongMaxToFloat(lspecMax, fspec256, linMax, fout256);
            testVectorCastLongMaxToFloat(lspecMax, fspec512, linMax, fout512);
            testVectorCastLongMaxToFloat(lspecMax, fspecMax, linMax, foutMax);

            testVectorCastLongMaxToDouble(lspecMax, dspec64, linMax, dout64);
            testVectorCastLongMaxToDouble(lspecMax, dspec128, linMax, dout128);
            testVectorCastLongMaxToDouble(lspecMax, dspec256, linMax, dout256);
            testVectorCastLongMaxToDouble(lspecMax, dspec512, linMax, dout512);
            testVectorCastLongMaxToDouble(lspecMax, dspecMax, linMax, doutMax);
        }
    }

    //@Test(dataProvider = "floatUnaryOpProvider")
    static void testCastFromFloatMax(IntFunction<float[]> fa) {
        float[] finMax = fa.apply(fspecMax.length());

        byte[] bout64 = new byte[bspec64.length()];
        byte[] bout128 = new byte[bspec128.length()];
        byte[] bout256 = new byte[bspec256.length()];
        byte[] bout512 = new byte[bspec512.length()];
        byte[] boutMax = new byte[bspecMax.length()];

        short[] sout64 = new short[sspec64.length()];
        short[] sout128 = new short[sspec128.length()];
        short[] sout256 = new short[sspec256.length()];
        short[] sout512 = new short[sspec512.length()];
        short[] soutMax = new short[sspecMax.length()];

        int[] iout64 = new int[ispec64.length()];
        int[] iout128 = new int[ispec128.length()];
        int[] iout256 = new int[ispec256.length()];
        int[] iout512 = new int[ispec512.length()];
        int[] ioutMax = new int[ispecMax.length()];

        long[] lout64 = new long[lspec64.length()];
        long[] lout128 = new long[lspec128.length()];
        long[] lout256 = new long[lspec256.length()];
        long[] lout512 = new long[lspec512.length()];
        long[] loutMax = new long[lspecMax.length()];

        float[] fout64 = new float[fspec64.length()];
        float[] fout128 = new float[fspec128.length()];
        float[] fout256 = new float[fspec256.length()];
        float[] fout512 = new float[fspec512.length()];
        float[] foutMax = new float[fspecMax.length()];

        double[] dout64 = new double[dspec64.length()];
        double[] dout128 = new double[dspec128.length()];
        double[] dout256 = new double[dspec256.length()];
        double[] dout512 = new double[dspec512.length()];
        double[] doutMax = new double[dspecMax.length()];

        for (int i = 0; i < NUM_ITER; i++) {
            testVectorCastFloatMaxToByte(fspecMax, bspec64, finMax, bout64);
            testVectorCastFloatMaxToByte(fspecMax, bspec128, finMax, bout128);
            testVectorCastFloatMaxToByte(fspecMax, bspec256, finMax, bout256);
            testVectorCastFloatMaxToByte(fspecMax, bspec512, finMax, bout512);
            testVectorCastFloatMaxToByte(fspecMax, bspecMax, finMax, boutMax);

            testVectorCastFloatMaxToShort(fspecMax, sspec64, finMax, sout64);
            testVectorCastFloatMaxToShort(fspecMax, sspec128, finMax, sout128);
            testVectorCastFloatMaxToShort(fspecMax, sspec256, finMax, sout256);
            testVectorCastFloatMaxToShort(fspecMax, sspec512, finMax, sout512);
            testVectorCastFloatMaxToShort(fspecMax, sspecMax, finMax, soutMax);

            testVectorCastFloatMaxToInt(fspecMax, ispec64, finMax, iout64);
            testVectorCastFloatMaxToInt(fspecMax, ispec128, finMax, iout128);
            testVectorCastFloatMaxToInt(fspecMax, ispec256, finMax, iout256);
            testVectorCastFloatMaxToInt(fspecMax, ispec512, finMax, iout512);
            testVectorCastFloatMaxToInt(fspecMax, ispecMax, finMax, ioutMax);

            testVectorCastFloatMaxToLong(fspecMax, lspec64, finMax, lout64);
            testVectorCastFloatMaxToLong(fspecMax, lspec128, finMax, lout128);
            testVectorCastFloatMaxToLong(fspecMax, lspec256, finMax, lout256);
            testVectorCastFloatMaxToLong(fspecMax, lspec512, finMax, lout512);
            testVectorCastFloatMaxToLong(fspecMax, lspecMax, finMax, loutMax);

            testVectorCastFloatMaxToFloat(fspecMax, fspec64, finMax, fout64);
            testVectorCastFloatMaxToFloat(fspecMax, fspec128, finMax, fout128);
            testVectorCastFloatMaxToFloat(fspecMax, fspec256, finMax, fout256);
            testVectorCastFloatMaxToFloat(fspecMax, fspec512, finMax, fout512);
            testVectorCastFloatMaxToFloat(fspecMax, fspecMax, finMax, foutMax);

            testVectorCastFloatMaxToDouble(fspecMax, dspec64, finMax, dout64);
            testVectorCastFloatMaxToDouble(fspecMax, dspec128, finMax, dout128);
            testVectorCastFloatMaxToDouble(fspecMax, dspec256, finMax, dout256);
            testVectorCastFloatMaxToDouble(fspecMax, dspec512, finMax, dout512);
            testVectorCastFloatMaxToDouble(fspecMax, dspecMax, finMax, doutMax);
        }
    }

    //@Test(dataProvider = "doubleUnaryOpProvider")
    static void testCastFromDoubleMax(IntFunction<double[]> fa) {
        double[] dinMax = fa.apply(dspecMax.length());

        byte[] bout64 = new byte[bspec64.length()];
        byte[] bout128 = new byte[bspec128.length()];
        byte[] bout256 = new byte[bspec256.length()];
        byte[] bout512 = new byte[bspec512.length()];
        byte[] boutMax = new byte[bspecMax.length()];

        short[] sout64 = new short[sspec64.length()];
        short[] sout128 = new short[sspec128.length()];
        short[] sout256 = new short[sspec256.length()];
        short[] sout512 = new short[sspec512.length()];
        short[] soutMax = new short[sspecMax.length()];

        int[] iout64 = new int[ispec64.length()];
        int[] iout128 = new int[ispec128.length()];
        int[] iout256 = new int[ispec256.length()];
        int[] iout512 = new int[ispec512.length()];
        int[] ioutMax = new int[ispecMax.length()];

        long[] lout64 = new long[lspec64.length()];
        long[] lout128 = new long[lspec128.length()];
        long[] lout256 = new long[lspec256.length()];
        long[] lout512 = new long[lspec512.length()];
        long[] loutMax = new long[lspecMax.length()];

        float[] fout64 = new float[fspec64.length()];
        float[] fout128 = new float[fspec128.length()];
        float[] fout256 = new float[fspec256.length()];
        float[] fout512 = new float[fspec512.length()];
        float[] foutMax = new float[fspecMax.length()];

        double[] dout64 = new double[dspec64.length()];
        double[] dout128 = new double[dspec128.length()];
        double[] dout256 = new double[dspec256.length()];
        double[] dout512 = new double[dspec512.length()];
        double[] doutMax = new double[dspecMax.length()];

        for (int i = 0; i < NUM_ITER; i++) {
            testVectorCastDoubleMaxToByte(dspecMax, bspec64, dinMax, bout64);
            testVectorCastDoubleMaxToByte(dspecMax, bspec128, dinMax, bout128);
            testVectorCastDoubleMaxToByte(dspecMax, bspec256, dinMax, bout256);
            testVectorCastDoubleMaxToByte(dspecMax, bspec512, dinMax, bout512);
            testVectorCastDoubleMaxToByte(dspecMax, bspecMax, dinMax, boutMax);

            testVectorCastDoubleMaxToShort(dspecMax, sspec64, dinMax, sout64);
            testVectorCastDoubleMaxToShort(dspecMax, sspec128, dinMax, sout128);
            testVectorCastDoubleMaxToShort(dspecMax, sspec256, dinMax, sout256);
            testVectorCastDoubleMaxToShort(dspecMax, sspec512, dinMax, sout512);
            testVectorCastDoubleMaxToShort(dspecMax, sspecMax, dinMax, soutMax);

            testVectorCastDoubleMaxToInt(dspecMax, ispec64, dinMax, iout64);
            testVectorCastDoubleMaxToInt(dspecMax, ispec128, dinMax, iout128);
            testVectorCastDoubleMaxToInt(dspecMax, ispec256, dinMax, iout256);
            testVectorCastDoubleMaxToInt(dspecMax, ispec512, dinMax, iout512);
            testVectorCastDoubleMaxToInt(dspecMax, ispecMax, dinMax, ioutMax);

            testVectorCastDoubleMaxToLong(dspecMax, lspec64, dinMax, lout64);
            testVectorCastDoubleMaxToLong(dspecMax, lspec128, dinMax, lout128);
            testVectorCastDoubleMaxToLong(dspecMax, lspec256, dinMax, lout256);
            testVectorCastDoubleMaxToLong(dspecMax, lspec512, dinMax, lout512);
            testVectorCastDoubleMaxToLong(dspecMax, lspecMax, dinMax, loutMax);

            testVectorCastDoubleMaxToFloat(dspecMax, fspec64, dinMax, fout64);
            testVectorCastDoubleMaxToFloat(dspecMax, fspec128, dinMax, fout128);
            testVectorCastDoubleMaxToFloat(dspecMax, fspec256, dinMax, fout256);
            testVectorCastDoubleMaxToFloat(dspecMax, fspec512, dinMax, fout512);
            testVectorCastDoubleMaxToFloat(dspecMax, fspecMax, dinMax, foutMax);

            testVectorCastDoubleMaxToDouble(dspecMax, dspec64, dinMax, dout64);
            testVectorCastDoubleMaxToDouble(dspecMax, dspec128, dinMax, dout128);
            testVectorCastDoubleMaxToDouble(dspecMax, dspec256, dinMax, dout256);
            testVectorCastDoubleMaxToDouble(dspecMax, dspec512, dinMax, dout512);
            testVectorCastDoubleMaxToDouble(dspecMax, dspecMax, dinMax, doutMax);
        }
    }
}
