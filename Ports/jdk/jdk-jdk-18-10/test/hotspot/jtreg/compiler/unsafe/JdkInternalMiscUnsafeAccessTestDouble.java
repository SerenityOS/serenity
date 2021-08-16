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
 * @summary Test unsafe access for double
 *
 * @modules java.base/jdk.internal.misc:+open
 * @run testng/othervm -Diters=100   -Xint                   compiler.unsafe.JdkInternalMiscUnsafeAccessTestDouble
 * @run testng/othervm -Diters=20000 -XX:TieredStopAtLevel=1 compiler.unsafe.JdkInternalMiscUnsafeAccessTestDouble
 * @run testng/othervm -Diters=20000 -XX:-TieredCompilation  compiler.unsafe.JdkInternalMiscUnsafeAccessTestDouble
 * @run testng/othervm -Diters=20000                         compiler.unsafe.JdkInternalMiscUnsafeAccessTestDouble
 */

package compiler.unsafe;

import org.testng.annotations.Test;

import java.lang.reflect.Field;

import static org.testng.Assert.*;

public class JdkInternalMiscUnsafeAccessTestDouble {
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
            Field staticVField = JdkInternalMiscUnsafeAccessTestDouble.class.getDeclaredField("static_v");
            STATIC_V_BASE = UNSAFE.staticFieldBase(staticVField);
            STATIC_V_OFFSET = UNSAFE.staticFieldOffset(staticVField);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        try {
            Field vField = JdkInternalMiscUnsafeAccessTestDouble.class.getDeclaredField("v");
            V_OFFSET = UNSAFE.objectFieldOffset(vField);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        ARRAY_OFFSET = UNSAFE.arrayBaseOffset(double[].class);
        int ascale = UNSAFE.arrayIndexScale(double[].class);
        ARRAY_SHIFT = 31 - Integer.numberOfLeadingZeros(ascale);
    }

    static double static_v;

    double v;

    @Test
    public void testFieldInstance() {
        JdkInternalMiscUnsafeAccessTestDouble t = new JdkInternalMiscUnsafeAccessTestDouble();
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
        double[] array = new double[10];
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
            UNSAFE.putDouble(base, offset, 1.0d);
            double x = UNSAFE.getDouble(base, offset);
            assertEquals(x, 1.0d, "set double value");
        }

        // Volatile
        {
            UNSAFE.putDoubleVolatile(base, offset, 2.0d);
            double x = UNSAFE.getDoubleVolatile(base, offset);
            assertEquals(x, 2.0d, "putVolatile double value");
        }


        // Lazy
        {
            UNSAFE.putDoubleRelease(base, offset, 1.0d);
            double x = UNSAFE.getDoubleAcquire(base, offset);
            assertEquals(x, 1.0d, "putRelease double value");
        }

        // Opaque
        {
            UNSAFE.putDoubleOpaque(base, offset, 2.0d);
            double x = UNSAFE.getDoubleOpaque(base, offset);
            assertEquals(x, 2.0d, "putOpaque double value");
        }


        UNSAFE.putDouble(base, offset, 1.0d);

        // Compare
        {
            boolean r = UNSAFE.compareAndSetDouble(base, offset, 1.0d, 2.0d);
            assertEquals(r, true, "success compareAndSet double");
            double x = UNSAFE.getDouble(base, offset);
            assertEquals(x, 2.0d, "success compareAndSet double value");
        }

        {
            boolean r = UNSAFE.compareAndSetDouble(base, offset, 1.0d, 3.0d);
            assertEquals(r, false, "failing compareAndSet double");
            double x = UNSAFE.getDouble(base, offset);
            assertEquals(x, 2.0d, "failing compareAndSet double value");
        }

        // Advanced compare
        {
            double r = UNSAFE.compareAndExchangeDouble(base, offset, 2.0d, 1.0d);
            assertEquals(r, 2.0d, "success compareAndExchange double");
            double x = UNSAFE.getDouble(base, offset);
            assertEquals(x, 1.0d, "success compareAndExchange double value");
        }

        {
            double r = UNSAFE.compareAndExchangeDouble(base, offset, 2.0d, 3.0d);
            assertEquals(r, 1.0d, "failing compareAndExchange double");
            double x = UNSAFE.getDouble(base, offset);
            assertEquals(x, 1.0d, "failing compareAndExchange double value");
        }

        {
            double r = UNSAFE.compareAndExchangeDoubleAcquire(base, offset, 1.0d, 2.0d);
            assertEquals(r, 1.0d, "success compareAndExchangeAcquire double");
            double x = UNSAFE.getDouble(base, offset);
            assertEquals(x, 2.0d, "success compareAndExchangeAcquire double value");
        }

        {
            double r = UNSAFE.compareAndExchangeDoubleAcquire(base, offset, 1.0d, 3.0d);
            assertEquals(r, 2.0d, "failing compareAndExchangeAcquire double");
            double x = UNSAFE.getDouble(base, offset);
            assertEquals(x, 2.0d, "failing compareAndExchangeAcquire double value");
        }

        {
            double r = UNSAFE.compareAndExchangeDoubleRelease(base, offset, 2.0d, 1.0d);
            assertEquals(r, 2.0d, "success compareAndExchangeRelease double");
            double x = UNSAFE.getDouble(base, offset);
            assertEquals(x, 1.0d, "success compareAndExchangeRelease double value");
        }

        {
            double r = UNSAFE.compareAndExchangeDoubleRelease(base, offset, 2.0d, 3.0d);
            assertEquals(r, 1.0d, "failing compareAndExchangeRelease double");
            double x = UNSAFE.getDouble(base, offset);
            assertEquals(x, 1.0d, "failing compareAndExchangeRelease double value");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = UNSAFE.weakCompareAndSetDoublePlain(base, offset, 1.0d, 2.0d);
            }
            assertEquals(success, true, "weakCompareAndSetPlain double");
            double x = UNSAFE.getDouble(base, offset);
            assertEquals(x, 2.0d, "weakCompareAndSetPlain double value");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = UNSAFE.weakCompareAndSetDoubleAcquire(base, offset, 2.0d, 1.0d);
            }
            assertEquals(success, true, "weakCompareAndSetAcquire double");
            double x = UNSAFE.getDouble(base, offset);
            assertEquals(x, 1.0d, "weakCompareAndSetAcquire double");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = UNSAFE.weakCompareAndSetDoubleRelease(base, offset, 1.0d, 2.0d);
            }
            assertEquals(success, true, "weakCompareAndSetRelease double");
            double x = UNSAFE.getDouble(base, offset);
            assertEquals(x, 2.0d, "weakCompareAndSetRelease double");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = UNSAFE.weakCompareAndSetDouble(base, offset, 2.0d, 1.0d);
            }
            assertEquals(success, true, "weakCompareAndSet double");
            double x = UNSAFE.getDouble(base, offset);
            assertEquals(x, 1.0d, "weakCompareAndSet double");
        }

        UNSAFE.putDouble(base, offset, 2.0d);

        // Compare set and get
        {
            double o = UNSAFE.getAndSetDouble(base, offset, 1.0d);
            assertEquals(o, 2.0d, "getAndSet double");
            double x = UNSAFE.getDouble(base, offset);
            assertEquals(x, 1.0d, "getAndSet double value");
        }

        UNSAFE.putDouble(base, offset, 1.0d);

        // get and add, add and get
        {
            double o = UNSAFE.getAndAddDouble(base, offset, 2.0d);
            assertEquals(o, 1.0d, "getAndAdd double");
            double x = UNSAFE.getDouble(base, offset);
            assertEquals(x, (double)(1.0d + 2.0d), "getAndAdd double");
        }
    }

    static void testAccess(long address) {
        // Plain
        {
            UNSAFE.putDouble(address, 1.0d);
            double x = UNSAFE.getDouble(address);
            assertEquals(x, 1.0d, "set double value");
        }
    }
}
