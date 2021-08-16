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
 * @bug 8238385
 * @summary CTW: C2 (Shenandoah) compilation fails with "Range check dependent CastII node was not removed"
 * @requires vm.gc.Shenandoah
 * @modules java.base/jdk.internal.misc:+open
 *
 * @run main/othervm -XX:-UseOnStackReplacement -XX:-BackgroundCompilation -XX:-TieredCompilation -XX:+UnlockExperimentalVMOptions -XX:+UseShenandoahGC
 *                   FoldIfAfterExpansion
 *
 */

import jdk.internal.misc.Unsafe;

public class FoldIfAfterExpansion {
    private static int[] field1 = new int[100];
    private static int[] field2 = new int[100];
    private static int[] field3;
    private static volatile int barrier;

    static final jdk.internal.misc.Unsafe UNSAFE = Unsafe.getUnsafe();

    public static void main(String[] args) {
        for (int i = 0; i < 20_000; i++) {
            test(true, 10, false, true);
            test(false, 10, false, false);
        }
    }

    private static Object test(boolean flag, int i, boolean flag2, boolean flag3) {
        int[] array;
        if (flag) {
            barrier = 1;
            array = field1;
            final int length = array.length;
            if (flag2) {
                field3 = array;
            }
        } else {
            barrier = 1;
            array = field1;
            final int length = array.length;
            if (flag2) {
                field3 = array;
            }
        }

        array = field1;

        if (flag3) {
            if (i < 0 || i >= array.length) {
                throw new RuntimeException();
            }
            long l = (long)i;
            l = l * UNSAFE.ARRAY_INT_INDEX_SCALE + UNSAFE.ARRAY_INT_BASE_OFFSET;
            UNSAFE.putInt(array, l, i);
        } else {
            if (i < 0 || i >= array.length) {
                throw new RuntimeException();
            }
            long l = (long)i;
            l = l * UNSAFE.ARRAY_INT_INDEX_SCALE + UNSAFE.ARRAY_INT_BASE_OFFSET;
            UNSAFE.putInt(array, l, i);
        }

        return array;
    }
}
