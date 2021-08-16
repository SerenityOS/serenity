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
 */

/*
 * @test
 * @bug 8227766
 * @run main/othervm -XX:+IgnoreUnrecognizedVMOptions -XX:+CheckUnhandledOops -Xmx100m TestOutOfMemory
 */

public class TestOutOfMemory {
    public static void main(java.lang.String[] unused) {
        final int BIG = 0x100000;
        // Getting OOM breaks the unhandled oop detector
        try {
            int[][] X = new int[BIG][];
            for (int i = 0; i < BIG; i++) {
                X[i] = new int[BIG];
                System.out.println("length = " + X.length);
            }
         } catch (OutOfMemoryError oom) {
            System.out.println("OOM expected");
         }
    }
}
