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
 * @summary Test unsafe access for boolean
 *
 * @modules java.base/jdk.internal.misc:+open
 * @run testng/othervm -Diters=100   -Xint                   compiler.unsafe.JdkInternalMiscUnsafeAccessTestBoolean
 * @run testng/othervm -Diters=20000 -XX:TieredStopAtLevel=1 compiler.unsafe.JdkInternalMiscUnsafeAccessTestBoolean
 * @run testng/othervm -Diters=20000 -XX:-TieredCompilation  compiler.unsafe.JdkInternalMiscUnsafeAccessTestBoolean
 * @run testng/othervm -Diters=20000                         compiler.unsafe.JdkInternalMiscUnsafeAccessTestBoolean
 */

package compiler.unsafe;

import org.testng.annotations.Test;

import java.lang.reflect.Field;

import static org.testng.Assert.*;

public class JdkInternalMiscUnsafeAccessTestBoolean {
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
            Field staticVField = JdkInternalMiscUnsafeAccessTestBoolean.class.getDeclaredField("static_v");
            STATIC_V_BASE = UNSAFE.staticFieldBase(staticVField);
            STATIC_V_OFFSET = UNSAFE.staticFieldOffset(staticVField);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        try {
            Field vField = JdkInternalMiscUnsafeAccessTestBoolean.class.getDeclaredField("v");
            V_OFFSET = UNSAFE.objectFieldOffset(vField);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        ARRAY_OFFSET = UNSAFE.arrayBaseOffset(boolean[].class);
        int ascale = UNSAFE.arrayIndexScale(boolean[].class);
        ARRAY_SHIFT = 31 - Integer.numberOfLeadingZeros(ascale);
    }

    static boolean static_v;

    boolean v;

    @Test
    public void testFieldInstance() {
        JdkInternalMiscUnsafeAccessTestBoolean t = new JdkInternalMiscUnsafeAccessTestBoolean();
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
        boolean[] array = new boolean[10];
        for (int c = 0; c < ITERS; c++) {
            for (int i = 0; i < array.length; i++) {
                testAccess(array, (((long) i) << ARRAY_SHIFT) + ARRAY_OFFSET);
            }
        }
    }


    static void testAccess(Object base, long offset) {
        // Plain
        {
            UNSAFE.putBoolean(base, offset, true);
            boolean x = UNSAFE.getBoolean(base, offset);
            assertEquals(x, true, "set boolean value");
        }

        // Volatile
        {
            UNSAFE.putBooleanVolatile(base, offset, false);
            boolean x = UNSAFE.getBooleanVolatile(base, offset);
            assertEquals(x, false, "putVolatile boolean value");
        }


        // Lazy
        {
            UNSAFE.putBooleanRelease(base, offset, true);
            boolean x = UNSAFE.getBooleanAcquire(base, offset);
            assertEquals(x, true, "putRelease boolean value");
        }

        // Opaque
        {
            UNSAFE.putBooleanOpaque(base, offset, false);
            boolean x = UNSAFE.getBooleanOpaque(base, offset);
            assertEquals(x, false, "putOpaque boolean value");
        }


        UNSAFE.putBoolean(base, offset, true);

        // Compare
        {
            boolean r = UNSAFE.compareAndSetBoolean(base, offset, true, false);
            assertEquals(r, true, "success compareAndSet boolean");
            boolean x = UNSAFE.getBoolean(base, offset);
            assertEquals(x, false, "success compareAndSet boolean value");
        }

        {
            boolean r = UNSAFE.compareAndSetBoolean(base, offset, true, false);
            assertEquals(r, false, "failing compareAndSet boolean");
            boolean x = UNSAFE.getBoolean(base, offset);
            assertEquals(x, false, "failing compareAndSet boolean value");
        }

        // Advanced compare
        {
            boolean r = UNSAFE.compareAndExchangeBoolean(base, offset, false, true);
            assertEquals(r, false, "success compareAndExchange boolean");
            boolean x = UNSAFE.getBoolean(base, offset);
            assertEquals(x, true, "success compareAndExchange boolean value");
        }

        {
            boolean r = UNSAFE.compareAndExchangeBoolean(base, offset, false, false);
            assertEquals(r, true, "failing compareAndExchange boolean");
            boolean x = UNSAFE.getBoolean(base, offset);
            assertEquals(x, true, "failing compareAndExchange boolean value");
        }

        {
            boolean r = UNSAFE.compareAndExchangeBooleanAcquire(base, offset, true, false);
            assertEquals(r, true, "success compareAndExchangeAcquire boolean");
            boolean x = UNSAFE.getBoolean(base, offset);
            assertEquals(x, false, "success compareAndExchangeAcquire boolean value");
        }

        {
            boolean r = UNSAFE.compareAndExchangeBooleanAcquire(base, offset, true, false);
            assertEquals(r, false, "failing compareAndExchangeAcquire boolean");
            boolean x = UNSAFE.getBoolean(base, offset);
            assertEquals(x, false, "failing compareAndExchangeAcquire boolean value");
        }

        {
            boolean r = UNSAFE.compareAndExchangeBooleanRelease(base, offset, false, true);
            assertEquals(r, false, "success compareAndExchangeRelease boolean");
            boolean x = UNSAFE.getBoolean(base, offset);
            assertEquals(x, true, "success compareAndExchangeRelease boolean value");
        }

        {
            boolean r = UNSAFE.compareAndExchangeBooleanRelease(base, offset, false, false);
            assertEquals(r, true, "failing compareAndExchangeRelease boolean");
            boolean x = UNSAFE.getBoolean(base, offset);
            assertEquals(x, true, "failing compareAndExchangeRelease boolean value");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = UNSAFE.weakCompareAndSetBooleanPlain(base, offset, true, false);
            }
            assertEquals(success, true, "weakCompareAndSetPlain boolean");
            boolean x = UNSAFE.getBoolean(base, offset);
            assertEquals(x, false, "weakCompareAndSetPlain boolean value");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = UNSAFE.weakCompareAndSetBooleanAcquire(base, offset, false, true);
            }
            assertEquals(success, true, "weakCompareAndSetAcquire boolean");
            boolean x = UNSAFE.getBoolean(base, offset);
            assertEquals(x, true, "weakCompareAndSetAcquire boolean");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = UNSAFE.weakCompareAndSetBooleanRelease(base, offset, true, false);
            }
            assertEquals(success, true, "weakCompareAndSetRelease boolean");
            boolean x = UNSAFE.getBoolean(base, offset);
            assertEquals(x, false, "weakCompareAndSetRelease boolean");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = UNSAFE.weakCompareAndSetBoolean(base, offset, false, true);
            }
            assertEquals(success, true, "weakCompareAndSet boolean");
            boolean x = UNSAFE.getBoolean(base, offset);
            assertEquals(x, true, "weakCompareAndSet boolean");
        }

        UNSAFE.putBoolean(base, offset, false);

        // Compare set and get
        {
            boolean o = UNSAFE.getAndSetBoolean(base, offset, true);
            assertEquals(o, false, "getAndSet boolean");
            boolean x = UNSAFE.getBoolean(base, offset);
            assertEquals(x, true, "getAndSet boolean value");
        }

    }

}
