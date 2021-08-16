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
 * @bug 6769124
 * @summary arraycopy may crash the VM with c1 on 64 bit
 *
 * @run main compiler.c1.TestArrayCopy
 */

package compiler.c1;

public class TestArrayCopy {

    public static void main(String[] args) {

        int k = 1 << 31;


        for(int j = 0; j <1000000; j++) {
            int i = -1;
            while(i < 10) {
                i++;
            }

            int m = k * i;

            int[] O1 = new int[20];
            int[] O2 = new int[20];

            System.arraycopy(O1, i, O2, i, 1); //will crash on amd64
            System.arraycopy(O1, m, O2, m, 1); //will crash on sparcv9
        }
    }
}
