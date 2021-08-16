/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8143628
 * @summary Test unsafe access for int
 *
 * @modules jdk.unsupported/sun.misc
 * @run testng/othervm -Diters=100   -Xint                   compiler.unsafe.SunMiscUnsafeAccessTestInt
 * @run testng/othervm -Diters=20000 -XX:TieredStopAtLevel=1 compiler.unsafe.SunMiscUnsafeAccessTestInt
 * @run testng/othervm -Diters=20000 -XX:-TieredCompilation  compiler.unsafe.SunMiscUnsafeAccessTestInt
 * @run testng/othervm -Diters=20000                         compiler.unsafe.SunMiscUnsafeAccessTestInt
 */

package compiler.unsafe;

import org.testng.annotations.Test;

import java.lang.reflect.Field;

import static org.testng.Assert.*;

public class SunMiscUnsafeAccessTestInt {
    static final int ITERS = Integer.getInteger("iters", 1);
    static final int WEAK_ATTEMPTS = Integer.getInteger("weakAttempts", 10);

    static final sun.misc.Unsafe UNSAFE;

    static final long V_OFFSET;

    static final Object STATIC_V_BASE;

    static final long STATIC_V_OFFSET;

    static int ARRAY_OFFSET;

    static int ARRAY_SHIFT;

    static {
        try {
            Field f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            UNSAFE = (sun.misc.Unsafe) f.get(null);
        } catch (Exception e) {
            throw new RuntimeException("Unable to get Unsafe instance.", e);
        }

        try {
            Field staticVField = SunMiscUnsafeAccessTestInt.class.getDeclaredField("static_v");
            STATIC_V_BASE = UNSAFE.staticFieldBase(staticVField);
            STATIC_V_OFFSET = UNSAFE.staticFieldOffset(staticVField);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        try {
            Field vField = SunMiscUnsafeAccessTestInt.class.getDeclaredField("v");
            V_OFFSET = UNSAFE.objectFieldOffset(vField);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        ARRAY_OFFSET = UNSAFE.arrayBaseOffset(int[].class);
        int ascale = UNSAFE.arrayIndexScale(int[].class);
        ARRAY_SHIFT = 31 - Integer.numberOfLeadingZeros(ascale);
    }

    static int static_v;

    int v;

    @Test
    public void testFieldInstance() {
        SunMiscUnsafeAccessTestInt t = new SunMiscUnsafeAccessTestInt();
        for (int c = 0; c < ITERS; c++) {
            testAccess(t, V_OFFSET);
        }
    }

    @Test
    public void testFieldStatic() {
        for (int c = 0; c < ITERS; c++) {
            testAccess(STATIC_V_BASE, STATIC_V_OFFSET);
        }
    }

    @Test
    public void testArray() {
        int[] array = new int[10];
        for (int c = 0; c < ITERS; c++) {
            for (int i = 0; i < array.length; i++) {
                testAccess(array, (((long) i) << ARRAY_SHIFT) + ARRAY_OFFSET);
            }
        }
    }

    @Test
    public void testArrayOffHeap() {
        int size = 10;
        long address = UNSAFE.allocateMemory(size << ARRAY_SHIFT);
        try {
            for (int c = 0; c < ITERS; c++) {
                for (int i = 0; i < size; i++) {
                    testAccess(null, (((long) i) << ARRAY_SHIFT) + address);
                }
            }
        } finally {
            UNSAFE.freeMemory(address);
        }
    }

    @Test
    public void testArrayOffHeapDirect() {
        int size = 10;
        long address = UNSAFE.allocateMemory(size << ARRAY_SHIFT);
        try {
            for (int c = 0; c < ITERS; c++) {
                for (int i = 0; i < size; i++) {
                    testAccess((((long) i) << ARRAY_SHIFT) + address);
                }
            }
        } finally {
            UNSAFE.freeMemory(address);
        }
    }

    static void testAccess(Object base, long offset) {
        // Plain
        {
            UNSAFE.putInt(base, offset, 0x01234567);
            int x = UNSAFE.getInt(base, offset);
            assertEquals(x, 0x01234567, "set int value");
        }

        // Volatile
        {
            UNSAFE.putIntVolatile(base, offset, 0x89ABCDEF);
            int x = UNSAFE.getIntVolatile(base, offset);
            assertEquals(x, 0x89ABCDEF, "putVolatile int value");
        }

        // Lazy
        {
            UNSAFE.putOrderedInt(base, offset, 0x01234567);
            int x = UNSAFE.getIntVolatile(base, offset);
            assertEquals(x, 0x01234567, "putRelease int value");
        }



        UNSAFE.putInt(base, offset, 0x01234567);

        // Compare
        {
            boolean r = UNSAFE.compareAndSwapInt(base, offset, 0x01234567, 0x89ABCDEF);
            assertEquals(r, true, "success compareAndSwap int");
            int x = UNSAFE.getInt(base, offset);
            assertEquals(x, 0x89ABCDEF, "success compareAndSwap int value");
        }

        {
            boolean r = UNSAFE.compareAndSwapInt(base, offset, 0x01234567, 0xCAFEBABE);
            assertEquals(r, false, "failing compareAndSwap int");
            int x = UNSAFE.getInt(base, offset);
            assertEquals(x, 0x89ABCDEF, "failing compareAndSwap int value");
        }

        UNSAFE.putInt(base, offset, 0x89ABCDEF);

        // Compare set and get
        {
            int o = UNSAFE.getAndSetInt(base, offset, 0x01234567);
            assertEquals(o, 0x89ABCDEF, "getAndSet int");
            int x = UNSAFE.getInt(base, offset);
            assertEquals(x, 0x01234567, "getAndSet int value");
        }

        UNSAFE.putInt(base, offset, 0x01234567);

        // get and add, add and get
        {
            int o = UNSAFE.getAndAddInt(base, offset, 0x89ABCDEF);
            assertEquals(o, 0x01234567, "getAndAdd int");
            int x = UNSAFE.getInt(base, offset);
            assertEquals(x, (int)(0x01234567 + 0x89ABCDEF), "getAndAdd int");
        }
    }

    static void testAccess(long address) {
        // Plain
        {
            UNSAFE.putInt(address, 0x01234567);
            int x = UNSAFE.getInt(address);
            assertEquals(x, 0x01234567, "set int value");
        }
    }
}
