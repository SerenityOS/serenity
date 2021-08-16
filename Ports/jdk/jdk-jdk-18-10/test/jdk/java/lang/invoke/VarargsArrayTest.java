/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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

import sun.invoke.util.Wrapper;
import test.java.lang.invoke.lib.CodeCacheOverflowProcessor;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandleHelper;
import java.lang.invoke.MethodType;
import java.util.Arrays;
import java.util.Collections;

/* @test
 * @summary unit tests for varargs array methods: MethodHandleInfo.varargsArray(int),
 *          MethodHandleInfo.varargsArray(Class,int) & MethodHandleInfo.varargsList(int)
 * @modules java.base/sun.invoke.util
 * @library /test/lib /java/lang/invoke/common
 * @compile/module=java.base java/lang/invoke/MethodHandleHelper.java
 * @run main/bootclasspath VarargsArrayTest
 * @run main/bootclasspath/othervm -DVarargsArrayTest.MAX_ARITY=255 -DVarargsArrayTest.START_ARITY=250
 *                         VarargsArrayTest
 */

/* This might take a while and burn lots of metadata:
 * @run main/bootclasspath -DVarargsArrayTest.MAX_ARITY=255 -DVarargsArrayTest.EXHAUSTIVE=true VarargsArrayTest
 */
public class VarargsArrayTest {
    private static final Class<?> CLASS = VarargsArrayTest.class;
    private static final int MAX_ARITY = Integer.getInteger(
            CLASS.getSimpleName()+".MAX_ARITY", 40);
    private static final int START_ARITY = Integer.getInteger(
            CLASS.getSimpleName()+".START_ARITY", 0);
    private static final boolean EXHAUSTIVE = Boolean.getBoolean(
            CLASS.getSimpleName()+".EXHAUSTIVE");

    public static void main(String[] args) throws Throwable {
        CodeCacheOverflowProcessor.runMHTest(VarargsArrayTest::test);
    }

    public static void test() throws Throwable {
        testVarargsArray();
        testVarargsReferenceArray();
        testVarargsPrimitiveArray();
    }

    public static void testVarargsArray() throws Throwable {
        final int MIN = START_ARITY;
        final int MAX = MAX_ARITY-2;  // 253+1 would cause parameter overflow with 'this' added
        for (int nargs = MIN; nargs <= MAX; nargs = nextArgCount(nargs, 17, MAX)) {
            MethodHandle target = MethodHandleHelper.varargsArray(nargs);
            Object[] args = new Object[nargs];
            for (int i = 0; i < nargs; i++)
                args[i] = "#"+i;
            Object res = target.invokeWithArguments(args);
            assertArrayEquals(args, (Object[])res);
        }
    }

    private static class CustomClass {}

    public static void testVarargsReferenceArray() throws Throwable {
        testTypedVarargsArray(Object[].class);
        testTypedVarargsArray(String[].class);
        testTypedVarargsArray(Number[].class);
        testTypedVarargsArray(CustomClass[].class);
    }

    public static void testVarargsPrimitiveArray() throws Throwable {
        testTypedVarargsArray(int[].class);
        testTypedVarargsArray(long[].class);
        testTypedVarargsArray(byte[].class);
        testTypedVarargsArray(boolean[].class);
        testTypedVarargsArray(short[].class);
        testTypedVarargsArray(char[].class);
        testTypedVarargsArray(float[].class);
        testTypedVarargsArray(double[].class);
    }

    private static int nextArgCount(int nargs, int density, int MAX) {
        if (EXHAUSTIVE)  return nargs + 1;
        if (nargs >= MAX)  return Integer.MAX_VALUE;
        int BOT = 20, TOP = MAX-5;
        if (density < 10) { BOT = 10; MAX = TOP-2; }
        if (nargs <= BOT || nargs >= TOP) {
            ++nargs;
        } else {
            int bump = Math.max(1, 100 / density);
            nargs += bump;
            if (nargs > TOP)  nargs = TOP;
        }
        return nargs;
    }

    private static void testTypedVarargsArray(Class<?> arrayType) throws Throwable {
        Class<?> elemType = arrayType.getComponentType();
        int MIN = START_ARITY;
        int MAX = MAX_ARITY-2;  // 253+1 would cause parameter overflow with 'this' added
        int density = 3;
        if (elemType == int.class || elemType == long.class)  density = 7;
        if (elemType == long.class || elemType == double.class) { MAX /= 2; MIN /= 2; }
        for (int nargs = MIN; nargs <= MAX; nargs = nextArgCount(nargs, density, MAX)) {
            Object[] args = makeTestArray(elemType, nargs);
            MethodHandle varargsArray = MethodHandleHelper.varargsArray(arrayType, nargs);
            MethodType vaType = varargsArray.type();
            assertEquals(arrayType, vaType.returnType());
            if (nargs != 0) {
                assertEquals(elemType, vaType.parameterType(0));
                assertEquals(elemType, vaType.parameterType(vaType.parameterCount()-1));
            }
            assertEquals(MethodType.methodType(arrayType, Collections.<Class<?>>nCopies(nargs, elemType)),
                         vaType);
            Object res = varargsArray.invokeWithArguments(args);
            assertEquals(res.getClass(), arrayType);
            String resString = toArrayString(res);
            assertEquals(Arrays.toString(args), resString);

            MethodHandle spreader = varargsArray.asSpreader(arrayType, nargs);
            MethodType stype = spreader.type();
            assert(stype == MethodType.methodType(arrayType, arrayType));
            if (nargs <= 5) {
                // invoke target as a spreader also:
                @SuppressWarnings("cast")
                Object res2 = spreader.invokeWithArguments((Object)res);
                String res2String = toArrayString(res2);
                assertEquals(Arrays.toString(args), res2String);
                // invoke the spreader on a generic Object[] array; check for error
                try {
                    Object res3 = spreader.invokeWithArguments((Object)args);
                    String res3String = toArrayString(res3);
                    assertTrue(arrayType.getName(), arrayType.isAssignableFrom(Object[].class));
                    assertEquals(Arrays.toString(args), res3String);
                } catch (ClassCastException ex) {
                    assertFalse(arrayType.getName(), arrayType.isAssignableFrom(Object[].class));
                }
            }
            if (nargs == 0) {
                // invoke spreader on null arglist
                Object res3 = spreader.invokeWithArguments((Object)null);
                String res3String = toArrayString(res3);
                assertEquals(Arrays.toString(args), res3String);
            }
        }
    }

    private static Object[] makeTestArray(Class<?> elemType, int len) {
        Wrapper elem = null;
        if (elemType.isPrimitive())
            elem = Wrapper.forPrimitiveType(elemType);
        else if (Wrapper.isWrapperType(elemType))
            elem = Wrapper.forWrapperType(elemType);
        Object[] args = new Object[len];
        for (int i = 0; i < len; i++) {
            Object arg = i * 100;
            if (elem == null) {
                if (elemType == String.class)
                    arg = "#"+arg;
                if (elemType  == CustomClass.class)
                    arg = new CustomClass();
                arg = elemType.cast(arg);  // just to make sure
            } else {
                switch (elem) {
                    case BOOLEAN: arg = (i % 3 == 0);           break;
                    case CHAR:    arg = 'a' + i;                break;
                    case LONG:    arg = (long)i * 1000_000_000; break;
                    case FLOAT:   arg = (float)i / 100;         break;
                    case DOUBLE:  arg = (double)i / 1000_000;   break;
                }
                arg = elem.cast(arg, elemType);
            }
            args[i] = arg;
        }
        return args;
    }

    private static String toArrayString(Object a) {
        if (a == null)  return "null";
        Class<?> elemType = a.getClass().getComponentType();
        if (elemType == null)  return a.toString();
        if (elemType.isPrimitive()) {
            switch (Wrapper.forPrimitiveType(elemType)) {
                case INT:      return Arrays.toString((int[])a);
                case BYTE:     return Arrays.toString((byte[])a);
                case BOOLEAN:  return Arrays.toString((boolean[])a);
                case SHORT:    return Arrays.toString((short[])a);
                case CHAR:     return Arrays.toString((char[])a);
                case FLOAT:    return Arrays.toString((float[])a);
                case LONG:     return Arrays.toString((long[])a);
                case DOUBLE:   return Arrays.toString((double[])a);
            }
        }
        return Arrays.toString((Object[])a);
    }

    public static void assertArrayEquals(Object[] arr1, Object[] arr2) {
        if (arr1 == null && arr2 == null)  return;
        if (arr1 != null && arr2 != null && arr1.length == arr2.length) {
            for (int i = 0; i < arr1.length; i++) {
                assertEquals(arr1[i], arr2[i]);
            }
            return;
        }
        throw new AssertionError(Arrays.deepToString(arr1)
                + " != " + Arrays.deepToString(arr2));
    }

    public static void assertEquals(Object o1, Object o2) {
        if (o1 == null && o2 == null)    return;
        if (o1 != null && o1.equals(o2)) return;
        throw new AssertionError(o1 + " != " + o2);
    }

    public static void assertTrue(String msg, boolean b) {
        if (!b) {
            throw new AssertionError(msg);
        }
    }

    public static void assertFalse(String msg, boolean b) {
        assertTrue(msg, !b);
    }
}
