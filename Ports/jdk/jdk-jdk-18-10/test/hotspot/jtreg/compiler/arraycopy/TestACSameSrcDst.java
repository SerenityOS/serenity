/*
 * Copyright (c) 2017, Red Hat, Inc. All rights reserved.
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
 * @bug 8179678
 * @summary ArrayCopy with same src and dst can cause incorrect execution or compiler crash
 *
 * @run main/othervm -XX:CompileCommand=compileonly,TestACSameSrcDst::test* TestACSameSrcDst
 *
 */

public class TestACSameSrcDst {

    static int test1(int[] src, int[] dst) {
        System.arraycopy(src, 5, dst, 0, 10);
         // this shouldn't be transformed to src[5] because the copy
         // can modify src[5] if src and dst are the same.
        return dst[0];
    }

    static int test2(int[] src) {
        System.arraycopy(src, 0, src, 0, 10);
        // same source and destination. If load from destination is
        // transformed to load of source, the compiler performs that
        // optimization in an infinite loop.
        return src[0];
    }

    static int test3() {
        int[] src = new int[15];
        src[5] = 0x42;
        System.arraycopy(src, 5, src, 0, 10);
        // That load can't bypass the arraycopy
        return src[0];
    }

    static int test4() {
        int[] src = new int[15];
        System.arraycopy(src, 0, src, 5, 10);
        return src[0];
    }

    // The dst[0] load can't bypass the arraycopy. After ArrayCopyNode
    // is expanded, C2 looks for a stub call on the control paths of
    // the array copy subgraph to decide whether the load's memory
    // input can bypass the arraycopy. This test verifies the case of
    // a source array that's not declared as an array.
    static int test5(Object src, int l, boolean flag) {
        int[] dst = new int[10];
        if (flag) {
            dst[0] = 0x42;
            System.arraycopy(src, 0, dst, 0, l);
            return dst[0];
        }
        return 0;
    }

    public static void main(String[] args) {
        int[] array = new int[15];
        for (int i = 0; i < 20000; i++) {
            int res;
            for (int j = 0; j < array.length; j++) {
                array[j] = j;
            }
            int expected = array[5];
            res = test1(array, array);
            if (res != expected) {
                throw new RuntimeException("bad result: " + res + " != " + expected);
            }
            test2(array);
            res = test3();
            if (res != 0x42) {
                throw new RuntimeException("bad result: " + res + " != " + 0x42);
            }
            test4();
            for (int j = 0; j < array.length; j++) {
                array[j] = j;
            }
            res = test5(array, 10, (i%2) == 0);
            if (res != 0) {
                throw new RuntimeException("bad result: " + res + " != " + 0);
            }
        }
    }
}
