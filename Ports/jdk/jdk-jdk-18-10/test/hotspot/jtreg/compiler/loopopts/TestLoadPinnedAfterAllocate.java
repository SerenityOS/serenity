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
 * @bug 8260709
 * @summary C2: assert(false) failed: unscheduable graph
 *
 * @run main/othervm -XX:-BackgroundCompilation TestLoadPinnedAfterAllocate
 *
 */

public class TestLoadPinnedAfterAllocate {
    private int field;
    private static volatile int barrier;
    private static Object field2;

    public static void main(String[] args) {
        final TestLoadPinnedAfterAllocate test = new TestLoadPinnedAfterAllocate();
        for (int i = 0; i < 20_000; i++) {
            test.test(1, 10);
        }
    }

    private int test(int start, int stop) {
        int[] array = new int[10];
        for (int j = 0; j < 10; j++) {
            barrier = 1;
            // early control for field load below
            for (int i = 1; i < 10; i *= 2) {
                field2 = array;
                array = new int[10];
                // late control for field load below
            }
        }
        int v = field;
        array[0] = v;
        return v+v;
    }
}
