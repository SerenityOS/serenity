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
 * @bug 8244086
 * @summary Following 8241492, strip mined loop may run extra iterations
 * @requires vm.compiler2.enabled
 *
 * @run main/othervm -XX:-BackgroundCompilation -XX:LoopUnrollLimit=0 -XX:-TieredCompilation TestStripMinedLimitBelowInit
 *
 */

public class TestStripMinedLimitBelowInit {
    public static void main(String[] args) {
        for (int i = 0; i < 20_000; i++) {
            test1(0, 1000);
            test2(1000, 0);
        }
        int sum = test1(1000, 0);
        if (sum != 1000) {
            throw new RuntimeException("wrong result: " + sum);
        }
        sum = test2(0, 1000);
        if (sum != 0) {
            throw new RuntimeException("wrong result: " + sum);
        }
    }

    private static int test1(int start, int stop) {
        int sum = 0;
        int i = start;
        do {
            // So ciTypeFlow doesn't clone the loop head
            synchronized (new Object()) {
            }
            sum += i;
            i++;
        } while (i < stop);
        return sum;
    }

    private static int test2(int start, int stop) {
        int sum = 0;
        int i = start;
        do {
            synchronized (new Object()) {
            }
            sum += i;
            i--;
        } while (i >= stop);
        return sum;
    }
}
