/*
 * Copyright (c) 2005, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     4655503
 * @summary Test for array cloning and slicing methods.
 * @author  John Rose
 * @key randomness
 */

import java.util.*;
import java.lang.reflect.*;

public class CopyMethods {
    static int muzzle;  // if !=0, suppresses ("muzzles") messages

    static int maxLen = 40;  // maximum length of test arrays
    static int shortStepsNear = 4;  // interesting span near critical values
    static int downShift = 3;

    static int testCasesRun = 0;
    static long consing = 0;

    // very simple tests, mainly to test the framework itself
    static void simpleTests() {
        int[] a = (int[]) makeArray(3, int.class);
        if (muzzle == 0)
            System.out.println("int[] a = "+Arrays.toString(a));
        check(a.length == 3);
        check(a[0] == testValues[0]);
        check(a[1] == testValues[1]);
        check(a[2] == testValues[2]);
        checkArray(a, int.class, 3, 0, 3);
        // negative test of testing framework:
        for (int bad = -2; bad < a.length; bad++) {
            try {
                int[] aa = a.clone();
                if (bad < 0)  aa = new int[4];
                else          aa[bad] = 0;
                ++muzzle;
                // the following check should fail!
                if (bad == -2)
                    checkArray(new String[3], int.class, 0, 0, a.length);
                else
                    checkArray(aa, int.class, 0, 0, a.length);
                throw new Error("Should Not Reach Here");
            } catch (RuntimeException ee) {
                --muzzle;
                if (muzzle == 0)
                    System.out.println("Expected: "+ee);
            }
        }
        checkArray(Arrays.copyOf(a, 0), int.class, 0, 0, 3);
        checkArray(Arrays.copyOf(a, 1), int.class, 1, 0, 3);
        checkArray(Arrays.copyOf(a, 2), int.class, 2, 0, 3);
        checkArray(Arrays.copyOf(a, 3), int.class, 3, 0, 3);
        checkArray(Arrays.copyOf(a, 4), int.class, 4, 0, 3);

        // quick test of copyOfRange
        int[] ar = Arrays.copyOfRange(a, 1, 3);
        check(ar.length == 2);
        check(ar[0] == a[1]);
        check(ar[1] == a[2]);
        checkArray(ar, int.class, 2, 1, 2);
        ar = Arrays.copyOfRange(a, 2, 4);
        check(ar.length == 2);
        check(ar[0] == a[2]);
        check(ar[1] == 0);
        checkArray(ar, int.class, 2, 2, 1);
        ar = Arrays.copyOfRange(a, 3, 5);
        check(ar.length == 2);
        check(ar[0] == 0);
        check(ar[1] == 0);
        checkArray(ar, int.class, 2, 3, 0);
        byte[] ba = (byte[]) makeArray(3, byte.class);
        if (muzzle == 0)
            System.out.println("byte[] ba = "+Arrays.toString(ba));
        for (int j = 0; j <= ba.length+2; j++) {
            byte[] bb = Arrays.copyOf(ba, j);
            if (muzzle == 0)
                System.out.println("copyOf(ba,"+j+") = "+
                                   Arrays.toString(bb));
            checkArray(bb, byte.class, j, 0, ba.length);
            byte[] bbr = Arrays.copyOfRange(ba, 0, j);
            check(Arrays.equals(bb, bbr));
        }
        for (int i = 0; i <= a.length; i++) {
            for (int j = i; j <= a.length+2; j++) {
                byte[] br = Arrays.copyOfRange(ba, i, j);
                if (muzzle == 0)
                    System.out.println("copyOfRange(ba,"+i+","+j+") = "+
                                       Arrays.toString(br));
                checkArray(br, byte.class, j-i, i, ba.length-i);
            }
        }
        String[] sa = (String[]) makeArray(3, String.class);
        if (muzzle == 0)
            System.out.println("String[] sa = "+Arrays.toString(sa));
        check(sa[0].equals(Integer.toHexString(testValues[0])));
        check(sa[1].equals(Integer.toHexString(testValues[1])));
        check(sa[2].equals(Integer.toHexString(testValues[2])));
        checkArray(sa, String.class, sa.length, 0, sa.length);
        String[] sa4 = Arrays.copyOf(sa, sa.length+1);
        check(sa4[0] == sa[0]);
        check(sa4[1] == sa[1]);
        check(sa4[2] == sa[2]);
        check(sa4[sa.length] == null);
        checkArray(sa4, String.class, sa4.length, 0, sa.length);
        String[] sr4 = Arrays.copyOfRange(sa, 1, 5);
        check(sr4[0] == sa[1]);
        check(sr4[1] == sa[2]);
        check(sr4[2] == null);
        check(sr4[3] == null);
        checkArray(sr4, String.class, 4, 1, sa.length-1);
        if (muzzle == 0)
            System.out.println("simpleTests done");
    }

    // the framework:  a fixed series of test values
    static final int[] testValues;
    static {
        testValues = new int[1000];
        Random r = new Random();
        for (int i = 0; i < testValues.length; i++) {
            testValues[i] = r.nextInt();
        }
    }
    /** Return a canonical test value of a desired index and type.
     *  The original test values are random ints.  Derive other test
     *  values as follows:
     *  <pre>
     *  int tv = testValues[i]
     *  (C)tv                    C is byte, short, char, long, float, double
     *  (tv&1)!=0                C is boolean
     *  (Integer)tv              C is Object and tv%16 != 0
     *  null                     C is Object and tv%16 == 0
     *  Integer.toHexString(tv)  C is String and tv != 0
     *  null                     C is String and tv == 0
     *  </pre>
     *  are derived by ordinary Java coercions, except that boolean
     *  samples the LSB of the int value, and String is the hex numeral.
     *
     *  (Also, the 0th String is null, and the 0th Object mod 16 is null,
     *  regardless of the original int test value.)
     */
    static Object testValue(int i, Class<?> c) {
        int tv = testValues[i % testValues.length];
        if (i >= testValues.length)  tv ^= i;
        // Turn the canonical int to a float, boolean, String, whatever:
        return invoke(coercers.get(c), tv);
    }
    /** Build a test array of the given length,
     *  packed with a subsequence of the test values.
     *  The first element of the array is always testValue(0).
     */
    static Object makeArray(int len, Class<?> c) {
        Object a = Array.newInstance(c, len);
        for (int i = 0; i < len; i++) {
            Array.set(a, i, testValue(i, c));
        }
        return a;
    }
    /** Check that the given array has the required length.
     *  Check also that it is packed, up to firstNull, with
     *  a particular subsequence of the canonical test values.
     *  The subsequence must begin with a[0] == testValue(offset).
     *  At a[firstNull] and beyond, the array must contain null values.
     */
    static void checkArray(Object a, Class<?> c, int requiredLen, int offset, int firstNull) {
        check(c == a.getClass().getComponentType());
        Object nullValue = nullValues.get(c);
        // Note:  asserts in here are not part of the test program.
        // They verify the integrity of the test method itself.
        assert(nullValues.containsKey(c));

        int misses = 0;
        int firstMiss = -1;
        // Check required length first.
        int length = Array.getLength(a);
        if (length != requiredLen && requiredLen != -1) {
            if (muzzle == 0)
                System.out.println("*** a.length = "+length+" != "+requiredLen);
            ++misses;
        }

        for (int i = 0; i < length; i++) {
            Object tv = (i >= firstNull) ? nullValue : testValue(i+offset, c);
            Object ai = Array.get(a, i);
            if (!eq(ai, tv)) {
                if (muzzle == 0)
                    System.out.println("*** a["+i+"] = "+ai+" != "+tv);
                if (misses == 0)  firstMiss = i;
                if (++misses > 10)  break;
            }
        }
        if (misses != 0) {
            Method toString = toStrings.get(c);
            if (toString == null)  toString = toStrings.get(Object.class);
            throw new RuntimeException("checkArray failed at "+firstMiss
                                       +" "+c+"[]"
                                       +" : "+invoke(toString, a));
        }
    }
    // Typical comparison helper.  Why isn't this a method somewhere.
    static boolean eq(Object x, Object y) {
        return x == null? y == null: x.equals(y);
    }
    // Exception-ignoring invoke function.
    static Object invoke(Method m, Object... args) {
        Exception ex;
        try {
            return m.invoke(null, args);
        } catch (InvocationTargetException ee) {
            ex = ee;
        } catch (IllegalAccessException ee) {
            ex = ee;
        } catch (IllegalArgumentException ee) {
            ex = ee;
        }
        ArrayList<Object> call = new ArrayList<Object>();
        call.add(m); Collections.addAll(call, args);
        throw new RuntimeException(call+" : "+ex);
    }
    // version of assert() that runs unconditionally
    static void check(boolean z) {
        if (!z)  throw new RuntimeException("check failed");
    }


    /** Run about 10**5 distinct parameter combinations
     *  on copyOf and copyOfRange.  Use all primitive types,
     *  and String and Object.
     *  Try to all critical values, looking for fencepost errors.
     */
    static void fullTests(int maxLen, Class<?> c) {
        Method cloner      = cloners.get(c);
        assert(cloner != null) : c;
        Method cloneRanger = cloneRangers.get(c);
        // Note:  asserts in here are not part of the test program.
        // They verify the integrity of the test method itself.
        assert(cloneRanger != null) : c;
        for (int src = 0; src <= maxLen; src = inc(src, 0, maxLen)) {
            Object a = makeArray(src, c);
            for (int x : new ArrayList<Integer>()) {}
            for (int j = 0; j <= maxLen; j = inc(j, src, maxLen)) {
                // b = Arrays.copyOf(a, j);
                Object b = invoke(cloner, a, j);
                checkArray(b, c, j, 0, src);
                testCasesRun++;
                consing += j;

                int maxI = Math.min(src, j);
                for (int i = 0; i <= maxI; i = inc(i, src, maxI)) {
                    // r = Arrays.copyOfRange(a, i, j);
                    Object r = invoke(cloneRanger, a, i, j);
                    checkArray(r, c, j-i, i, src-i);
                    //System.out.println("case c="+c+" src="+src+" i="+i+" j="+j);
                    testCasesRun++;
                    consing += j-i;
                }
            }
        }
    }
    // Increment x by at least one.  Increment by a little more unless
    // it is near a critical value, either zero, crit1, or crit2.
    static int inc(int x, int crit1, int crit2) {
        int D = shortStepsNear;
        if (crit1 > crit2) { int t = crit1; crit1 = crit2; crit2 = t; }
        assert(crit1 <= crit2);
        assert(x <= crit2);  // next1 or next2 must be the limit value
        x += 1;
        if (x > D) {
            if (x < crit1-D) {
                x += (x << 1) >> downShift;  // giant step toward crit1-D
                if (x > crit1-D)  x = crit1-D;
            } else if (x >= crit1+D && x < crit2-D) {
                x += (x << 1) >> downShift;  // giant step toward crit2-D
                if (x > crit2-D)  x = crit2-D;
            }
        }
        return x;
    }

    public static void main(String[] av) {
        boolean verbose = (av.length != 0);
        muzzle = (verbose? 0: 1);
        if (muzzle == 0)
            System.out.println("test values: "+Arrays.toString(Arrays.copyOf(testValues, 5))+"...");

        simpleTests();

        muzzle = 0;  // turn on print statements (affects failures only)

        fullTests();
        if (verbose)
            System.out.println("ran "+testCasesRun+" tests, avg len="
                               +(float)consing/testCasesRun);

        // test much larger arrays, more sparsely
        maxLen = 500;
        shortStepsNear = 2;
        downShift = 0;
        testCasesRun = 0;
        consing = 0;
        fullTests();
        if (verbose)
            System.out.println("ran "+testCasesRun+" tests, avg len="
                               +(float)consing/testCasesRun);
    }

    static void fullTests() {
        for (Class<?> c : allTypes) {
            fullTests(maxLen, c);
        }
    }

    // We must run all the our tests on each of 8 distinct primitive types,
    // and two reference types (Object, String) for good measure.
    // This would be a pain to write out by hand, statically typed.
    // So, use reflection.  Following are the tables of methods we use.
    // (The initial simple tests exercise enough of the static typing
    // features of the API to ensure that they compile as advertised.)

    static Object  coerceToObject(int x) { return (x & 0xF) == 0? null: new Integer(x); }
    static String  coerceToString(int x) { return (x == 0)? null: Integer.toHexString(x); }
    static Integer coerceToInteger(int x) { return (x == 0)? null: x; }
    static byte    coerceToByte(int x) { return (byte)x; }
    static short   coerceToShort(int x) { return (short)x; }
    static int     coerceToInt(int x) { return x; }
    static long    coerceToLong(int x) { return x; }
    static char    coerceToChar(int x) { return (char)x; }
    static float   coerceToFloat(int x) { return x; }
    static double  coerceToDouble(int x) { return x; }
    static boolean coerceToBoolean(int x) { return (x&1) != 0; }

    static Integer[] copyOfIntegerArray(Object[] a, int len) {
        // This guy exercises the API based on a type-token.
        // Note the static typing.
        return Arrays.copyOf(a, len, Integer[].class);
    }
    static Integer[] copyOfIntegerArrayRange(Object[] a, int m, int n) {
        // This guy exercises the API based on a type-token.
        // Note the static typing.
        return Arrays.copyOfRange(a, m, n, Integer[].class);
    }

    static final List<Class<?>> allTypes
        = Arrays.asList(new Class<?>[]
                        {   Object.class, String.class, Integer.class,
                            byte.class, short.class, int.class, long.class,
                            char.class, float.class, double.class,
                            boolean.class
                        });
    static final HashMap<Class<?>,Method> coercers;
    static final HashMap<Class<?>,Method> cloners;
    static final HashMap<Class<?>,Method> cloneRangers;
    static final HashMap<Class<?>,Method> toStrings;
    static final HashMap<Class<?>,Object> nullValues;
    static {
        coercers = new HashMap<Class<?>,Method>();
        Method[] testMethods = CopyMethods.class.getDeclaredMethods();
        Method cia = null, ciar = null;
        for (int i = 0; i < testMethods.length; i++) {
            Method m = testMethods[i];
            if (!Modifier.isStatic(m.getModifiers()))  continue;
            Class<?> rt = m.getReturnType();
            if (m.getName().startsWith("coerceTo") && allTypes.contains(rt))
                coercers.put(m.getReturnType(), m);
            if (m.getName().equals("copyOfIntegerArray"))
                cia = m;
            if (m.getName().equals("copyOfIntegerArrayRange"))
                ciar = m;
        }
        Method[] arrayMethods = Arrays.class.getDeclaredMethods();
        cloners      = new HashMap<Class<?>,Method>();
        cloneRangers = new HashMap<Class<?>,Method>();
        toStrings    = new HashMap<Class<?>,Method>();
        for (int i = 0; i < arrayMethods.length; i++) {
            Method m = arrayMethods[i];
            if (!Modifier.isStatic(m.getModifiers()))  continue;
            Class<?> rt = m.getReturnType();
            if (m.getName().equals("copyOf")
                && m.getParameterTypes().length == 2)
                cloners.put(rt.getComponentType(), m);
            if (m.getName().equals("copyOfRange")
                && m.getParameterTypes().length == 3)
                cloneRangers.put(rt.getComponentType(), m);
            if (m.getName().equals("toString")) {
                Class<?> pt = m.getParameterTypes()[0];
                toStrings.put(pt.getComponentType(), m);
            }
        }
        cloners.put(String.class, cloners.get(Object.class));
        cloneRangers.put(String.class, cloneRangers.get(Object.class));
        assert(cia != null);
        cloners.put(Integer.class, cia);
        assert(ciar != null);
        cloneRangers.put(Integer.class, ciar);
        nullValues = new HashMap<Class<?>,Object>();
        for (Class<?> c : allTypes) {
            nullValues.put(c, invoke(coercers.get(c), 0));
        }
    }
}
