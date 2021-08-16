/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.LongFunction;
import java.util.stream.IntStream;

/*
 * @test
 * @bug 8193085 8199773
 * @summary tests for buffer equals and compare
 * @run testng EqualsCompareTest
 */

public class EqualsCompareTest {

    // Maximum width in bits
    static final int MAX_WIDTH = 512;

    static final Map<Class, Integer> typeToWidth;

    static {
        typeToWidth = new HashMap<>();
        typeToWidth.put(byte.class, Byte.SIZE);
        typeToWidth.put(short.class, Short.SIZE);
        typeToWidth.put(char.class, Character.SIZE);
        typeToWidth.put(int.class, Integer.SIZE);
        typeToWidth.put(long.class, Long.SIZE);
        typeToWidth.put(float.class, Float.SIZE);
        typeToWidth.put(double.class, Double.SIZE);
    }

    static int arraySizeFor(Class<?> type) {
        assert type.isPrimitive();
        return 4 * MAX_WIDTH / typeToWidth.get(type);
    }

    enum BufferKind {
        HEAP,
        HEAP_VIEW,
        DIRECT;
    }

    static abstract class BufferType<T extends Buffer, E> {
        final BufferKind k;
        final Class<?> bufferType;
        final Class<?> elementType;

        final MethodHandle eq;
        final MethodHandle cmp;
        final MethodHandle mismtch;

        final MethodHandle getter;
        final MethodHandle setter;

        BufferType(BufferKind k, Class<T> bufferType, Class<?> elementType) {
            this.k = k;
            this.bufferType = bufferType;
            this.elementType = elementType;

            var lookup = MethodHandles.lookup();
            try {
                eq = lookup.findVirtual(bufferType, "equals", MethodType.methodType(boolean.class, Object.class));
                cmp = lookup.findVirtual(bufferType, "compareTo", MethodType.methodType(int.class, bufferType));
                mismtch = lookup.findVirtual(bufferType, "mismatch", MethodType.methodType(int.class, bufferType));

                getter = lookup.findVirtual(bufferType, "get", MethodType.methodType(elementType, int.class));
                setter = lookup.findVirtual(bufferType, "put", MethodType.methodType(bufferType, int.class, elementType));
            }
            catch (Exception e) {
                throw new AssertionError(e);
            }
        }

        @Override
        public String toString() {
            return bufferType.getName() + " " + k;
        }

        T construct(int length) {
            return construct(length, ByteOrder.BIG_ENDIAN);
        }

        abstract T construct(int length, ByteOrder bo);

        @SuppressWarnings("unchecked")
        T slice(T a, int from, int to, boolean dupOtherwiseSlice) {
            a = (T) a.position(from).limit(to);
            return (T) (dupOtherwiseSlice ? a.duplicate() : a.slice());
        }

        @SuppressWarnings("unchecked")
        E get(T a, int i) {
            try {
                return (E) getter.invoke(a, i);
            }
            catch (RuntimeException | Error e) {
                throw e;
            }
            catch (Throwable t) {
                throw new Error(t);
            }
        }

        void set(T a, int i, Object v) {
            try {
                setter.invoke(a, i, convert(v));
            }
            catch (RuntimeException | Error e) {
                throw e;
            }
            catch (Throwable t) {
                throw new Error(t);
            }
        }

        abstract Object convert(Object o);

        boolean equals(T a, T b) {
            try {
                return (boolean) eq.invoke(a, b);
            }
            catch (RuntimeException | Error e) {
                throw e;
            }
            catch (Throwable t) {
                throw new Error(t);
            }
        }

        int compare(T a, T b) {
            try {
                return (int) cmp.invoke(a, b);
            }
            catch (RuntimeException | Error e) {
                throw e;
            }
            catch (Throwable t) {
                throw new Error(t);
            }
        }

        boolean pairWiseEquals(T a, T b) {
            if (a.remaining() != b.remaining())
                return false;
            int p = a.position();
            for (int i = a.limit() - 1, j = b.limit() - 1; i >= p; i--, j--)
                if (!get(a, i).equals(get(b, j)))
                    return false;
            return true;
        }

        int mismatch(T a, T b) {
            try {
                return (int) mismtch.invoke(a, b);
            }
            catch (RuntimeException | Error e) {
                throw e;
            }
            catch (Throwable t) {
              throw new Error(t);
            }
        }

        static class Bytes extends BufferType<ByteBuffer, Byte> {
            Bytes(BufferKind k) {
                super(k, ByteBuffer.class, byte.class);
            }

            @Override
            ByteBuffer construct(int length, ByteOrder bo) {
                switch (k) {
                    case DIRECT:
                        return ByteBuffer.allocateDirect(length).order(bo);
                    default:
                    case HEAP_VIEW:
                    case HEAP:
                        return ByteBuffer.allocate(length).order(bo);
                }
            }

            @Override
            Object convert(Object o) {
                return o instanceof Integer
                       ? ((Integer) o).byteValue()
                       : o;
            }
        }

        static class Chars extends BufferType<CharBuffer, Character> {
            Chars(BufferKind k) {
                super(k, CharBuffer.class, char.class);
            }

            @Override
            CharBuffer construct(int length, ByteOrder bo) {
                switch (k) {
                    case DIRECT:
                        return ByteBuffer.allocateDirect(length * Character.BYTES).
                                order(bo).
                                asCharBuffer();
                    case HEAP_VIEW:
                        return ByteBuffer.allocate(length * Character.BYTES).
                                order(bo).
                                asCharBuffer();
                    default:
                    case HEAP:
                        return CharBuffer.allocate(length);
                }
            }

            @Override
            Object convert(Object o) {
                return o instanceof Integer
                       ? (char) ((Integer) o).intValue()
                       : o;
            }

            CharBuffer transformToStringBuffer(CharBuffer c) {
                char[] chars = new char[c.remaining()];
                c.get(chars);
                return CharBuffer.wrap(new String(chars));
            }
        }

        static class Shorts extends BufferType<ShortBuffer, Short> {
            Shorts(BufferKind k) {
                super(k, ShortBuffer.class, short.class);
            }

            @Override
            ShortBuffer construct(int length, ByteOrder bo) {
                switch (k) {
                    case DIRECT:
                        return ByteBuffer.allocateDirect(length * Short.BYTES).
                                order(bo).
                                asShortBuffer();
                    case HEAP_VIEW:
                        return ByteBuffer.allocate(length * Short.BYTES).
                                order(bo).
                                asShortBuffer();
                    default:
                    case HEAP:
                        return ShortBuffer.allocate(length);
                }
            }

            @Override
            Object convert(Object o) {
                return o instanceof Integer
                       ? ((Integer) o).shortValue()
                       : o;
            }
        }

        static class Ints extends BufferType<IntBuffer, Integer> {
            Ints(BufferKind k) {
                super(k, IntBuffer.class, int.class);
            }

            @Override
            IntBuffer construct(int length, ByteOrder bo) {
                switch (k) {
                    case DIRECT:
                        return ByteBuffer.allocateDirect(length * Integer.BYTES).
                                order(bo).
                                asIntBuffer();
                    case HEAP_VIEW:
                        return ByteBuffer.allocate(length * Integer.BYTES).
                                order(bo).
                                asIntBuffer();
                    default:
                    case HEAP:
                        return IntBuffer.allocate(length);
                }
            }

            Object convert(Object o) {
                return o;
            }
        }

        static class Floats extends BufferType<FloatBuffer, Float> {
            Floats(BufferKind k) {
                super(k, FloatBuffer.class, float.class);
            }

            @Override
            FloatBuffer construct(int length, ByteOrder bo) {
                switch (k) {
                    case DIRECT:
                        return ByteBuffer.allocateDirect(length * Float.BYTES).
                                order(bo).
                                asFloatBuffer();
                    case HEAP_VIEW:
                        return ByteBuffer.allocate(length * Float.BYTES).
                                order(bo).
                                asFloatBuffer();
                    default:
                    case HEAP:
                        return FloatBuffer.allocate(length);
                }
            }

            @Override
            Object convert(Object o) {
                return o instanceof Integer
                       ? ((Integer) o).floatValue()
                       : o;
            }

            @Override
            boolean pairWiseEquals(FloatBuffer a, FloatBuffer b) {
                if (a.remaining() != b.remaining())
                    return false;
                int p = a.position();
                for (int i = a.limit() - 1, j = b.limit() - 1; i >= p; i--, j--) {
                    float av = a.get(i);
                    float bv = b.get(j);
                    if (av != bv && (!Float.isNaN(av) || !Float.isNaN(bv)))
                        return false;
                }
                return true;
            }
        }

        static class Longs extends BufferType<LongBuffer, Long> {
            Longs(BufferKind k) {
                super(k, LongBuffer.class, long.class);
            }

            @Override
            LongBuffer construct(int length, ByteOrder bo) {
                switch (k) {
                    case DIRECT:
                        return ByteBuffer.allocateDirect(length * Long.BYTES).
                                order(bo).
                                asLongBuffer();
                    case HEAP_VIEW:
                        return ByteBuffer.allocate(length * Long.BYTES).
                                order(bo).
                                asLongBuffer();
                    default:
                    case HEAP:
                        return LongBuffer.allocate(length);
                }
            }

            @Override
            Object convert(Object o) {
                return o instanceof Integer
                       ? ((Integer) o).longValue()
                       : o;
            }
        }

        static class Doubles extends BufferType<DoubleBuffer, Double> {
            Doubles(BufferKind k) {
                super(k, DoubleBuffer.class, double.class);
            }

            @Override
            DoubleBuffer construct(int length, ByteOrder bo) {
                switch (k) {
                    case DIRECT:
                        return ByteBuffer.allocateDirect(length * Double.BYTES).
                                order(bo).
                                asDoubleBuffer();
                    case HEAP_VIEW:
                        return ByteBuffer.allocate(length * Double.BYTES).
                                order(bo).
                                asDoubleBuffer();
                    default:
                    case HEAP:
                        return DoubleBuffer.allocate(length);
                }
            }

            @Override
            Object convert(Object o) {
                return o instanceof Integer
                       ? ((Integer) o).doubleValue()
                       : o;
            }

            @Override
            boolean pairWiseEquals(DoubleBuffer a, DoubleBuffer b) {
                if (a.remaining() != b.remaining())
                    return false;
                int p = a.position();
                for (int i = a.limit() - 1, j = b.limit() - 1; i >= p; i--, j--) {
                    double av = a.get(i);
                    double bv = b.get(j);
                    if (av != bv && (!Double.isNaN(av) || !Double.isNaN(bv)))
                        return false;
                }
                return true;
            }
        }
    }

    static Object[][] bufferTypes;

    @DataProvider
    public static Object[][] bufferTypesProvider() {
        if (bufferTypes == null) {
            bufferTypes = new Object[][]{
                    {new BufferType.Bytes(BufferKind.HEAP)},
                    {new BufferType.Bytes(BufferKind.DIRECT)},
                    {new BufferType.Chars(BufferKind.HEAP)},
                    {new BufferType.Chars(BufferKind.HEAP_VIEW)},
                    {new BufferType.Chars(BufferKind.DIRECT)},
                    {new BufferType.Shorts(BufferKind.HEAP)},
                    {new BufferType.Shorts(BufferKind.HEAP_VIEW)},
                    {new BufferType.Shorts(BufferKind.DIRECT)},
                    {new BufferType.Ints(BufferKind.HEAP)},
                    {new BufferType.Ints(BufferKind.HEAP_VIEW)},
                    {new BufferType.Ints(BufferKind.DIRECT)},
                    {new BufferType.Floats(BufferKind.HEAP)},
                    {new BufferType.Floats(BufferKind.HEAP_VIEW)},
                    {new BufferType.Floats(BufferKind.DIRECT)},
                    {new BufferType.Longs(BufferKind.HEAP)},
                    {new BufferType.Longs(BufferKind.HEAP_VIEW)},
                    {new BufferType.Longs(BufferKind.DIRECT)},
                    {new BufferType.Doubles(BufferKind.HEAP)},
                    {new BufferType.Doubles(BufferKind.HEAP_VIEW)},
                    {new BufferType.Doubles(BufferKind.DIRECT)},
            };
        }
        return bufferTypes;
    }


    static Object[][] floatbufferTypes;

    @DataProvider
    public static Object[][] floatBufferTypesProvider() {
        if (floatbufferTypes == null) {
            LongFunction<Object> bTof = rb -> Float.intBitsToFloat((int) rb);
            LongFunction<Object> bToD = Double::longBitsToDouble;

            floatbufferTypes = new Object[][]{
                    // canonical and non-canonical NaNs
                    // If conversion is a signalling NaN it may be subject to conversion to a
                    // quiet NaN on some processors, even if a copy is performed
                    // The tests assume that if conversion occurs it does not convert to the
                    // canonical NaN
                    new Object[]{new BufferType.Floats(BufferKind.HEAP), 0x7fc00000L, 0x7f800001L, bTof},
                    new Object[]{new BufferType.Floats(BufferKind.HEAP_VIEW), 0x7fc00000L, 0x7f800001L, bTof},
                    new Object[]{new BufferType.Floats(BufferKind.DIRECT), 0x7fc00000L, 0x7f800001L, bTof},
                    new Object[]{new BufferType.Doubles(BufferKind.HEAP), 0x7ff8000000000000L, 0x7ff0000000000001L, bToD},
                    new Object[]{new BufferType.Doubles(BufferKind.HEAP_VIEW), 0x7ff8000000000000L, 0x7ff0000000000001L, bToD},
                    new Object[]{new BufferType.Doubles(BufferKind.DIRECT), 0x7ff8000000000000L, 0x7ff0000000000001L, bToD},

                    // +0.0 and -0.0
                    new Object[]{new BufferType.Floats(BufferKind.HEAP), 0x0L, 0x80000000L, bTof},
                    new Object[]{new BufferType.Floats(BufferKind.HEAP_VIEW), 0x0L, 0x80000000L, bTof},
                    new Object[]{new BufferType.Floats(BufferKind.DIRECT), 0x0L, 0x80000000L, bTof},
                    new Object[]{new BufferType.Doubles(BufferKind.HEAP), 0x0L, 0x8000000000000000L, bToD},
                    new Object[]{new BufferType.Doubles(BufferKind.HEAP_VIEW), 0x0L, 0x8000000000000000L, bToD},
                    new Object[]{new BufferType.Doubles(BufferKind.DIRECT), 0x0L, 0x8000000000000000L, bToD},
            };
        }
        return floatbufferTypes;
    }


    static Object[][] charBufferTypes;

    @DataProvider
    public static Object[][] charBufferTypesProvider() {
        if (charBufferTypes == null) {
            charBufferTypes = new Object[][]{
                    {new BufferType.Chars(BufferKind.HEAP)},
                    {new BufferType.Chars(BufferKind.HEAP_VIEW)},
                    {new BufferType.Chars(BufferKind.DIRECT)},
            };
        }
        return charBufferTypes;
    }


    // Tests all primitive buffers
    @Test(dataProvider = "bufferTypesProvider")
    <E>
    void testBuffers(BufferType<Buffer, E> bufferType) {
        // Test with buffers of the same byte order (BE)
        BiFunction<BufferType<Buffer, E>, Integer, Buffer> constructor = (at, s) -> {
            Buffer a = at.construct(s);
            for (int x = 0; x < s; x++) {
                at.set(a, x, x % 8);
            }
            return a;
        };

        testBufferType(bufferType, constructor, constructor);

        // Test with buffers of different byte order
        if (bufferType.elementType != byte.class &&
            (bufferType.k == BufferKind.HEAP_VIEW ||
             bufferType.k == BufferKind.DIRECT)) {

            BiFunction<BufferType<Buffer, E>, Integer, Buffer> leConstructor = (at, s) -> {
                Buffer a = at.construct(s, ByteOrder.LITTLE_ENDIAN);
                for (int x = 0; x < s; x++) {
                    at.set(a, x, x % 8);
                }
                return a;
            };
            testBufferType(bufferType, constructor, leConstructor);
        }
    }

    // Tests float and double buffers with edge-case values (NaN, -0.0, +0.0)
    @Test(dataProvider = "floatBufferTypesProvider")
    public void testFloatBuffers(
            BufferType<Buffer, Float> bufferType,
            long rawBitsA, long rawBitsB,
            LongFunction<Object> bitsToFloat) {
        Object av = bitsToFloat.apply(rawBitsA);
        Object bv = bitsToFloat.apply(rawBitsB);

        BiFunction<BufferType<Buffer, Float>, Integer, Buffer> allAs = (at, s) -> {
            Buffer b = at.construct(s);
            for (int x = 0; x < s; x++) {
                at.set(b, x, av);
            }
            return b;
        };

        BiFunction<BufferType<Buffer, Float>, Integer, Buffer> allBs = (at, s) -> {
            Buffer b = at.construct(s);
            for (int x = 0; x < s; x++) {
                at.set(b, x, bv);
            }
            return b;
        };

        BiFunction<BufferType<Buffer, Float>, Integer, Buffer> halfBs = (at, s) -> {
            Buffer b = at.construct(s);
            for (int x = 0; x < s / 2; x++) {
                at.set(b, x, bv);
            }
            for (int x = s / 2; x < s; x++) {
                at.set(b, x, 1);
            }
            return b;
        };

        // Sanity check
        int size = arraySizeFor(bufferType.elementType);
        Assert.assertTrue(bufferType.pairWiseEquals(allAs.apply(bufferType, size),
                                                    allBs.apply(bufferType, size)));
        Assert.assertTrue(bufferType.equals(allAs.apply(bufferType, size),
                                            allBs.apply(bufferType, size)));

        testBufferType(bufferType, allAs, allBs);
        testBufferType(bufferType, allAs, halfBs);
    }

    // Tests CharBuffer for region sources and CharSequence sources
    @Test(dataProvider = "charBufferTypesProvider")
    public void testCharBuffers(BufferType.Chars charBufferType) {

        BiFunction<BufferType.Chars, Integer, CharBuffer> constructor = (at, s) -> {
            CharBuffer a = at.construct(s);
            for (int x = 0; x < s; x++) {
                at.set(a, x, x % 8);
            }
            return a;
        };

        BiFunction<BufferType.Chars, Integer, CharBuffer> constructorX = constructor.
                andThen(charBufferType::transformToStringBuffer);

        testBufferType(charBufferType, constructor, constructorX);
    }


    <B extends Buffer, E, BT extends BufferType<B, E>>
    void testBufferType(BT bt,
                        BiFunction<BT, Integer, B> aConstructor,
                        BiFunction<BT, Integer, B> bConstructor) {
        int n = arraySizeFor(bt.elementType);

        for (boolean dupOtherwiseSlice : new boolean[]{ false, true }) {
            for (int s : ranges(0, n)) {
                B a = aConstructor.apply(bt, s);
                B b = bConstructor.apply(bt, s);

                for (int aFrom : ranges(0, s)) {
                    for (int aTo : ranges(aFrom, s)) {
                        int aLength = aTo - aFrom;

                        B as = aLength != s
                               ? bt.slice(a, aFrom, aTo, dupOtherwiseSlice)
                               : a;

                        for (int bFrom : ranges(0, s)) {
                            for (int bTo : ranges(bFrom, s)) {
                                int bLength = bTo - bFrom;

                                B bs = bLength != s
                                       ? bt.slice(b, bFrom, bTo, dupOtherwiseSlice)
                                       : b;

                                boolean eq = bt.pairWiseEquals(as, bs);
                                Assert.assertEquals(bt.equals(as, bs), eq);
                                Assert.assertEquals(bt.equals(bs, as), eq);
                                if (eq) {
                                    Assert.assertEquals(bt.compare(as, bs), 0);
                                    Assert.assertEquals(bt.compare(bs, as), 0);

                                    // If buffers are equal, there shall be no mismatch
                                    Assert.assertEquals(bt.mismatch(as, bs), -1);
                                    Assert.assertEquals(bt.mismatch(bs, as), -1);
                                }
                                else {
                                    int aCb = bt.compare(as, bs);
                                    int bCa = bt.compare(bs, as);
                                    int v = Integer.signum(aCb) * Integer.signum(bCa);
                                    Assert.assertTrue(v == -1);

                                    int aMs = bt.mismatch(as, bs);
                                    int bMs = bt.mismatch(bs, as);
                                    Assert.assertNotEquals(aMs, -1);
                                    Assert.assertEquals(aMs, bMs);
                                }
                            }
                        }

                        if (aLength > 0 && !a.isReadOnly()) {
                            for (int i = aFrom; i < aTo; i++) {
                                B c = aConstructor.apply(bt, a.capacity());
                                B cs = aLength != s
                                       ? bt.slice(c, aFrom, aTo, dupOtherwiseSlice)
                                       : c;

                                // Create common prefix with a length of i - aFrom
                                bt.set(c, i, -1);

                                Assert.assertFalse(bt.equals(c, a));

                                int cCa = bt.compare(cs, as);
                                int aCc = bt.compare(as, cs);
                                int v = Integer.signum(cCa) * Integer.signum(aCc);
                                Assert.assertTrue(v == -1);

                                int cMa = bt.mismatch(cs, as);
                                int aMc = bt.mismatch(as, cs);
                                Assert.assertEquals(cMa, aMc);
                                Assert.assertEquals(cMa, i - aFrom);
                            }
                        }
                    }
                }
            }
        }
    }

    static int[] ranges(int from, int to) {
        int width = to - from;
        switch (width) {
            case 0:
                return new int[]{};
            case 1:
                return new int[]{from, to};
            case 2:
                return new int[]{from, from + 1, to};
            case 3:
                return new int[]{from, from + 1, from + 2, to};
            default:
                return IntStream.of(from, from + 1, from + 2, to / 2 - 1, to / 2, to / 2 + 1, to - 2, to - 1, to)
                        .filter(i -> i >= from && i <= to)
                        .distinct().toArray();
        }
    }
}
