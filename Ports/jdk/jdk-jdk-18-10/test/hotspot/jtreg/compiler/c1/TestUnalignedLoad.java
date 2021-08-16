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
 * @summary unaligned load may fail with c1 on 64 bit
 *
 * @run main compiler.c1.TestUnalignedLoad
 */

package compiler.c1;

public class TestUnalignedLoad {

    static long l1v = 0x200000003L;
    static long l2v = 0x400000005L;
    static double d1v = Double.MAX_VALUE;
    static double d2v = Double.MIN_VALUE;

    public static void main(String[] args) {
        long l1 = l1v;
        double d1 = d1v;
        long l2 = l2v;
        double d2 = d2v;

        // Run long enough to induce an OSR
        for (int i = 0; i < 10000000; i++) {
        }
        boolean error = false;

        if (l1 != l1v) {
            System.out.println(l1 + " != " + l1v);
            error = true;
        }
        if (l2 != l2v) {
            System.out.println(l2 + " != " + l2v);
            error = true;
        }
        if (d1 != d1v) {
            System.out.println(d1 + " != " + d1v);
            error = true;
        }
        if (d2 != d2v) {
            System.out.println(d2 + " != " + d2v);
            error = true;
        }
        if (error) {
            throw new InternalError();
        }
    }
}
