/*
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6958485
 * @summary fix for 6879921 was insufficient
 *
 * @run main/othervm -Xbatch
 *      -XX:CompileCommand=compileonly,compiler.c2.Test6958485::init
 *      compiler.c2.Test6958485
 */

package compiler.c2;

public class Test6958485 {

    public static void init(Object src[], boolean[] dst) {
        // initialize the arrays
        for (int i =0; i<src.length; i++) {
            dst[i] = src[i] != null ? false : true;
        }
    }

    public static void test() {
        Object[] src = new Object[34];
        boolean[] dst = new boolean[34];

        init(src, dst);
    }

    public static void main(String[] args) {
        for (int i=0; i< 2000; i++) {
            test();
        }
    }
}
