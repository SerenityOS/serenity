/*
 * Copyright (C) 2020 THL A29 Limited, a Tencent company. All rights reserved.
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

/**
 * @test
 * @bug 8250825
 * @summary "assert(field != __null) failed: missing field" in TypeOopPtr::TypeOopPt(...) with misaligned unsafe accesses
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 * @run main/othervm -XX:-TieredCompilation -Xcomp
 *                   -XX:CompileCommand=compileonly,TestMisalignedUnsafeAccess::test* TestMisalignedUnsafeAccess
 */

import java.lang.reflect.Field;
import jdk.internal.misc.Unsafe;
import jdk.test.lib.Asserts;

public class TestMisalignedUnsafeAccess {

    private static final Unsafe UNSAFE = Unsafe.getUnsafe();;

    private static short onHeapStaticMemory; // For static field testing
    private static final Object onHeapStaticMemoryBase;
    private static final long onHeapStaticMemoryOffset;

    private short onHeapInstanceMemory; // For instance field testing
    private static final long onHeapInstanceMemoryOffset;

    static {
        try {
            Field staticField = TestMisalignedUnsafeAccess.class.getDeclaredField("onHeapStaticMemory");
            onHeapStaticMemoryBase = UNSAFE.staticFieldBase(staticField);
            onHeapStaticMemoryOffset = UNSAFE.staticFieldOffset(staticField);

            Field instanceField = TestMisalignedUnsafeAccess.class.getDeclaredField("onHeapInstanceMemory");
            onHeapInstanceMemoryOffset = UNSAFE.objectFieldOffset(instanceField);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void testStaticField() {
        byte b1 = 0x01;
        byte b2 = 0x02;

        UNSAFE.putByte(onHeapStaticMemoryBase, onHeapStaticMemoryOffset, b1);
        UNSAFE.putByte(onHeapStaticMemoryBase, onHeapStaticMemoryOffset + 1, b2);

        Asserts.assertEquals(b1, UNSAFE.getByte(onHeapStaticMemoryBase, onHeapStaticMemoryOffset));
        Asserts.assertEquals(b2, UNSAFE.getByte(onHeapStaticMemoryBase, onHeapStaticMemoryOffset + 1));
    }

    public static void testInstanceField() {
        byte b1 = 0x03;
        byte b2 = 0x04;
        TestMisalignedUnsafeAccess obj = new TestMisalignedUnsafeAccess();

        UNSAFE.putByte(obj, onHeapInstanceMemoryOffset, b1);
        UNSAFE.putByte(obj, onHeapInstanceMemoryOffset + 1, b2);

        Asserts.assertEquals(b1, UNSAFE.getByte(obj, onHeapInstanceMemoryOffset));
        Asserts.assertEquals(b2, UNSAFE.getByte(obj, onHeapInstanceMemoryOffset + 1));
    }

    public static void main(String[] args) {
        testStaticField();
        testInstanceField();
    }
}
