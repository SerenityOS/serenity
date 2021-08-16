/*
 * Copyright (c) 2021, Red Hat, Inc. All rights reserved.
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
 * @bug 8260407
 * @summary cmp != __null && cmp->Opcode() == Op_CmpL failure with -XX:StressLongCountedLoop=200000000 in lucene
 *
 * @run main/othervm -XX:-BackgroundCompilation TestLongCountedLoopSplitIf
 *
 */

public class TestLongCountedLoopSplitIf {
    public static void main(String[] args) {
        for (int i = 0; i < 20_000; i++) {
            test(0, 100);
            test_helper(100, 0, 0);
        }
    }

    private static float test(long start, long stop) {
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
            }
        }
        float res = 1;

        long l = start;

        for (; ; ) {
            res = test_helper(l, stop, res);
            if (shouldStop(l, stop)) {
                break;
            }
            l++;
        }
        return res;
    }

    private static boolean shouldStop(long l, long stop) {
        return l >= stop;
    }

    private static float test_helper(long l, long stop, float res) {
        if (l < stop) {
            res += l;
        } else {
            res *= l;
        }
        return res;
    }
}
