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
 * @bug 8247824
 * @summary CTW: C2 (Shenandoah) compilation fails with SEGV in SBC2Support::pin_and_expand
 * @requires vm.flavor == "server"
 * @requires vm.gc.Shenandoah
 *
 * @run main/othervm -XX:-BackgroundCompilation -XX:+UseShenandoahGC -XX:LoopMaxUnroll=0 TestShenandoahLRBInOuterStripMinedLoop
 *
 */

import java.util.Arrays;

public class TestShenandoahLRBInOuterStripMinedLoop {
    public static void main(String[] args) {
        A[] array = new A[4000];
        Arrays.fill(array, new A());
        for (int i = 0; i < 20_0000; i++) {
            test(array);
        }
    }

    private static int test(A[] array) {
        A a = null;
        int v = 1;
        A b = null;
        for (int i = 0; i < 2000; i++) {
            a = array[i];
            b = array[2*i];
            v *= 2;
        }
        return a.f + b.f + v;
    }

    private static class A {
        public int f;
    }
}
