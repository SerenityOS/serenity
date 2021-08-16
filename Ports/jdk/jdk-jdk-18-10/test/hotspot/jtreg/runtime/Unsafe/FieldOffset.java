/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Verifies the behaviour of Unsafe.fieldOffset
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @run main FieldOffset
 */

import java.lang.reflect.Field;
import jdk.internal.misc.Unsafe;
import java.lang.reflect.*;
import static jdk.test.lib.Asserts.*;

public class FieldOffset {
    public static void main(String args[]) throws Exception {
        Unsafe unsafe = Unsafe.getUnsafe();
        Field[] fields = Test.class.getDeclaredFields();

        for (int i = 0; i < fields.length; i++) {
            long offset = unsafe.objectFieldOffset(fields[i]);
            // Ensure we got a valid offset value back
            assertNotEquals(offset, unsafe.INVALID_FIELD_OFFSET);

            // Make sure the field offset is unique
            for (int j = 0; j < i; j++) {
                assertNotEquals(offset, unsafe.objectFieldOffset(fields[j]));
            }
        }

        fields = StaticTest.class.getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
            long offset = unsafe.staticFieldOffset(fields[i]);
            // Ensure we got a valid offset value back
            assertNotEquals(offset, unsafe.INVALID_FIELD_OFFSET);

            // Make sure the field offset is unique
            for (int j = 0; j < i; j++) {
                assertNotEquals(offset, unsafe.staticFieldOffset(fields[j]));
            }
        }

    }

    class Test {
        boolean booleanField;
        byte byteField;
        char charField;
        double doubleField;
        float floatField;
        int intField;
        long longField;
        Object objectField;
        short shortField;
    }

    static class StaticTest {
        static boolean booleanField;
        static byte byteField;
        static char charField;
        static double doubleField;
        static float floatField;
        static int intField;
        static long longField;
        static Object objectField;
        static short shortField;
    }

}
