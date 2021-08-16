/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 8181872
 *
 * @run main/othervm -XX:+IgnoreUnrecognizedVMOptions
 *                   -XX:CompileThreshold=100 -XX:+TieredCompilation -XX:TieredStopAtLevel=1
 *                   -XX:-BackgroundCompilation -XX:CompileCommand=dontinline,compiler.c1.MultiplyByMaxInt::test
 *                   compiler.c1.MultiplyByMaxInt
 * @run main/othervm -XX:+IgnoreUnrecognizedVMOptions -XX:-BackgroundCompilation
 *                   -XX:CompileThreshold=100 -XX:+TieredCompilation -XX:TieredStopAtLevel=3
 *                   -XX:CompileCommand=dontinline,compiler.c1.MultiplyByMaxInt::test
 *                   compiler.c1.MultiplyByMaxInt
 */
package compiler.c1;

public class MultiplyByMaxInt {
    static int test(int x) {
        int loops = (x >>> 4) & 7;
        while (loops-- > 0) {
            x = (x * 2147483647) % 16807;
        }
        return x;
    }

    public static void main(String[] args) {
        for (int i = 0; i < 20000; i++) {
            test(i);
        }
    }
}
