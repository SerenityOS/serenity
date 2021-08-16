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
 *      -XX:CompileCommand=compileonly,compiler.c2.Test6636138_1::init
 *      compiler.c2.Test6636138_1
 */

package compiler.c2;

public class Test6636138_1 {

    public static void init(int src[], int [] dst, int[] ref) {
        // initialize the arrays
        for (int i =0; i<src.length; i++) {
            src[i] =  i;
            dst[i] = 2;      // yes, dst[i] needed(otherwise src[i] will be replaced with i)
            ref[i] = src[i]; // src[i] depends on the store src[i]
        }
    }

    public static void verify(int src[], int[] ref) {
        // check whether src and ref are equal
        for (int i = 0; i < src.length; i++) {
            if (src[i] != ref[i]) {
                System.out.println("Error: src and ref don't match at " + i);
                System.exit(97);
            }
        }
    }

    public static void test() {
        int[] src = new int[34];
        int[] dst = new int[34];
        int[] ref = new int[34];

        init(src, dst, ref);
        verify(src, ref);
    }

    public static void main(String[] args) {
        for (int i=0; i< 2000; i++) {
            test();
        }
    }
}
