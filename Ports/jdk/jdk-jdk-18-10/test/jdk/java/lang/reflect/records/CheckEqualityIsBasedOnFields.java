/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8257598
 * @summary check that Record::equals uses the fields and not the accessors for the comparison
 * @run testng CheckEqualityIsBasedOnFields
 */

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class CheckEqualityIsBasedOnFields {
    public record R01(boolean x) {
        public boolean x() {
            return x ? x : !x;
        }
    }

    public record R02(byte x) {
        public byte x() {
            return (x >= 50) ? (byte)(x - 50) : x;
        }
    }

    public record R03(short x) {
        public short x() {
            return (x >= 50) ? (short)(x - 50) : x;
        }
    }

    public record R04(char x) {
        public char x() {
            return (x >= 50) ? (char)(x - 50) : x;
        }
    }

    public record R05(int x) {
        public int x() {
            return (x >= 50) ? (x - 50) : x;
        }
    }

    public record R06(long x) {
        public long x() {
            return (x >= 50) ? (long)(x - 50) : x;
        }
    }

    public record R07(float x) {
        public float x() {
            return (x >= 50) ? (float)(x - 50) : x;
        }
    }
    public record R08(double x) {
        public double x() {
            return (x >= 50) ? (double)(x - 50) : x;
        }
    }

    public record R09(String x) {
        public String x() {
            return (x.length() > 1) ? x.substring(0, 1) : x;
        }
    }

    @DataProvider(name = "recordData")
    public Object[][] recordTypeAndExpectedValue() {
        return new Object[][] {
                new Object[] { R01.class, boolean.class, new Object[]{true, false} },
                new Object[] { R02.class, byte.class, new Object[]{(byte)0, (byte)1, (byte)2, (byte)3, (byte)4, (byte)5,
                        (byte)50, (byte)51, (byte)52, (byte)53, (byte)54, (byte)55} },
                new Object[] { R03.class, short.class, new Object[]{(short)0, (short)1, (short)2, (short)3, (short)4, (short)5,
                        (short)50, (short)51, (short)52, (short)53, (short)54, (short)55} },
                new Object[] { R04.class, char.class, new Object[]{(char)0, (char)1, (char)2, (char)3, (char)4, (char)5,
                        (char)50, (char)51, (char)52, (char)53, (char)54, (char)55} },
                new Object[] { R05.class, int.class, new Object[]{0, 1, 2, 3, 4, 5, 50, 51, 52, 53, 54, 55} },
                new Object[] { R06.class, long.class, new Object[]{0L, 1L, 2L, 3L, 4L, 5L, 50L, 51L, 52L, 53L, 54L, 55L} },
                new Object[] { R07.class, float.class, new Object[]{(float)0, (float)1, (float)2, (float)3, (float)4, (float)5,
                        (float)50, (float)51, (float)52, (float)53, (float)54, (float)55} },
                new Object[] { R08.class, double.class, new Object[]{(double)0, (double)1, (double)2, (double)3, (double)4, (double)5,
                        (double)50, (double)51, (double)52, (double)53, (double)54, (double)55} },
                new Object[] { R09.class, String.class, new Object[]{"1", "2", "3", "4", "5",
                        "1_", "2_", "3_", "4_", "5_"} },
        };
    }

    @Test(dataProvider = "recordData")
    public void testEqualsDoesntUseAccessors(Class<?> clazz, Class<?> componentClass, Object[] expectedXValues) throws Exception {
        Constructor<?> ctor;
        Method getter, equalsMethod;
        ctor = clazz.getConstructor(componentClass);
        equalsMethod = clazz.getMethod("equals", Object.class);
        getter = clazz.getMethod("x");
        for (int i = 0; i < expectedXValues.length / 2; i++) {
            Object rec1 = ctor.newInstance(expectedXValues[i]);
            Object rec2 = ctor.newInstance(expectedXValues[i + expectedXValues.length / 2]);
            System.out.println(rec1.toString());
            System.out.println(rec2.toString());
            assertFalse((boolean) equalsMethod.invoke(rec1, rec2));
            assertNotEquals(expectedXValues[i], expectedXValues[i + expectedXValues.length / 2]);
            assertEquals(getter.invoke(rec1), getter.invoke(rec2));
        }
    }
}
