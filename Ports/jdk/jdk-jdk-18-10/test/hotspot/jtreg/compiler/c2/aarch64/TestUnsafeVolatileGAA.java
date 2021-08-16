/*
 * Copyright (c) 2018, Red Hat, Inc. All rights reserved.
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

package compiler.c2.aarch64;

import java.lang.reflect.Field;
import jdk.internal.misc.Unsafe;

class TestUnsafeVolatileGAA
{
    public volatile int f_int = -1;
    public volatile long f_long = -1;

    public static Unsafe unsafe = Unsafe.getUnsafe();
    public static Field f_int_field;
    public static Field f_long_field;
    public static long f_int_off;
    public static long f_long_off;

    static {
        try {
            f_int_field = TestUnsafeVolatileGAA.class.getField("f_int");
            f_long_field = TestUnsafeVolatileGAA.class.getField("f_long");
            f_int_off = unsafe.objectFieldOffset(f_int_field);
            f_long_off = unsafe.objectFieldOffset(f_long_field);
        } catch (Exception e) {
            System.out.println("reflection failed " + e);
            e.printStackTrace();
        }
    }

    public static void main(String[] args)
    {
        final TestUnsafeVolatileGAA t = new TestUnsafeVolatileGAA();
        for (int i = 0; i < 100_000; i++) {
            if (t.testInt() != i-1) {
                throw new RuntimeException("bad result!");
            }
        }
        for (int i = 0; i < 100_000; i++) {
            if (t.testLong() != i-1) {
                throw new RuntimeException("bad result!");
            }
        }
    }

    public int testInt()
    {
        return unsafe.getAndAddInt(this, f_int_off, 1);
    }

    public long testLong()
    {
        return unsafe.getAndAddLong(this, f_long_off, 1);
    }
}
