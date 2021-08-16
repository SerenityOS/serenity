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
 * @bug 8186125
 * @summary cast before unsafe access moved in dominating null check null path causes crash
 * @modules java.base/jdk.internal.misc:+open
 *
 * @run main/othervm -XX:-BackgroundCompilation TestSplitIf
 *
 */

import jdk.internal.misc.Unsafe;
import java.lang.reflect.Field;

public class TestSplitIf {

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

    static int test(A a1, A a2, boolean flag1) {
        boolean flag2;
        int f = 0;
        A a = null;
        if (flag1) {
            flag2 = true;
            a = a1;
        } else {
            flag2 = false;
            a = a2;
        }
        if (flag2) {
            f = UNSAFE.getInt(a, F_OFFSET);
        } else {
            f = UNSAFE.getInt(a, F_OFFSET);
        }
        return f;
    }

    static public void main(String[] args) {
        A a = new A(0x42);
        for (int i = 0; i < 20000; i++) {
            test(a, a, (i % 2) == 0);
        }
    }

}
