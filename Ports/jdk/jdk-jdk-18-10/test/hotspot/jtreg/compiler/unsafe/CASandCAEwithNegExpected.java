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

/*
 * @test
 * @bug 8211320
 * @summary Aarch64: unsafe.compareAndSetByte() and unsafe.compareAndSetShort() c2 intrinsics broken with negative expected value
 *
 * @modules java.base/jdk.internal.misc
 * @run main/othervm -XX:-UseOnStackReplacement -XX:-BackgroundCompilation  CASandCAEwithNegExpected
 */

import java.lang.reflect.Field;
import jdk.internal.misc.Unsafe;

public class CASandCAEwithNegExpected {
    public volatile int f_int = -1;
    public volatile long f_long = -1;
    public volatile byte f_byte = -1;
    public volatile short f_short = -1;

    public static Unsafe unsafe = Unsafe.getUnsafe();
    public static Field f_int_field;
    public static Field f_long_field;
    public static Field f_byte_field;
    public static Field f_short_field;
    public static long f_int_off;
    public static long f_long_off;
    public static long f_byte_off;
    public static long f_short_off;

    static {
        try {
            f_int_field = CASandCAEwithNegExpected.class.getField("f_int");
            f_long_field = CASandCAEwithNegExpected.class.getField("f_long");
            f_byte_field = CASandCAEwithNegExpected.class.getField("f_byte");
            f_short_field = CASandCAEwithNegExpected.class.getField("f_short");
            f_int_off = unsafe.objectFieldOffset(f_int_field);
            f_long_off = unsafe.objectFieldOffset(f_long_field);
            f_byte_off = unsafe.objectFieldOffset(f_byte_field);
            f_short_off = unsafe.objectFieldOffset(f_short_field);
        } catch (Exception e) {
            System.out.println("reflection failed " + e);
            e.printStackTrace();
        }
    }

    static public void main(String[] args) {
        CASandCAEwithNegExpected t = new CASandCAEwithNegExpected();
        for (int i = 0; i < 20_000; i++) {
            t.test();
        }
    }

    // check proper handling of sign extension of expected value in comparison
    void test() {
        f_int = -1;
        f_long = -1;
        f_byte = -1;
        f_short = -1;

        unsafe.compareAndSetInt(this, f_int_off, -1, 42);
        if (f_int != 42) {
            throw new RuntimeException("CAS failed");
        }
        unsafe.compareAndSetLong(this, f_long_off, -1, 42);
        if (f_long != 42) {
            throw new RuntimeException("CAS failed");
        }
        unsafe.compareAndSetByte(this, f_byte_off, (byte)-1, (byte)42);
        if (f_byte != 42) {
            throw new RuntimeException("CAS failed");
        }
        unsafe.compareAndSetShort(this, f_short_off, (short)-1, (short)42);
        if (f_short != 42) {
            throw new RuntimeException("CAS failed");
        }

        f_int = -1;
        f_long = -1;
        f_byte = -1;
        f_short = -1;

        unsafe.compareAndExchangeInt(this, f_int_off, -1, 42);
        if (f_int != 42) {
            throw new RuntimeException("CAE failed");
        }
        unsafe.compareAndExchangeLong(this, f_long_off, -1, 42);
        if (f_long != 42) {
            throw new RuntimeException("CAE failed");
        }
        unsafe.compareAndExchangeByte(this, f_byte_off, (byte)-1, (byte)42);
        if (f_byte != 42) {
            throw new RuntimeException("CAE failed");
        }
        unsafe.compareAndExchangeShort(this, f_short_off, (short)-1, (short)42);
        if (f_short != 42) {
            throw new RuntimeException("CAE failed");
        }

        f_int = -1;
        f_long = -1;
        f_byte = -1;
        f_short = -1;

        if (unsafe.weakCompareAndSetInt(this, f_int_off, -1, 42) && f_int != 42) {
            throw new RuntimeException("CAS failed");
        }

        if (unsafe.weakCompareAndSetLong(this, f_long_off, -1, 42) && f_long != 42) {
            throw new RuntimeException("CAS failed");
        }

        if (unsafe.weakCompareAndSetByte(this, f_byte_off, (byte)-1, (byte)42) && f_byte != 42) {
            throw new RuntimeException("CAS failed");
        }

        if (unsafe.weakCompareAndSetShort(this, f_short_off, (short)-1, (short)42) && f_short != 42) {
            throw new RuntimeException("CAS failed");
        }
    }

}
