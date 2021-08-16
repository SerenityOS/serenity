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
 * @bug 6860965
 * @summary Project Coin: binary literals
 */

public class BinaryLiterals {
    public static void main(String... args) throws Exception {
        new BinaryLiterals().run();
    }

    public void run() throws Exception {
        test(0,  0B0);
        test(1,  0B1);
        test(2, 0B10);
        test(3, 0B11);

        test(0,  0b0);
        test(1,  0b1);
        test(2, 0b10);
        test(3, 0b11);

        test(-0,  -0b0);
        test(-1,  -0b1);
        test(-2, -0b10);
        test(-3, -0b11);

        test(-1,  0b11111111111111111111111111111111);
        test(-2,  0b11111111111111111111111111111110);
        test(-3,  0b11111111111111111111111111111101);

        test( 1, -0b11111111111111111111111111111111);
        test( 2, -0b11111111111111111111111111111110);
        test( 3, -0b11111111111111111111111111111101);

        test(0,     0b00);
        test(1,    0b001);
        test(2,  0b00010);
        test(3, 0b000011);

        //                 aaaabbbbccccddddeeeeffffgggghhhh
        test(      0x10,                            0b10000);
        test(     0x100,                        0b100000000);
        test(   0x10000,                0b10000000000000000);
        test(0x80000000, 0b10000000000000000000000000000000);
        test(0xffffffff, 0b11111111111111111111111111111111);

        test(0L,  0b0L);
        test(1L,  0b1L);
        test(2L, 0b10L);
        test(3L, 0b11L);

        test(0,     0b00L);
        test(1,    0b001L);
        test(2,  0b00010L);
        test(3, 0b000011L);

        //                          aaaabbbbccccddddeeeeffffgggghhhhiiiijjjjkkkkllllmmmmnnnnoooopppp
        test(              0x10L,                                                            0b10000L);
        test(             0x100L,                                                        0b100000000L);
        test(           0x10000L,                                                0b10000000000000000L);
        test(        0x80000000L,                                 0b10000000000000000000000000000000L);
        test(        0xffffffffL,                                 0b11111111111111111111111111111111L);
        test(0x8000000000000000L, 0b1000000000000000000000000000000000000000000000000000000000000000L);
        test(0xffffffffffffffffL, 0b1111111111111111111111111111111111111111111111111111111111111111L);

        test(0l,  0b0l);
        test(1l,  0b1l);
        test(2l, 0b10l);
        test(3l, 0b11l);

        test(0,     0b00l);
        test(1,    0b001l);
        test(2,  0b00010l);
        test(3, 0b000011l);

        //                          aaaabbbbccccddddeeeeffffgggghhhhiiiijjjjkkkkllllmmmmnnnnoooopppp
        test(              0x10l,                                                            0b10000l);
        test(             0x100l,                                                        0b100000000l);
        test(           0x10000l,                                                0b10000000000000000l);
        test(        0x80000000l,                                 0b10000000000000000000000000000000l);
        test(        0xffffffffl,                                 0b11111111111111111111111111111111l);
        test(0x8000000000000000l, 0b1000000000000000000000000000000000000000000000000000000000000000l);
        test(0xffffffffffffffffl, 0b1111111111111111111111111111111111111111111111111111111111111111l);

        if (errors > 0)
             throw new Exception(errors + " errors found");
    }

    void test(int expect, int found) {
        count++;
        if (found != expect)
            error("test " + count + "\nexpected: 0x" + Integer.toHexString(expect) + "\n   found: 0x" + Integer.toHexString(found));
    }

    void test(long expect, long found) {
        count++;
        if (found != expect)
            error("test " + count + "\nexpected: 0x" + Long.toHexString(expect) + "\n   found: 0x" + Long.toHexString(found));
    }

    void error(String message) {
        System.out.println(message);
        errors++;
    }

    int count;
    int errors;
}
