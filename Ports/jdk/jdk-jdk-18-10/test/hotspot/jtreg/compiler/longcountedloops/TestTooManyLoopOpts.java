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
 * @bug 8254998
 * @summary C2: assert(!n->as_Loop()->is_transformed_long_loop()) failure with -XX:StressLongCountedLoop=1
 * @requires vm.compiler2.enabled & vm.gc.Parallel
 *
 * @run main/othervm -Xcomp -XX:-TieredCompilation -XX:MaxRecursiveInlineLevel=26 -XX:MaxInlineLevel=26 -XX:LoopOptsCount=41 -XX:+UseParallelGC TestTooManyLoopOpts
 *
 */

public class TestTooManyLoopOpts {
    private static volatile int field;

    public static void main(String[] args) {
        test(0);
    }

    private static void test(int stop) {
        for (long l = 0; l < 10; l++) {
            test_helper(stop, 26);
            field = 0x42;
        }
    }
    private static void test_helper(int stop, int rec) {
        if (rec <= 0) {
            return;
        }
        for (int i = 0; i < stop; i++) {
            test_helper(stop, rec - 1);
        }
    }
}
