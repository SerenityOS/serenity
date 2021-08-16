/*
 * Copyright (c) 2020, Red Hat, Inc. All rights reserved.
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
 * @bug 8255400
 * @summary C2 failures after JDK-8255000
 * @requires vm.gc.Shenandoah
 * @modules java.base/jdk.internal.misc:+open
 *
 * @run main/othervm -XX:-UseOnStackReplacement -XX:-BackgroundCompilation -XX:-TieredCompilation -XX:+UseShenandoahGC TestBarrierExpansionDeadMemPhi
 *
 *
 */

import jdk.internal.misc.Unsafe;
import java.util.Arrays;
import java.lang.reflect.Field;

public class TestBarrierExpansionDeadMemPhi {

    static final jdk.internal.misc.Unsafe UNSAFE = Unsafe.getUnsafe();

    static final long F_OFFSET;

    static class A {
        int f;
    }

    static {
        try {
            Field fField = A.class.getDeclaredField("f");
            F_OFFSET = UNSAFE.objectFieldOffset(fField);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static int test(Object[] array) {
        int f = 0;
        for (int i = 0; i < 100; i++) {
            f += UNSAFE.getInt(array[i], F_OFFSET);
        }
        return f;
    }

    static public void main(String[] args) {
        Object[] array = new Object[100];
        Arrays.fill(array, new A());

        for (int i = 0; i < 20000; i++) {
            test(array);
        }
    }
}
