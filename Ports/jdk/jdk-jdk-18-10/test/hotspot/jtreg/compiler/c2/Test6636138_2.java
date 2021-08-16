/*
 * Copyright (c) 2009, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6636138
 * @summary SuperWord::co_locate_pack(Node_List* p) generates memory graph that leads to memory order violation.
 *
 * @run main/othervm -Xbatch
 *      -XX:CompileCommand=compileonly,compiler.c2.Test6636138_2::shift
 *      compiler.c2.Test6636138_2
 */

package compiler.c2;

public class Test6636138_2 {

    public static void init(int src[]) {
        // Initialize the array
        for (int i = 0; i < src.length; i++)
            src[i] = i;
    }

    public static void shift(int src[]) {
        //left-shift the array
        for (int i = src.length - 1; i > 0; i--) {
            int tmp = src[i];
            src[i] = src[i - 1];
            src[i - 1] = tmp;
        }
    }

    public static void verify(int src[]) {
        for (int i = 0; i < src.length; i++) {
            int value = (i - 1 + src.length) % src.length; // correct value after shifting
            if (src[i] != value) {
                System.out.println("Error: src[" + i + "] should be " + value + " instead of " + src[i]);
                System.exit(97);
            }
        }
    }

    public static void test() {
        int[] src = new int[10];
        init(src);
        shift(src);
        verify(src);
    }

    public static void main(String[] args) {
        for (int i = 0; i < 2000; i++)
            test();
    }
}