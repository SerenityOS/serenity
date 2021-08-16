/*
 * Copyright (c) 2018, Red Hat, Inc. All rights reserved.
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
 * @bug 8202123
 * @summary C2 Crash in Node::in(unsigned int) const+0x14
 *
 * @run main/othervm TestLimitLoadBelowLoopLimitCheck
 *
 */

public class TestLimitLoadBelowLoopLimitCheck {
    public static int[] run(int[] arr) {
        int max = 0;
        for (int i : arr) {
            if (i > max) {
                max = i;
            }
        }

        int[] counts = new int[10];

        int i = 0;
        for (i = 0; i < counts.length; i += 1) {
            for (int j = 0; j < counts[i]; j += 1) {
            }
        }

        while (i < max) {
            for (int j = 0; j < counts[i]; j += 1) {
                arr[0] = i;
            }
        }

        return arr;
    }

    public static void main(String[] args) {
        int[] arr = new int[1000 * 1000];

        for (int i = 0; i < 100; i++) {
            run(arr);
        }
    }
}
