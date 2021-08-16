/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8240905
 * @summary Test matching of instructions that have multiple memory inputs.
 * @run main/othervm -Xbatch -XX:-TieredCompilation
 *                   compiler.codegen.TestMultiMemInstructionMatching
 */

package compiler.codegen;

public class TestMultiMemInstructionMatching {

    static volatile int iFldV = 42;
    static volatile long lFldV = 42;
    static int iFld = 42;
    static long lFld = 42;

    // Integer versions

    static int test_blsiI_rReg_mem_1() {
        return (0 - iFldV) & iFldV;
    }

    static int test_blsiI_rReg_mem_2() {
        int sub = (0 - iFld);
        iFldV++;
        return sub & iFld;
    }

    static int test_blsrI_rReg_mem_1() {
        return (iFldV - 1) & iFldV;
    }

    static int test_blsrI_rReg_mem_2() {
        int sub = (iFld - 1);
        iFldV++;
        return sub & iFld;
    }

    static int test_blsmskI_rReg_mem_1() {
        return (iFldV - 1) ^ iFldV;
    }

    static int test_blsmskI_rReg_mem_2() {
        int sub = (iFld - 1);
        iFldV++;
        return sub ^ iFld;
    }

    // Long versions

    static long test_blsiL_rReg_mem_1() {
        return (0 - lFldV) & lFldV;
    }

    static long test_blsiL_rReg_mem_2() {
        long sub = (0 - lFld);
        lFldV++;
        return sub & lFld;
    }

    static long test_blsrL_rReg_mem_1() {
        return (lFldV - 1) & lFldV;
    }

    static long test_blsrL_rReg_mem_2() {
        long sub = (lFld - 1);
        lFldV++;
        return sub & lFld;
    }

    static long test_blsmskL_rReg_mem_1() {
        return (lFldV - 1) ^ lFldV;
    }

    static long test_blsmskL_rReg_mem_2() {
        long sub = (lFld - 1);
        lFldV++;
        return sub ^ lFld;
    }

    public static void main(String[] args) {
        for (int i = 0;i < 100_000;++i) {
            test_blsiI_rReg_mem_1();
            test_blsiI_rReg_mem_2();
            test_blsrI_rReg_mem_1();
            test_blsrI_rReg_mem_2();
            test_blsmskI_rReg_mem_1();
            test_blsmskI_rReg_mem_2();

            test_blsiL_rReg_mem_1();
            test_blsiL_rReg_mem_2();
            test_blsrL_rReg_mem_1();
            test_blsrL_rReg_mem_2();
            test_blsmskL_rReg_mem_1();
            test_blsmskL_rReg_mem_2();
        }
    }
}
