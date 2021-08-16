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
 * @summary Test unsafe access for float
 *
 * @modules java.base/jdk.internal.misc:+open
 * @run testng/othervm -Diters=100   -Xint                   compiler.unsafe.JdkInternalMiscUnsafeAccessTestFloat
 * @run testng/othervm -Diters=20000 -XX:TieredStopAtLevel=1 compiler.unsafe.JdkInternalMiscUnsafeAccessTestFloat
 * @run testng/othervm -Diters=20000 -XX:-TieredCompilation  compiler.unsafe.JdkInternalMiscUnsafeAccessTestFloat
 * @run testng/othervm -Diters=20000                         compiler.unsafe.JdkInternalMiscUnsafeAccessTestFloat
 */

package compiler.unsafe;

import org.testng.annotations.Test;

import java.lang.reflect.Field;

import static org.testng.Assert.*;

public class JdkInternalMiscUnsafeAccessTestFloat {
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
            Field staticVField = JdkInternalMiscUnsafeAccessTestFloat.class.getDeclaredField("static_v");
            STATIC_V_BASE = UNSAFE.staticFieldBase(staticVField);
            STATIC_V_OFFSET = UNSAFE.staticFieldOffset(staticVField);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        try {
            Field vField = JdkInternalMiscUnsafeAccessTestFloat.class.getDeclaredField("v");
            V_OFFSET = UNSAFE.objectFieldOffset(vField);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        ARRAY_OFFSET = UNSAFE.arrayBaseOffset(float[].class);
        int ascale = UNSAFE.arrayIndexScale(float[].class);
        ARRAY_SHIFT = 31 - Integer.numberOfLeadingZeros(ascale);
    }

    static float static_v;

    float v;

    @Test
    public void testFieldInstance() {
        JdkInternalMiscUnsafeAccessTestFloat t = new JdkInternalMiscUnsafeAccessTestFloat();
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
        float[] array = new float[10];
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
            UNSAFE.putFloat(base, offset, 1.0f);
            float x = UNSAFE.getFloat(base, offset);
            assertEquals(x, 1.0f, "set float value");
        }

        // Volatile
        {
            UNSAFE.putFloatVolatile(base, offset, 2.0f);
            float x = UNSAFE.getFloatVolatile(base, offset);
            assertEquals(x, 2.0f, "putVolatile float value");
        }


        // Lazy
        {
            UNSAFE.putFloatRelease(base, offset, 1.0f);
            float x = UNSAFE.getFloatAcquire(base, offset);
            assertEquals(x, 1.0f, "putRelease float value");
        }

        // Opaque
        {
            UNSAFE.putFloatOpaque(base, offset, 2.0f);
            float x = UNSAFE.getFloatOpaque(base, offset);
            assertEquals(x, 2.0f, "putOpaque float value");
        }


        UNSAFE.putFloat(base, offset, 1.0f);

        // Compare
        {
            boolean r = UNSAFE.compareAndSetFloat(base, offset, 1.0f, 2.0f);
            assertEquals(r, true, "success compareAndSet float");
            float x = UNSAFE.getFloat(base, offset);
            assertEquals(x, 2.0f, "success compareAndSet float value");
        }

        {
            boolean r = UNSAFE.compareAndSetFloat(base, offset, 1.0f, 3.0f);
            assertEquals(r, false, "failing compareAndSet float");
            float x = UNSAFE.getFloat(base, offset);
            assertEquals(x, 2.0f, "failing compareAndSet float value");
        }

        // Advanced compare
        {
            float r = UNSAFE.compareAndExchangeFloat(base, offset, 2.0f, 1.0f);
            assertEquals(r, 2.0f, "success compareAndExchange float");
            float x = UNSAFE.getFloat(base, offset);
            assertEquals(x, 1.0f, "success compareAndExchange float value");
        }

        {
            float r = UNSAFE.compareAndExchangeFloat(base, offset, 2.0f, 3.0f);
            assertEquals(r, 1.0f, "failing compareAndExchange float");
            float x = UNSAFE.getFloat(base, offset);
            assertEquals(x, 1.0f, "failing compareAndExchange float value");
        }

        {
            float r = UNSAFE.compareAndExchangeFloatAcquire(base, offset, 1.0f, 2.0f);
            assertEquals(r, 1.0f, "success compareAndExchangeAcquire float");
            float x = UNSAFE.getFloat(base, offset);
            assertEquals(x, 2.0f, "success compareAndExchangeAcquire float value");
        }

        {
            float r = UNSAFE.compareAndExchangeFloatAcquire(base, offset, 1.0f, 3.0f);
            assertEquals(r, 2.0f, "failing compareAndExchangeAcquire float");
            float x = UNSAFE.getFloat(base, offset);
            assertEquals(x, 2.0f, "failing compareAndExchangeAcquire float value");
        }

        {
            float r = UNSAFE.compareAndExchangeFloatRelease(base, offset, 2.0f, 1.0f);
            assertEquals(r, 2.0f, "success compareAndExchangeRelease float");
            float x = UNSAFE.getFloat(base, offset);
            assertEquals(x, 1.0f, "success compareAndExchangeRelease float value");
        }

        {
            float r = UNSAFE.compareAndExchangeFloatRelease(base, offset, 2.0f, 3.0f);
            assertEquals(r, 1.0f, "failing compareAndExchangeRelease float");
            float x = UNSAFE.getFloat(base, offset);
            assertEquals(x, 1.0f, "failing compareAndExchangeRelease float value");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = UNSAFE.weakCompareAndSetFloatPlain(base, offset, 1.0f, 2.0f);
            }
            assertEquals(success, true, "weakCompareAndSetPlain float");
            float x = UNSAFE.getFloat(base, offset);
            assertEquals(x, 2.0f, "weakCompareAndSetPlain float value");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = UNSAFE.weakCompareAndSetFloatAcquire(base, offset, 2.0f, 1.0f);
            }
            assertEquals(success, true, "weakCompareAndSetAcquire float");
            float x = UNSAFE.getFloat(base, offset);
            assertEquals(x, 1.0f, "weakCompareAndSetAcquire float");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = UNSAFE.weakCompareAndSetFloatRelease(base, offset, 1.0f, 2.0f);
            }
            assertEquals(success, true, "weakCompareAndSetRelease float");
            float x = UNSAFE.getFloat(base, offset);
            assertEquals(x, 2.0f, "weakCompareAndSetRelease float");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = UNSAFE.weakCompareAndSetFloat(base, offset, 2.0f, 1.0f);
            }
            assertEquals(success, true, "weakCompareAndSet float");
            float x = UNSAFE.getFloat(base, offset);
            assertEquals(x, 1.0f, "weakCompareAndSet float");
        }

        UNSAFE.putFloat(base, offset, 2.0f);

        // Compare set and get
        {
            float o = UNSAFE.getAndSetFloat(base, offset, 1.0f);
            assertEquals(o, 2.0f, "getAndSet float");
            float x = UNSAFE.getFloat(base, offset);
            assertEquals(x, 1.0f, "getAndSet float value");
        }

        UNSAFE.putFloat(base, offset, 1.0f);

        // get and add, add and get
        {
            float o = UNSAFE.getAndAddFloat(base, offset, 2.0f);
            assertEquals(o, 1.0f, "getAndAdd float");
            float x = UNSAFE.getFloat(base, offset);
            assertEquals(x, (float)(1.0f + 2.0f), "getAndAdd float");
        }
    }

    static void testAccess(long address) {
        // Plain
        {
            UNSAFE.putFloat(address, 1.0f);
            float x = UNSAFE.getFloat(address);
            assertEquals(x, 1.0f, "set float value");
        }
    }
}
