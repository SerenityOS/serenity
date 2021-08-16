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
 * @summary Test unsafe access for char
 *
 * @modules java.base/jdk.internal.misc:+open
 * @run testng/othervm -Diters=100   -Xint                   compiler.unsafe.JdkInternalMiscUnsafeAccessTestChar
 * @run testng/othervm -Diters=20000 -XX:TieredStopAtLevel=1 compiler.unsafe.JdkInternalMiscUnsafeAccessTestChar
 * @run testng/othervm -Diters=20000 -XX:-TieredCompilation  compiler.unsafe.JdkInternalMiscUnsafeAccessTestChar
 * @run testng/othervm -Diters=20000                         compiler.unsafe.JdkInternalMiscUnsafeAccessTestChar
 */

package compiler.unsafe;

import org.testng.annotations.Test;

import java.lang.reflect.Field;

import static org.testng.Assert.*;

public class JdkInternalMiscUnsafeAccessTestChar {
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
            Field staticVField = JdkInternalMiscUnsafeAccessTestChar.class.getDeclaredField("static_v");
            STATIC_V_BASE = UNSAFE.staticFieldBase(staticVField);
            STATIC_V_OFFSET = UNSAFE.staticFieldOffset(staticVField);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        try {
            Field vField = JdkInternalMiscUnsafeAccessTestChar.class.getDeclaredField("v");
            V_OFFSET = UNSAFE.objectFieldOffset(vField);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        ARRAY_OFFSET = UNSAFE.arrayBaseOffset(char[].class);
        int ascale = UNSAFE.arrayIndexScale(char[].class);
        ARRAY_SHIFT = 31 - Integer.numberOfLeadingZeros(ascale);
    }

    static char static_v;

    char v;

    @Test
    public void testFieldInstance() {
        JdkInternalMiscUnsafeAccessTestChar t = new JdkInternalMiscUnsafeAccessTestChar();
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
        char[] array = new char[10];
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
            UNSAFE.putChar(base, offset, '\u0123');
            char x = UNSAFE.getChar(base, offset);
            assertEquals(x, '\u0123', "set char value");
        }

        // Volatile
        {
            UNSAFE.putCharVolatile(base, offset, '\u4567');
            char x = UNSAFE.getCharVolatile(base, offset);
            assertEquals(x, '\u4567', "putVolatile char value");
        }


        // Lazy
        {
            UNSAFE.putCharRelease(base, offset, '\u0123');
            char x = UNSAFE.getCharAcquire(base, offset);
            assertEquals(x, '\u0123', "putRelease char value");
        }

        // Opaque
        {
            UNSAFE.putCharOpaque(base, offset, '\u4567');
            char x = UNSAFE.getCharOpaque(base, offset);
            assertEquals(x, '\u4567', "putOpaque char value");
        }

        // Unaligned
        {
            UNSAFE.putCharUnaligned(base, offset, '\u4567');
            char x = UNSAFE.getCharUnaligned(base, offset);
            assertEquals(x, '\u4567', "putUnaligned char value");
        }

        {
            UNSAFE.putCharUnaligned(base, offset, '\u0123', true);
            char x = UNSAFE.getCharUnaligned(base, offset, true);
            assertEquals(x, '\u0123', "putUnaligned big endian char value");
        }

        {
            UNSAFE.putCharUnaligned(base, offset, '\u4567', false);
            char x = UNSAFE.getCharUnaligned(base, offset, false);
            assertEquals(x, '\u4567', "putUnaligned little endian char value");
        }

        UNSAFE.putChar(base, offset, '\u0123');

        // Compare
        {
            boolean r = UNSAFE.compareAndSetChar(base, offset, '\u0123', '\u4567');
            assertEquals(r, true, "success compareAndSet char");
            char x = UNSAFE.getChar(base, offset);
            assertEquals(x, '\u4567', "success compareAndSet char value");
        }

        {
            boolean r = UNSAFE.compareAndSetChar(base, offset, '\u0123', '\u89AB');
            assertEquals(r, false, "failing compareAndSet char");
            char x = UNSAFE.getChar(base, offset);
            assertEquals(x, '\u4567', "failing compareAndSet char value");
        }

        // Advanced compare
        {
            char r = UNSAFE.compareAndExchangeChar(base, offset, '\u4567', '\u0123');
            assertEquals(r, '\u4567', "success compareAndExchange char");
            char x = UNSAFE.getChar(base, offset);
            assertEquals(x, '\u0123', "success compareAndExchange char value");
        }

        {
            char r = UNSAFE.compareAndExchangeChar(base, offset, '\u4567', '\u89AB');
            assertEquals(r, '\u0123', "failing compareAndExchange char");
            char x = UNSAFE.getChar(base, offset);
            assertEquals(x, '\u0123', "failing compareAndExchange char value");
        }

        {
            char r = UNSAFE.compareAndExchangeCharAcquire(base, offset, '\u0123', '\u4567');
            assertEquals(r, '\u0123', "success compareAndExchangeAcquire char");
            char x = UNSAFE.getChar(base, offset);
            assertEquals(x, '\u4567', "success compareAndExchangeAcquire char value");
        }

        {
            char r = UNSAFE.compareAndExchangeCharAcquire(base, offset, '\u0123', '\u89AB');
            assertEquals(r, '\u4567', "failing compareAndExchangeAcquire char");
            char x = UNSAFE.getChar(base, offset);
            assertEquals(x, '\u4567', "failing compareAndExchangeAcquire char value");
        }

        {
            char r = UNSAFE.compareAndExchangeCharRelease(base, offset, '\u4567', '\u0123');
            assertEquals(r, '\u4567', "success compareAndExchangeRelease char");
            char x = UNSAFE.getChar(base, offset);
            assertEquals(x, '\u0123', "success compareAndExchangeRelease char value");
        }

        {
            char r = UNSAFE.compareAndExchangeCharRelease(base, offset, '\u4567', '\u89AB');
            assertEquals(r, '\u0123', "failing compareAndExchangeRelease char");
            char x = UNSAFE.getChar(base, offset);
            assertEquals(x, '\u0123', "failing compareAndExchangeRelease char value");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = UNSAFE.weakCompareAndSetCharPlain(base, offset, '\u0123', '\u4567');
            }
            assertEquals(success, true, "weakCompareAndSetPlain char");
            char x = UNSAFE.getChar(base, offset);
            assertEquals(x, '\u4567', "weakCompareAndSetPlain char value");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = UNSAFE.weakCompareAndSetCharAcquire(base, offset, '\u4567', '\u0123');
            }
            assertEquals(success, true, "weakCompareAndSetAcquire char");
            char x = UNSAFE.getChar(base, offset);
            assertEquals(x, '\u0123', "weakCompareAndSetAcquire char");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = UNSAFE.weakCompareAndSetCharRelease(base, offset, '\u0123', '\u4567');
            }
            assertEquals(success, true, "weakCompareAndSetRelease char");
            char x = UNSAFE.getChar(base, offset);
            assertEquals(x, '\u4567', "weakCompareAndSetRelease char");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = UNSAFE.weakCompareAndSetChar(base, offset, '\u4567', '\u0123');
            }
            assertEquals(success, true, "weakCompareAndSet char");
            char x = UNSAFE.getChar(base, offset);
            assertEquals(x, '\u0123', "weakCompareAndSet char");
        }

        UNSAFE.putChar(base, offset, '\u4567');

        // Compare set and get
        {
            char o = UNSAFE.getAndSetChar(base, offset, '\u0123');
            assertEquals(o, '\u4567', "getAndSet char");
            char x = UNSAFE.getChar(base, offset);
            assertEquals(x, '\u0123', "getAndSet char value");
        }

        UNSAFE.putChar(base, offset, '\u0123');

        // get and add, add and get
        {
            char o = UNSAFE.getAndAddChar(base, offset, '\u4567');
            assertEquals(o, '\u0123', "getAndAdd char");
            char x = UNSAFE.getChar(base, offset);
            assertEquals(x, (char)('\u0123' + '\u4567'), "getAndAdd char");
        }
    }

    static void testAccess(long address) {
        // Plain
        {
            UNSAFE.putChar(address, '\u0123');
            char x = UNSAFE.getChar(address);
            assertEquals(x, '\u0123', "set char value");
        }
    }
}
