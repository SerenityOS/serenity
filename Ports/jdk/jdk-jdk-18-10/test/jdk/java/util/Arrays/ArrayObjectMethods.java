/*
 * Copyright (c) 2003, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     4906359 6239296
 * @summary Basic test for content-based array object methods
 * @author  Josh Bloch, Martin Buchholz
 * @key randomness
 */

import java.util.*;
import java.io.*;

public class ArrayObjectMethods {
    int[] sizes = {0, 10, 100, 200, 1000};

    void test(String[] args) throws Throwable {
        equal(Arrays.deepToString(null), "null");
        equal(Arrays.deepToString(new Object[]{}), "[]");
        equal(Arrays.deepToString(new Object[]{null}), "[null]");
        equal(Arrays.deepToString(new Object[]{null, 1}), "[null, 1]");
        equal(Arrays.deepToString(new Object[]{1, null}), "[1, null]");
        equal(Arrays.deepToString(new Object[]{new Object[]{}, null}), "[[], null]");

        {
            Object[] a = {1, null};
            a[1] = a;
            equal(Arrays.deepToString(a), "[1, [...]]");
            a[0] = a;
            equal(Arrays.deepToString(a), "[[...], [...]]");
            a[0] = a[1] = new Object[]{1, null, a};
            equal(Arrays.deepToString(a), "[[1, null, [...]], [1, null, [...]]]");
        }

        for (int size : sizes) {
            {
                long[] a = Rnd.longArray(size);
                equal(Arrays.toString(a), PrimitiveArrays.asList(a).toString());
                equal(Arrays.hashCode(a), PrimitiveArrays.asList(a).hashCode());
            }
            {
                int[] a = Rnd.intArray(size);
                equal(Arrays.toString(a), PrimitiveArrays.asList(a).toString());
                equal(Arrays.hashCode(a), PrimitiveArrays.asList(a).hashCode());
            }
            {
                short[] a = Rnd.shortArray(size);
                equal(Arrays.toString(a), PrimitiveArrays.asList(a).toString());
                equal(Arrays.hashCode(a), PrimitiveArrays.asList(a).hashCode());
            }
            {
                char[] a = Rnd.charArray(size);
                equal(Arrays.toString(a), PrimitiveArrays.asList(a).toString());
                equal(Arrays.hashCode(a), PrimitiveArrays.asList(a).hashCode());
            }
            {
                byte[] a = Rnd.byteArray(size);
                equal(Arrays.toString(a), PrimitiveArrays.asList(a).toString());
                equal(Arrays.hashCode(a), PrimitiveArrays.asList(a).hashCode());
            }
            {
                boolean[] a = Rnd.booleanArray(size);
                equal(Arrays.toString(a), PrimitiveArrays.asList(a).toString());
                equal(Arrays.hashCode(a), PrimitiveArrays.asList(a).hashCode());
            }
            {
                double[] a = Rnd.doubleArray(size);
                equal(Arrays.toString(a), PrimitiveArrays.asList(a).toString());
                equal(Arrays.hashCode(a), PrimitiveArrays.asList(a).hashCode());
            }
            {
                float[] a = Rnd.floatArray(size);
                equal(Arrays.toString(a), PrimitiveArrays.asList(a).toString());
                equal(Arrays.hashCode(a), PrimitiveArrays.asList(a).hashCode());
            }
            {
                Object[] a = Rnd.flatObjectArray(size);
                equal(Arrays.toString(a), Arrays.asList(a).toString());
                equal(Arrays.deepToString(a), Arrays.asList(a).toString());
                equal(Arrays.hashCode(a), Arrays.asList(a).hashCode());
            }

            if (size <= 200) {
                Object[] a = Rnd.nestedObjectArray(size);
                List aList = deepToList(a);
                equal(Arrays.toString(a), Arrays.asList(a).toString());
                equal(Arrays.deepToString(a), aList.toString());
                equal(Arrays.deepHashCode(a), aList.hashCode());
                equal(Arrays.hashCode(a), Arrays.asList(a).hashCode());

                Object[] deepCopy = (Object[]) deepCopy(a);
                check(Arrays.deepEquals(a, deepCopy));
                check(Arrays.deepEquals(deepCopy, a));

                // Make deepCopy != a
                if (size == 0)
                    deepCopy = new Object[] {"foo"};
                else if (deepCopy[deepCopy.length - 1] == null)
                    deepCopy[deepCopy.length - 1] = "baz";
                else
                    deepCopy[deepCopy.length - 1] = null;
                check(! Arrays.deepEquals(a, deepCopy));
                check(! Arrays.deepEquals(deepCopy, a));
            }
        }
    }

    // Utility method to turn an array into a list "deeply," turning
    // all primitives into objects
    List<Object> deepToList(Object[] a) {
        List<Object> result = new ArrayList<Object>();
        for (Object e : a) {
            if (e instanceof byte[])
                result.add(PrimitiveArrays.asList((byte[])e));
            else if (e instanceof short[])
                result.add(PrimitiveArrays.asList((short[])e));
            else if (e instanceof int[])
                result.add(PrimitiveArrays.asList((int[])e));
            else if (e instanceof long[])
                result.add(PrimitiveArrays.asList((long[])e));
            else if (e instanceof char[])
                result.add(PrimitiveArrays.asList((char[])e));
            else if (e instanceof double[])
                result.add(PrimitiveArrays.asList((double[])e));
            else if (e instanceof float[])
                result.add(PrimitiveArrays.asList((float[])e));
            else if (e instanceof boolean[])
                result.add(PrimitiveArrays.asList((boolean[])e));
            else if (e instanceof Object[])
                result.add(deepToList((Object[])e));
            else
                result.add(e);
        }
        return result;
    }

    // Utility method to do a deep copy of an object *very slowly* using
    // serialization/deserialization
    Object deepCopy(Object oldObj) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(oldObj);
            oos.flush();
            ByteArrayInputStream bin = new ByteArrayInputStream(
                bos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bin);
            return ois.readObject();
        } catch(Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    //--------------------- Infrastructure ---------------------------
    volatile int passed = 0, failed = 0;
    void pass() {passed++;}
    void fail() {failed++; Thread.dumpStack();}
    void fail(String msg) {System.err.println(msg); fail();}
    void unexpected(Throwable t) {failed++; t.printStackTrace();}
    void check(boolean cond) {if (cond) pass(); else fail();}
    void equal(Object x, Object y) {
        if (x == null ? y == null : x.equals(y)) pass();
        else fail(x + " not equal to " + y);}
    public static void main(String[] args) throws Throwable {
        new ArrayObjectMethods().instanceMain(args);}
    void instanceMain(String[] args) throws Throwable {
        try {test(args);} catch (Throwable t) {unexpected(t);}
        System.out.printf("%nPassed = %d, failed = %d%n%n", passed, failed);
        if (failed > 0) throw new AssertionError("Some tests failed");}
}

/**
 * Methods to generate "interesting" random primitives and primitive
 * arrays.  Unlike Random.nextXxx, these methods return small values
 * and boundary values (e.g., 0, -1, NaN) with greater than normal
 * likelihood.
 */

class Rnd {
    private static Random rnd = new Random();

    public static long nextLong() {
        switch(rnd.nextInt(10)) {
            case 0:  return 0;
            case 1:  return Long.MIN_VALUE;
            case 2:  return Long.MAX_VALUE;
            case 3: case 4: case 5:
                     return (long) (rnd.nextInt(20) - 10);
            default: return rnd.nextLong();
        }
    }

    public static int nextInt() {
        switch(rnd.nextInt(10)) {
            case 0:  return 0;
            case 1:  return Integer.MIN_VALUE;
            case 2:  return Integer.MAX_VALUE;
            case 3: case 4: case 5:
                     return rnd.nextInt(20) - 10;
            default: return rnd.nextInt();
        }
    }

    public static short nextShort() {
        switch(rnd.nextInt(10)) {
            case 0:  return 0;
            case 1:  return Short.MIN_VALUE;
            case 2:  return Short.MAX_VALUE;
            case 3: case 4: case 5:
                     return (short) (rnd.nextInt(20) - 10);
            default: return (short) rnd.nextInt();
        }
    }

    public static char nextChar() {
        switch(rnd.nextInt(10)) {
            case 0:  return 0;
            case 1:  return Character.MIN_VALUE;
            case 2:  return Character.MAX_VALUE;
            case 3: case 4: case 5:
                     return (char) (rnd.nextInt(20) - 10);
            default: return (char) rnd.nextInt();
        }
    }

    public static byte nextByte() {
        switch(rnd.nextInt(10)) {
            case 0:  return 0;
            case 1:  return Byte.MIN_VALUE;
            case 2:  return Byte.MAX_VALUE;
            case 3: case 4: case 5:
                     return (byte) (rnd.nextInt(20) - 10);
            default: return (byte) rnd.nextInt();
        }
    }

    public static boolean nextBoolean() {
        return rnd.nextBoolean();
    }

    public static double nextDouble() {
        switch(rnd.nextInt(20)) {
            case 0:  return 0;
            case 1:  return -0.0;
            case 2:  return Double.MIN_VALUE;
            case 3:  return Double.MAX_VALUE;
            case 4:  return Double.NaN;
            case 5:  return Double.NEGATIVE_INFINITY;
            case 6:  return Double.POSITIVE_INFINITY;
            case 7: case 8: case 9:
                     return (rnd.nextInt(20) - 10);
            default: return rnd.nextDouble();
        }
    }

    public static float nextFloat() {
        switch(rnd.nextInt(20)) {
            case 0:  return 0;
            case 1:  return -0.0f;
            case 2:  return Float.MIN_VALUE;
            case 3:  return Float.MAX_VALUE;
            case 4:  return Float.NaN;
            case 5:  return Float.NEGATIVE_INFINITY;
            case 6:  return Float.POSITIVE_INFINITY;
            case 7: case 8: case 9:
                     return (rnd.nextInt(20) - 10);
            default: return rnd.nextFloat();
        }
    }

    public static Object nextObject() {
        switch(rnd.nextInt(10)) {
            case 0:  return null;
            case 1:  return "foo";
            case 2:  case 3: case 4:
                     return Double.valueOf(nextDouble());
            default: return Integer.valueOf(nextInt());
        }
    }

    public static long[] longArray(int length) {
        long[] result = new long[length];
        for (int i = 0; i < length; i++)
            result[i] = Rnd.nextLong();
        return result;
    }

    public static int[] intArray(int length) {
        int[] result = new int[length];
        for (int i = 0; i < length; i++)
            result[i] = Rnd.nextInt();
        return result;
    }

    public static short[] shortArray(int length) {
        short[] result = new short[length];
        for (int i = 0; i < length; i++)
            result[i] = Rnd.nextShort();
        return result;
    }

    public static char[] charArray(int length) {
        char[] result = new char[length];
        for (int i = 0; i < length; i++)
            result[i] = Rnd.nextChar();
        return result;
    }

    public static byte[] byteArray(int length) {
        byte[] result = new byte[length];
        for (int i = 0; i < length; i++)
            result[i] = Rnd.nextByte();
        return result;
    }

    public static boolean[] booleanArray(int length) {
        boolean[] result = new boolean[length];
        for (int i = 0; i < length; i++)
            result[i] = Rnd.nextBoolean();
        return result;
    }

    public static double[] doubleArray(int length) {
        double[] result = new double[length];
        for (int i = 0; i < length; i++)
            result[i] = Rnd.nextDouble();
        return result;
    }

    public static float[] floatArray(int length) {
        float[] result = new float[length];
        for (int i = 0; i < length; i++)
            result[i] = Rnd.nextFloat();
        return result;
    }

    public static Object[] flatObjectArray(int length) {
        Object[] result = new Object[length];
        for (int i = 0; i < length; i++)
            result[i] = Rnd.nextObject();
        return result;
    }

    // Calling this for length >> 100 is likely to run out of memory!  It
    // should be perhaps be tuned to allow for longer arrays
    public static Object[] nestedObjectArray(int length) {
        Object[] result = new Object[length];
        for (int i = 0; i < length; i++) {
            switch(rnd.nextInt(16)) {
                case 0:  result[i] = nestedObjectArray(length/2);
                         break;
                case 1:  result[i] = longArray(length/2);
                         break;
                case 2:  result[i] = intArray(length/2);
                         break;
                case 3:  result[i] = shortArray(length/2);
                         break;
                case 4:  result[i] = charArray(length/2);
                         break;
                case 5:  result[i] = byteArray(length/2);
                         break;
                case 6:  result[i] = floatArray(length/2);
                         break;
                case 7:  result[i] = doubleArray(length/2);
                         break;
                case 8:  result[i] = longArray(length/2);
                         break;
                default: result[i] = Rnd.nextObject();
            }
        }
        return result;
    }
}

/**
 * Primitive arrays viewed as lists.  Inefficient but cool.
 * This utility should be generally useful in writing regression/unit/basic
 * tests.
 */

class PrimitiveArrays {
    public static List<Long> asList(final long[] a) {
        return new AbstractList<Long>() {
            public Long get(int i) { return a[i]; }
            public int size()      { return a.length; }

            public Long set(int i, Long e) {
                long oldVal = a[i];
                a[i] = e;
                return oldVal;
            }
        };
    }

    public static List<Integer> asList(final int[] a) {
        return new AbstractList<Integer>() {
            public Integer get(int i) { return a[i]; }
            public int size()         { return a.length; }

            public Integer set(int i, Integer e) {
                int oldVal = a[i];
                a[i] = e;
                return oldVal;
            }
        };
    }

    public static List<Short> asList(final short[] a) {
        return new AbstractList<Short>() {
            public Short get(int i) { return a[i]; }
            public int size()       { return a.length; }

            public Short set(int i, Short e) {
                short oldVal = a[i];
                a[i] = e;
                return oldVal;
            }
        };
    }

    public static List<Character> asList(final char[] a) {
        return new AbstractList<Character>() {
            public Character get(int i) { return a[i]; }
            public int size()           { return a.length; }

            public Character set(int i, Character e) {
                Character oldVal = a[i];
                a[i] = e;
                return oldVal;
            }
        };
    }

    public static List<Byte> asList(final byte[] a) {
        return new AbstractList<Byte>() {
            public Byte get(int i) { return a[i]; }
            public int size()      { return a.length; }

            public Byte set(int i, Byte e) {
                Byte oldVal = a[i];
                a[i] = e;
                return oldVal;
            }
        };
    }

    public static List<Boolean> asList(final boolean[] a) {
        return new AbstractList<Boolean>() {
            public Boolean get(int i) { return a[i]; }
            public int size()         { return a.length; }

            public Boolean set(int i, Boolean e) {
                Boolean oldVal = a[i];
                a[i] = e;
                return oldVal;
            }
        };
    }

    public static List<Double> asList(final double[] a) {
        return new AbstractList<Double>() {
            public Double get(int i) { return a[i]; }
            public int size()        { return a.length; }

            public Double set(int i, Double e) {
                Double oldVal = a[i];
                a[i] = e;
                return oldVal;
            }
        };
    }

    public static List<Float> asList(final float[] a) {
        return new AbstractList<Float>() {
            public Float get(int i) { return a[i]; }
            public int size()       { return a.length; }

            public Float set(int i, Float e) {
                Float oldVal = a[i];
                a[i] = e;
                return oldVal;
            }
        };
    }
}
