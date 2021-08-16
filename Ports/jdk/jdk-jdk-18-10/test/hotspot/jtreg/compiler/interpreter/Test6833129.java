/*
 * Copyright (c) 2009, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6833129
 * @summary Object.clone() and Arrays.copyOf ignore coping with -XX:+DeoptimizeALot
 * @run main/othervm -Xbatch -XX:+IgnoreUnrecognizedVMOptions -XX:+DeoptimizeALot
 *      compiler.interpreter.Test6833129
 */

package compiler.interpreter;

public class Test6833129 {
    public static void init(int src[]) {
        for (int i =0; i<src.length; i++) {
            src[i] =  i;
        }
    }

    public static void clone_and_verify(int src[]) {
        for (int i = 0; i < src.length; i++) {
            int [] src_clone = src.clone();
            if (src[i] != src_clone[i]) {
                System.out.println("Error: allocated but not copied: ");
                for( int j =0; j < src_clone.length; j++)
                    System.out.print(" " + src_clone[j]);
                System.out.println();
                System.exit(97);
            }
        }
    }

    public static void test() {
        int[] src = new int[34];
        init(src);
        clone_and_verify(src);
    }

    public static void main(String[] args) {
        for (int i=0; i< 20000; i++) {
            test();
        }
    }
}
