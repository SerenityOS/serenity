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
 * @summary Test unsafe access for Object
 *
 * @modules java.base/jdk.internal.misc:+open
 * @run testng/othervm -Diters=100   -Xint                   compiler.unsafe.JdkInternalMiscUnsafeAccessTestObject
 * @run testng/othervm -Diters=20000 -XX:TieredStopAtLevel=1 compiler.unsafe.JdkInternalMiscUnsafeAccessTestObject
 * @run testng/othervm -Diters=20000 -XX:-TieredCompilation  compiler.unsafe.JdkInternalMiscUnsafeAccessTestObject
 * @run testng/othervm -Diters=20000                         compiler.unsafe.JdkInternalMiscUnsafeAccessTestObject
 */

package compiler.unsafe;

import org.testng.annotations.Test;

import java.lang.reflect.Field;

import static org.testng.Assert.*;

public class JdkInternalMiscUnsafeAccessTestObject {
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
            Field staticVField = JdkInternalMiscUnsafeAccessTestObject.class.getDeclaredField("static_v");
            STATIC_V_BASE = UNSAFE.staticFieldBase(staticVField);
            STATIC_V_OFFSET = UNSAFE.staticFieldOffset(staticVField);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        try {
            Field vField = JdkInternalMiscUnsafeAccessTestObject.class.getDeclaredField("v");
            V_OFFSET = UNSAFE.objectFieldOffset(vField);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        ARRAY_OFFSET = UNSAFE.arrayBaseOffset(Object[].class);
        int ascale = UNSAFE.arrayIndexScale(Object[].class);
        ARRAY_SHIFT = 31 - Integer.numberOfLeadingZeros(ascale);
    }

    static Object static_v;

    Object v;

    @Test
    public void testFieldInstance() {
        JdkInternalMiscUnsafeAccessTestObject t = new JdkInternalMiscUnsafeAccessTestObject();
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
        Object[] array = new Object[10];
        for (int c = 0; c < ITERS; c++) {
            for (int i = 0; i < array.length; i++) {
                testAccess(array, (((long) i) << ARRAY_SHIFT) + ARRAY_OFFSET);
            }
        }
    }


    static void testAccess(Object base, long offset) {
        // Plain
        {
            UNSAFE.putReference(base, offset, "foo");
            Object x = UNSAFE.getReference(base, offset);
            assertEquals(x, "foo", "set Object value");
        }

        // Volatile
        {
            UNSAFE.putReferenceVolatile(base, offset, "bar");
            Object x = UNSAFE.getReferenceVolatile(base, offset);
            assertEquals(x, "bar", "putVolatile Object value");
        }


        // Lazy
        {
            UNSAFE.putReferenceRelease(base, offset, "foo");
            Object x = UNSAFE.getReferenceAcquire(base, offset);
            assertEquals(x, "foo", "putRelease Object value");
        }

        // Opaque
        {
            UNSAFE.putReferenceOpaque(base, offset, "bar");
            Object x = UNSAFE.getReferenceOpaque(base, offset);
            assertEquals(x, "bar", "putOpaque Object value");
        }


        UNSAFE.putReference(base, offset, "foo");

        // Compare
        {
            boolean r = UNSAFE.compareAndSetReference(base, offset, "foo", "bar");
            assertEquals(r, true, "success compareAndSet Object");
            Object x = UNSAFE.getReference(base, offset);
            assertEquals(x, "bar", "success compareAndSet Object value");
        }

        {
            boolean r = UNSAFE.compareAndSetReference(base, offset, "foo", "baz");
            assertEquals(r, false, "failing compareAndSet Object");
            Object x = UNSAFE.getReference(base, offset);
            assertEquals(x, "bar", "failing compareAndSet Object value");
        }

        // Advanced compare
        {
            Object r = UNSAFE.compareAndExchangeReference(base, offset, "bar", "foo");
            assertEquals(r, "bar", "success compareAndExchange Object");
            Object x = UNSAFE.getReference(base, offset);
            assertEquals(x, "foo", "success compareAndExchange Object value");
        }

        {
            Object r = UNSAFE.compareAndExchangeReference(base, offset, "bar", "baz");
            assertEquals(r, "foo", "failing compareAndExchange Object");
            Object x = UNSAFE.getReference(base, offset);
            assertEquals(x, "foo", "failing compareAndExchange Object value");
        }

        {
            Object r = UNSAFE.compareAndExchangeReferenceAcquire(base, offset, "foo", "bar");
            assertEquals(r, "foo", "success compareAndExchangeAcquire Object");
            Object x = UNSAFE.getReference(base, offset);
            assertEquals(x, "bar", "success compareAndExchangeAcquire Object value");
        }

        {
            Object r = UNSAFE.compareAndExchangeReferenceAcquire(base, offset, "foo", "baz");
            assertEquals(r, "bar", "failing compareAndExchangeAcquire Object");
            Object x = UNSAFE.getReference(base, offset);
            assertEquals(x, "bar", "failing compareAndExchangeAcquire Object value");
        }

        {
            Object r = UNSAFE.compareAndExchangeReferenceRelease(base, offset, "bar", "foo");
            assertEquals(r, "bar", "success compareAndExchangeRelease Object");
            Object x = UNSAFE.getReference(base, offset);
            assertEquals(x, "foo", "success compareAndExchangeRelease Object value");
        }

        {
            Object r = UNSAFE.compareAndExchangeReferenceRelease(base, offset, "bar", "baz");
            assertEquals(r, "foo", "failing compareAndExchangeRelease Object");
            Object x = UNSAFE.getReference(base, offset);
            assertEquals(x, "foo", "failing compareAndExchangeRelease Object value");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = UNSAFE.weakCompareAndSetReferencePlain(base, offset, "foo", "bar");
            }
            assertEquals(success, true, "weakCompareAndSetPlain Object");
            Object x = UNSAFE.getReference(base, offset);
            assertEquals(x, "bar", "weakCompareAndSetPlain Object value");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = UNSAFE.weakCompareAndSetReferenceAcquire(base, offset, "bar", "foo");
            }
            assertEquals(success, true, "weakCompareAndSetAcquire Object");
            Object x = UNSAFE.getReference(base, offset);
            assertEquals(x, "foo", "weakCompareAndSetAcquire Object");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = UNSAFE.weakCompareAndSetReferenceRelease(base, offset, "foo", "bar");
            }
            assertEquals(success, true, "weakCompareAndSetRelease Object");
            Object x = UNSAFE.getReference(base, offset);
            assertEquals(x, "bar", "weakCompareAndSetRelease Object");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = UNSAFE.weakCompareAndSetReference(base, offset, "bar", "foo");
            }
            assertEquals(success, true, "weakCompareAndSet Object");
            Object x = UNSAFE.getReference(base, offset);
            assertEquals(x, "foo", "weakCompareAndSet Object");
        }

        UNSAFE.putReference(base, offset, "bar");

        // Compare set and get
        {
            Object o = UNSAFE.getAndSetReference(base, offset, "foo");
            assertEquals(o, "bar", "getAndSet Object");
            Object x = UNSAFE.getReference(base, offset);
            assertEquals(x, "foo", "getAndSet Object value");
        }

    }

}
