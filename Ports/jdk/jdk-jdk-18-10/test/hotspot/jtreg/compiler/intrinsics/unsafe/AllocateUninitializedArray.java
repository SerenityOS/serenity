/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8150465 8259339
 * @summary Unsafe methods to produce uninitialized arrays
 * @modules java.base/jdk.internal.misc:+open
 *
 * @run main/othervm -ea -Diters=200   -Xint
 *      compiler.intrinsics.unsafe.AllocateUninitializedArray
 * @run main/othervm -ea -Diters=30000 -XX:TieredStopAtLevel=1
 *      compiler.intrinsics.unsafe.AllocateUninitializedArray
 * @run main/othervm -ea -Diters=30000 -XX:TieredStopAtLevel=4
 *      compiler.intrinsics.unsafe.AllocateUninitializedArray
 */

package compiler.intrinsics.unsafe;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.concurrent.Callable;

public class AllocateUninitializedArray {
    static final int ITERS = Integer.getInteger("iters", 1);
    static final jdk.internal.misc.Unsafe UNSAFE;

    static {
        try {
            Field f = jdk.internal.misc.Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            UNSAFE = (jdk.internal.misc.Unsafe) f.get(null);
        } catch (Exception e) {
            throw new RuntimeException("Unable to get Unsafe instance.", e);
        }
    }

    public static void main(String... args) throws Exception {
        testIAE(AllConstants::testObject);
        testIAE(LengthIsConstant::testObject);
        testIAE(ClassIsConstant::testObject);
        testIAE(NothingIsConstant::testObject);

        testIAE(AllConstants::testArray);
        testIAE(LengthIsConstant::testArray);
        testIAE(ClassIsConstant::testArray);
        testIAE(NothingIsConstant::testArray);

        testIAE(AllConstants::testNull);
        testIAE(LengthIsConstant::testNull);
        testIAE(ClassIsConstant::testNull);
        testIAE(NothingIsConstant::testNull);

        testOK(boolean[].class, 10, AllConstants::testBoolean);
        testOK(byte[].class,    10, AllConstants::testByte);
        testOK(short[].class,   10, AllConstants::testShort);
        testOK(char[].class,    10, AllConstants::testChar);
        testOK(int[].class,     10, AllConstants::testInt);
        testOK(float[].class,   10, AllConstants::testFloat);
        testOK(long[].class,    10, AllConstants::testLong);
        testOK(double[].class,  10, AllConstants::testDouble);
        testOK(null,            10, AllConstants::testVoid);

        testOK(boolean[].class, 10, LengthIsConstant::testBoolean);
        testOK(byte[].class,    10, LengthIsConstant::testByte);
        testOK(short[].class,   10, LengthIsConstant::testShort);
        testOK(char[].class,    10, LengthIsConstant::testChar);
        testOK(int[].class,     10, LengthIsConstant::testInt);
        testOK(float[].class,   10, LengthIsConstant::testFloat);
        testOK(long[].class,    10, LengthIsConstant::testLong);
        testOK(double[].class,  10, LengthIsConstant::testDouble);
        testOK(null,            10, LengthIsConstant::testVoid);

        testOK(boolean[].class, 10, ClassIsConstant::testBoolean);
        testOK(byte[].class,    10, ClassIsConstant::testByte);
        testOK(short[].class,   10, ClassIsConstant::testShort);
        testOK(char[].class,    10, ClassIsConstant::testChar);
        testOK(int[].class,     10, ClassIsConstant::testInt);
        testOK(float[].class,   10, ClassIsConstant::testFloat);
        testOK(long[].class,    10, ClassIsConstant::testLong);
        testOK(double[].class,  10, ClassIsConstant::testDouble);
        testOK(null,            10, ClassIsConstant::testVoid);

        testOK(boolean[].class, 10, NothingIsConstant::testBoolean);
        testOK(byte[].class,    10, NothingIsConstant::testByte);
        testOK(short[].class,   10, NothingIsConstant::testShort);
        testOK(char[].class,    10, NothingIsConstant::testChar);
        testOK(int[].class,     10, NothingIsConstant::testInt);
        testOK(float[].class,   10, NothingIsConstant::testFloat);
        testOK(long[].class,    10, NothingIsConstant::testLong);
        testOK(double[].class,  10, NothingIsConstant::testDouble);
        testOK(null,            10, NothingIsConstant::testVoid);
    }

    public static void testOK(Class<?> expectClass, int expectLen, Callable<Object> test) throws Exception {
        for (int c = 0; c < ITERS; c++) {
            Object res = test.call();
            if (res == null) {
                if (expectClass != null) {
                    throw new IllegalStateException("Unexpected null result");
                }
                continue;
            }
            Class<?> actualClass = res.getClass();
            if (!actualClass.equals(expectClass)) {
                throw new IllegalStateException("Wrong class: expected = " + expectClass + ", but got " + actualClass);
            }
            int actualLen = Array.getLength(res);
            if (actualLen != expectLen) {
                throw new IllegalStateException("Wrong length: expected = " + expectLen + ", but got " + actualLen);
            }
        }
    }

    static volatile Object sink;

    public static void testIAE(Callable<Object> test) throws Exception {
        for (int c = 0; c < ITERS; c++) {
            try {
               sink = test.call();
               throw new IllegalStateException("Expected IAE");
            } catch (IllegalArgumentException iae) {
               // expected
            }
        }
    }

    static volatile int sampleLenNeg  = -1;
    static volatile int sampleLenZero = 0;
    static volatile int sampleLen     = 10;


    static volatile Class<?> classBoolean = boolean.class;
    static volatile Class<?> classByte    = byte.class;
    static volatile Class<?> classShort   = short.class;
    static volatile Class<?> classChar    = char.class;
    static volatile Class<?> classInt     = int.class;
    static volatile Class<?> classFloat   = float.class;
    static volatile Class<?> classLong    = long.class;
    static volatile Class<?> classDouble  = double.class;
    static volatile Class<?> classVoid    = void.class;
    static volatile Class<?> classObject  = Object.class;
    static volatile Class<?> classArray   = Object[].class;
    static volatile Class<?> classNull    = null;

    static class AllConstants {
        static Object testBoolean() { return UNSAFE.allocateUninitializedArray(boolean.class,  10); }
        static Object testByte()    { return UNSAFE.allocateUninitializedArray(byte.class,     10); }
        static Object testShort()   { return UNSAFE.allocateUninitializedArray(short.class,    10); }
        static Object testChar()    { return UNSAFE.allocateUninitializedArray(char.class,     10); }
        static Object testInt()     { return UNSAFE.allocateUninitializedArray(int.class,      10); }
        static Object testFloat()   { return UNSAFE.allocateUninitializedArray(float.class,    10); }
        static Object testLong()    { return UNSAFE.allocateUninitializedArray(long.class,     10); }
        static Object testDouble()  { return UNSAFE.allocateUninitializedArray(double.class,   10); }
        static Object testVoid()    { return UNSAFE.allocateUninitializedArray(void.class,     10); }
        static Object testObject()  { return UNSAFE.allocateUninitializedArray(Object.class,   10); }
        static Object testArray()   { return UNSAFE.allocateUninitializedArray(Object[].class, 10); }
        static Object testNull()    { return UNSAFE.allocateUninitializedArray(null,           10); }
        static Object testZero()    { return UNSAFE.allocateUninitializedArray(int.class,      0);  }
        static Object testNeg()     { return UNSAFE.allocateUninitializedArray(int.class,      -1); }
    }

    static class ClassIsConstant {
        static Object testBoolean() { return UNSAFE.allocateUninitializedArray(boolean.class,  sampleLen); }
        static Object testByte()    { return UNSAFE.allocateUninitializedArray(byte.class,     sampleLen); }
        static Object testShort()   { return UNSAFE.allocateUninitializedArray(short.class,    sampleLen); }
        static Object testChar()    { return UNSAFE.allocateUninitializedArray(char.class,     sampleLen); }
        static Object testInt()     { return UNSAFE.allocateUninitializedArray(int.class,      sampleLen); }
        static Object testFloat()   { return UNSAFE.allocateUninitializedArray(float.class,    sampleLen); }
        static Object testLong()    { return UNSAFE.allocateUninitializedArray(long.class,     sampleLen); }
        static Object testDouble()  { return UNSAFE.allocateUninitializedArray(double.class,   sampleLen); }
        static Object testVoid()    { return UNSAFE.allocateUninitializedArray(void.class,     sampleLen); }
        static Object testObject()  { return UNSAFE.allocateUninitializedArray(Object.class,   sampleLen); }
        static Object testArray()   { return UNSAFE.allocateUninitializedArray(Object[].class, sampleLen); }
        static Object testNull()    { return UNSAFE.allocateUninitializedArray(null,           sampleLen); }
        static Object testZero()    { return UNSAFE.allocateUninitializedArray(int.class,      sampleLenZero); }
        static Object testNeg()     { return UNSAFE.allocateUninitializedArray(int.class,      sampleLenNeg); }
    }

    static class LengthIsConstant {
        static Object testBoolean() { return UNSAFE.allocateUninitializedArray(classBoolean, 10); }
        static Object testByte()    { return UNSAFE.allocateUninitializedArray(classByte,    10); }
        static Object testShort()   { return UNSAFE.allocateUninitializedArray(classShort,   10); }
        static Object testChar()    { return UNSAFE.allocateUninitializedArray(classChar,    10); }
        static Object testInt()     { return UNSAFE.allocateUninitializedArray(classInt,     10); }
        static Object testFloat()   { return UNSAFE.allocateUninitializedArray(classFloat,   10); }
        static Object testLong()    { return UNSAFE.allocateUninitializedArray(classLong,    10); }
        static Object testDouble()  { return UNSAFE.allocateUninitializedArray(classDouble,  10); }
        static Object testVoid()    { return UNSAFE.allocateUninitializedArray(classVoid,    10); }
        static Object testObject()  { return UNSAFE.allocateUninitializedArray(classObject,  10); }
        static Object testArray()   { return UNSAFE.allocateUninitializedArray(classArray,   10); }
        static Object testNull()    { return UNSAFE.allocateUninitializedArray(classNull,    10); }
        static Object testZero()    { return UNSAFE.allocateUninitializedArray(classInt,     0);  }
        static Object testNeg()     { return UNSAFE.allocateUninitializedArray(classInt,     -1); }
    }

    static class NothingIsConstant {
        static Object testBoolean() { return UNSAFE.allocateUninitializedArray(classBoolean, sampleLen); }
        static Object testByte()    { return UNSAFE.allocateUninitializedArray(classByte,    sampleLen); }
        static Object testShort()   { return UNSAFE.allocateUninitializedArray(classShort,   sampleLen); }
        static Object testChar()    { return UNSAFE.allocateUninitializedArray(classChar,    sampleLen); }
        static Object testInt()     { return UNSAFE.allocateUninitializedArray(classInt,     sampleLen); }
        static Object testFloat()   { return UNSAFE.allocateUninitializedArray(classFloat,   sampleLen); }
        static Object testLong()    { return UNSAFE.allocateUninitializedArray(classLong,    sampleLen); }
        static Object testDouble()  { return UNSAFE.allocateUninitializedArray(classDouble,  sampleLen); }
        static Object testVoid()    { return UNSAFE.allocateUninitializedArray(classVoid,    sampleLen); }
        static Object testObject()  { return UNSAFE.allocateUninitializedArray(classObject,  sampleLen); }
        static Object testArray()   { return UNSAFE.allocateUninitializedArray(classArray,   sampleLen); }
        static Object testNull()    { return UNSAFE.allocateUninitializedArray(classNull,    sampleLen); }
        static Object testZero()    { return UNSAFE.allocateUninitializedArray(classInt,     sampleLenZero); }
        static Object testNeg()     { return UNSAFE.allocateUninitializedArray(classInt,     sampleLenNeg); }
    }
}

