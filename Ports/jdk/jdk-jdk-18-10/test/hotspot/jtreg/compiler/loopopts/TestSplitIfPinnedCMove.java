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
 * @bug 8191153
 * @summary too strong assert from 8186125
 *
 * @run main/othervm -XX:-BackgroundCompilation -XX:CompileCommand=dontinline,TestSplitIfPinnedCMove::not_inlined -XX:CompileOnly=TestSplitIfPinnedCMove::test TestSplitIfPinnedCMove
 *
 */

public class TestSplitIfPinnedCMove {
    static void not_inlined() {}

    static class A {
        A(int f) {
            this.f = f;
        }
        int f;
    }

    static int test(int i1, int i3, A a1, A a2) {
        // loops to trigger more loop optimization passes
        int res = 0;
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                for (int k = 0; k < 2; k++) {
                    res++;
                }
            }
        }
        // null check a1 and a2
        res += a1.f + a2.f;

        boolean f2 = false;
        if (i1 > 0) {
            not_inlined();
            f2 = true;
        }

        // Should become CMoveP and be pinned here
        res += (i3 > 0 ? a1 : a2).f;

        // f2 should be split through phi with above if
        if (f2) {
            not_inlined();
            res += 42;
        }

        // Another use for i3 > 0
        if (i3 > 0) {
            res++;
        }
        return res;
    }

    public static void main(String[] args) {
        A a = new A(42);
        for (int i = 0; i < 20_000; i++) {
                test((i % 2) == 0 ? 0 : 1, (i % 2) == 0 ? 0 : 1, a, a);
        }
    }
}
