/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8033148 8141409
 * @summary tests for array equals and compare
 * @run testng ArraysEqCmpTest
*/

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.LongFunction;
import java.util.stream.IntStream;

public class ArraysEqCmpTest {

    // Maximum width in bits
    static final int MAX_WIDTH = 512;

    static final Map<Class, Integer> typeToWidth;

    static {
        typeToWidth = new HashMap<>();
        typeToWidth.put(boolean.class, Byte.SIZE);
        typeToWidth.put(byte.class, Byte.SIZE);
        typeToWidth.put(short.class, Short.SIZE);
        typeToWidth.put(char.class, Character.SIZE);
        typeToWidth.put(int.class, Integer.SIZE);
        typeToWidth.put(long.class, Long.SIZE);
        typeToWidth.put(float.class, Float.SIZE);
        typeToWidth.put(double.class, Double.SIZE);
        typeToWidth.put(Object.class, Integer.SIZE); // @@@ 32 or 64?
    }

    static int arraySizeFor(Class<?> type) {
        type = type.isPrimitive() ? type : Object.class;
        return 4 * MAX_WIDTH / typeToWidth.get(type);
    }

    static abstract class ArrayType<T> {
        final Class<?> arrayType;
        final Class<?> componentType;
        final boolean unsigned;

        final MethodHandle cpy;

        final MethodHandle eq;
        final MethodHandle eqr;
        final MethodHandle cmp;
        final MethodHandle cmpr;
        final MethodHandle mm;
        final MethodHandle mmr;

        final MethodHandle getter;

        final MethodHandle toString;

        public ArrayType(Class<T> arrayType) {
            this(arrayType, false);
        }

        public ArrayType(Class<T> arrayType, boolean unsigned) {
            this.arrayType = arrayType;
            this.componentType = arrayType.getComponentType();
            this.unsigned = unsigned;

            try {
                MethodHandles.Lookup l = MethodHandles.lookup();

                getter = MethodHandles.arrayElementGetter(arrayType);

                if (componentType.isPrimitive()) {
                    cpy = l.findStatic(Arrays.class, "copyOfRange",
                                       MethodType.methodType(arrayType, arrayType, int.class, int.class));

                    MethodType eqt = MethodType.methodType(
                            boolean.class, arrayType, arrayType);
                    MethodType eqrt = MethodType.methodType(
                            boolean.class, arrayType, int.class, int.class, arrayType, int.class, int.class);

                    eq = l.findStatic(Arrays.class, "equals", eqt);
                    eqr = l.findStatic(Arrays.class, "equals", eqrt);

                    String compareName = unsigned ? "compareUnsigned" : "compare";
                    cmp = l.findStatic(Arrays.class, compareName,
                                       eqt.changeReturnType(int.class));
                    cmpr = l.findStatic(Arrays.class, compareName,
                                        eqrt.changeReturnType(int.class));

                    mm = l.findStatic(Arrays.class, "mismatch",
                                       eqt.changeReturnType(int.class));
                    mmr = l.findStatic(Arrays.class, "mismatch",
                                       eqrt.changeReturnType(int.class));

                    toString = l.findStatic(Arrays.class, "toString",
                                            MethodType.methodType(String.class, arrayType));
                }
                else {
                    cpy = l.findStatic(Arrays.class, "copyOfRange",
                                       MethodType.methodType(Object[].class, Object[].class, int.class, int.class));

                    MethodType eqt = MethodType.methodType(
                            boolean.class, Object[].class, Object[].class);
                    MethodType eqrt = MethodType.methodType(
                            boolean.class, Object[].class, int.class, int.class, Object[].class, int.class, int.class);

                    eq = l.findStatic(Arrays.class, "equals", eqt);
                    eqr = l.findStatic(Arrays.class, "equals", eqrt);

                    MethodType cmpt = MethodType.methodType(
                            int.class, Comparable[].class, Comparable[].class);
                    MethodType cmprt = MethodType.methodType(
                            int.class, Comparable[].class, int.class, int.class, Comparable[].class, int.class, int.class);

                    cmp = l.findStatic(Arrays.class, "compare", cmpt);
                    cmpr = l.findStatic(Arrays.class, "compare", cmprt);

                    mm = l.findStatic(Arrays.class, "mismatch",
                                      eqt.changeReturnType(int.class));
                    mmr = l.findStatic(Arrays.class, "mismatch",
                                       eqrt.changeReturnType(int.class));

                    toString = l.findStatic(Arrays.class, "toString",
                                            MethodType.methodType(String.class, Object[].class));
                }

            }
            catch (Exception e) {
                throw new Error(e);
            }
        }

        @Override
        public String toString() {
            String s = arrayType.getCanonicalName();
            return unsigned ? "unsigned " + s : s;
        }

        Object construct(int length) {
            return Array.newInstance(componentType, length);
        }

        Object copyOf(Object a) {
            return copyOf(a, 0, Array.getLength(a));
        }

        Object copyOf(Object a, int from, int to) {
            try {
                return (Object) cpy.invoke(a, from, to);
            }
            catch (RuntimeException | Error e) {
                throw e;
            }
            catch (Throwable t) {
                throw new Error(t);
            }
        }

        Object get(Object a, int i) {
            try {
                return (Object) getter.invoke(a, i);
            }
            catch (RuntimeException | Error e) {
                throw e;
            }
            catch (Throwable t) {
                throw new Error(t);
            }
        }

        abstract void set(Object a, int i, Object v);

        boolean equals(Object a, Object b) {
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

        boolean equals(Object a, int aFromIndex, int aToIndex,
                       Object b, int bFromIndex, int bToIndex) {
            try {
                return (boolean) eqr.invoke(a, aFromIndex, aToIndex, b, bFromIndex, bToIndex);
            }
            catch (RuntimeException | Error e) {
                throw e;
            }
            catch (Throwable t) {
                throw new Error(t);
            }
        }

        int compare(Object a, Object b) {
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

        int compare(Object a, int aFromIndex, int aToIndex,
                    Object b, int bFromIndex, int bToIndex) {
            try {
                return (int) cmpr.invoke(a, aFromIndex, aToIndex, b, bFromIndex, bToIndex);
            }
            catch (RuntimeException | Error e) {
                throw e;
            }
            catch (Throwable t) {
                throw new Error(t);
            }
        }

        int mismatch(Object a, Object b) {
            try {
                return (int) mm.invoke(a, b);
            }
            catch (RuntimeException | Error e) {
                throw e;
            }
            catch (Throwable t) {
                throw new Error(t);
            }
        }

        int mismatch(Object a, int aFromIndex, int aToIndex,
                     Object b, int bFromIndex, int bToIndex) {
            try {
                return (int) mmr.invoke(a, aFromIndex, aToIndex, b, bFromIndex, bToIndex);
            }
            catch (RuntimeException | Error e) {
                throw e;
            }
            catch (Throwable t) {
                throw new Error(t);
            }
        }

        String toString(Object a) {
            try {
                return (String) toString.invoke(a);
            }
            catch (RuntimeException | Error e) {
                throw e;
            }
            catch (Throwable t) {
                throw new Error(t);
            }
        }

        static class BoxedIntegers extends ArrayType<Integer[]> {
            public BoxedIntegers() {
                super(Integer[].class);
            }

            @Override
            void set(Object a, int i, Object v) {
                // Ensure unique reference
                ((Integer[]) a)[i] = v != null ? new Integer((Integer) v) : null;
            }
        }

        static class BoxedIntegersWithReverseComparator extends BoxedIntegers {
            final Comparator<Integer> c = (a, b) -> {
                // Nulls sort after non-nulls
                if (a == null || b == null)
                    return a == null ? b == null ? 0 : 1 : -1;

                return Integer.compare(b, a);
            };

            final MethodHandle eqc;
            final MethodHandle eqcr;
            final MethodHandle cmpc;
            final MethodHandle cmpcr;
            final MethodHandle mismatchc;
            final MethodHandle mismatchcr;

            public BoxedIntegersWithReverseComparator() {
                try {
                    MethodHandles.Lookup l = MethodHandles.lookup();

                    MethodType cmpt = MethodType.methodType(
                            int.class, Object[].class, Object[].class, Comparator.class);
                    MethodType cmprt = MethodType.methodType(
                            int.class, Object[].class, int.class, int.class,
                            Object[].class, int.class, int.class, Comparator.class);

                    eqc = l.findStatic(Arrays.class, "equals", cmpt.changeReturnType(boolean.class));
                    eqcr = l.findStatic(Arrays.class, "equals", cmprt.changeReturnType(boolean.class));
                    cmpc = l.findStatic(Arrays.class, "compare", cmpt);
                    cmpcr = l.findStatic(Arrays.class, "compare", cmprt);
                    mismatchc = l.findStatic(Arrays.class, "mismatch", cmpt);
                    mismatchcr = l.findStatic(Arrays.class, "mismatch", cmprt);
                }
                catch (Exception e) {
                    throw new Error(e);
                }
            }

            @Override
            boolean equals(Object a, Object b) {
                try {
                    return (boolean) eqc.invoke(a, b, c);
                }
                catch (RuntimeException | Error e) {
                    throw e;
                }
                catch (Throwable t) {
                    throw new Error(t);
                }
            }

            @Override
            boolean equals(Object a, int aFromIndex, int aToIndex,
                           Object b, int bFromIndex, int bToIndex) {
                try {
                    return (boolean) eqcr.invoke(a, aFromIndex, aToIndex, b, bFromIndex, bToIndex, c);
                }
                catch (RuntimeException | Error e) {
                    throw e;
                }
                catch (Throwable t) {
                    throw new Error(t);
                }
            }

            @Override
            int compare(Object a, Object b) {
                try {
                    return (int) cmpc.invoke(a, b, c);
                }
                catch (RuntimeException | Error e) {
                    throw e;
                }
                catch (Throwable t) {
                    throw new Error(t);
                }
            }

            @Override
            int compare(Object a, int aFromIndex, int aToIndex,
                        Object b, int bFromIndex, int bToIndex) {
                try {
                    return (int) cmpcr.invoke(a, aFromIndex, aToIndex, b, bFromIndex, bToIndex, c);
                }
                catch (RuntimeException | Error e) {
                    throw e;
                }
                catch (Throwable t) {
                    throw new Error(t);
                }
            }

            @Override
            int mismatch(Object a, Object b) {
                try {
                    return (int) mismatchc.invoke(a, b, c);
                }
                catch (RuntimeException | Error e) {
                    throw e;
                }
                catch (Throwable t) {
                    throw new Error(t);
                }
            }

            @Override
            int mismatch(Object a, int aFromIndex, int aToIndex,
                         Object b, int bFromIndex, int bToIndex) {
                try {
                    return (int) mismatchcr.invoke(a, aFromIndex, aToIndex, b, bFromIndex, bToIndex, c);
                }
                catch (RuntimeException | Error e) {
                    throw e;
                }
                catch (Throwable t) {
                    throw new Error(t);
                }
            }

            @Override
            public String toString() {
                return arrayType.getCanonicalName() + " with Comparator";
            }
        }

        static class Booleans extends ArrayType<boolean[]> {
            public Booleans() {
                super(boolean[].class);
            }

            @Override
            void set(Object a, int i, Object v) {
                boolean pv;
                if (v instanceof Boolean) {
                    pv = (Boolean) v;
                }
                else if (v instanceof Integer) {
                    pv = ((Integer) v) >= 0;
                }
                else throw new IllegalStateException();

                ((boolean[]) a)[i] = pv;
            }
        }

        static class Bytes extends ArrayType<byte[]> {
            public Bytes(boolean unsigned) {
                super(byte[].class, unsigned);
            }

            @Override
            void set(Object a, int i, Object v) {
                byte pv;
                if (v instanceof Byte) {
                    pv = (Byte) v;
                }
                else if (v instanceof Integer) {
                    pv = ((Integer) v).byteValue();
                }
                else throw new IllegalStateException();

                ((byte[]) a)[i] = pv;
            }
        }

        static class Characters extends ArrayType<char[]> {
            public Characters() {
                super(char[].class);
            }

            @Override
            void set(Object a, int i, Object v) {
                char pv;
                if (v instanceof Character) {
                    pv = (Character) v;
                }
                else if (v instanceof Integer) {
                    pv = (char) ((Integer) v).intValue();
                }
                else throw new IllegalStateException();

                ((char[]) a)[i] = pv;
            }
        }

        static class Shorts extends ArrayType<short[]> {
            public Shorts(boolean unsigned) {
                super(short[].class, unsigned);
            }

            @Override
            void set(Object a, int i, Object v) {
                short pv;
                if (v instanceof Short) {
                    pv = (Short) v;
                }
                else if (v instanceof Integer) {
                    pv = ((Integer) v).shortValue();
                }
                else throw new IllegalStateException();

                ((short[]) a)[i] = pv;
            }
        }

        static class Integers extends ArrayType<int[]> {
            public Integers(boolean unsigned) {
                super(int[].class, unsigned);
            }

            @Override
            void set(Object a, int i, Object v) {
                int pv;
                if (v instanceof Integer) {
                    pv = ((Integer) v).shortValue();
                }
                else throw new IllegalStateException();

                ((int[]) a)[i] = pv;
            }
        }

        static class Longs extends ArrayType<long[]> {
            public Longs(boolean unsigned) {
                super(long[].class, unsigned);
            }

            @Override
            void set(Object a, int i, Object v) {
                long pv;
                if (v instanceof Long) {
                    pv = (Long) v;
                }
                else if (v instanceof Integer) {
                    pv = ((Integer) v).longValue();
                }
                else throw new IllegalStateException();

                ((long[]) a)[i] = pv;
            }
        }

        static class Floats extends ArrayType<float[]> {
            public Floats() {
                super(float[].class);
            }

            @Override
            void set(Object a, int i, Object v) {
                float pv;
                if (v instanceof Float) {
                    pv = (Float) v;
                }
                else if (v instanceof Integer) {
                    pv = ((Integer) v).floatValue();
                }
                else throw new IllegalStateException();

                ((float[]) a)[i] = pv;
            }
        }

        static class Doubles extends ArrayType<double[]> {
            public Doubles() {
                super(double[].class);
            }

            @Override
            void set(Object a, int i, Object v) {
                double pv;
                if (v instanceof Double) {
                    pv = (Double) v;
                }
                else if (v instanceof Integer) {
                    pv = ((Integer) v).doubleValue();
                }
                else throw new IllegalStateException();

                ((double[]) a)[i] = pv;
            }
        }
    }

    static Object[][] arrayTypes;

    @DataProvider
    public static Object[][] arrayTypesProvider() {
        if (arrayTypes == null) {
            arrayTypes = new Object[][]{
                    new Object[]{new ArrayType.BoxedIntegers()},
                    new Object[]{new ArrayType.BoxedIntegersWithReverseComparator()},
                    new Object[]{new ArrayType.Booleans()},
                    new Object[]{new ArrayType.Bytes(false)},
                    new Object[]{new ArrayType.Bytes(true)},
                    new Object[]{new ArrayType.Characters()},
                    new Object[]{new ArrayType.Shorts(false)},
                    new Object[]{new ArrayType.Shorts(true)},
                    new Object[]{new ArrayType.Integers(false)},
                    new Object[]{new ArrayType.Integers(true)},
                    new Object[]{new ArrayType.Longs(false)},
                    new Object[]{new ArrayType.Longs(true)},
                    new Object[]{new ArrayType.Floats()},
                    new Object[]{new ArrayType.Doubles()},
            };
        }
        return arrayTypes;
    }

    static Object[][] floatArrayTypes;

    @DataProvider
    public static Object[][] floatArrayTypesProvider() {
        if (floatArrayTypes == null) {
            LongFunction<Object> bTof = rb -> Float.intBitsToFloat((int) rb);
            LongFunction<Object> bToD = Double::longBitsToDouble;

            floatArrayTypes = new Object[][]{
                    new Object[]{new ArrayType.Floats(), 0x7fc00000L, 0x7f800001L, bTof},
                    new Object[]{new ArrayType.Doubles(), 0x7ff8000000000000L, 0x7ff0000000000001L, bToD},
            };
        }
        return floatArrayTypes;
    }

    static Object[][] objectArrayTypes;

    @DataProvider
    public static Object[][] objectArrayTypesProvider() {
        if (objectArrayTypes == null) {
            LongFunction<Object> bTof = rb -> Float.intBitsToFloat((int) rb);
            LongFunction<Object> bToD = Double::longBitsToDouble;

            objectArrayTypes = new Object[][]{
                    new Object[]{new ArrayType.BoxedIntegers()},
                    new Object[]{new ArrayType.BoxedIntegersWithReverseComparator()},
            };
        }
        return objectArrayTypes;
    }


    static Object[][] signedUnsignedArrayTypes;

    @DataProvider
    public static Object[][] signedUnsignedArrayTypes() {
        if (signedUnsignedArrayTypes == null) {
            signedUnsignedArrayTypes = new Object[][]{
                    new Object[]{new ArrayType.Bytes(false), new ArrayType.Bytes(true)},
                    new Object[]{new ArrayType.Shorts(false), new ArrayType.Shorts(true)},
                    new Object[]{new ArrayType.Integers(false), new ArrayType.Integers(true)},
                    new Object[]{new ArrayType.Longs(false), new ArrayType.Longs(true)},
            };
        }
        return signedUnsignedArrayTypes;
    }

    // Equality and comparison tests

    @Test(dataProvider = "arrayTypesProvider")
    public void testArray(ArrayType<?> arrayType) {
        BiFunction<ArrayType<?>, Integer, Object> constructor = (at, s) -> {
            Object a = at.construct(s);
            for (int x = 0; x < s; x++) {
                at.set(a, x, x % 8);
            }
            return a;
        };

        BiFunction<ArrayType<?>, Object, Object> cloner = (at, a) ->
                constructor.apply(at, Array.getLength(a));

        testArrayType(arrayType, constructor, cloner);
    }

    @Test(dataProvider = "floatArrayTypesProvider")
    public void testPrimitiveFloatArray(
            ArrayType<?> arrayType,
            long canonicalNanRawBits, long nonCanonicalNanRawBits,
            LongFunction<Object> bitsToFloat) {
        Object canonicalNan = bitsToFloat.apply(canonicalNanRawBits);
        // If conversion is a signalling NaN it may be subject to conversion to a
        // quiet NaN on some processors, even if a copy is performed
        // The tests assume that if conversion occurs it does not convert to the
        // canonical NaN
        Object nonCanonicalNan = bitsToFloat.apply(nonCanonicalNanRawBits);

        BiFunction<ArrayType<?>, Integer, Object> canonicalNaNs = (at, s) -> {
            Object a = at.construct(s);
            for (int x = 0; x < s; x++) {
                at.set(a, x, canonicalNan);
            }
            return a;
        };

        BiFunction<ArrayType<?>, Object, Object> nonCanonicalNaNs = (at, a) -> {
            int s = Array.getLength(a);
            Object ac = at.construct(s);
            for (int x = 0; x < s; x++) {
                at.set(ac, x, nonCanonicalNan);
            }
            return ac;
        };

        BiFunction<ArrayType<?>, Object, Object> halfNonCanonicalNaNs = (at, a) -> {
            int s = Array.getLength(a);
            Object ac = at.construct(s);
            for (int x = 0; x < s / 2; x++) {
                at.set(ac, x, nonCanonicalNan);
            }
            for (int x = s / 2; x < s; x++) {
                at.set(ac, x, 1);
            }
            return ac;
        };

        testArrayType(arrayType, canonicalNaNs, nonCanonicalNaNs);
        testArrayType(arrayType, canonicalNaNs, halfNonCanonicalNaNs);
    }

    @Test(dataProvider = "objectArrayTypesProvider")
    public void testNullElementsInObjectArray(ArrayType<?> arrayType) {
        BiFunction<ArrayType<?>, Object, Object> cloner = ArrayType::copyOf;

        // All nulls
        testArrayType(arrayType,
                      (at, s) -> {
                          Object a = at.construct(s);
                          for (int x = 0; x < s; x++) {
                              at.set(a, x, null);
                          }
                          return a;
                      },
                      cloner);


        // Some nulls
        testArrayType(arrayType,
                      (at, s) -> {
                          Object a = at.construct(s);
                          for (int x = 0; x < s; x++) {
                              int v = x % 8;
                              at.set(a, x, v == 0 ? null : v);
                          }
                          return a;
                      },
                      cloner);

        Integer[] a = new Integer[]{null, 0};
        Integer[] b = new Integer[]{0, 0};
        Assert.assertTrue(Arrays.compare(a, b) < 0);
        Assert.assertTrue(Arrays.compare(b, a) > 0);
    }

    @Test(dataProvider = "objectArrayTypesProvider")
    public void testSameRefElementsInObjectArray(ArrayType<?> arrayType) {
        BiFunction<ArrayType<?>, Object, Object> cloner = ArrayType::copyOf;

        // One ref
        Integer one = 1;
        testArrayType(arrayType,
                      (at, s) -> {
                          Integer[] a = (Integer[]) at.construct(s);
                          for (int x = 0; x < s; x++) {
                              a[x] = one;
                          }
                          return a;
                      },
                      cloner);

        // All ref
        testArrayType(arrayType,
                      (at, s) -> {
                          Integer[] a = (Integer[]) at.construct(s);
                          for (int x = 0; x < s; x++) {
                              a[x] = Integer.valueOf(s);
                          }
                          return a;
                      },
                      cloner);

        // Some same ref
        testArrayType(arrayType,
                      (at, s) -> {
                          Integer[] a = (Integer[]) at.construct(s);
                          for (int x = 0; x < s; x++) {
                              int v = x % 8;
                              a[x] = v == 1 ? one : new Integer(v);
                          }
                          return a;
                      },
                      cloner);
    }

    @Test(dataProvider = "signedUnsignedArrayTypes")
    public void testSignedUnsignedArray(ArrayType<?> sat, ArrayType<?> uat) {
        BiFunction<ArrayType<?>, Integer, Object> constructor = (at, s) -> {
            Object a = at.construct(s);
            for (int x = 0; x < s; x++) {
                at.set(a, x, 1);
            }
            return a;
        };

        int n = arraySizeFor(sat.componentType);

        for (int s : ranges(0, n)) {
            Object a = constructor.apply(sat, s);

            for (int aFrom : ranges(0, s)) {
                for (int aTo : ranges(aFrom, s)) {
                    int aLength = aTo - aFrom;

                    if (aLength > 0) {
                        for (int i = aFrom; i < aTo; i++) {
                            Object ac = sat.copyOf(a);
                            // Create common prefix with a length of i - aFrom
                            sat.set(ac, i, -1);

                            int sc = sat.compare(ac, aFrom, aTo, a, aFrom, aTo);
                            int uc = uat.compare(ac, aFrom, aTo, a, aFrom, aTo);

                            Assert.assertTrue(sc < 0);
                            Assert.assertTrue(uc > 0);
                        }
                    }
                }
            }
        }
    }

    void testArrayType(ArrayType<?> at,
                       BiFunction<ArrayType<?>, Integer, Object> constructor,
                       BiFunction<ArrayType<?>, Object, Object> cloner) {
        int n = arraySizeFor(at.componentType);

        for (int s : ranges(0, n)) {
            Object a = constructor.apply(at, s);
            Object b = cloner.apply(at, a);

            for (int aFrom : ranges(0, s)) {
                for (int aTo : ranges(aFrom, s)) {
                    int aLength = aTo - aFrom;

                    for (int bFrom : ranges(0, s)) {
                        for (int bTo : ranges(bFrom, s)) {
                            int bLength = bTo - bFrom;

                            Object anr = at.copyOf(a, aFrom, aTo);
                            Object bnr = at.copyOf(b, bFrom, bTo);

                            boolean eq = isEqual(at, a, aFrom, aTo, b, bFrom, bTo);
                            Assert.assertEquals(at.equals(a, aFrom, aTo, b, bFrom, bTo), eq);
                            Assert.assertEquals(at.equals(b, bFrom, bTo, a, aFrom, aTo), eq);
                            Assert.assertEquals(at.equals(anr, bnr), eq);
                            Assert.assertEquals(at.equals(bnr, anr), eq);
                            if (eq) {
                                Assert.assertEquals(at.compare(a, aFrom, aTo, b, bFrom, bTo), 0);
                                Assert.assertEquals(at.compare(b, bFrom, bTo, a, aFrom, aTo), 0);
                                Assert.assertEquals(at.compare(anr, bnr), 0);
                                Assert.assertEquals(at.compare(bnr, anr), 0);

                                Assert.assertEquals(at.mismatch(a, aFrom, aTo, b, bFrom, bTo), -1);
                                Assert.assertEquals(at.mismatch(b, bFrom, bTo, a, aFrom, aTo), -1);
                                Assert.assertEquals(at.mismatch(anr, bnr), -1);
                                Assert.assertEquals(at.mismatch(bnr, anr), -1);
                            }
                            else {
                                int aCb = at.compare(a, aFrom, aTo, b, bFrom, bTo);
                                int bCa = at.compare(b, bFrom, bTo, a, aFrom, aTo);
                                int v = Integer.signum(aCb) * Integer.signum(bCa);
                                Assert.assertTrue(v == -1);

                                int anrCbnr = at.compare(anr, bnr);
                                int bnrCanr = at.compare(bnr, anr);
                                Assert.assertEquals(anrCbnr, aCb);
                                Assert.assertEquals(bnrCanr, bCa);


                                int aMb = at.mismatch(a, aFrom, aTo, b, bFrom, bTo);
                                int bMa = at.mismatch(b, bFrom, bTo, a, aFrom, aTo);
                                int anrMbnr = at.mismatch(anr, bnr);
                                int bnrManr = at.mismatch(bnr, anr);

                                Assert.assertNotEquals(aMb, -1);
                                Assert.assertEquals(aMb, bMa);
                                Assert.assertNotEquals(anrMbnr, -1);
                                Assert.assertEquals(anrMbnr, bnrManr);
                                Assert.assertEquals(aMb, anrMbnr);
                                Assert.assertEquals(bMa, bnrManr);

                                // Common or proper prefix
                                Assert.assertTrue(at.equals(a, aFrom, aFrom + aMb, b, bFrom, bFrom + aMb));
                                if (aMb < Math.min(aLength, bLength)) {
                                    // Common prefix
                                    Assert.assertFalse(isEqual(at, a, aFrom + aMb, b, bFrom + aMb));
                                }
                            }
                        }
                    }

                    if (aLength > 0) {
                        for (int i = aFrom; i < aTo; i++) {
                            Object ac = at.copyOf(a);
                            // Create common prefix with a length of i - aFrom
                            at.set(ac, i, -1);

                            Object acnr = at.copyOf(ac, aFrom, aTo);
                            Object anr = at.copyOf(a, aFrom, aTo);

                            Assert.assertFalse(at.equals(ac, aFrom, aTo, a, aFrom, aTo));
                            Assert.assertFalse(at.equals(acnr, anr));

                            int acCa = at.compare(ac, aFrom, aTo, a, aFrom, aTo);
                            int aCac = at.compare(a, aFrom, aTo, ac, aFrom, aTo);
                            int v = Integer.signum(acCa) * Integer.signum(aCac);
                            Assert.assertTrue(v == -1);

                            int acnrCanr = at.compare(acnr, anr);
                            int anrCacnr = at.compare(anr, acnr);
                            Assert.assertEquals(acnrCanr, acCa);
                            Assert.assertEquals(anrCacnr, aCac);


                            int acMa = at.mismatch(ac, aFrom, aTo, a, aFrom, aTo);
                            int aMac = at.mismatch(a, aFrom, aTo, ac, aFrom, aTo);
                            Assert.assertEquals(acMa, aMac);
                            Assert.assertEquals(acMa, i - aFrom);

                            int acnrManr = at.mismatch(acnr, anr);
                            int anrMacnr = at.mismatch(anr, acnr);
                            Assert.assertEquals(acnrManr, anrMacnr);
                            Assert.assertEquals(acnrManr, i - aFrom);
                        }
                    }
                }
            }
        }
    }

    static boolean isEqual(ArrayType<?> at, Object a, int aFromIndex, int aToIndex,
                           Object b, int bFromIndex, int bToIndex) {
        int aLength = aToIndex - aFromIndex;
        int bLength = bToIndex - bFromIndex;
        if (aLength != bLength)
            return false;

        for (int i = 0; i < aLength; i++) {
            Object av = at.get(a, aFromIndex++);
            Object bv = at.get(b, bFromIndex++);
            if (!Objects.equals(av, bv)) return false;
        }

        return true;
    }

    static boolean isEqual(ArrayType<?> at, Object a, int aFrom, Object b, int bFrom) {
        Object av = at.get(a, aFrom);
        Object bv = at.get(b, bFrom);

        return Objects.equals(av, bv);
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


    // Null array reference tests

    @Test(dataProvider = "arrayTypesProvider")
    public void testNullArrayRefs(ArrayType<?> arrayType) {
        Object n = null;
        Object a = arrayType.construct(0);

        Assert.assertTrue(arrayType.equals(n, n));
        Assert.assertFalse(arrayType.equals(n, a));
        Assert.assertFalse(arrayType.equals(a, n));

        Assert.assertEquals(arrayType.compare(n, n), 0);
        Assert.assertTrue(arrayType.compare(n, a) < 0);
        Assert.assertTrue(arrayType.compare(a, n) > 0);
    }


    // Exception throwing tests

    @Test(dataProvider = "arrayTypesProvider")
    public void testNPEs(ArrayType<?> arrayType) {
        Object[] values = new Object[]{null, arrayType.construct(0)};

        for (Object o1 : values) {
            for (Object o2 : values) {
                if (o1 != null && o2 != null)
                    continue;

                testNPE(() -> arrayType.equals(o1, 0, 0, o2, 0, 0));
                testNPE(() -> arrayType.compare(o1, 0, 0, o2, 0, 0));
                testNPE(() -> arrayType.mismatch(o1, o2));
                testNPE(() -> arrayType.mismatch(o1, 0, 0, o2, 0, 0));
            }
        }
    }

    @Test
    public void testObjectNPEs() {
        String[][] values = new String[][]{null, new String[0]};
        Comparator<String> c = String::compareTo;
        Comparator[] cs = new Comparator[]{null, c};

        for (String[] o1 : values) {
            for (String[] o2 : values) {
                for (Comparator o3 : cs) {
                    if (o1 != null && o2 != null && o3 != null)
                        continue;

                    if (o3 == null) {
                        testNPE(() -> Arrays.equals(o1, o2, o3));
                        testNPE(() -> Arrays.compare(o1, o2, o3));
                        testNPE(() -> Arrays.mismatch(o1, o2, o3));
                    }

                    testNPE(() -> Arrays.equals(o1, 0, 0, o2, 0, 0, o3));
                    testNPE(() -> Arrays.compare(o1, 0, 0, o2, 0, 0, o3));
                    testNPE(() -> Arrays.mismatch(o1, 0, 0, o2, 0, 0, o3));
                }
            }
        }
    }

    @Test(dataProvider = "arrayTypesProvider")
    public void testIAEs(ArrayType<?> arrayType) {
        List<Integer> values = Arrays.asList(0, 1);

        for (int s : values) {
            Object a = arrayType.construct(s);

            for (int o1 : values) {
                for (int o2 : values) {
                    if (o1 <= o2) continue;

                    testIAE(() -> arrayType.equals(a, o1, 0, a, o2, 0));
                    testIAE(() -> arrayType.compare(a, o1, 0, a, o2, 0));
                    testIAE(() -> arrayType.mismatch(a, o1, 0, a, o2, 0));
                }
            }
        }
    }

    @Test(dataProvider = "arrayTypesProvider")
    public void testAIOBEs(ArrayType<?> arrayType) {
        List<Integer> froms = Arrays.asList(-1, 0);

        for (int s : Arrays.asList(0, 1)) {
            List<Integer> tos = Arrays.asList(s, s + 1);
            Object a = arrayType.construct(s);

            for (int aFrom : froms) {
                for (int aTo : tos) {
                    for (int bFrom : froms) {
                        for (int bTo : tos) {
                            if (aFrom >= 0 && aTo <= s &&
                                bFrom >= 0 && bTo <= s) continue;

                            testAIOBE(() -> arrayType.equals(a, aFrom, aTo, a, bFrom, bTo));
                            testAIOBE(() -> arrayType.compare(a, aFrom, aTo, a, bFrom, bTo));
                            testAIOBE(() -> arrayType.mismatch(a, aFrom, aTo, a, bFrom, bTo));
                        }
                    }
                }
            }
        }
    }

    static void testNPE(Runnable r) {
        testThrowable(r, NullPointerException.class);
    }

    static void testIAE(Runnable r) {
        testThrowable(r, IllegalArgumentException.class);
    }

    static void testAIOBE(Runnable r) {
        testThrowable(r, ArrayIndexOutOfBoundsException.class);
    }

    static void testThrowable(Runnable r, Class<? extends Throwable> expected) {
        Throwable caught = null;
        try {
            r.run();
        }
        catch (Throwable t) {
            caught = t;
        }
        Assert.assertNotNull(caught);
        Assert.assertTrue(expected.isInstance(caught));
    }
}