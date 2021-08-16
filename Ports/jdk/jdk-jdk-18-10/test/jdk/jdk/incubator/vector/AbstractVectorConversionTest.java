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

import jdk.incubator.vector.Vector;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorShape;
import jdk.incubator.vector.VectorMask;
import jdk.incubator.vector.VectorSpecies;
import jdk.incubator.vector.VectorShuffle;
import org.testng.Assert;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.IntFunction;

abstract class AbstractVectorConversionTest {

    @AfterMethod
    public void getRunTime(ITestResult tr) {
        long time = tr.getEndMillis() - tr.getStartMillis();
        System.out.println(tr.getName() + " took " + time + " ms");
    }

    static final int INVOC_COUNT = Integer.getInteger("jdk.incubator.vector.test.loop-iterations", 100);

    static <T> IntFunction<T> withToString(String s, IntFunction<T> f) {
        return new IntFunction<>() {
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

    static byte[] fill_byte(int s, ToByteF f) {
        return fill_byte(new byte[s], f);
    }

    static byte[] fill_byte(byte[] a, ToByteF f) {
        for (int i = 0; i < a.length; i++) {
            a[i] = f.apply(i);
        }
        return a;
    }

    interface ToShortF {
        short apply(int i);
    }

    static short[] fill_short(int s, ToShortF f) {
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

    static int[] fill_int(int s, ToIntF f) {
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

    static long[] fill_long(int s, ToLongF f) {
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

    static float[] fill_float(int s, ToFloatF f) {
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

    static double[] fill_double(int s, ToDoubleF f) {
        return fill_double(new double[s], f);
    }

    static double[] fill_double(double[] a, ToDoubleF f) {
        for (int i = 0; i < a.length; i++) {
            a[i] = f.apply(i);
        }
        return a;
    }

    static final List<IntFunction<byte[]>> BYTE_GENERATORS = List.of(
            withToString("byte(i)", (int s) -> fill_byte(s, i -> (byte) (i + 1)))
    );

    static final List<IntFunction<short[]>> SHORT_GENERATORS = List.of(
            withToString("short(i)", (int s) -> fill_short(s, i -> (short) (i * 100 + 1)))
    );

    static final List<IntFunction<int[]>> INT_GENERATORS = List.of(
            withToString("int(i)", (int s) -> fill_int(s, i -> (int) (i ^ ((i & 1) - 1))))
    );

    static final List<IntFunction<long[]>> LONG_GENERATORS = List.of(
            withToString("long(i)", (int s) -> fill_long(s, i -> (long) (i ^ ((i & 1) - 1))))
    );

    static final List<IntFunction<float[]>> FLOAT_GENERATORS = List.of(
            withToString("float(i)", (int s) -> fill_float(s, i -> (float) (i * 10 + 0.1)))
    );

    static final List<IntFunction<double[]>> DOUBLE_GENERATORS = List.of(
            withToString("double(i)", (int s) -> fill_double(s, i -> (double) (i * 10 + 0.1)))
    );

    static List<?> sourceGenerators(Class<?> src) {
        if (src == byte.class) {
            return BYTE_GENERATORS;
        }
        else if (src == short.class) {
            return SHORT_GENERATORS;
        }
        else if (src == int.class) {
            return INT_GENERATORS;
        }
        else if (src == long.class) {
            return LONG_GENERATORS;
        }
        else if (src == float.class) {
            return FLOAT_GENERATORS;
        }
        else if (src == double.class) {
            return DOUBLE_GENERATORS;
        }
        else
            throw new IllegalStateException();
    }

    static Object[][] fixedShapeXFixedShapeSpeciesArgs(VectorShape shape) {
        List<Object[]> args = new ArrayList<>();

        for (Class<?> srcE : List.of(byte.class, short.class, int.class, long.class, float.class, double.class)) {
            VectorSpecies<?> src = VectorSpecies.of(srcE, shape);
            List<?> srcGens = sourceGenerators(srcE);

            for (Class<?> dstE : List.of(byte.class, short.class, int.class, long.class, float.class, double.class)) {
                VectorSpecies<?> dst = VectorSpecies.of(dstE, shape);

                for (Object srcGen : srcGens) {
                    args.add(new Object[]{src, dst, srcGen});
                }
            }
        }

        return args.toArray(Object[][]::new);
    }

    static Object[][] fixedShapeXShapeSpeciesArgs(VectorShape srcShape) {
        List<Object[]> args = new ArrayList<>();

        for (Class<?> srcE : List.of(byte.class, short.class, int.class, long.class, float.class, double.class)) {
            VectorSpecies<?> src = VectorSpecies.of(srcE, srcShape);
            List<?> srcGens = sourceGenerators(srcE);

            for (VectorShape dstShape : VectorShape.values()) {
                for (Class<?> dstE : List.of(byte.class, short.class, int.class, long.class, float.class, double.class)) {
                    VectorSpecies<?> dst = VectorSpecies.of(dstE, dstShape);

                    for (Object srcGen : srcGens) {
                        args.add(new Object[]{src, dst, srcGen});
                    }
                }
            }
        }

        return args.toArray(Object[][]::new);
    }

    static Object[][] fixedShapeXSegmentedCastSpeciesArgs(VectorShape srcShape, boolean legal) {
        List<Object[]> args = new ArrayList<>();
        for (Class<?> srcE : List.of(byte.class, short.class, int.class, long.class, float.class, double.class)) {
            VectorSpecies<?> src = VectorSpecies.of(srcE, srcShape);
            for (VectorShape dstShape : VectorShape.values()) {
                for (Class<?> dstE : List.of(byte.class, short.class, int.class, long.class, float.class, double.class)) {
                    VectorSpecies<?> dst = VectorSpecies.of(dstE, dstShape);
                    if (legal == (dst.length() == src.length())) {
                        args.add(new Object[]{src, dst});
                    }
                }
            }
        }
        return args.toArray(Object[][]::new);
    }

    public enum ConvAPI {CONVERT, CONVERTSHAPE, CASTSHAPE, REINTERPRETSHAPE}


    static Function<Number, Object> convertValueFunction(Class<?> to) {
        if (to == byte.class)
            return Number::byteValue;
        else if (to == short.class)
            return Number::shortValue;
        else if (to == int.class)
            return Number::intValue;
        else if (to == long.class)
            return Number::longValue;
        else if (to == float.class)
            return Number::floatValue;
        else if (to == double.class)
            return Number::doubleValue;
        else
            throw new IllegalStateException();
    }

    static BiConsumer<ByteBuffer, Object> putBufferValueFunction(Class<?> from) {
        if (from == byte.class)
            return (bb, o) -> bb.put((byte) o);
        else if (from == short.class)
            return (bb, o) -> bb.putShort((short) o);
        else if (from == int.class)
            return (bb, o) -> bb.putInt((int) o);
        else if (from == long.class)
            return (bb, o) -> bb.putLong((long) o);
        else if (from == float.class)
            return (bb, o) -> bb.putFloat((float) o);
        else if (from == double.class)
            return (bb, o) -> bb.putDouble((double) o);
        else
            throw new IllegalStateException();
    }

    static Function<ByteBuffer, Number> getBufferValueFunction(Class<?> to) {
        if (to == byte.class)
            return ByteBuffer::get;
        else if (to == short.class)
            return ByteBuffer::getShort;
        else if (to == int.class)
            return ByteBuffer::getInt;
        else if (to == long.class)
            return ByteBuffer::getLong;
        else if (to == float.class)
            return ByteBuffer::getFloat;
        else if (to == double.class)
            return ByteBuffer::getDouble;
        else
            throw new IllegalStateException();
    }

    static final ClassValue<Object> ZERO = new ClassValue<>() {
        @Override
        protected Object computeValue(Class<?> type) {
            MethodHandle zeroHandle = MethodHandles.zero(type);
            try {
                return zeroHandle.invoke();
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }
    };

    static void zeroArray(Object a, int offset, int length) {
        Object zero = ZERO.get(a.getClass().getComponentType());
        for (int i = 0; i < length; i++) {
            Array.set(a, offset + i, zero);
        }
    }

    static void copyConversionArray(Object src, int srcPos,
                                    Object dest, int destPos,
                                    int length,
                                    Function<Number, Object> c) {
        for (int i = 0; i < length; i++) {
            Number v = (Number) Array.get(src, srcPos + i);
            Array.set(dest, destPos + i, c.apply(v));
        }
    }

    static void
    expanding_reinterpret_scalar(Object in, Object out,
                                 int in_vec_size, int out_vec_size,
                                 int in_vec_lane_cnt, int out_vec_lane_cnt,
                                 int in_idx, int out_idx, int part,
                                 BiConsumer<ByteBuffer, Object> putValue,
                                 Function<ByteBuffer, Number> getValue) {
        int SLICE_FACTOR = Math.max(in_vec_size, out_vec_size) / Math.min(in_vec_size, out_vec_size);
        int ELEMENTS_IN_SLICE = in_vec_lane_cnt / SLICE_FACTOR;
        assert (part < SLICE_FACTOR && part >= 0);

        int start_idx = in_idx + part * ELEMENTS_IN_SLICE;
        int end_idx = start_idx + ELEMENTS_IN_SLICE;

        var bb = ByteBuffer.allocate(out_vec_size).order(ByteOrder.nativeOrder());
        for (int i = start_idx; i < end_idx; i++) {
            Object v = Array.get(in, i);
            putValue.accept(bb, v);
        }
        bb.rewind();

        for (int i = 0; i < out_vec_lane_cnt; i++) {
            Number v = getValue.apply(bb);
            Array.set(out, i + out_idx, v);
        }
    }

    static void
    contracting_reinterpret_scalar(Object in, Object out,
                                   int in_vec_size, int out_vec_size,
                                   int in_vec_lane_cnt, int out_vec_lane_cnt,
                                   int in_idx, int out_idx, int part,
                                   BiConsumer<ByteBuffer, Object> putValue,
                                   Function<ByteBuffer, Number> getValue) {
        int SLICE_FACTOR = Math.max(in_vec_size, out_vec_size) / Math.min(in_vec_size, out_vec_size);
        int ELEMENTS_OUT_SLICE = out_vec_lane_cnt / SLICE_FACTOR;
        assert (part > -SLICE_FACTOR && part <= 0);

        int start_idx = out_idx + (-part) * ELEMENTS_OUT_SLICE;
        int end_idx = start_idx + ELEMENTS_OUT_SLICE;

        zeroArray(out, out_idx, out_vec_lane_cnt);

        var bb = ByteBuffer.allocate(in_vec_size).order(ByteOrder.nativeOrder());
        for (int i = 0; i < in_vec_lane_cnt; i++) {
            Object v = Array.get(in, i + in_idx);
            putValue.accept(bb, v);
        }
        bb.rewind();

        for (int i = start_idx; i < end_idx; i++) {
            Number v = getValue.apply(bb);
            Array.set(out, i, v);
        }
    }

    static int[] getPartsArray(int m, boolean is_contracting_conv) {
        int[] parts = new int[m];
        int part_init = is_contracting_conv ? -m + 1 : 0;
        for (int i = 0; i < parts.length; i++)
            parts[i] = part_init + i;
        return parts;
    }

    static <I, O> void conversion_kernel(VectorSpecies<I> srcSpecies, VectorSpecies<O> destSpecies,
                                         Object in,
                                         ConvAPI conv) {
        VectorOperators.Conversion<I, O> convOp = VectorOperators.Conversion.ofCast(
                srcSpecies.elementType(), destSpecies.elementType());
        int in_len = Array.getLength(in);
        int out_len = (in_len / srcSpecies.length()) * destSpecies.length();
        int src_species_len = srcSpecies.length();
        int dst_species_len = destSpecies.length();
        boolean is_contracting_conv = src_species_len * destSpecies.elementSize() < destSpecies.vectorBitSize();
        int m = Math.max(dst_species_len, src_species_len) / Math.min(src_species_len, dst_species_len);

        int[] parts = getPartsArray(m, is_contracting_conv);

        Object expected = Array.newInstance(destSpecies.elementType(), out_len);
        Object actual = Array.newInstance(destSpecies.elementType(), out_len);

        Function<Number, Object> convertValue = convertValueFunction(destSpecies.elementType());

        // Calculate expected result
        for (int i = 0, j = 0; i < in_len; i += src_species_len, j += dst_species_len) {
            int part = parts[i % parts.length];

            if (is_contracting_conv) {
                int start_idx = -part * src_species_len;
                zeroArray(expected, j, dst_species_len);
                copyConversionArray(in, i, expected, start_idx + j, src_species_len, convertValue);
            } else {
                int start_idx = part * dst_species_len;
                copyConversionArray(in, start_idx + i, expected, j, dst_species_len, convertValue);
            }
        }

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0, j = 0; i < in_len; i += src_species_len, j += dst_species_len) {
                int part = parts[i % parts.length];
                var av = srcSpecies.fromArray(in, i);
                Vector<O> rv = switch(conv) {
                    case CONVERT -> av.convert(convOp, part);
                    case CONVERTSHAPE -> av.convertShape(convOp, destSpecies, part);
                    case CASTSHAPE -> av.castShape(destSpecies, part);
                    case REINTERPRETSHAPE -> throw new UnsupportedOperationException();
                };
                System.arraycopy(rv.toArray(), 0, actual, j, dst_species_len);
            }
        }

        Assert.assertEquals(actual, expected);
    }

    static <I, O> void reinterpret_kernel(VectorSpecies<I> srcSpecies, VectorSpecies<O> dstSpecies,
                                          Object in) {
        int in_len = Array.getLength(in);
        int out_len = (in_len / srcSpecies.length()) * dstSpecies.length();
        int src_vector_size = srcSpecies.vectorBitSize();
        int dst_vector_size = dstSpecies.vectorBitSize();
        int src_species_len = srcSpecies.length();
        int dst_species_len = dstSpecies.length();
        boolean is_contracting_conv = src_vector_size < dst_vector_size;
        int m = Math.max(dst_vector_size, src_vector_size) / Math.min(dst_vector_size, src_vector_size);

        int[] parts = getPartsArray(m, is_contracting_conv);

        Object expected = Array.newInstance(dstSpecies.elementType(), out_len);
        Object actual = Array.newInstance(dstSpecies.elementType(), out_len);

        BiConsumer<ByteBuffer, Object> putValue = putBufferValueFunction(srcSpecies.elementType());
        Function<ByteBuffer, Number> getValue = getBufferValueFunction(dstSpecies.elementType());

        // Calculate expected result
        for (int i = 0, j = 0; i < in_len; i += src_species_len, j += dst_species_len) {
            int part = parts[i % parts.length];

            if (is_contracting_conv) {
                contracting_reinterpret_scalar(in, expected,
                        src_vector_size, dst_vector_size,
                        src_species_len, dst_species_len,
                        i, j, part,
                        putValue, getValue);
            } else {
                expanding_reinterpret_scalar(in, expected,
                        src_vector_size, dst_vector_size,
                        src_species_len, dst_species_len,
                        i, j, part,
                        putValue, getValue);
            }
        }

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0, j = 0; i < in_len; i += src_species_len, j += dst_species_len) {
                int part = parts[i % parts.length];
                var av = srcSpecies.fromArray(in, i);
                var rv = av.reinterpretShape(dstSpecies, part);
                System.arraycopy(rv.toArray(), 0, actual, j, dst_species_len);
            }
        }

        Assert.assertEquals(actual, expected);
    }

    static <E,F> void legal_mask_cast_kernel(VectorSpecies<E> src, VectorSpecies<F> dst) {
        for(int i = 0; i < INVOC_COUNT; i++) {
            VectorMask<E> mask = VectorMask.fromLong(src, i);
            VectorMask<F> res = mask.cast(dst);
            Assert.assertEquals(res.toLong(), mask.toLong());
        }
    }

    static <E,F> void illegal_mask_cast_kernel(VectorSpecies<E> src, VectorSpecies<F> dst) {
        VectorMask<E> mask = VectorMask.fromLong(src, -1);
        try {
            mask.cast(dst);
            Assert.fail();
        } catch (IllegalArgumentException e) {
        }
    }

    static <E,F> void legal_shuffle_cast_kernel(VectorSpecies<E> src, VectorSpecies<F> dst) {
        int [] arr = new int[src.length()*INVOC_COUNT];
        for(int i = 0; i < arr.length; i++) {
            arr[i] = i;
        }
        for(int i = 0; i < INVOC_COUNT; i++) {
            VectorShuffle<E> shuffle = VectorShuffle.fromArray(src, arr, i);
            VectorShuffle<F> res = shuffle.cast(dst);
            Assert.assertEquals(res.toArray(), shuffle.toArray());
        }
    }

    static <E,F> void illegal_shuffle_cast_kernel(VectorSpecies<E> src, VectorSpecies<F> dst) {
        int [] arr = new int[src.length()];
        for(int i = 0; i < arr.length; i++) {
            arr[i] = i;
        }
        VectorShuffle<E> shuffle = VectorShuffle.fromArray(src, arr, 0);
        try {
            shuffle.cast(dst);
            Assert.fail();
        } catch (IllegalArgumentException e) {
        }
    }
}
