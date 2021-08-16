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
 * @bug 8173147
 * @summary Loads generated at uncommon trap from eliminated arraycopy have incorrect memory state
 * @run main/othervm -XX:CompileOnly=TestArrayCopyUNCBadMem::test -Xcomp TestArrayCopyUNCBadMem
 *
 */


public class TestArrayCopyUNCBadMem {

    volatile static int field;

    static class unloaded {
        static int dummy;
    }

    static int test(int[] input) {
        int[] alloc = new int[10];
        System.arraycopy(input, 0, alloc, 0, 10);

        // membars to have anti-dependence edges and make scheduling
        // fail
        field = 0x42;

        // uncommon trap
        unloaded.dummy = 0x42;

        return alloc[0] + alloc[1];
    }

    public static void main(String[] args) {
        int[] array = new int[10];
        System.arraycopy(array, 0, array, 0, 0); // load System class
        test(array);
    }
}
