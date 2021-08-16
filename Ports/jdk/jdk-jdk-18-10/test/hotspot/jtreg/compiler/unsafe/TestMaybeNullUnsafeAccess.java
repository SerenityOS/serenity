/*
 * Copyright (c) 2017, Red Hat, Inc. All rights reserved.
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

/**
 * @test
 * @bug 8176506
 * @summary cast before unsafe access moved in dominating null check null path causes crash
 * @modules java.base/jdk.internal.misc:+open
 *
 * @run main/othervm -Xbatch -XX:-UseOnStackReplacement TestMaybeNullUnsafeAccess
 *
 */

import jdk.internal.misc.Unsafe;
import java.lang.reflect.Field;

public class TestMaybeNullUnsafeAccess {

    static final jdk.internal.misc.Unsafe UNSAFE = Unsafe.getUnsafe();
    static final long F_OFFSET;

    static class A {
        int f;
        A(int f) {
            this.f = f;
        }
    }

    static {
        try {
            Field fField = A.class.getDeclaredField("f");
            F_OFFSET = UNSAFE.objectFieldOffset(fField);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static A test_helper(Object o) {
        // this includes a check for null with both branches taken
        return (A)o;
    }


    // Loop is unswitched because of the test for null from the
    // checkcast above, unsafe access is copied in each branch, the
    // compiler sees a memory access to a null object
    static int test1(Object o, long offset) {
        int f = 0;
        for (int i = 0; i < 100; i++) {
            A a = test_helper(o);
            f = UNSAFE.getInt(a, offset);
        }
        return f;
    }

    // Same as above except because we know the offset of the access
    // is small, we can deduce object a cannot be null
    static int test2(Object o) {
        int f = 0;
        for (int i = 0; i < 100; i++) {
            A a = test_helper(o);
            f = UNSAFE.getInt(a, F_OFFSET);
        }
        return f;
    }

    static public void main(String[] args) {
        A a = new A(0x42);
        for (int i = 0; i < 20000; i++) {
            test_helper(null);
            test_helper(a);
            test1(a, F_OFFSET);
            test2(a);
        }
    }

}
