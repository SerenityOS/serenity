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
 * @bug 8196296
 * @summary Bad graph when unrolled loop bounds conflicts with range checks
 *
 * @run main/othervm -XX:-BackgroundCompilation -XX:+IgnoreUnrecognizedVMOptions -XX:LoopUnrollLimit=0 TestStripMinedBackToBackIfs
 *
 */


public class TestStripMinedBackToBackIfs {
    public static void main(String[] args) {
        for (int i = 0; i < 20_000; i++) {
            test(100);
        }
    }

    private static double test(int limit) {
        double v = 1;
        for (int i = 0; i < limit; i++) {
            v = v * 4;
            // We don't want this test to be merged with identical
            // loop end test
            if (i+1 < limit) {
                v = v * 2;
            }
        }
        return v;
    }
}
