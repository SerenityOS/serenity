/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @modules jdk.incubator.vector
 * @run testng MismatchTest
 *
 */

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.stream.IntStream;

public class MismatchTest {

    // Maximum width in bits
    static final int MAX_WIDTH = 1024;

    static final Map<Class, Integer> typeToWidth;
    static Object[][] arrayTypes;

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

    @DataProvider
    public static Object[][] arrayTypesProvider() {
        if (arrayTypes == null) {
            arrayTypes = new Object[][]{
                    new Object[]{new MismatchTest.ArrayType.Bytes(false)},
            };
        }
        return arrayTypes;
    }

    static boolean isEqual(MismatchTest.ArrayType<?> at, Object a, int aFromIndex, int aToIndex,
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


    // Equality and comparison tests

    static boolean isEqual(MismatchTest.ArrayType<?> at, Object a, int aFrom, Object b, int bFrom) {
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


    @Test(dataProvider = "arrayTypesProvider")
    public void testArray(MismatchTest.ArrayType<?> arrayType) {
        BiFunction<MismatchTest.ArrayType<?>, Integer, Object> constructor = (at, s) -> {
            Object a = at.construct(s);
            for (int x = 0; x < s; x++) {
                at.set(a, x, x % 8);
            }
            return a;
        };

        BiFunction<MismatchTest.ArrayType<?>, Object, Object> cloner = (at, a) ->
                constructor.apply(at, Array.getLength(a));

        testArrayType(arrayType, constructor, cloner);
    }

    void testArrayType(MismatchTest.ArrayType<?> at,
                       BiFunction<MismatchTest.ArrayType<?>, Integer, Object> constructor,
                       BiFunction<MismatchTest.ArrayType<?>, Object, Object> cloner) {
        int n = arraySizeFor(at.componentType);

        for (int s : ranges(0, n)) {
            Object a = constructor.apply(at, s);
            Object b = cloner.apply(at, a);

            for (int aFrom : ranges(0, s)) {
                for (int aTo : ranges(aFrom, s)) {
                    int aLength = aTo - aFrom;

                    for (int bFrom : ranges(0, s)) {
                        for (int bTo : ranges(bFrom, s)) {
                            Object anr = at.copyOf(a, aFrom, aTo);
                            Object bnr = at.copyOf(b, bFrom, bTo);

                            boolean eq = isEqual(at, a, aFrom, aTo, b, bFrom, bTo);
                            Assert.assertEquals(at.equals(anr, bnr), eq);
                            Assert.assertEquals(at.equals(bnr, anr), eq);
                            if (eq) {
                                Assert.assertEquals(at.compare(anr, bnr), 0);
                                Assert.assertEquals(at.compare(bnr, anr), 0);

                                Assert.assertEquals(at.mismatch(anr, bnr), -1);
                                Assert.assertEquals(at.mismatch(bnr, anr), -1);
                            }
                            else {
                                int anrMbnr = at.mismatch(anr, bnr);
                                int bnrManr = at.mismatch(bnr, anr);

                                if (anrMbnr == -1) {
                                    System.out.println(at.toString(anr));
                                    System.out.println(at.toString(bnr));
                                }
                                Assert.assertNotEquals(anrMbnr, -1);
                                Assert.assertEquals(anrMbnr, bnrManr);
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

                            Assert.assertFalse(at.equals(acnr, anr));

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


    // Leap ahead steps in the time sort algorithm
    //   partial sorts mostly sorted
    // String hash code (AES hashing is even faster)
    // Array hash code

    static abstract class ArrayType<T> {
        final Class<?> arrayType;
        final Class<?> componentType;
        final boolean unsigned;

        final MethodHandle cpy;

        final MethodHandle eq;
        final MethodHandle cmp;
        final MethodHandle mm;

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

                    eq = l.findStatic(VectorArrays.class, "equals", eqt);

                    String compareName = unsigned ? "compareUnsigned" : "compare";
                    cmp = l.findStatic(VectorArrays.class, compareName,
                                       eqt.changeReturnType(int.class));

                    mm = l.findStatic(VectorArrays.class, "mismatch",
                                      eqt.changeReturnType(int.class));

                    toString = l.findStatic(Arrays.class, "toString",
                                            MethodType.methodType(String.class, arrayType));
                }
                else {
                    cpy = l.findStatic(Arrays.class, "copyOfRange",
                                       MethodType.methodType(Object[].class, Object[].class, int.class, int.class));

                    MethodType eqt = MethodType.methodType(
                            boolean.class, Object[].class, Object[].class);

                    eq = l.findStatic(VectorArrays.class, "equals", eqt);

                    MethodType cmpt = MethodType.methodType(
                            int.class, Comparable[].class, Comparable[].class);

                    cmp = l.findStatic(VectorArrays.class, "compare", cmpt);

                    mm = l.findStatic(VectorArrays.class, "mismatch",
                                      eqt.changeReturnType(int.class));

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

        static class BoxedIntegers extends MismatchTest.ArrayType<Integer[]> {
            public BoxedIntegers() {
                super(Integer[].class);
            }

            @Override
            void set(Object a, int i, Object v) {
                // Ensure unique reference
                ((Integer[]) a)[i] = v != null ? new Integer((Integer) v) : null;
            }
        }

        static class Booleans extends MismatchTest.ArrayType<boolean[]> {
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

        static class Bytes extends MismatchTest.ArrayType<byte[]> {
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

        static class Characters extends MismatchTest.ArrayType<char[]> {
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

        static class Shorts extends MismatchTest.ArrayType<short[]> {
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

        static class Integers extends MismatchTest.ArrayType<int[]> {
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

        static class Longs extends MismatchTest.ArrayType<long[]> {
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

        static class Floats extends MismatchTest.ArrayType<float[]> {
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

        static class Doubles extends MismatchTest.ArrayType<double[]> {
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
}
