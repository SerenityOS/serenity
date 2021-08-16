/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8229495
 * @summary SIGILL in C2 generated OSR compilation.
 *
 * @run main/othervm -Xcomp -XX:-TieredCompilation -XX:CompileOnly=TestRCEAfterUnrolling::test TestRCEAfterUnrolling
 *
 */

public class TestRCEAfterUnrolling {

     public static int iFld = 0;
     public static short sFld = 1;

     public static void main(String[] strArr) {
         test();
     }

     public static int test() {
         int x = 11;
         int y = 0;
         int j = 0;
         int iArr[] = new int[400];

         init(iArr);

         for (int i = 0; i < 2; i++) {
             doNothing();
             for (j = 10; j > 1; j -= 2) {
                 sFld += (short)j;
                 iArr = iArr;
                 y += (j * 3);
                 x = (iArr[j - 1]/ x);
                 x = sFld;
             }
             int k = 1;
             while (++k < 8) {
                 iFld += x;
             }
         }
         return Float.floatToIntBits(654) + x + j + y;
     }

     // Inlined
     public static void doNothing() {
     }

     // Inlined
     public static void init(int[] a) {
         for (int j = 0; j < a.length; j++) {
             a[j] = 0;
         }
     }
}


