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
 * @bug 8237767
 * @summary Verify behaviour of field layout algorithm
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @run main/othervm FieldDensityTest
 */

/*
 * @test
 * @requires vm.bits == "64"
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @run main/othervm -XX:+UseCompressedOops -XX:+UseCompressedClassPointers FieldDensityTest
 * @run main/othervm -XX:+UseCompressedOops -XX:-UseCompressedClassPointers FieldDensityTest
 */

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Comparator;
import jdk.internal.misc.Unsafe;

import jdk.test.lib.Asserts;

public class FieldDensityTest {

    static int OOP_SIZE_IN_BYTES = 0;

    static {
        if (System.getProperty("sun.arch.data.model").equals("64")) {
            if (System.getProperty("java.vm.compressedOopsMode") == null) {
                OOP_SIZE_IN_BYTES = 8;
            } else {
                OOP_SIZE_IN_BYTES = 4;
            }
        } else {
            OOP_SIZE_IN_BYTES = 4;
        }
    }

    static class FieldInfo {
        public Field field;
        public long offset;

        FieldInfo(Field field, long offset) {
            this.field = field;
            this.offset = offset;
        }

        static void checkFieldsContiguity(FieldInfo[] fieldInfo) {
            Arrays.sort(fieldInfo, new SortByOffset());
            for (int i = 0 ; i <  fieldInfo.length - 2; i++) {
                int size = sizeInBytesFromType(fieldInfo[i].field.getType());
                Asserts.assertEquals((int)(fieldInfo[i].offset + size), (int)fieldInfo[i+1].offset,
                                     "Empty slot between fields, should not happen");
            }
        }
    }

    static int sizeInBytesFromType(Class type) {
        if (!type.isPrimitive()) {
            return OOP_SIZE_IN_BYTES;
        }
        switch(type.getTypeName()) {
        case "boolean":
        case "byte": return 1;
        case "char":
        case "short": return 2;
        case "int":
        case "float": return 4;
        case "long":
        case "double": return 8;
        default:
            throw new RuntimeException("Unrecognized signature");
        }
    }

    static class SortByOffset implements Comparator<FieldInfo> {
        public int compare(FieldInfo a, FieldInfo b)
        {
            return (int)(a.offset - b.offset);
        }
    }

    static class E {
        public byte b0;
    }

    static class F extends E {
        public byte b1;
    }

    static class G extends F {
        public byte b2;
    }

    static class H extends G {
        public byte b3;
    }

    public static class A {
        public int i;
        public byte b;
        public long l;
        public Object o;
    }

    public static class B extends A {
        public byte b0, b1, b2;
    }

    static void testFieldsContiguity(Class c) {
        Unsafe unsafe = Unsafe.getUnsafe();
        Field[] fields = c.getFields();
        FieldInfo[] fieldsInfo = new FieldInfo[fields.length];
        int i = 0;
        for (Field f : fields) {
            long offset = unsafe.objectFieldOffset(f);
            fieldsInfo[i] = new FieldInfo(f, offset);
            i++;
        }
        FieldInfo.checkFieldsContiguity(fieldsInfo);
    }

    public static void main(String[] args) {
        H h = new H();
        testFieldsContiguity(h.getClass());
        B b = new B();
        testFieldsContiguity(b.getClass());
    }
}
