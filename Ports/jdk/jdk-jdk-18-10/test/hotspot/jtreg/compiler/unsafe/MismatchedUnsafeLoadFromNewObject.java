/*
 * Copyright (c) 2019, Red Hat, Inc. All rights reserved.
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
 * @bug 8216549
 * @summary Mismatched unsafe access to non escaping object fails
 *
 * @modules java.base/jdk.internal.misc
 * @run main/othervm -XX:-UseOnStackReplacement -XX:-BackgroundCompilation MismatchedUnsafeLoadFromNewObject
 */

import java.lang.reflect.Field;
import jdk.internal.misc.Unsafe;

public class MismatchedUnsafeLoadFromNewObject {
    public volatile int f_int = -1;
    public volatile int f_int2 = -1;

    public static Unsafe unsafe = Unsafe.getUnsafe();
    public static final long f_int_off;
    public static final long f_int2_off;

    static {
        Field f_int_field = null;
        Field f_int2_field = null;
        try {
            f_int_field = MismatchedUnsafeLoadFromNewObject.class.getField("f_int");
            f_int2_field = MismatchedUnsafeLoadFromNewObject.class.getField("f_int2");
        } catch (Exception e) {
            System.out.println("reflection failed " + e);
            e.printStackTrace();
        }
        f_int_off = unsafe.objectFieldOffset(f_int_field);
        f_int2_off = unsafe.objectFieldOffset(f_int2_field);
    }

    static public void main(String[] args) {
        for (int i = 0; i < 20_000; i++) {
            byte res = test1();
            if (res != -1) {
                throw new RuntimeException("Incorrect result: " + res);
            }
            res = test2();
            if (res != -1) {
                throw new RuntimeException("Incorrect result: " + res);
            }
            int res2 = test3();
            if (res2 != -1) {
                throw new RuntimeException("Incorrect result: " + res2);
            }
        }
    }

    static byte test1() {
        MismatchedUnsafeLoadFromNewObject t = new MismatchedUnsafeLoadFromNewObject();
        return unsafe.getByte(t, f_int_off);
    }

    static byte test2() {
        MismatchedUnsafeLoadFromNewObject t = new MismatchedUnsafeLoadFromNewObject();
        return unsafe.getByte(t, f_int_off+1);
    }

    static int test3() {
        MismatchedUnsafeLoadFromNewObject t = new MismatchedUnsafeLoadFromNewObject();
        if (f_int_off < f_int2_off) {
            return unsafe.getIntUnaligned(t, f_int_off+1);
        } else {
            return unsafe.getIntUnaligned(t, f_int2_off+1);
        }
    }
}
