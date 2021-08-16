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
 * @bug 8257575
 * @summary C2: "failed: only phis" assert failure in loop strip mining verfication
 *
 * @run main/othervm -XX:LoopUnrollLimit=0 -XX:-UseOnStackReplacement -XX:-BackgroundCompilation -XX:CompileCommand=dontinline,TestEliminatedLoadPinnedOnBackedge::notInlined
 *                    -XX:CompileCommand=inline,TestEliminatedLoadPinnedOnBackedge::inlined TestEliminatedLoadPinnedOnBackedge
 *
 */

public class TestEliminatedLoadPinnedOnBackedge {
    private static Object field2;

    final static int iters = 2000;

    public static void main(String[] args) {
        boolean[] flags = new boolean[iters];
        for (int i = 0; i < iters; i++) {
            flags[i] = i < iters/2;
        }
        for (int i = 0; i < 20_000; i++) {
            test1(flags);
            test2(flags, 1);
            test3(flags);
            inlined(new Object(), 1);
            inlined(new Object(), 4);
            inlined2(42);
            inlined2(0x42);
        }
    }

    static int field;

    private static int test1(boolean[] flags) {
        int k = 2;
        for (; k < 4; k *= 2) {
        }
        int[] array = new int[10];
        notInlined(array);
        // This load commons with the load on the backedge after the
        // outer strip mined loop is expanded.
        int v = array[0];
        array[1] = 42;
        // No use for o. Allocation removed at macro expansion time.
        Object o = new Object();
        inlined(o, k);
        int i = 0;
        for (; ; ) {
            synchronized (array) {
            }
            if (i >= iters) {
                break;
            }
            v = array[0]; // This load ends up on the backedge
            if (flags[i]) {
                inlined2(array[1]);
            }
            i++;
        }
        return v;
    }

    private static int test2(boolean[] flags, int d) {
        int k = 2;
        for (; k < 4; k *= 2) {
        }
        int[] array = new int[10];
        notInlined(array);
        int v = array[0];
        array[1] = 42;
        Object o = new Object();
        inlined(o, k);
        int i = 0;
        for (; ; ) {
            synchronized (array) {
            }
            if (d == 0) {}
            if (i >= iters) {
                break;
            }
            v = (array[0] + array[2]) / d;
            if (flags[i]) {
                inlined2(array[1]);
            }
            i++;
        }
        return v;
    }

    private static int test3(boolean[] flags) {
        int k = 2;
        for (; k < 4; k *= 2) {
        }
        int[] array = new int[10];
        notInlined(array);
        int v1 = array[0];
        int v2 = array[2];
        array[1] = 42;
        Object o = new Object();
        inlined(o, k);
        int i = 0;
        for (; ; ) {
            synchronized (array) {
            }
            if (i >= iters) {
                break;
            }
            v1 = array[0];
            v2 = array[2];
            if (flags[i]) {
                inlined2(array[1]);
            }
            i++;
        }
        return v1 + v2;
    }

    private static void inlined2(int i) {
        if (i != 42) {
            field = 42;
        }
    }

    private static void inlined(Object o, int i) {
        if (i != 4) {
            field2 = o;
        }
    }

    private static void notInlined(int[] array) {
        java.util.Arrays.fill(array, 1);
    }
}
