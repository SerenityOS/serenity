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
 * @bug 8237837 8244721
 * @summary  Shenandoah: assert(mem == __null) failed: only one safepoint
 * @requires vm.flavor == "server"
 * @requires vm.gc.Shenandoah
 *
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseShenandoahGC -Xcomp -XX:CompileOnly=BarrierInInfiniteLoop::test1
 *                   -XX:CompileOnly=BarrierInInfiniteLoop::test2 -XX:CompileOnly=BarrierInInfiniteLoop::test3 -XX:CompileCommand=quiet BarrierInInfiniteLoop
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseShenandoahGC -XX:ShenandoahGCMode=iu -Xcomp -XX:CompileOnly=BarrierInInfiniteLoop::test1
 *                   -XX:CompileOnly=BarrierInInfiniteLoop::test2 -XX:CompileOnly=BarrierInInfiniteLoop::test3 -XX:CompileCommand=quiet BarrierInInfiniteLoop
 *
 */

public class BarrierInInfiniteLoop {
    private static Object field1 = new Object();
    private static Object field2 = new Object();
    private static int field3;

    public static void main(String[] args) {
        test1(false);
        test2(false, false);
        test3(false);
    }

    private static void test1(boolean flag) {
        if (flag) {
            for (;;) {
                field1 = field2;
            }
        }
    }

    private static void test2(boolean flag1, boolean flag2) {
        if (flag1) {
            for (;;) {
                for (;;) {
                    if (flag2) {
                        break;
                    }
                    field1 = field2;
                }
            }
        }
    }

    private static void test3(boolean flag) {
        if (flag) {
            for (;;) {
                for (;;) {
                    field3 = 42;
                    if (field1 == field2) {
                        break;
                    }
                }
            }
        }
    }
}
