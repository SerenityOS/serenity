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

/*
 * @test
 * @modules jdk.incubator.foreign jdk.incubator.vector java.base/jdk.internal.vm.annotation
 * @run testng/othervm -XX:-TieredCompilation Double256VectorLoadStoreTests
 *
 */

// -- This file was mechanically generated: Do not edit! -- //

import jdk.incubator.vector.DoubleVector;
import jdk.incubator.vector.VectorMask;
import jdk.incubator.vector.VectorSpecies;
import jdk.incubator.vector.VectorShuffle;
import jdk.internal.vm.annotation.DontInline;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.ByteOrder;
import java.nio.ReadOnlyBufferException;
import java.util.List;
import java.util.function.*;

@Test
public class Double256VectorLoadStoreTests extends AbstractVectorLoadStoreTest {
    static final VectorSpecies<Double> SPECIES =
                DoubleVector.SPECIES_256;

    static final int INVOC_COUNT = Integer.getInteger("jdk.incubator.vector.test.loop-iterations", 100);


    static final int BUFFER_REPS = Integer.getInteger("jdk.incubator.vector.test.buffer-vectors", 25000 / 256);

    static void assertArraysEquals(double[] r, double[] a, boolean[] mask) {
        int i = 0;
        try {
            for (; i < a.length; i++) {
                Assert.assertEquals(r[i], mask[i % SPECIES.length()] ? a[i] : (double) 0);
            }
        } catch (AssertionError e) {
            Assert.assertEquals(r[i], mask[i % SPECIES.length()] ? a[i] : (double) 0, "at index #" + i);
        }
    }

    static void assertArraysEquals(byte[] r, byte[] a, boolean[] mask) {
        int i = 0;
        try {
            for (; i < a.length; i++) {
                Assert.assertEquals(r[i], mask[(i*8/SPECIES.elementSize()) % SPECIES.length()] ? a[i] : (byte) 0);
            }
        } catch (AssertionError e) {
            Assert.assertEquals(r[i], mask[(i*8/SPECIES.elementSize()) % SPECIES.length()] ? a[i] : (byte) 0, "at index #" + i);
        }
    }

    static final List<IntFunction<double[]>> DOUBLE_GENERATORS = List.of(
            withToString("double[i * 5]", (int s) -> {
                return fill(s * BUFFER_REPS,
                            i -> (double)(i * 5));
            }),
            withToString("double[i + 1]", (int s) -> {
                return fill(s * BUFFER_REPS,
                            i -> (((double)(i + 1) == 0) ? 1 : (double)(i + 1)));
            })
    );

    // Relative to array.length
    static final List<IntFunction<Integer>> INDEX_GENERATORS = List.of(
            withToString("-1", (int l) -> {
                return -1;
            }),
            withToString("l", (int l) -> {
                return l;
            }),
            withToString("l - 1", (int l) -> {
                return l - 1;
            }),
            withToString("l + 1", (int l) -> {
                return l + 1;
            }),
            withToString("l - speciesl + 1", (int l) -> {
                return l - SPECIES.length() + 1;
            }),
            withToString("l + speciesl - 1", (int l) -> {
                return l + SPECIES.length() - 1;
            }),
            withToString("l + speciesl", (int l) -> {
                return l + SPECIES.length();
            }),
            withToString("l + speciesl + 1", (int l) -> {
                return l + SPECIES.length() + 1;
            })
    );

    // Relative to byte[] array.length or ByteBuffer.limit()
    static final List<IntFunction<Integer>> BYTE_INDEX_GENERATORS = List.of(
            withToString("-1", (int l) -> {
                return -1;
            }),
            withToString("l", (int l) -> {
                return l;
            }),
            withToString("l - 1", (int l) -> {
                return l - 1;
            }),
            withToString("l + 1", (int l) -> {
                return l + 1;
            }),
            withToString("l - speciesl*ebsize + 1", (int l) -> {
                return l - SPECIES.vectorByteSize() + 1;
            }),
            withToString("l + speciesl*ebsize - 1", (int l) -> {
                return l + SPECIES.vectorByteSize() - 1;
            }),
            withToString("l + speciesl*ebsize", (int l) -> {
                return l + SPECIES.vectorByteSize();
            }),
            withToString("l + speciesl*ebsize + 1", (int l) -> {
                return l + SPECIES.vectorByteSize() + 1;
            })
    );

    @DataProvider
    public Object[][] doubleProvider() {
        return DOUBLE_GENERATORS.stream().
                map(f -> new Object[]{f}).
                toArray(Object[][]::new);
    }

    @DataProvider
    public Object[][] maskProvider() {
        return BOOLEAN_MASK_GENERATORS.stream().
                map(f -> new Object[]{f}).
                toArray(Object[][]::new);
    }

    @DataProvider
    public Object[][] doubleProviderForIOOBE() {
        var f = DOUBLE_GENERATORS.get(0);
        return INDEX_GENERATORS.stream().map(fi -> {
                    return new Object[] {f, fi};
                }).
                toArray(Object[][]::new);
    }

    @DataProvider
    public Object[][] doubleMaskProvider() {
        return BOOLEAN_MASK_GENERATORS.stream().
                flatMap(fm -> DOUBLE_GENERATORS.stream().map(fa -> {
                    return new Object[] {fa, fm};
                })).
                toArray(Object[][]::new);
    }

    @DataProvider
    public Object[][] doubleMaskProviderForIOOBE() {
        var f = DOUBLE_GENERATORS.get(0);
        return BOOLEAN_MASK_GENERATORS.stream().
                flatMap(fm -> INDEX_GENERATORS.stream().map(fi -> {
                    return new Object[] {f, fi, fm};
                })).
                toArray(Object[][]::new);
    }

    @DataProvider
    public Object[][] doubleByteBufferProvider() {
        return DOUBLE_GENERATORS.stream().
                flatMap(fa -> BYTE_BUFFER_GENERATORS.stream().
                        flatMap(fb -> BYTE_ORDER_VALUES.stream().map(bo -> {
                            return new Object[]{fa, fb, bo};
                        }))).
                toArray(Object[][]::new);
    }

    @DataProvider
    public Object[][] doubleByteBufferMaskProvider() {
        return BOOLEAN_MASK_GENERATORS.stream().
                flatMap(fm -> DOUBLE_GENERATORS.stream().
                        flatMap(fa -> BYTE_BUFFER_GENERATORS.stream().
                                flatMap(fb -> BYTE_ORDER_VALUES.stream().map(bo -> {
                            return new Object[]{fa, fb, fm, bo};
                        })))).
                toArray(Object[][]::new);
    }

    @DataProvider
    public Object[][] doubleByteArrayProvider() {
        return DOUBLE_GENERATORS.stream().
                flatMap(fa -> BYTE_ORDER_VALUES.stream().map(bo -> {
                    return new Object[]{fa, bo};
                })).
                toArray(Object[][]::new);
    }

    @DataProvider
    public Object[][] doubleByteArrayMaskProvider() {
        return BOOLEAN_MASK_GENERATORS.stream().
                flatMap(fm -> DOUBLE_GENERATORS.stream().
                    flatMap(fa -> BYTE_ORDER_VALUES.stream().map(bo -> {
                        return new Object[]{fa, fm, bo};
                    }))).
                toArray(Object[][]::new);
    }

    @DataProvider
    public Object[][] doubleByteProviderForIOOBE() {
        var f = DOUBLE_GENERATORS.get(0);
        return BYTE_INDEX_GENERATORS.stream().map(fi -> {
                    return new Object[] {f, fi};
                }).
                toArray(Object[][]::new);
    }

    @DataProvider
    public Object[][] doubleByteMaskProviderForIOOBE() {
        var f = DOUBLE_GENERATORS.get(0);
        return BOOLEAN_MASK_GENERATORS.stream().
                flatMap(fm -> BYTE_INDEX_GENERATORS.stream().map(fi -> {
                    return new Object[] {f, fi, fm};
                })).
                toArray(Object[][]::new);
    }

    static ByteBuffer toBuffer(double[] a, IntFunction<ByteBuffer> fb) {
        ByteBuffer bb = fb.apply(a.length * SPECIES.elementSize() / 8);
        for (double v : a) {
            bb.putDouble(v);
        }
        return bb.clear();
    }

    static double[] bufferToArray(ByteBuffer bb) {
        DoubleBuffer db = bb.asDoubleBuffer();
        double[] d = new double[db.capacity()];
        db.get(0, d);
        return d;
    }

    static byte[] toByteArray(double[] a, IntFunction<byte[]> fb, ByteOrder bo) {
        byte[] b = fb.apply(a.length * SPECIES.elementSize() / 8);
        DoubleBuffer bb = ByteBuffer.wrap(b, 0, b.length).order(bo).asDoubleBuffer();
        for (double v : a) {
            bb.put(v);
        }
        return b;
    }


    interface ToDoubleF {
        double apply(int i);
    }

    static double[] fill(int s , ToDoubleF f) {
        return fill(new double[s], f);
    }

    static double[] fill(double[] a, ToDoubleF f) {
        for (int i = 0; i < a.length; i++) {
            a[i] = f.apply(i);
        }
        return a;
    }

    @DontInline
    static DoubleVector fromArray(double[] a, int i) {
        return DoubleVector.fromArray(SPECIES, a, i);
    }

    @DontInline
    static DoubleVector fromArray(double[] a, int i, VectorMask<Double> m) {
        return DoubleVector.fromArray(SPECIES, a, i, m);
    }

    @DontInline
    static void intoArray(DoubleVector v, double[] a, int i) {
        v.intoArray(a, i);
    }

    @DontInline
    static void intoArray(DoubleVector v, double[] a, int i, VectorMask<Double> m) {
        v.intoArray(a, i, m);
    }

    @DontInline
    static DoubleVector fromByteArray(byte[] a, int i, ByteOrder bo) {
        return DoubleVector.fromByteArray(SPECIES, a, i, bo);
    }

    @DontInline
    static DoubleVector fromByteArray(byte[] a, int i, ByteOrder bo, VectorMask<Double> m) {
        return DoubleVector.fromByteArray(SPECIES, a, i, bo, m);
    }

    @DontInline
    static void intoByteArray(DoubleVector v, byte[] a, int i, ByteOrder bo) {
        v.intoByteArray(a, i, bo);
    }

    @DontInline
    static void intoByteArray(DoubleVector v, byte[] a, int i, ByteOrder bo, VectorMask<Double> m) {
        v.intoByteArray(a, i, bo, m);
    }

    @DontInline
    static DoubleVector fromByteBuffer(ByteBuffer a, int i, ByteOrder bo) {
        return DoubleVector.fromByteBuffer(SPECIES, a, i, bo);
    }

    @DontInline
    static DoubleVector fromByteBuffer(ByteBuffer a, int i, ByteOrder bo, VectorMask<Double> m) {
        return DoubleVector.fromByteBuffer(SPECIES, a, i, bo, m);
    }

    @DontInline
    static void intoByteBuffer(DoubleVector v, ByteBuffer a, int i, ByteOrder bo) {
        v.intoByteBuffer(a, i, bo);
    }

    @DontInline
    static void intoByteBuffer(DoubleVector v, ByteBuffer a, int i, ByteOrder bo, VectorMask<Double> m) {
        v.intoByteBuffer(a, i, bo, m);
    }


    @Test(dataProvider = "doubleProvider")
    static void loadStoreArray(IntFunction<double[]> fa) {
        double[] a = fa.apply(SPECIES.length());
        double[] r = new double[a.length];

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                av.intoArray(r, i);
            }
        }
        Assert.assertEquals(r, a);
    }

    @Test(dataProvider = "doubleProviderForIOOBE")
    static void loadArrayIOOBE(IntFunction<double[]> fa, IntFunction<Integer> fi) {
        double[] a = fa.apply(SPECIES.length());
        double[] r = new double[a.length];

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = fromArray(a, i);
                av.intoArray(r, i);
            }
        }

        int index = fi.apply(a.length);
        boolean shouldFail = isIndexOutOfBounds(SPECIES.length(), index, a.length);
        try {
            fromArray(a, index);
            if (shouldFail) {
                Assert.fail("Failed to throw IndexOutOfBoundsException");
            }
        } catch (IndexOutOfBoundsException e) {
            if (!shouldFail) {
                Assert.fail("Unexpected IndexOutOfBoundsException");
            }
        }
    }

    @Test(dataProvider = "doubleProviderForIOOBE")
    static void storeArrayIOOBE(IntFunction<double[]> fa, IntFunction<Integer> fi) {
        double[] a = fa.apply(SPECIES.length());
        double[] r = new double[a.length];

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                intoArray(av, r, i);
            }
        }

        int index = fi.apply(a.length);
        boolean shouldFail = isIndexOutOfBounds(SPECIES.length(), index, a.length);
        try {
            DoubleVector av = DoubleVector.fromArray(SPECIES, a, 0);
            intoArray(av, r, index);
            if (shouldFail) {
                Assert.fail("Failed to throw IndexOutOfBoundsException");
            }
        } catch (IndexOutOfBoundsException e) {
            if (!shouldFail) {
                Assert.fail("Unexpected IndexOutOfBoundsException");
            }
        }
    }


    @Test(dataProvider = "doubleMaskProvider")
    static void loadStoreMaskArray(IntFunction<double[]> fa,
                                   IntFunction<boolean[]> fm) {
        double[] a = fa.apply(SPECIES.length());
        double[] r = new double[a.length];
        boolean[] mask = fm.apply(SPECIES.length());
        VectorMask<Double> vmask = VectorMask.fromValues(SPECIES, mask);

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i, vmask);
                av.intoArray(r, i);
            }
        }
        assertArraysEquals(r, a, mask);


        r = new double[a.length];

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                av.intoArray(r, i, vmask);
            }
        }
        assertArraysEquals(r, a, mask);
    }

    @Test(dataProvider = "doubleMaskProviderForIOOBE")
    static void loadArrayMaskIOOBE(IntFunction<double[]> fa, IntFunction<Integer> fi, IntFunction<boolean[]> fm) {
        double[] a = fa.apply(SPECIES.length());
        double[] r = new double[a.length];
        boolean[] mask = fm.apply(SPECIES.length());
        VectorMask<Double> vmask = VectorMask.fromValues(SPECIES, mask);

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = fromArray(a, i, vmask);
                av.intoArray(r, i);
            }
        }

        int index = fi.apply(a.length);
        boolean shouldFail = isIndexOutOfBoundsForMask(mask, index, a.length);
        try {
            fromArray(a, index, vmask);
            if (shouldFail) {
                Assert.fail("Failed to throw IndexOutOfBoundsException");
            }
        } catch (IndexOutOfBoundsException e) {
            if (!shouldFail) {
                Assert.fail("Unexpected IndexOutOfBoundsException");
            }
        }
    }

    @Test(dataProvider = "doubleMaskProviderForIOOBE")
    static void storeArrayMaskIOOBE(IntFunction<double[]> fa, IntFunction<Integer> fi, IntFunction<boolean[]> fm) {
        double[] a = fa.apply(SPECIES.length());
        double[] r = new double[a.length];
        boolean[] mask = fm.apply(SPECIES.length());
        VectorMask<Double> vmask = VectorMask.fromValues(SPECIES, mask);

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                intoArray(av, r, i, vmask);
            }
        }

        int index = fi.apply(a.length);
        boolean shouldFail = isIndexOutOfBoundsForMask(mask, index, a.length);
        try {
            DoubleVector av = DoubleVector.fromArray(SPECIES, a, 0);
            intoArray(av, a, index, vmask);
            if (shouldFail) {
                Assert.fail("Failed to throw IndexOutOfBoundsException");
            }
        } catch (IndexOutOfBoundsException e) {
            if (!shouldFail) {
                Assert.fail("Unexpected IndexOutOfBoundsException");
            }
        }
    }


    @Test(dataProvider = "doubleMaskProvider")
    static void loadStoreMask(IntFunction<double[]> fa,
                              IntFunction<boolean[]> fm) {
        boolean[] mask = fm.apply(SPECIES.length());
        boolean[] r = new boolean[mask.length];

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < mask.length; i += SPECIES.length()) {
                VectorMask<Double> vmask = VectorMask.fromArray(SPECIES, mask, i);
                vmask.intoArray(r, i);
            }
        }
        Assert.assertEquals(r, mask);
    }


    @Test(dataProvider = "doubleByteBufferProvider")
    static void loadStoreByteBuffer(IntFunction<double[]> fa,
                                    IntFunction<ByteBuffer> fb,
                                    ByteOrder bo) {
        ByteBuffer a = toBuffer(fa.apply(SPECIES.length()), fb);
        ByteBuffer r = fb.apply(a.limit());

        int l = a.limit();
        int s = SPECIES.vectorByteSize();

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < l; i += s) {
                DoubleVector av = DoubleVector.fromByteBuffer(SPECIES, a, i, bo);
                av.intoByteBuffer(r, i, bo);
            }
        }
        Assert.assertEquals(a.position(), 0, "Input buffer position changed");
        Assert.assertEquals(a.limit(), l, "Input buffer limit changed");
        Assert.assertEquals(r.position(), 0, "Result buffer position changed");
        Assert.assertEquals(r.limit(), l, "Result buffer limit changed");
        Assert.assertEquals(r, a, "Buffers not equal");
    }

    @Test(dataProvider = "doubleByteProviderForIOOBE")
    static void loadByteBufferIOOBE(IntFunction<double[]> fa, IntFunction<Integer> fi) {
        ByteBuffer a = toBuffer(fa.apply(SPECIES.length()), ByteBuffer::allocateDirect);
        ByteBuffer r = ByteBuffer.allocateDirect(a.limit());

        int l = a.limit();
        int s = SPECIES.vectorByteSize();

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < l; i += s) {
                DoubleVector av = fromByteBuffer(a, i, ByteOrder.nativeOrder());
                av.intoByteBuffer(r, i, ByteOrder.nativeOrder());
            }
        }

        int index = fi.apply(a.limit());
        boolean shouldFail = isIndexOutOfBounds(SPECIES.vectorByteSize(), index, a.limit());
        try {
            fromByteBuffer(a, index, ByteOrder.nativeOrder());
            if (shouldFail) {
                Assert.fail("Failed to throw IndexOutOfBoundsException");
            }
        } catch (IndexOutOfBoundsException e) {
            if (!shouldFail) {
                Assert.fail("Unexpected IndexOutOfBoundsException");
            }
        }
    }

    @Test(dataProvider = "doubleByteProviderForIOOBE")
    static void storeByteBufferIOOBE(IntFunction<double[]> fa, IntFunction<Integer> fi) {
        ByteBuffer a = toBuffer(fa.apply(SPECIES.length()), ByteBuffer::allocateDirect);
        ByteBuffer r = ByteBuffer.allocateDirect(a.limit());

        int l = a.limit();
        int s = SPECIES.vectorByteSize();

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < l; i += s) {
                DoubleVector av = DoubleVector.fromByteBuffer(SPECIES, a, i, ByteOrder.nativeOrder());
                intoByteBuffer(av, r, i, ByteOrder.nativeOrder());
            }
        }

        int index = fi.apply(a.limit());
        boolean shouldFail = isIndexOutOfBounds(SPECIES.vectorByteSize(), index, a.limit());
        try {
            DoubleVector av = DoubleVector.fromByteBuffer(SPECIES, a, 0, ByteOrder.nativeOrder());
            intoByteBuffer(av, r, index, ByteOrder.nativeOrder());
            if (shouldFail) {
                Assert.fail("Failed to throw IndexOutOfBoundsException");
            }
        } catch (IndexOutOfBoundsException e) {
            if (!shouldFail) {
                Assert.fail("Unexpected IndexOutOfBoundsException");
            }
        }
    }


    @Test(dataProvider = "doubleByteBufferMaskProvider")
    static void loadStoreByteBufferMask(IntFunction<double[]> fa,
                                        IntFunction<ByteBuffer> fb,
                                        IntFunction<boolean[]> fm,
                                        ByteOrder bo) {
        double[] _a = fa.apply(SPECIES.length());
        ByteBuffer a = toBuffer(_a, fb);
        ByteBuffer r = fb.apply(a.limit());
        boolean[] mask = fm.apply(SPECIES.length());
        VectorMask<Double> vmask = VectorMask.fromValues(SPECIES, mask);

        int l = a.limit();
        int s = SPECIES.vectorByteSize();

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < l; i += s) {
                DoubleVector av = DoubleVector.fromByteBuffer(SPECIES, a, i, bo, vmask);
                av.intoByteBuffer(r, i, bo);
            }
        }
        Assert.assertEquals(a.position(), 0, "Input buffer position changed");
        Assert.assertEquals(a.limit(), l, "Input buffer limit changed");
        Assert.assertEquals(r.position(), 0, "Result buffer position changed");
        Assert.assertEquals(r.limit(), l, "Result buffer limit changed");
        assertArraysEquals(bufferToArray(r), _a, mask);


        r = fb.apply(a.limit());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < l; i += s) {
                DoubleVector av = DoubleVector.fromByteBuffer(SPECIES, a, i, bo);
                av.intoByteBuffer(r, i, bo, vmask);
            }
        }
        Assert.assertEquals(a.position(), 0, "Input buffer position changed");
        Assert.assertEquals(a.limit(), l, "Input buffer limit changed");
        Assert.assertEquals(r.position(), 0, "Result buffer position changed");
        Assert.assertEquals(r.limit(), l, "Result buffer limit changed");
        assertArraysEquals(bufferToArray(r), _a, mask);
    }

    @Test(dataProvider = "doubleByteMaskProviderForIOOBE")
    static void loadByteBufferMaskIOOBE(IntFunction<double[]> fa, IntFunction<Integer> fi, IntFunction<boolean[]> fm) {
        ByteBuffer a = toBuffer(fa.apply(SPECIES.length()), ByteBuffer::allocateDirect);
        ByteBuffer r = ByteBuffer.allocateDirect(a.limit());
        boolean[] mask = fm.apply(SPECIES.length());
        VectorMask<Double> vmask = VectorMask.fromValues(SPECIES, mask);

        int l = a.limit();
        int s = SPECIES.vectorByteSize();

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < l; i += s) {
                DoubleVector av = fromByteBuffer(a, i, ByteOrder.nativeOrder(), vmask);
                av.intoByteBuffer(r, i, ByteOrder.nativeOrder());
            }
        }

        int index = fi.apply(a.limit());
        boolean shouldFail = isIndexOutOfBoundsForMask(mask, index, a.limit(), SPECIES.elementSize() / 8);
        try {
            fromByteBuffer(a, index, ByteOrder.nativeOrder(), vmask);
            if (shouldFail) {
                Assert.fail("Failed to throw IndexOutOfBoundsException");
            }
        } catch (IndexOutOfBoundsException e) {
            if (!shouldFail) {
                Assert.fail("Unexpected IndexOutOfBoundsException");
            }
        }
    }

    @Test(dataProvider = "doubleByteMaskProviderForIOOBE")
    static void storeByteBufferMaskIOOBE(IntFunction<double[]> fa, IntFunction<Integer> fi, IntFunction<boolean[]> fm) {
        ByteBuffer a = toBuffer(fa.apply(SPECIES.length()), ByteBuffer::allocateDirect);
        ByteBuffer r = ByteBuffer.allocateDirect(a.limit());
        boolean[] mask = fm.apply(SPECIES.length());
        VectorMask<Double> vmask = VectorMask.fromValues(SPECIES, mask);

        int l = a.limit();
        int s = SPECIES.vectorByteSize();

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < l; i += s) {
                DoubleVector av = DoubleVector.fromByteBuffer(SPECIES, a, i, ByteOrder.nativeOrder());
                intoByteBuffer(av, r, i, ByteOrder.nativeOrder(), vmask);
            }
        }

        int index = fi.apply(a.limit());
        boolean shouldFail = isIndexOutOfBoundsForMask(mask, index, a.limit(), SPECIES.elementSize() / 8);
        try {
            DoubleVector av = DoubleVector.fromByteBuffer(SPECIES, a, 0, ByteOrder.nativeOrder());
            intoByteBuffer(av, a, index, ByteOrder.nativeOrder(), vmask);
            if (shouldFail) {
                Assert.fail("Failed to throw IndexOutOfBoundsException");
            }
        } catch (IndexOutOfBoundsException e) {
            if (!shouldFail) {
                Assert.fail("Unexpected IndexOutOfBoundsException");
            }
        }
    }


    @Test(dataProvider = "doubleByteBufferProvider")
    static void loadStoreReadonlyByteBuffer(IntFunction<double[]> fa,
                                    IntFunction<ByteBuffer> fb,
                                    ByteOrder bo) {
        ByteBuffer a = toBuffer(fa.apply(SPECIES.length()), fb).asReadOnlyBuffer();

        try {
            SPECIES.zero().intoByteBuffer(a, 0, bo);
            Assert.fail("ReadOnlyBufferException expected");
        } catch (ReadOnlyBufferException e) {
        }

        try {
            SPECIES.zero().intoByteBuffer(a, 0, bo, SPECIES.maskAll(true));
            Assert.fail("ReadOnlyBufferException expected");
        } catch (ReadOnlyBufferException e) {
        }

        try {
            SPECIES.zero().intoByteBuffer(a, 0, bo, SPECIES.maskAll(false));
            Assert.fail("ReadOnlyBufferException expected");
        } catch (ReadOnlyBufferException e) {
        }

        try {
            VectorMask<Double> m = SPECIES.shuffleFromOp(i -> i % 2 == 0 ? 1 : -1)
                    .laneIsValid();
            SPECIES.zero().intoByteBuffer(a, 0, bo, m);
            Assert.fail("ReadOnlyBufferException expected");
        } catch (ReadOnlyBufferException e) {
        }
    }


    @Test(dataProvider = "doubleByteArrayProvider")
    static void loadStoreByteArray(IntFunction<double[]> fa,
                                    ByteOrder bo) {
        byte[] a = toByteArray(fa.apply(SPECIES.length()), byte[]::new, bo);
        byte[] r = new byte[a.length];

        int s = SPECIES.vectorByteSize();
        int l = a.length;

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < l; i += s) {
                DoubleVector av = DoubleVector.fromByteArray(SPECIES, a, i, bo);
                av.intoByteArray(r, i, bo);
            }
        }
        Assert.assertEquals(r, a, "Byte arrays not equal");
    }

    @Test(dataProvider = "doubleByteProviderForIOOBE")
    static void loadByteArrayIOOBE(IntFunction<double[]> fa, IntFunction<Integer> fi) {
        byte[] a = toByteArray(fa.apply(SPECIES.length()), byte[]::new, ByteOrder.nativeOrder());
        byte[] r = new byte[a.length];

        int s = SPECIES.vectorByteSize();
        int l = a.length;

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < l; i += s) {
                DoubleVector av = fromByteArray(a, i, ByteOrder.nativeOrder());
                av.intoByteArray(r, i, ByteOrder.nativeOrder());
            }
        }

        int index = fi.apply(a.length);
        boolean shouldFail = isIndexOutOfBounds(SPECIES.vectorByteSize(), index, a.length);
        try {
            fromByteArray(a, index, ByteOrder.nativeOrder());
            if (shouldFail) {
                Assert.fail("Failed to throw IndexOutOfBoundsException");
            }
        } catch (IndexOutOfBoundsException e) {
            if (!shouldFail) {
                Assert.fail("Unexpected IndexOutOfBoundsException");
            }
        }
    }

    @Test(dataProvider = "doubleByteProviderForIOOBE")
    static void storeByteArrayIOOBE(IntFunction<double[]> fa, IntFunction<Integer> fi) {
        byte[] a = toByteArray(fa.apply(SPECIES.length()), byte[]::new, ByteOrder.nativeOrder());
        byte[] r = new byte[a.length];

        int s = SPECIES.vectorByteSize();
        int l = a.length;

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < l; i += s) {
                DoubleVector av = DoubleVector.fromByteArray(SPECIES, a, i, ByteOrder.nativeOrder());
                intoByteArray(av, r, i, ByteOrder.nativeOrder());
            }
        }

        int index = fi.apply(a.length);
        boolean shouldFail = isIndexOutOfBounds(SPECIES.vectorByteSize(), index, a.length);
        try {
            DoubleVector av = DoubleVector.fromByteArray(SPECIES, a, 0, ByteOrder.nativeOrder());
            intoByteArray(av, r, index, ByteOrder.nativeOrder());
            if (shouldFail) {
                Assert.fail("Failed to throw IndexOutOfBoundsException");
            }
        } catch (IndexOutOfBoundsException e) {
            if (!shouldFail) {
                Assert.fail("Unexpected IndexOutOfBoundsException");
            }
        }
    }


    @Test(dataProvider = "doubleByteArrayMaskProvider")
    static void loadStoreByteArrayMask(IntFunction<double[]> fa,
                                  IntFunction<boolean[]> fm,
                                  ByteOrder bo) {
        byte[] a = toByteArray(fa.apply(SPECIES.length()), byte[]::new, bo);
        byte[] r = new byte[a.length];
        boolean[] mask = fm.apply(SPECIES.length());
        VectorMask<Double> vmask = VectorMask.fromValues(SPECIES, mask);

        int s = SPECIES.vectorByteSize();
        int l = a.length;

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
          for (int i = 0; i < l; i += s) {
              DoubleVector av = DoubleVector.fromByteArray(SPECIES, a, i, bo, vmask);
              av.intoByteArray(r, i, bo);
          }
        }
        assertArraysEquals(r, a, mask);


        r = new byte[a.length];

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < l; i += s) {
                DoubleVector av = DoubleVector.fromByteArray(SPECIES, a, i, bo);
                av.intoByteArray(r, i, bo, vmask);
            }
        }
        assertArraysEquals(r, a, mask);
    }

    @Test(dataProvider = "doubleByteMaskProviderForIOOBE")
    static void loadByteArrayMaskIOOBE(IntFunction<double[]> fa, IntFunction<Integer> fi, IntFunction<boolean[]> fm) {
        byte[] a = toByteArray(fa.apply(SPECIES.length()), byte[]::new, ByteOrder.nativeOrder());
        byte[] r = new byte[a.length];
        boolean[] mask = fm.apply(SPECIES.length());
        VectorMask<Double> vmask = VectorMask.fromValues(SPECIES, mask);

        int s = SPECIES.vectorByteSize();
        int l = a.length;

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < l; i += s) {
                DoubleVector av = fromByteArray(a, i, ByteOrder.nativeOrder(), vmask);
                av.intoByteArray(r, i, ByteOrder.nativeOrder());
            }
        }

        int index = fi.apply(a.length);
        boolean shouldFail = isIndexOutOfBoundsForMask(mask, index, a.length, SPECIES.elementSize() / 8);
        try {
            fromByteArray(a, index, ByteOrder.nativeOrder(), vmask);
            if (shouldFail) {
                Assert.fail("Failed to throw IndexOutOfBoundsException");
            }
        } catch (IndexOutOfBoundsException e) {
            if (!shouldFail) {
                Assert.fail("Unexpected IndexOutOfBoundsException");
            }
        }
    }

    @Test(dataProvider = "doubleByteMaskProviderForIOOBE")
    static void storeByteArrayMaskIOOBE(IntFunction<double[]> fa, IntFunction<Integer> fi, IntFunction<boolean[]> fm) {
        byte[] a = toByteArray(fa.apply(SPECIES.length()), byte[]::new, ByteOrder.nativeOrder());
        byte[] r = new byte[a.length];
        boolean[] mask = fm.apply(SPECIES.length());
        VectorMask<Double> vmask = VectorMask.fromValues(SPECIES, mask);

        int s = SPECIES.vectorByteSize();
        int l = a.length;

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < l; i += s) {
                DoubleVector av = DoubleVector.fromByteArray(SPECIES, a, i, ByteOrder.nativeOrder());
                intoByteArray(av, r, i, ByteOrder.nativeOrder(), vmask);
            }
        }

        int index = fi.apply(a.length);
        boolean shouldFail = isIndexOutOfBoundsForMask(mask, index, a.length, SPECIES.elementSize() / 8);
        try {
            DoubleVector av = DoubleVector.fromByteArray(SPECIES, a, 0, ByteOrder.nativeOrder());
            intoByteArray(av, a, index, ByteOrder.nativeOrder(), vmask);
            if (shouldFail) {
                Assert.fail("Failed to throw IndexOutOfBoundsException");
            }
        } catch (IndexOutOfBoundsException e) {
            if (!shouldFail) {
                Assert.fail("Unexpected IndexOutOfBoundsException");
            }
        }
    }

    @Test(dataProvider = "maskProvider")
    static void loadStoreMask(IntFunction<boolean[]> fm) {
        boolean[] a = fm.apply(SPECIES.length());
        boolean[] r = new boolean[a.length];

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                VectorMask<Double> vmask = SPECIES.loadMask(a, i);
                vmask.intoArray(r, i);
            }
        }
        Assert.assertEquals(r, a);
    }

    @Test
    static void loadStoreShuffle() {
        IntUnaryOperator fn = a -> a + 5;
        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            var shuffle = VectorShuffle.fromOp(SPECIES, fn);
            int [] r = shuffle.toArray();

            int [] a = expectedShuffle(SPECIES.length(), fn);
            Assert.assertEquals(r, a);
       }
    }





    // Gather/Scatter load/store tests

    static void assertGatherArraysEquals(double[] r, double[] a, int[] indexMap) {
        int i = 0;
        int j = 0;
        try {
            for (; i < a.length; i += SPECIES.length()) {
                j = i;
                for (; j < i + SPECIES.length(); j++) {
                    Assert.assertEquals(r[j], a[i + indexMap[j]]);
                }
            }
        } catch (AssertionError e) {
            Assert.assertEquals(r[j], a[i + indexMap[j]], "at index #" + j);
        }
    }

    static void assertGatherArraysEquals(double[] r, double[] a, int[] indexMap, boolean[] mask) {
        int i = 0;
        int j = 0;
        try {
            for (; i < a.length; i += SPECIES.length()) {
                j = i;
                for (; j < i + SPECIES.length(); j++) {
                    Assert.assertEquals(r[j], mask[j % SPECIES.length()] ? a[i + indexMap[j]]: (double) 0);
                }
            }
        } catch (AssertionError e) {
            Assert.assertEquals(r[i], mask[j % SPECIES.length()] ? a[i + indexMap[j]]: (double) 0, "at index #" + j);
        }
    }

    static void assertScatterArraysEquals(double[] r, double[] a, int[] indexMap, boolean[] mask) {
        double[] expected = new double[r.length];

        // Store before checking, since the same location may be stored to more than once
        for (int i = 0; i < a.length; i += SPECIES.length()) {
            for (int j = i; j < i + SPECIES.length(); j++) {
                if (mask[j % SPECIES.length()]) {
                    expected[i + indexMap[j]] = a[j];
                }
            }
        }

        Assert.assertEquals(r, expected);
    }

    static void assertScatterArraysEquals(double[] r, double[] a, int[] indexMap) {
        double[] expected = new double[r.length];

        // Store before checking, since the same location may be stored to more than once
        for (int i = 0; i < a.length; i += SPECIES.length()) {
            for (int j = i; j < i + SPECIES.length(); j++) {
                expected[i + indexMap[j]] = a[j];
            }
        }

        Assert.assertEquals(r, expected);
    }

    @DataProvider
    public Object[][] gatherScatterProvider() {
        return INT_INDEX_GENERATORS.stream().
                flatMap(fs -> DOUBLE_GENERATORS.stream().map(fa -> {
                    return new Object[] {fa, fs};
                })).
                toArray(Object[][]::new);
    }

    @DataProvider
    public Object[][] gatherScatterMaskProvider() {
        return BOOLEAN_MASK_GENERATORS.stream().
          flatMap(fs -> INT_INDEX_GENERATORS.stream().flatMap(fm ->
            DOUBLE_GENERATORS.stream().map(fa -> {
                    return new Object[] {fa, fm, fs};
            }))).
            toArray(Object[][]::new);
    }


    @Test(dataProvider = "gatherScatterProvider")
    static void gather(IntFunction<double[]> fa, BiFunction<Integer,Integer,int[]> fs) {
        double[] a = fa.apply(SPECIES.length());
        int[] b = fs.apply(a.length, SPECIES.length());
        double[] r = new double[a.length];

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i, b, i);
                av.intoArray(r, i);
            }
        }

        assertGatherArraysEquals(r, a, b);
    }

    @Test(dataProvider = "gatherScatterMaskProvider")
    static void gatherMask(IntFunction<double[]> fa, BiFunction<Integer,Integer,int[]> fs, IntFunction<boolean[]> fm) {
        double[] a = fa.apply(SPECIES.length());
        int[] b = fs.apply(a.length, SPECIES.length());
        double[] r = new double[a.length];
        boolean[] mask = fm.apply(SPECIES.length());
        VectorMask<Double> vmask = VectorMask.fromArray(SPECIES, mask, 0);

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i, b, i, vmask);
                av.intoArray(r, i);
            }
        }

        assertGatherArraysEquals(r, a, b, mask);
    }

    @Test(dataProvider = "gatherScatterProvider")
    static void scatter(IntFunction<double[]> fa, BiFunction<Integer,Integer,int[]> fs) {
        double[] a = fa.apply(SPECIES.length());
        int[] b = fs.apply(a.length, SPECIES.length());
        double[] r = new double[a.length];

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                av.intoArray(r, i, b, i);
            }
        }

        assertScatterArraysEquals(r, a, b);
    }

    @Test(dataProvider = "gatherScatterMaskProvider")
    static void scatterMask(IntFunction<double[]> fa, BiFunction<Integer,Integer,int[]> fs, IntFunction<boolean[]> fm) {
        double[] a = fa.apply(SPECIES.length());
        int[] b = fs.apply(a.length, SPECIES.length());
        double[] r = new double[a.length];
        boolean[] mask = fm.apply(SPECIES.length());
        VectorMask<Double> vmask = VectorMask.fromArray(SPECIES, mask, 0);

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                av.intoArray(r, i, b, i, vmask);
            }
        }

        assertScatterArraysEquals(r, a, b, mask);
    }



}
