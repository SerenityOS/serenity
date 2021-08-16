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
 * @bug 8237007 8260637
 * @summary Shenandoah: assert(_base == Tuple) failure during C2 compilation
 * @requires vm.flavor == "server"
 * @requires vm.gc.Shenandoah
 *
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:-BackgroundCompilation -XX:+UseShenandoahGC LRBRightAfterMemBar
 *
 */

public class LRBRightAfterMemBar {
    private static A field1;
    private static Object field2;
    static volatile int barrier;

    public static void main(String[] args) {
        for (int i = 0; i < 20_000; i++) {
            test1(true, true, new Object());
            test1(false, false, new Object());
            test2(new Object(), 0, 10);
        }
    }

    private static Object test1(boolean flag, boolean flag2, Object o2) {
        for (int i = 0; i < 10; i++) {
            barrier = 0x42; // Membar
            if (o2 == null) { // hoisted out of loop
            }
            // The following line is converted to a CMove with an out
            // of loop control once the null check above is
            // hoisted. The CMove is pinned right after the membar and
            // assigned the membar as control.
            Object o = flag ? field1 : field2;
            if (flag2) {
                return o;
            }
        }

        return null;
    }

    private static int test2(Object o2, int start, int stop) {
        A a1 = null;
        A a2 = null;
        int v = 0;
        for (int i = start; i < stop; i++) {
            a2 = new A();
            a1 = new A();
            a1.a = a2;
            barrier = 0x42; // Membar
            if (o2 == null) { // hoisted out of loop
            }
            A a3 = a1.a;
            v = a3.f; // null check optimized out by EA but CastPP left in
        }

        a1.f = 0x42;
        a2.f = 0x42;

        return v;
    }

    static class A {
        A a;
        int f;
    }
}
