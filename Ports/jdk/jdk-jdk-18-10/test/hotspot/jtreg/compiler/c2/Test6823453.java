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

/*
 * @test
 * @bug 6823453
 * @summary DeoptimizeALot causes fastdebug server jvm to fail with assert(false,"unscheduable graph")
 *
 * @run main/othervm -Xcomp -XX:+IgnoreUnrecognizedVMOptions -XX:+DeoptimizeALot
 *      -XX:CompileCommand=compileonly,compiler.c2.Test6823453::*
 *      compiler.c2.Test6823453
 */

package compiler.c2;

public class Test6823453 {

    static long vara_1 = 1L;

    static void testa() {
        short var_2 = (byte) 1.0E10;

        for (Object temp = new byte[(byte) 1.0E10]; true;
             var_2 = "1".equals("0") ? ((byte) vara_1) : 1) {
        }
    }

    static void testb() {
        long var_1 = -1L;

        short var_2 = (byte) 1.0E10;

        for (Object temp = new byte[(byte) 1.0E10]; true;
             var_2 = "1".equals("0") ? ((byte) var_1) : 1) {
        }
    }

    static void testc() {
        long var_1 = -1L;
        if (vara_1 > 0) var_1 = 1L;

        int var_2 = (byte) var_1 - 128;

        for (Object temp = new byte[var_2]; true;
             var_2 = "1".equals("0") ? 2 : 1) {
        }
    }

    static void testd() {
        long var_1 = 0L;

        int var_2 = (byte) var_1 + 1;
        for (int i = 0; i < 2; i++) var_2 = var_2 - 1;

        for (Object temp = new byte[var_2]; true;
             var_2 = "1".equals("0") ? 2 : 1) {
        }
    }

    public static void main(String[] args) throws Exception {
        int nex = 0;

        try {
            testa();
        } catch (java.lang.NegativeArraySizeException ex) {
            nex++;
        }
        try {
            testb();
        } catch (java.lang.NegativeArraySizeException ex) {
            nex++;
        }
        try {
            testc();
        } catch (java.lang.NegativeArraySizeException ex) {
            nex++;
        }
        try {
            testd();
        } catch (java.lang.NegativeArraySizeException ex) {
            nex++;
        }

        if (nex != 4)
            System.exit(97);
    }
}

