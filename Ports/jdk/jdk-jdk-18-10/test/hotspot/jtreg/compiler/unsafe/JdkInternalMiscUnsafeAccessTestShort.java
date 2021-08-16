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
 * @summary Test unsafe access for short
 *
 * @modules java.base/jdk.internal.misc:+open
 * @run testng/othervm -Diters=100   -Xint                   compiler.unsafe.JdkInternalMiscUnsafeAccessTestShort
 * @run testng/othervm -Diters=20000 -XX:TieredStopAtLevel=1 compiler.unsafe.JdkInternalMiscUnsafeAccessTestShort
 * @run testng/othervm -Diters=20000 -XX:-TieredCompilation  compiler.unsafe.JdkInternalMiscUnsafeAccessTestShort
 * @run testng/othervm -Diters=20000                         compiler.unsafe.JdkInternalMiscUnsafeAccessTestShort
 */

package compiler.unsafe;

import org.testng.annotations.Test;

import java.lang.reflect.Field;

import static org.testng.Assert.*;

public class JdkInternalMiscUnsafeAccessTestShort {
    static final int ITERS = Integer.getInteger("iters", 1);
    static final int WEAK_ATTEMPTS = Integer.getInteger("weakAttempts", 10);

    static final jdk.internal.misc.Unsafe UNSAFE;

    static final long V_OFFSET;

    static final Object STATIC_V_BASE;

    static final long STATIC_V_OFFSET;

    static int ARRAY_OFFSET;

    static int ARRAY_SHIFT;

    static {
        try {
            Field f = jdk.internal.misc.Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            UNSAFE = (jdk.internal.misc.Unsafe) f.get(null);
        } catch (Exception e) {
            throw new RuntimeException("Unable to get Unsafe instance.", e);
        }

        try {
            Field staticVField = JdkInternalMiscUnsafeAccessTestShort.class.getDeclaredField("static_v");
            STATIC_V_BASE = UNSAFE.staticFieldBase(staticVField);
            STATIC_V_OFFSET = UNSAFE.staticFieldOffset(staticVField);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        try {
            Field vField = JdkInternalMiscUnsafeAccessTestShort.class.getDeclaredField("v");
            V_OFFSET = UNSAFE.objectFieldOffset(vField);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        ARRAY_OFFSET = UNSAFE.arrayBaseOffset(short[].class);
        int ascale = UNSAFE.arrayIndexScale(short[].class);
        ARRAY_SHIFT = 31 - Integer.numberOfLeadingZeros(ascale);
    }

    static short static_v;

    short v;

    @Test
    public void testFieldInstance() {
        JdkInternalMiscUnsafeAccessTestShort t = new JdkInternalMiscUnsafeAccessTestShort();
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
        short[] array = new short[10];
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
            UNSAFE.putShort(base, offset, (short)0x0123);
            short x = UNSAFE.getShort(base, offset);
            assertEquals(x, (short)0x0123, "set short value");
        }

        // Volatile
        {
            UNSAFE.putShortVolatile(base, offset, (short)0x4567);
            short x = UNSAFE.getShortVolatile(base, offset);
            assertEquals(x, (short)0x4567, "putVolatile short value");
        }


        // Lazy
        {
            UNSAFE.putShortRelease(base, offset, (short)0x0123);
            short x = UNSAFE.getShortAcquire(base, offset);
            assertEquals(x, (short)0x0123, "putRelease short value");
        }

        // Opaque
        {
            UNSAFE.putShortOpaque(base, offset, (short)0x4567);
            short x = UNSAFE.getShortOpaque(base, offset);
            assertEquals(x, (short)0x4567, "putOpaque short value");
        }

        // Unaligned
        {
            UNSAFE.putShortUnaligned(base, offset, (short)0x4567);
            short x = UNSAFE.getShortUnaligned(base, offset);
            assertEquals(x, (short)0x4567, "putUnaligned short value");
        }

        {
            UNSAFE.putShortUnaligned(base, offset, (short)0x0123, true);
            short x = UNSAFE.getShortUnaligned(base, offset, true);
            assertEquals(x, (short)0x0123, "putUnaligned big endian short value");
        }

        {
            UNSAFE.putShortUnaligned(base, offset, (short)0x4567, false);
            short x = UNSAFE.getShortUnaligned(base, offset, false);
            assertEquals(x, (short)0x4567, "putUnaligned little endian short value");
        }

        UNSAFE.putShort(base, offset, (short)0x0123);

        // Compare
        {
            boolean r = UNSAFE.compareAndSetShort(base, offset, (short)0x0123, (short)0x4567);
            assertEquals(r, true, "success compareAndSet short");
            short x = UNSAFE.getShort(base, offset);
            assertEquals(x, (short)0x4567, "success compareAndSet short value");
        }

        {
            boolean r = UNSAFE.compareAndSetShort(base, offset, (short)0x0123, (short)0x89AB);
            assertEquals(r, false, "failing compareAndSet short");
            short x = UNSAFE.getShort(base, offset);
            assertEquals(x, (short)0x4567, "failing compareAndSet short value");
        }

        // Advanced compare
        {
            short r = UNSAFE.compareAndExchangeShort(base, offset, (short)0x4567, (short)0x0123);
            assertEquals(r, (short)0x4567, "success compareAndExchange short");
            short x = UNSAFE.getShort(base, offset);
            assertEquals(x, (short)0x0123, "success compareAndExchange short value");
        }

        {
            short r = UNSAFE.compareAndExchangeShort(base, offset, (short)0x4567, (short)0x89AB);
            assertEquals(r, (short)0x0123, "failing compareAndExchange short");
            short x = UNSAFE.getShort(base, offset);
            assertEquals(x, (short)0x0123, "failing compareAndExchange short value");
        }

        {
            short r = UNSAFE.compareAndExchangeShortAcquire(base, offset, (short)0x0123, (short)0x4567);
            assertEquals(r, (short)0x0123, "success compareAndExchangeAcquire short");
            short x = UNSAFE.getShort(base, offset);
            assertEquals(x, (short)0x4567, "success compareAndExchangeAcquire short value");
        }

        {
            short r = UNSAFE.compareAndExchangeShortAcquire(base, offset, (short)0x0123, (short)0x89AB);
            assertEquals(r, (short)0x4567, "failing compareAndExchangeAcquire short");
            short x = UNSAFE.getShort(base, offset);
            assertEquals(x, (short)0x4567, "failing compareAndExchangeAcquire short value");
        }

        {
            short r = UNSAFE.compareAndExchangeShortRelease(base, offset, (short)0x4567, (short)0x0123);
            assertEquals(r, (short)0x4567, "success compareAndExchangeRelease short");
            short x = UNSAFE.getShort(base, offset);
            assertEquals(x, (short)0x0123, "success compareAndExchangeRelease short value");
        }

        {
            short r = UNSAFE.compareAndExchangeShortRelease(base, offset, (short)0x4567, (short)0x89AB);
            assertEquals(r, (short)0x0123, "failing compareAndExchangeRelease short");
            short x = UNSAFE.getShort(base, offset);
            assertEquals(x, (short)0x0123, "failing compareAndExchangeRelease short value");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = UNSAFE.weakCompareAndSetShortPlain(base, offset, (short)0x0123, (short)0x4567);
            }
            assertEquals(success, true, "weakCompareAndSetPlain short");
            short x = UNSAFE.getShort(base, offset);
            assertEquals(x, (short)0x4567, "weakCompareAndSetPlain short value");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = UNSAFE.weakCompareAndSetShortAcquire(base, offset, (short)0x4567, (short)0x0123);
            }
            assertEquals(success, true, "weakCompareAndSetAcquire short");
            short x = UNSAFE.getShort(base, offset);
            assertEquals(x, (short)0x0123, "weakCompareAndSetAcquire short");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = UNSAFE.weakCompareAndSetShortRelease(base, offset, (short)0x0123, (short)0x4567);
            }
            assertEquals(success, true, "weakCompareAndSetRelease short");
            short x = UNSAFE.getShort(base, offset);
            assertEquals(x, (short)0x4567, "weakCompareAndSetRelease short");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = UNSAFE.weakCompareAndSetShort(base, offset, (short)0x4567, (short)0x0123);
            }
            assertEquals(success, true, "weakCompareAndSet short");
            short x = UNSAFE.getShort(base, offset);
            assertEquals(x, (short)0x0123, "weakCompareAndSet short");
        }

        UNSAFE.putShort(base, offset, (short)0x4567);

        // Compare set and get
        {
            short o = UNSAFE.getAndSetShort(base, offset, (short)0x0123);
            assertEquals(o, (short)0x4567, "getAndSet short");
            short x = UNSAFE.getShort(base, offset);
            assertEquals(x, (short)0x0123, "getAndSet short value");
        }

        UNSAFE.putShort(base, offset, (short)0x0123);

        // get and add, add and get
        {
            short o = UNSAFE.getAndAddShort(base, offset, (short)0x4567);
            assertEquals(o, (short)0x0123, "getAndAdd short");
            short x = UNSAFE.getShort(base, offset);
            assertEquals(x, (short)((short)0x0123 + (short)0x4567), "getAndAdd short");
        }
    }

    static void testAccess(long address) {
        // Plain
        {
            UNSAFE.putShort(address, (short)0x0123);
            short x = UNSAFE.getShort(address);
            assertEquals(x, (short)0x0123, "set short value");
        }
    }
}
