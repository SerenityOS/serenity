/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8211100
 * @summary hotspot C1 issue with comparing long numbers on x86 32-bit
 *
 * @run main/othervm -XX:+PrintCompilation -XX:CompileOnly=compiler.c1.Test8211100::test
 *                   -XX:CompileCommand=quiet compiler.c1.Test8211100
 */

package compiler.c1;

public class Test8211100 {
    private static final int ITERATIONS = 100_000;

    public static void main(String[] args) {
        for (int i = 0; i < ITERATIONS; i++) {
            test(4558828911L,
                 4294967296L);
        }
    }

    private static void test(long one, long two) {
        while (true) {
            if (one >= two) {
                break;
            }
        }
    }
}
