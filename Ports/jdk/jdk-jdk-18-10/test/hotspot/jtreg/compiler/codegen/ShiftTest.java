/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4093292
 * @summary Test for correct code generation by the JIT
 * @run main compiler.codegen.ShiftTest
 */

package compiler.codegen;

public class ShiftTest {
    static final int w = 32;

    private static void doTest(long ct) throws Exception {
        int S22 = 0xc46cf7c2;
        int S23 = 0xcfda9162;
        int S24 = 0xd029aa4c;
        int S25 = 0x17cf1801;
        int A = (int)(ct & 0xffffffffL);
        int B = (int)(ct >>> 32);
        int x, y;
        x = B - S25;
        y = A & (w-1);
        B = ((x >>> y) | (x << (w-y))) ^ A;
        x = A - S24;
        y = B & (w-1);
        A = ((x >>> y) | (x << (w-y))) ^ B;
        x = B - S23;
        y = A & (w-1);
        B = ((x >>> y) | (x << (w-y))) ^ A;
        x = A - S22;
        y = B & (w-1);
        A = ((x >>> y) | (x << (w-y))) ^ B;
        String astr = Integer.toHexString(A);
        String bstr = Integer.toHexString(B);
        System.err.println("A = " + astr + " B = " + bstr);
        if ((!astr.equals("dcb38144")) ||
            (!bstr.equals("1916de73"))) {
            throw new RuntimeException("Unexpected shift results!");
        }
        System.err.println("Test passed");
    }

    public static void main(String[] args) throws Exception {
        doTest(0x496def29b74be041L);
    }
}
