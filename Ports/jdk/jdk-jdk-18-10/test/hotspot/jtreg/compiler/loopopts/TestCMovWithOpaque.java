/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

/*
 * @test
 * @bug 8231223
 * @summary Test conditional move optimization encountering an Opaque4Node.
 * @run main/othervm -Xbatch -XX:-TieredCompilation
 *                   -XX:CompileCommand=inline,compiler.loopopts.TestCMovWithOpaque::test
 *                   compiler.loopopts.TestCMovWithOpaque
 */

package compiler.loopopts;

public class TestCMovWithOpaque {

    public static void test(int array[]) {
        for (int i = 1; i < 8; i += 3) {
            for (int j = 0; j < 4; ++j) {
                switch (i % 4) {
                case 0:
                    break;
                case 1:
                    break;
                case 2:
                    break;
                case 3:
                    array[j] += 42;
                    break;
                }
            }
        }
    }

    public static void main(String[] args) {
        int[] array = new int[4];
        for (int i = 0; i < 20_000; ++i) {
            test(array);
        }
    }
}
