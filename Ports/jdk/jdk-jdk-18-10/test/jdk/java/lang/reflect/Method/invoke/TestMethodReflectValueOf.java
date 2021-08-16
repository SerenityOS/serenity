/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 5043030
 * @summary Verify that the method java.lang.reflect.Method.invoke(Object, Object...)
 *          makes use of the same caching mechanism as used for autoboxing
 *          when wrapping returned values of the primitive types.
 * @author Andrej Golovnin
 * @run main/othervm -Dsun.reflect.noInflation=true TestMethodReflectValueOf
 * @run main/othervm -Dsun.reflect.noInflation=false -Dsun.reflect.inflationThreshold=500 TestMethodReflectValueOf
 */

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


public class TestMethodReflectValueOf {

    public static void main(String[] args) {
        // When the inflation is disabled we compare values using "=="
        // as the returned values of the primitive types should be cached
        // by the same mechanism as used for autoboxing. When the inflation
        // is enabled we use "equals()"-method to compare values as the native
        // code still creates new instances to wrap values of the primitive
        // types.
        boolean checkIdentity = Boolean.getBoolean("sun.reflect.noInflation");

        // Boolean#valueOf test
        testMethod(Boolean.TYPE, Boolean.FALSE, checkIdentity);
        testMethod(Boolean.TYPE, Boolean.TRUE, checkIdentity);

        // Byte#valueOf test
        for (int b = Byte.MIN_VALUE; b < (Byte.MAX_VALUE + 1); b++) {
            testMethod(Byte.TYPE, Byte.valueOf((byte) b), checkIdentity);
        }

        // Character#valueOf test
        for (char c = '\u0000'; c <= '\u007F'; c++) {
            testMethod(Character.TYPE, Character.valueOf(c), checkIdentity);
        }

        // Integer#valueOf test
        for (int i = -128; i <= 127; i++) {
            testMethod(Integer.TYPE, Integer.valueOf(i), checkIdentity);
        }

        // Long#valueOf test
        for (long l = -128L; l <= 127L; l++) {
            testMethod(Long.TYPE, Long.valueOf(l), checkIdentity);
        }

        // Short#valueOf test
        for (short s = -128; s <= 127; s++) {
            testMethod(Short.TYPE, Short.valueOf(s), checkIdentity);
        }
    }

    public static void testMethod(Class<?> primType, Object wrappedValue,
            boolean checkIdentity)
    {
        String methodName = primType.getName() + "Method";
        try {
            Method method = TestMethodReflectValueOf.class.getMethod(methodName, primType);
            Object result = method.invoke(new TestMethodReflectValueOf(), wrappedValue);
            if (checkIdentity) {
                if (result != wrappedValue) {
                    throw new RuntimeException("The value " + wrappedValue
                        + " is not cached for the type " + primType);
                }
            } else {
                if (!result.equals(wrappedValue)) {
                    throw new RuntimeException("The result value " + result
                        + " is not equal to the expected value "
                        + wrappedValue + " for the type " + primType);
                }
            }
        } catch (  NoSuchMethodException | SecurityException
                 | IllegalAccessException | IllegalArgumentException
                 | InvocationTargetException e)
        {
            throw new RuntimeException(e);
        }
    }

    public int intMethod(int value) {
        return value;
    }

    public long longMethod(long value) {
        return value;
    }

    public short shortMethod(short value) {
        return value;
    }

    public byte byteMethod(byte value) {
        return value;
    }

    public char charMethod(char value) {
        return value;
    }

    public boolean booleanMethod(boolean value) {
        return value;
    }

}
