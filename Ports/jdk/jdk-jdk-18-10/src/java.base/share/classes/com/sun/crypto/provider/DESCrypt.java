/*
 * Copyright (c) 1997, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package com.sun.crypto.provider;

import java.security.InvalidKeyException;

/**
 * This is the internal DES class responsible for encryption and
 * decryption of a byte array of size <code>DES_BLOCK_SIZE</code>.
 *
 * @author Gigi Ankeny
 * @author Jan Luehe
 *
 *
 * @see DESConstants
 * @see DESCipher
 */

class DESCrypt extends SymmetricCipher implements DESConstants {
    private static final int s0p[] = {
        0x00410100, 0x00010000, 0x40400000, 0x40410100, 0x00400000,
        0x40010100, 0x40010000, 0x40400000, 0x40010100, 0x00410100,
        0x00410000, 0x40000100, 0x40400100, 0x00400000, 0x00000000,
        0x40010000, 0x00010000, 0x40000000, 0x00400100, 0x00010100,
        0x40410100, 0x00410000, 0x40000100, 0x00400100, 0x40000000,
        0x00000100, 0x00010100, 0x40410000, 0x00000100, 0x40400100,
        0x40410000, 0x00000000, 0x00000000, 0x40410100, 0x00400100,
        0x40010000, 0x00410100, 0x00010000, 0x40000100, 0x00400100,
        0x40410000, 0x00000100, 0x00010100, 0x40400000, 0x40010100,
        0x40000000, 0x40400000, 0x00410000, 0x40410100, 0x00010100,
        0x00410000, 0x40400100, 0x00400000, 0x40000100, 0x40010000,
        0x00000000, 0x00010000, 0x00400000, 0x40400100, 0x00410100,
        0x40000000, 0x40410000, 0x00000100, 0x40010100,
    };

    private static final int s1p[] = {
        0x08021002, 0x00000000, 0x00021000, 0x08020000, 0x08000002,
        0x00001002, 0x08001000, 0x00021000, 0x00001000, 0x08020002,
        0x00000002, 0x08001000, 0x00020002, 0x08021000, 0x08020000,
        0x00000002, 0x00020000, 0x08001002, 0x08020002, 0x00001000,
        0x00021002, 0x08000000, 0x00000000, 0x00020002, 0x08001002,
        0x00021002, 0x08021000, 0x08000002, 0x08000000, 0x00020000,
        0x00001002, 0x08021002, 0x00020002, 0x08021000, 0x08001000,
        0x00021002, 0x08021002, 0x00020002, 0x08000002, 0x00000000,
        0x08000000, 0x00001002, 0x00020000, 0x08020002, 0x00001000,
        0x08000000, 0x00021002, 0x08001002, 0x08021000, 0x00001000,
        0x00000000, 0x08000002, 0x00000002, 0x08021002, 0x00021000,
        0x08020000, 0x08020002, 0x00020000, 0x00001002, 0x08001000,
        0x08001002, 0x00000002, 0x08020000, 0x00021000,
    };

    private static final int s2p[] = {
        0x20800000, 0x00808020, 0x00000020, 0x20800020, 0x20008000,
        0x00800000, 0x20800020, 0x00008020, 0x00800020, 0x00008000,
        0x00808000, 0x20000000, 0x20808020, 0x20000020, 0x20000000,
        0x20808000, 0x00000000, 0x20008000, 0x00808020, 0x00000020,
        0x20000020, 0x20808020, 0x00008000, 0x20800000, 0x20808000,
        0x00800020, 0x20008020, 0x00808000, 0x00008020, 0x00000000,
        0x00800000, 0x20008020, 0x00808020, 0x00000020, 0x20000000,
        0x00008000, 0x20000020, 0x20008000, 0x00808000, 0x20800020,
        0x00000000, 0x00808020, 0x00008020, 0x20808000, 0x20008000,
        0x00800000, 0x20808020, 0x20000000, 0x20008020, 0x20800000,
        0x00800000, 0x20808020, 0x00008000, 0x00800020, 0x20800020,
        0x00008020, 0x00800020, 0x00000000, 0x20808000, 0x20000020,
        0x20800000, 0x20008020, 0x00000020, 0x00808000,
    };

    private static final int s3p[] = {
        0x00080201, 0x02000200, 0x00000001, 0x02080201, 0x00000000,
        0x02080000, 0x02000201, 0x00080001, 0x02080200, 0x02000001,
        0x02000000, 0x00000201, 0x02000001, 0x00080201, 0x00080000,
        0x02000000, 0x02080001, 0x00080200, 0x00000200, 0x00000001,
        0x00080200, 0x02000201, 0x02080000, 0x00000200, 0x00000201,
        0x00000000, 0x00080001, 0x02080200, 0x02000200, 0x02080001,
        0x02080201, 0x00080000, 0x02080001, 0x00000201, 0x00080000,
        0x02000001, 0x00080200, 0x02000200, 0x00000001, 0x02080000,
        0x02000201, 0x00000000, 0x00000200, 0x00080001, 0x00000000,
        0x02080001, 0x02080200, 0x00000200, 0x02000000, 0x02080201,
        0x00080201, 0x00080000, 0x02080201, 0x00000001, 0x02000200,
        0x00080201, 0x00080001, 0x00080200, 0x02080000, 0x02000201,
        0x00000201, 0x02000000, 0x02000001, 0x02080200,
    };

    private static final int s4p[] = {
        0x01000000, 0x00002000, 0x00000080, 0x01002084, 0x01002004,
        0x01000080, 0x00002084, 0x01002000, 0x00002000, 0x00000004,
        0x01000004, 0x00002080, 0x01000084, 0x01002004, 0x01002080,
        0x00000000, 0x00002080, 0x01000000, 0x00002004, 0x00000084,
        0x01000080, 0x00002084, 0x00000000, 0x01000004, 0x00000004,
        0x01000084, 0x01002084, 0x00002004, 0x01002000, 0x00000080,
        0x00000084, 0x01002080, 0x01002080, 0x01000084, 0x00002004,
        0x01002000, 0x00002000, 0x00000004, 0x01000004, 0x01000080,
        0x01000000, 0x00002080, 0x01002084, 0x00000000, 0x00002084,
        0x01000000, 0x00000080, 0x00002004, 0x01000084, 0x00000080,
        0x00000000, 0x01002084, 0x01002004, 0x01002080, 0x00000084,
        0x00002000, 0x00002080, 0x01002004, 0x01000080, 0x00000084,
        0x00000004, 0x00002084, 0x01002000, 0x01000004,
    };

    private static final int s5p[] = {
        0x10000008, 0x00040008, 0x00000000, 0x10040400, 0x00040008,
        0x00000400, 0x10000408, 0x00040000, 0x00000408, 0x10040408,
        0x00040400, 0x10000000, 0x10000400, 0x10000008, 0x10040000,
        0x00040408, 0x00040000, 0x10000408, 0x10040008, 0x00000000,
        0x00000400, 0x00000008, 0x10040400, 0x10040008, 0x10040408,
        0x10040000, 0x10000000, 0x00000408, 0x00000008, 0x00040400,
        0x00040408, 0x10000400, 0x00000408, 0x10000000, 0x10000400,
        0x00040408, 0x10040400, 0x00040008, 0x00000000, 0x10000400,
        0x10000000, 0x00000400, 0x10040008, 0x00040000, 0x00040008,
        0x10040408, 0x00040400, 0x00000008, 0x10040408, 0x00040400,
        0x00040000, 0x10000408, 0x10000008, 0x10040000, 0x00040408,
        0x00000000, 0x00000400, 0x10000008, 0x10000408, 0x10040400,
        0x10040000, 0x00000408, 0x00000008, 0x10040008,
    };

    private static final int s6p[] = {
        0x00000800, 0x00000040, 0x00200040, 0x80200000, 0x80200840,
        0x80000800, 0x00000840, 0x00000000, 0x00200000, 0x80200040,
        0x80000040, 0x00200800, 0x80000000, 0x00200840, 0x00200800,
        0x80000040, 0x80200040, 0x00000800, 0x80000800, 0x80200840,
        0x00000000, 0x00200040, 0x80200000, 0x00000840, 0x80200800,
        0x80000840, 0x00200840, 0x80000000, 0x80000840, 0x80200800,
        0x00000040, 0x00200000, 0x80000840, 0x00200800, 0x80200800,
        0x80000040, 0x00000800, 0x00000040, 0x00200000, 0x80200800,
        0x80200040, 0x80000840, 0x00000840, 0x00000000, 0x00000040,
        0x80200000, 0x80000000, 0x00200040, 0x00000000, 0x80200040,
        0x00200040, 0x00000840, 0x80000040, 0x00000800, 0x80200840,
        0x00200000, 0x00200840, 0x80000000, 0x80000800, 0x80200840,
        0x80200000, 0x00200840, 0x00200800, 0x80000800,
    };

    private static final int s7p[] = {
        0x04100010, 0x04104000, 0x00004010, 0x00000000, 0x04004000,
        0x00100010, 0x04100000, 0x04104010, 0x00000010, 0x04000000,
        0x00104000, 0x00004010, 0x00104010, 0x04004010, 0x04000010,
        0x04100000, 0x00004000, 0x00104010, 0x00100010, 0x04004000,
        0x04104010, 0x04000010, 0x00000000, 0x00104000, 0x04000000,
        0x00100000, 0x04004010, 0x04100010, 0x00100000, 0x00004000,
        0x04104000, 0x00000010, 0x00100000, 0x00004000, 0x04000010,
        0x04104010, 0x00004010, 0x04000000, 0x00000000, 0x00104000,
        0x04100010, 0x04004010, 0x04004000, 0x00100010, 0x04104000,
        0x00000010, 0x00100010, 0x04004000, 0x04104010, 0x00100000,
        0x04100000, 0x04000010, 0x00104000, 0x00004010, 0x04004010,
        0x04100000, 0x00000010, 0x04104000, 0x00104010, 0x00000000,
        0x04000000, 0x04100010, 0x00004000, 0x00104010,
    };

    private static final int permRight0[] = {
        0x00000000, 0x40000000, 0x00400000, 0x40400000, 0x00004000,
        0x40004000, 0x00404000, 0x40404000, 0x00000040, 0x40000040,
        0x00400040, 0x40400040, 0x00004040, 0x40004040, 0x00404040,
        0x40404040,
    };

    private static final int permLeft1[] = {
        0x00000000, 0x40000000, 0x00400000, 0x40400000, 0x00004000,
        0x40004000, 0x00404000, 0x40404000, 0x00000040, 0x40000040,
        0x00400040, 0x40400040, 0x00004040, 0x40004040, 0x00404040,
        0x40404040,
    };

    private static final int permRight2[] = {
        0x00000000, 0x10000000, 0x00100000, 0x10100000, 0x00001000,
        0x10001000, 0x00101000, 0x10101000, 0x00000010, 0x10000010,
        0x00100010, 0x10100010, 0x00001010, 0x10001010, 0x00101010,
        0x10101010,
    };

    private static final int permLeft3[] = {
        0x00000000, 0x10000000, 0x00100000, 0x10100000, 0x00001000,
        0x10001000, 0x00101000, 0x10101000, 0x00000010, 0x10000010,
        0x00100010, 0x10100010, 0x00001010, 0x10001010, 0x00101010,
        0x10101010,
    };

    private static final int permRight4[] = {
        0x00000000, 0x04000000, 0x00040000, 0x04040000, 0x00000400,
        0x04000400, 0x00040400, 0x04040400, 0x00000004, 0x04000004,
        0x00040004, 0x04040004, 0x00000404, 0x04000404, 0x00040404,
        0x04040404,
    };

    private static final int permLeft5[] = {
        0x00000000, 0x04000000, 0x00040000, 0x04040000, 0x00000400,
        0x04000400, 0x00040400, 0x04040400, 0x00000004, 0x04000004,
        0x00040004, 0x04040004, 0x00000404, 0x04000404, 0x00040404,
        0x04040404,
    };

    private static final int permRight6[] = {
        0x00000000, 0x01000000, 0x00010000, 0x01010000, 0x00000100,
        0x01000100, 0x00010100, 0x01010100, 0x00000001, 0x01000001,
        0x00010001, 0x01010001, 0x00000101, 0x01000101, 0x00010101,
        0x01010101,
    };

    private static final int permLeft7[] = {
        0x00000000, 0x01000000, 0x00010000, 0x01010000, 0x00000100,
        0x01000100, 0x00010100, 0x01010100, 0x00000001, 0x01000001,
        0x00010001, 0x01010001, 0x00000101, 0x01000101, 0x00010101,
        0x01010101,
    };

    private static final int permRight8[] = {
        0x00000000, 0x80000000, 0x00800000, 0x80800000, 0x00008000,
        0x80008000, 0x00808000, 0x80808000, 0x00000080, 0x80000080,
        0x00800080, 0x80800080, 0x00008080, 0x80008080, 0x00808080,
        0x80808080,
    };

    private static final int permLeft9[] = {
        0x00000000, 0x80000000, 0x00800000, 0x80800000, 0x00008000,
        0x80008000, 0x00808000, 0x80808000, 0x00000080, 0x80000080,
        0x00800080, 0x80800080, 0x00008080, 0x80008080, 0x00808080,
        0x80808080,
    };

    private static final int permRightA[] = {
        0x00000000, 0x20000000, 0x00200000, 0x20200000, 0x00002000,
        0x20002000, 0x00202000, 0x20202000, 0x00000020, 0x20000020,
        0x00200020, 0x20200020, 0x00002020, 0x20002020, 0x00202020,
        0x20202020,
    };

    private static final int permLeftB[] = {
        0x00000000, 0x20000000, 0x00200000, 0x20200000, 0x00002000,
        0x20002000, 0x00202000, 0x20202000, 0x00000020, 0x20000020,
        0x00200020, 0x20200020, 0x00002020, 0x20002020, 0x00202020,
        0x20202020,
    };

    private static final int permRightC[] = {
        0x00000000, 0x08000000, 0x00080000, 0x08080000, 0x00000800,
        0x08000800, 0x00080800, 0x08080800, 0x00000008, 0x08000008,
        0x00080008, 0x08080008, 0x00000808, 0x08000808, 0x00080808,
        0x08080808,
    };

    private static final int permLeftD[] = {
        0x00000000, 0x08000000, 0x00080000, 0x08080000, 0x00000800,
        0x08000800, 0x00080800, 0x08080800, 0x00000008, 0x08000008,
        0x00080008, 0x08080008, 0x00000808, 0x08000808, 0x00080808,
        0x08080808,
    };

    private static final int permRightE[] = {
        0x00000000, 0x02000000, 0x00020000, 0x02020000, 0x00000200,
        0x02000200, 0x00020200, 0x02020200, 0x00000002, 0x02000002,
        0x00020002, 0x02020002, 0x00000202, 0x02000202, 0x00020202,
        0x02020202,
    };

    private static final int permLeftF[] = {
        0x00000000, 0x02000000, 0x00020000, 0x02020000, 0x00000200,
        0x02000200, 0x00020200, 0x02020200, 0x00000002, 0x02000002,
        0x00020002, 0x02020002, 0x00000202, 0x02000202, 0x00020202,
        0x02020202,
    };

    /*
     *        Initial Permutation
     */
    private static final int initPermLeft0[] = {
       0x00000000, 0x00008000, 0x00000000, 0x00008000, 0x00000080,
       0x00008080, 0x00000080, 0x00008080, 0x00000000, 0x00008000,
       0x00000000, 0x00008000, 0x00000080, 0x00008080, 0x00000080,
       0x00008080,
    };

    private static final int initPermRight0[] = {
       0x00000000, 0x00000000, 0x00008000, 0x00008000, 0x00000000,
       0x00000000, 0x00008000, 0x00008000, 0x00000080, 0x00000080,
       0x00008080, 0x00008080, 0x00000080, 0x00000080, 0x00008080,
       0x00008080,
    };

    private static final int initPermLeft1[] = {
       0x00000000, 0x80000000, 0x00000000, 0x80000000, 0x00800000,
       0x80800000, 0x00800000, 0x80800000, 0x00000000, 0x80000000,
       0x00000000, 0x80000000, 0x00800000, 0x80800000, 0x00800000,
       0x80800000,
    };

    private static final int initPermRight1[] = {
       0x00000000, 0x00000000, 0x80000000, 0x80000000, 0x00000000,
       0x00000000, 0x80000000, 0x80000000, 0x00800000, 0x00800000,
       0x80800000, 0x80800000, 0x00800000, 0x00800000, 0x80800000,
       0x80800000,
    };

    private static final int initPermLeft2[] = {
       0x00000000, 0x00004000, 0x00000000, 0x00004000, 0x00000040,
       0x00004040, 0x00000040, 0x00004040, 0x00000000, 0x00004000,
       0x00000000, 0x00004000, 0x00000040, 0x00004040, 0x00000040,
       0x00004040,
    };

    private static final int initPermRight2[] = {
       0x00000000, 0x00000000, 0x00004000, 0x00004000, 0x00000000,
       0x00000000, 0x00004000, 0x00004000, 0x00000040, 0x00000040,
       0x00004040, 0x00004040, 0x00000040, 0x00000040, 0x00004040,
       0x00004040,
    };

    private static final int initPermLeft3[] = {
       0x00000000, 0x40000000, 0x00000000, 0x40000000, 0x00400000,
       0x40400000, 0x00400000, 0x40400000, 0x00000000, 0x40000000,
       0x00000000, 0x40000000, 0x00400000, 0x40400000, 0x00400000,
       0x40400000,
    };

    private static final int initPermRight3[] = {
       0x00000000, 0x00000000, 0x40000000, 0x40000000, 0x00000000,
       0x00000000, 0x40000000, 0x40000000, 0x00400000, 0x00400000,
       0x40400000, 0x40400000, 0x00400000, 0x00400000, 0x40400000,
       0x40400000,
    };

    private static final int initPermLeft4[] = {
       0x00000000, 0x00002000, 0x00000000, 0x00002000, 0x00000020,
       0x00002020, 0x00000020, 0x00002020, 0x00000000, 0x00002000,
       0x00000000, 0x00002000, 0x00000020, 0x00002020, 0x00000020,
       0x00002020,
    };

    private static final int initPermRight4[] = {
       0x00000000, 0x00000000, 0x00002000, 0x00002000, 0x00000000,
       0x00000000, 0x00002000, 0x00002000, 0x00000020, 0x00000020,
       0x00002020, 0x00002020, 0x00000020, 0x00000020, 0x00002020,
       0x00002020,
    };

    private static final int initPermLeft5[] = {
       0x00000000, 0x20000000, 0x00000000, 0x20000000, 0x00200000,
       0x20200000, 0x00200000, 0x20200000, 0x00000000, 0x20000000,
       0x00000000, 0x20000000, 0x00200000, 0x20200000, 0x00200000,
       0x20200000,
    };

    private static final int initPermRight5[] = {
       0x00000000, 0x00000000, 0x20000000, 0x20000000, 0x00000000,
       0x00000000, 0x20000000, 0x20000000, 0x00200000, 0x00200000,
       0x20200000, 0x20200000, 0x00200000, 0x00200000, 0x20200000,
       0x20200000,
    };

    private static final int initPermLeft6[] = {
       0x00000000, 0x00001000, 0x00000000, 0x00001000, 0x00000010,
       0x00001010, 0x00000010, 0x00001010, 0x00000000, 0x00001000,
       0x00000000, 0x00001000, 0x00000010, 0x00001010, 0x00000010,
       0x00001010,
    };

    private static final int initPermRight6[] = {
       0x00000000, 0x00000000, 0x00001000, 0x00001000, 0x00000000,
       0x00000000, 0x00001000, 0x00001000, 0x00000010, 0x00000010,
       0x00001010, 0x00001010, 0x00000010, 0x00000010, 0x00001010,
       0x00001010,
    };

    private static final int initPermLeft7[] = {
       0x00000000, 0x10000000, 0x00000000, 0x10000000, 0x00100000,
       0x10100000, 0x00100000, 0x10100000, 0x00000000, 0x10000000,
       0x00000000, 0x10000000, 0x00100000, 0x10100000, 0x00100000,
       0x10100000,
    };

    private static final int initPermRight7[] = {
       0x00000000, 0x00000000, 0x10000000, 0x10000000, 0x00000000,
       0x00000000, 0x10000000, 0x10000000, 0x00100000, 0x00100000,
       0x10100000, 0x10100000, 0x00100000, 0x00100000, 0x10100000,
       0x10100000,
    };

    private static final int initPermLeft8[] = {
       0x00000000, 0x00000800, 0x00000000, 0x00000800, 0x00000008,
       0x00000808, 0x00000008, 0x00000808, 0x00000000, 0x00000800,
       0x00000000, 0x00000800, 0x00000008, 0x00000808, 0x00000008,
       0x00000808,
    };

    private static final int initPermRight8[] = {
       0x00000000, 0x00000000, 0x00000800, 0x00000800, 0x00000000,
       0x00000000, 0x00000800, 0x00000800, 0x00000008, 0x00000008,
       0x00000808, 0x00000808, 0x00000008, 0x00000008, 0x00000808,
       0x00000808,
    };

    private static final int initPermLeft9[] = {
       0x00000000, 0x08000000, 0x00000000, 0x08000000, 0x00080000,
       0x08080000, 0x00080000, 0x08080000, 0x00000000, 0x08000000,
       0x00000000, 0x08000000, 0x00080000, 0x08080000, 0x00080000,
       0x08080000,
    };

    private static final int initPermRight9[] = {
       0x00000000, 0x00000000, 0x08000000, 0x08000000, 0x00000000,
       0x00000000, 0x08000000, 0x08000000, 0x00080000, 0x00080000,
       0x08080000, 0x08080000, 0x00080000, 0x00080000, 0x08080000,
       0x08080000,
    };

    private static final int initPermLeftA[] = {
       0x00000000, 0x00000400, 0x00000000, 0x00000400, 0x00000004,
       0x00000404, 0x00000004, 0x00000404, 0x00000000, 0x00000400,
       0x00000000, 0x00000400, 0x00000004, 0x00000404, 0x00000004,
       0x00000404,
    };

    private static final int initPermRightA[] = {
       0x00000000, 0x00000000, 0x00000400, 0x00000400, 0x00000000,
       0x00000000, 0x00000400, 0x00000400, 0x00000004, 0x00000004,
       0x00000404, 0x00000404, 0x00000004, 0x00000004, 0x00000404,
       0x00000404,
    };

    private static final int initPermLeftB[] = {
       0x00000000, 0x04000000, 0x00000000, 0x04000000, 0x00040000,
       0x04040000, 0x00040000, 0x04040000, 0x00000000, 0x04000000,
       0x00000000, 0x04000000, 0x00040000, 0x04040000, 0x00040000,
       0x04040000,
    };

    private static final int initPermRightB[] = {
       0x00000000, 0x00000000, 0x04000000, 0x04000000, 0x00000000,
       0x00000000, 0x04000000, 0x04000000, 0x00040000, 0x00040000,
       0x04040000, 0x04040000, 0x00040000, 0x00040000, 0x04040000,
       0x04040000,
    };

    private static final int initPermLeftC[] = {
       0x00000000, 0x00000200, 0x00000000, 0x00000200, 0x00000002,
       0x00000202, 0x00000002, 0x00000202, 0x00000000, 0x00000200,
       0x00000000, 0x00000200, 0x00000002, 0x00000202, 0x00000002,
       0x00000202,
    };

    private static final int initPermRightC[] = {
       0x00000000, 0x00000000, 0x00000200, 0x00000200, 0x00000000,
       0x00000000, 0x00000200, 0x00000200, 0x00000002, 0x00000002,
       0x00000202, 0x00000202, 0x00000002, 0x00000002, 0x00000202,
       0x00000202,
    };

    private static final int initPermLeftD[] = {
       0x00000000, 0x02000000, 0x00000000, 0x02000000, 0x00020000,
       0x02020000, 0x00020000, 0x02020000, 0x00000000, 0x02000000,
       0x00000000, 0x02000000, 0x00020000, 0x02020000, 0x00020000,
       0x02020000,
    };

    private static final int initPermRightD[] = {
       0x00000000, 0x00000000, 0x02000000, 0x02000000, 0x00000000,
       0x00000000, 0x02000000, 0x02000000, 0x00020000, 0x00020000,
       0x02020000, 0x02020000, 0x00020000, 0x00020000, 0x02020000,
       0x02020000,
    };

    private static final int initPermLeftE[] = {
       0x00000000, 0x00000100, 0x00000000, 0x00000100, 0x00000001,
       0x00000101, 0x00000001, 0x00000101, 0x00000000, 0x00000100,
       0x00000000, 0x00000100, 0x00000001, 0x00000101, 0x00000001,
       0x00000101,
    };

    private static final int initPermRightE[] = {
       0x00000000, 0x00000000, 0x00000100, 0x00000100, 0x00000000,
       0x00000000, 0x00000100, 0x00000100, 0x00000001, 0x00000001,
       0x00000101, 0x00000101, 0x00000001, 0x00000001, 0x00000101,
       0x00000101,
    };

    private static final int initPermLeftF[] = {
       0x00000000, 0x01000000, 0x00000000, 0x01000000, 0x00010000,
       0x01010000, 0x00010000, 0x01010000, 0x00000000, 0x01000000,
       0x00000000, 0x01000000, 0x00010000, 0x01010000, 0x00010000,
       0x01010000,
    };

    private static final int initPermRightF[] = {
       0x00000000, 0x00000000, 0x01000000, 0x01000000, 0x00000000,
       0x00000000, 0x01000000, 0x01000000, 0x00010000, 0x00010000,
       0x01010000, 0x01010000, 0x00010000, 0x00010000, 0x01010000,
       0x01010000,
    };

    /*
     * the encryption key array after expansion and permutation
     */
    byte[] expandedKey = null;

    /*
     * Are we encrypting or decrypting?
     */
    boolean decrypting = false;

    /**
     * Returns this cipher's block size.
     *
     * @return this cipher's block size
     */
    int getBlockSize() {
        return DES_BLOCK_SIZE;
    }

    void init(boolean decrypting, String algorithm, byte[] rawKey)
            throws InvalidKeyException {
        this.decrypting = decrypting;
        if (!algorithm.equalsIgnoreCase("DES")) {
            throw new InvalidKeyException("Wrong algorithm: DES required");
        }
        if (rawKey.length != DES_BLOCK_SIZE) {
            throw new InvalidKeyException("Wrong key size");
        }
        expandKey(rawKey);
    }

    /**
     * Performs encryption operation.
     *
     * <p>The input plain text <code>plain</code>, starting at
     * <code>plainOffset</code> and ending at
     * <code>(plainOffset + len - 1)</code>, is encrypted.
     * The result is stored in <code>cipher</code>, starting at
     * <code>cipherOffset</code>.
     *
     * <p>The subclass that implements Cipher should ensure that
     * <code>init</code> has been called before this method is called.
     *
     * @param plain the buffer with the input data to be encrypted
     * @param plainOffset the offset in <code>plain</code>
     * @param cipher the buffer for the result
     * @param cipherOffset the offset in <code>cipher</code>
     */
    void encryptBlock(byte[] plain, int plainOffset,
                 byte[] cipher, int cipherOffset)
    {
        cipherBlock(plain, plainOffset, cipher, cipherOffset);
    }

    /**
     * Performs decryption operation.
     *
     * <p>The input cipher text <code>cipher</code>, starting at
     * <code>cipherOffset</code> and ending at
     * <code>(cipherOffset + len - 1)</code>, is decrypted.
     * The result is stored in <code>plain</code>, starting at
     * <code>plainOffset</code>.
     *
     * <p>The subclass that implements Cipher should ensure that
     * <code>init</code> has been called before this method is called.
     *
     * @param cipher the buffer with the input data to be decrypted
     * @param cipherOffset the offset in <code>cipherOffset</code>
     * @param plain the buffer for the result
     * @param plainOffset the offset in <code>plain</code>
     */
    void decryptBlock(byte[] cipher, int cipherOffset,
                 byte[] plain, int plainOffset)
    {
        cipherBlock(cipher, cipherOffset, plain, plainOffset);
    }


    void cipherBlock(byte[] in, int inOffset, byte[] out, int outOffset) {
        byte key[];
        int temp;
        int i, j;
        int offset;
        int left, right;

        left = initialPermutationLeft(in, inOffset);
        right = initialPermutationRight(in, inOffset);

        key = expandedKey;

        if (decrypting) {
            offset = 16 - DES_BLOCK_SIZE;
            j = 128 - DES_BLOCK_SIZE;

        } else {
            offset = 0 - DES_BLOCK_SIZE;
            j = 0;
        }

        for (i = 0; i < 16; i++) {
            // make the first and last bit adjacent
            // move the first bit to the last
            temp = (right << 1) | ((right >> 31) & 1);

            // mangler function:
            // every 6 bit is fed into the sbox, which
            // produces 4-bit output
            left ^= s0p[(temp & 0x3f) ^ key[j+0]]
                ^ s1p[((temp >>  4) & 0x3f) ^ key[j+1]]
                ^ s2p[((temp >>  8) & 0x3f) ^ key[j+2]]
                ^ s3p[((temp >> 12) & 0x3f) ^ key[j+3]]
                ^ s4p[((temp >> 16) & 0x3f) ^ key[j+4]]
                ^ s5p[((temp >> 20) & 0x3f) ^ key[j+5]]
                ^ s6p[((temp >> 24) & 0x3f) ^ key[j+6]];

            // make the last sbox input the last bit from right[0]
            temp = ((right & 1) << 5) | ((right >> 27) & 0x1f);
            left ^= s7p[temp ^ key[j+7]];
            temp = left;
            left = right;
            right = temp;
            j -= offset;
        }

        temp = left;
        left = right;
        right = temp;
        perm(left, right, out, outOffset);
    }

    private static void perm(int left, int right,
                             byte out[], int offset) {
        int low, high, temp;

        temp = left;
        high = permRight0[temp & 0x0000000f];
        temp >>= 4;
        low  = permLeft1[temp & 0x0000000f];
        temp >>= 4;
        high |= permRight2[temp & 0x0000000f];
        temp >>= 4;
        low  |= permLeft3[temp & 0x0000000f];
        temp >>= 4;
        high |= permRight4[temp & 0x0000000f];
        temp >>= 4;
        low  |= permLeft5[temp & 0x0000000f];
        temp >>= 4;
        high |= permRight6[temp & 0x0000000f];
        temp >>= 4;
        low  |= permLeft7[temp & 0x0000000f];

        temp = right;
        high |= permRight8[temp & 0x0000000f];
        temp >>= 4;
        low  |= permLeft9[temp & 0x0000000f];
        temp >>= 4;
        high |= permRightA[temp & 0x0000000f];
        temp >>= 4;
        low  |= permLeftB[temp & 0x0000000f];
        temp >>= 4;
        high |= permRightC[temp & 0x0000000f];
        temp >>= 4;
        low  |= permLeftD[temp & 0x0000000f];
        temp >>= 4;
        high |= permRightE[temp & 0x0000000f];
        temp >>= 4;
        low  |= permLeftF[temp & 0x0000000f];


        out[offset + 0] = (byte)low;
        out[offset + 1] = (byte)(low >> 8);
        out[offset + 2] = (byte)(low >> 16);
        out[offset + 3] = (byte)(low >> 24);
        out[offset + 4] = (byte)high;
        out[offset + 5] = (byte)(high >> 8);
        out[offset + 6] = (byte)(high >> 16);
        out[offset + 7] = (byte)(high >> 24);

    }

    private static int initialPermutationLeft(byte block[], int offset) {
        int l;

        l  = initPermLeft1[block[offset] & 0xf];
        l |= initPermLeft0[(block[offset] >> 4) & 0xf];
        l |= initPermLeft3[block[offset + 1] & 0xf];
        l |= initPermLeft2[(block[offset + 1] >> 4) & 0xf];
        l |= initPermLeft5[block[offset + 2] & 0xf];
        l |= initPermLeft4[(block[offset + 2] >> 4) & 0xf];
        l |= initPermLeft7[block[offset + 3] & 0xf];
        l |= initPermLeft6[(block[offset + 3] >> 4) & 0xf];
        l |= initPermLeft9[block[offset + 4] & 0xf];
        l |= initPermLeft8[(block[offset + 4] >> 4) & 0xf];
        l |= initPermLeftB[block[offset + 5] & 0xf];
        l |= initPermLeftA[(block[offset + 5] >> 4) & 0xf];
        l |= initPermLeftD[block[offset + 6] & 0xf];
        l |= initPermLeftC[(block[offset + 6] >> 4) & 0xf];
        l |= initPermLeftF[block[offset + 7] & 0xf];
        l |= initPermLeftE[(block[offset + 7] >> 4) & 0xf];
        return l;
    }

    private static int initialPermutationRight(byte block[], int offset) {
        int l;

        l  = initPermRight1[block[offset] & 0xf];
        l |= initPermRight0[(block[offset] >> 4) & 0xf];
        l |= initPermRight3[block[offset + 1] & 0xf];
        l |= initPermRight2[(block[offset + 1] >> 4) & 0xf];
        l |= initPermRight5[block[offset + 2] & 0xf];
        l |= initPermRight4[(block[offset + 2] >> 4) & 0xf];
        l |= initPermRight7[block[offset + 3] & 0xf];
        l |= initPermRight6[(block[offset + 3] >> 4) & 0xf];
        l |= initPermRight9[block[offset + 4] & 0xf];
        l |= initPermRight8[(block[offset + 4] >> 4) & 0xf];
        l |= initPermRightB[block[offset + 5] & 0xf];
        l |= initPermRightA[(block[offset + 5] >> 4) & 0xf];
        l |= initPermRightD[block[offset + 6] & 0xf];
        l |= initPermRightC[(block[offset + 6] >> 4) & 0xf];
        l |= initPermRightF[block[offset + 7] & 0xf];
        l |= initPermRightE[(block[offset + 7] >> 4) & 0xf];
        return l;
    }

    void expandKey(byte key[]) {
        int octet;
        byte ek[] = new byte[128];

        octet = key[0];
        if ((octet & 0x80) != 0) {
            ek[  3] |=  2; ek[  9] |=  8; ek[ 18] |=  8;
            ek[ 27] |= 32; ek[ 33] |=  2; ek[ 42] |= 16;
            ek[ 48] |=  8; ek[ 65] |= 16; ek[ 74] |=  2;
            ek[ 80] |=  2; ek[ 89] |=  4; ek[ 99] |= 16;
            ek[104] |=  4; ek[122] |= 32;
        }
        if ((octet & 0x40) != 0) {
            ek[  1] |=  4; ek[  8] |=  1; ek[ 18] |=  4;
            ek[ 25] |= 32; ek[ 34] |= 32; ek[ 41] |=  8;
            ek[ 50] |=  8; ek[ 59] |= 32; ek[ 64] |= 16;
            ek[ 75] |=  4; ek[ 90] |=  1; ek[ 97] |= 16;
            ek[106] |=  2; ek[112] |=  2; ek[123] |=  1;
        }
        if ((octet & 0x20) != 0) {
            ek[  2] |=  1; ek[ 19] |=  8; ek[ 35] |=  1;
            ek[ 40] |=  1; ek[ 50] |=  4; ek[ 57] |= 32;
            ek[ 75] |=  2; ek[ 80] |= 32; ek[ 89] |=  1;
            ek[ 96] |= 16; ek[107] |=  4; ek[120] |=  8;
        }
        if ((octet & 0x10) != 0) {
            ek[  4] |= 32; ek[ 20] |=  2; ek[ 31] |=  4;
            ek[ 37] |= 32; ek[ 47] |=  1; ek[ 54] |=  1;
            ek[ 63] |=  2; ek[ 68] |=  1; ek[ 78] |=  4;
            ek[ 84] |=  8; ek[101] |= 16; ek[108] |=  4;
            ek[119] |= 16; ek[126] |=  8;
        }
        if ((octet & 0x8) != 0) {
            ek[  5] |=  4; ek[ 15] |=  4; ek[ 21] |= 32;
            ek[ 31] |=  1; ek[ 38] |=  1; ek[ 47] |=  2;
            ek[ 53] |=  2; ek[ 68] |=  8; ek[ 85] |= 16;
            ek[ 92] |=  4; ek[103] |= 16; ek[108] |= 32;
            ek[118] |= 32; ek[124] |=  2;
        }
        if ((octet & 0x4) != 0) {
            ek[ 15] |=  2; ek[ 21] |=  2; ek[ 39] |=  8;
            ek[ 46] |= 16; ek[ 55] |= 32; ek[ 61] |=  1;
            ek[ 71] |= 16; ek[ 76] |= 32; ek[ 86] |= 32;
            ek[ 93] |=  4; ek[102] |=  2; ek[108] |= 16;
            ek[117] |=  8; ek[126] |=  1;
        }
        if ((octet & 0x2) != 0) {
            ek[ 14] |= 16; ek[ 23] |= 32; ek[ 29] |=  1;
            ek[ 38] |=  8; ek[ 52] |=  2; ek[ 63] |=  4;
            ek[ 70] |=  2; ek[ 76] |= 16; ek[ 85] |=  8;
            ek[100] |=  1; ek[110] |=  4; ek[116] |=  8;
            ek[127] |=  8;
        }
        octet = key[1];
        if ((octet & 0x80) != 0) {
            ek[  1] |=  8; ek[  8] |= 32; ek[ 17] |=  1;
            ek[ 24] |= 16; ek[ 35] |=  4; ek[ 50] |=  1;
            ek[ 57] |= 16; ek[ 67] |=  8; ek[ 83] |=  1;
            ek[ 88] |=  1; ek[ 98] |=  4; ek[105] |= 32;
            ek[114] |= 32; ek[123] |=  2;
        }
        if ((octet & 0x40) != 0) {
            ek[  0] |=  1; ek[ 11] |= 16; ek[ 16] |=  4;
            ek[ 35] |=  2; ek[ 40] |= 32; ek[ 49] |=  1;
            ek[ 56] |= 16; ek[ 65] |=  2; ek[ 74] |= 16;
            ek[ 80] |=  8; ek[ 99] |=  8; ek[115] |=  1;
            ek[121] |=  4;
        }
        if ((octet & 0x20) != 0) {
            ek[  9] |= 16; ek[ 18] |=  2; ek[ 24] |=  2;
            ek[ 33] |=  4; ek[ 43] |= 16; ek[ 48] |=  4;
            ek[ 66] |= 32; ek[ 73] |=  8; ek[ 82] |=  8;
            ek[ 91] |= 32; ek[ 97] |=  2; ek[106] |= 16;
            ek[112] |=  8; ek[122] |=  1;
        }
        if ((octet & 0x10) != 0) {
            ek[ 14] |= 32; ek[ 21] |=  4; ek[ 30] |=  2;
            ek[ 36] |= 16; ek[ 45] |=  8; ek[ 60] |=  1;
            ek[ 69] |=  2; ek[ 87] |=  8; ek[ 94] |= 16;
            ek[103] |= 32; ek[109] |=  1; ek[118] |=  8;
            ek[124] |= 32;
        }
        if ((octet & 0x8) != 0) {
            ek[  7] |=  4; ek[ 14] |=  2; ek[ 20] |= 16;
            ek[ 29] |=  8; ek[ 44] |=  1; ek[ 54] |=  4;
            ek[ 60] |=  8; ek[ 71] |=  8; ek[ 78] |= 16;
            ek[ 87] |= 32; ek[ 93] |=  1; ek[102] |=  8;
            ek[116] |=  2; ek[125] |=  4;
        }
        if ((octet & 0x4) != 0) {
            ek[  7] |=  2; ek[ 12] |=  1; ek[ 22] |=  4;
            ek[ 28] |=  8; ek[ 45] |= 16; ek[ 52] |=  4;
            ek[ 63] |= 16; ek[ 70] |=  8; ek[ 84] |=  2;
            ek[ 95] |=  4; ek[101] |= 32; ek[111] |=  1;
            ek[118] |=  1;
        }
        if ((octet & 0x2) != 0) {
            ek[  6] |= 16; ek[ 13] |= 16; ek[ 20] |=  4;
            ek[ 31] |= 16; ek[ 36] |= 32; ek[ 46] |= 32;
            ek[ 53] |=  4; ek[ 62] |=  2; ek[ 69] |= 32;
            ek[ 79] |=  1; ek[ 86] |=  1; ek[ 95] |=  2;
            ek[101] |=  2; ek[119] |=  8;
        }
        octet = key[2];
        if ((octet & 0x80) != 0) {
            ek[  0] |= 32; ek[ 10] |=  8; ek[ 19] |= 32;
            ek[ 25] |=  2; ek[ 34] |= 16; ek[ 40] |=  8;
            ek[ 59] |=  8; ek[ 66] |=  2; ek[ 72] |=  2;
            ek[ 81] |=  4; ek[ 91] |= 16; ek[ 96] |=  4;
            ek[115] |=  2; ek[121] |=  8;
        }
        if ((octet & 0x40) != 0) {
            ek[  3] |= 16; ek[ 10] |=  4; ek[ 17] |= 32;
            ek[ 26] |= 32; ek[ 33] |=  8; ek[ 42] |=  8;
            ek[ 51] |= 32; ek[ 57] |=  2; ek[ 67] |=  4;
            ek[ 82] |=  1; ek[ 89] |= 16; ek[ 98] |=  2;
            ek[104] |=  2; ek[113] |=  4; ek[120] |=  1;
        }
        if ((octet & 0x20) != 0) {
            ek[  1] |= 16; ek[ 11] |=  8; ek[ 27] |=  1;
            ek[ 32] |=  1; ek[ 42] |=  4; ek[ 49] |= 32;
            ek[ 58] |= 32; ek[ 67] |=  2; ek[ 72] |= 32;
            ek[ 81] |=  1; ek[ 88] |= 16; ek[ 99] |=  4;
            ek[114] |=  1;
        }
        if ((octet & 0x10) != 0) {
            ek[  6] |= 32; ek[ 12] |=  2; ek[ 23] |=  4;
            ek[ 29] |= 32; ek[ 39] |=  1; ek[ 46] |=  1;
            ek[ 55] |=  2; ek[ 61] |=  2; ek[ 70] |=  4;
            ek[ 76] |=  8; ek[ 93] |= 16; ek[100] |=  4;
            ek[111] |= 16; ek[116] |= 32;
        }
        if ((octet & 0x8) != 0) {
            ek[  6] |=  2; ek[ 13] |= 32; ek[ 23] |=  1;
            ek[ 30] |=  1; ek[ 39] |=  2; ek[ 45] |=  2;
            ek[ 63] |=  8; ek[ 77] |= 16; ek[ 84] |=  4;
            ek[ 95] |= 16; ek[100] |= 32; ek[110] |= 32;
            ek[117] |=  4; ek[127] |=  4;
        }
        if ((octet & 0x4) != 0) {
            ek[  4] |=  1; ek[ 13] |=  2; ek[ 31] |=  8;
            ek[ 38] |= 16; ek[ 47] |= 32; ek[ 53] |=  1;
            ek[ 62] |=  8; ek[ 68] |= 32; ek[ 78] |= 32;
            ek[ 85] |=  4; ek[ 94] |=  2; ek[100] |= 16;
            ek[109] |=  8; ek[127] |=  2;
        }
        if ((octet & 0x2) != 0) {
            ek[  5] |= 16; ek[ 15] |= 32; ek[ 21] |=  1;
            ek[ 30] |=  8; ek[ 44] |=  2; ek[ 55] |=  4;
            ek[ 61] |= 32; ek[ 68] |= 16; ek[ 77] |=  8;
            ek[ 92] |=  1; ek[102] |=  4; ek[108] |=  8;
            ek[126] |= 16;
        }
        octet = key[3];
        if ((octet & 0x80) != 0) {
            ek[  2] |=  8; ek[  9] |=  1; ek[ 16] |= 16;
            ek[ 27] |=  4; ek[ 42] |=  1; ek[ 49] |= 16;
            ek[ 58] |=  2; ek[ 75] |=  1; ek[ 80] |=  1;
            ek[ 90] |=  4; ek[ 97] |= 32; ek[106] |= 32;
            ek[113] |=  8; ek[120] |= 32;
        }
        if ((octet & 0x40) != 0) {
            ek[  2] |=  4; ek[  8] |=  4; ek[ 27] |=  2;
            ek[ 32] |= 32; ek[ 41] |=  1; ek[ 48] |= 16;
            ek[ 59] |=  4; ek[ 66] |= 16; ek[ 72] |=  8;
            ek[ 91] |=  8; ek[107] |=  1; ek[112] |=  1;
            ek[123] |= 16;
        }
        if ((octet & 0x20) != 0) {
            ek[  3] |=  8; ek[ 10] |=  2; ek[ 16] |=  2;
            ek[ 25] |=  4; ek[ 35] |= 16; ek[ 40] |=  4;
            ek[ 59] |=  2; ek[ 65] |=  8; ek[ 74] |=  8;
            ek[ 83] |= 32; ek[ 89] |=  2; ek[ 98] |= 16;
            ek[104] |=  8; ek[121] |= 16;
        }
        if ((octet & 0x10) != 0) {
            ek[  4] |=  2; ek[ 13] |=  4; ek[ 22] |=  2;
            ek[ 28] |= 16; ek[ 37] |=  8; ek[ 52] |=  1;
            ek[ 62] |=  4; ek[ 79] |=  8; ek[ 86] |= 16;
            ek[ 95] |= 32; ek[101] |=  1; ek[110] |=  8;
            ek[126] |= 32;
        }
        if ((octet & 0x8) != 0) {
            ek[  5] |= 32; ek[ 12] |= 16; ek[ 21] |=  8;
            ek[ 36] |=  1; ek[ 46] |=  4; ek[ 52] |=  8;
            ek[ 70] |= 16; ek[ 79] |= 32; ek[ 85] |=  1;
            ek[ 94] |=  8; ek[108] |=  2; ek[119] |=  4;
            ek[126] |=  2;
        }
        if ((octet & 0x4) != 0) {
            ek[  5] |=  2; ek[ 14] |=  4; ek[ 20] |=  8;
            ek[ 37] |= 16; ek[ 44] |=  4; ek[ 55] |= 16;
            ek[ 60] |= 32; ek[ 76] |=  2; ek[ 87] |=  4;
            ek[ 93] |= 32; ek[103] |=  1; ek[110] |=  1;
            ek[119] |=  2; ek[124] |=  1;
        }
        if ((octet & 0x2) != 0) {
            ek[  7] |= 32; ek[ 12] |=  4; ek[ 23] |= 16;
            ek[ 28] |= 32; ek[ 38] |= 32; ek[ 45] |=  4;
            ek[ 54] |=  2; ek[ 60] |= 16; ek[ 71] |=  1;
            ek[ 78] |=  1; ek[ 87] |=  2; ek[ 93] |=  2;
            ek[111] |=  8; ek[118] |= 16; ek[125] |= 16;
        }
        octet = key[4];
        if ((octet & 0x80) != 0) {
            ek[  1] |=  1; ek[ 11] |= 32; ek[ 17] |=  2;
            ek[ 26] |= 16; ek[ 32] |=  8; ek[ 51] |=  8;
            ek[ 64] |=  2; ek[ 73] |=  4; ek[ 83] |= 16;
            ek[ 88] |=  4; ek[107] |=  2; ek[112] |= 32;
            ek[122] |=  8;
        }
        if ((octet & 0x40) != 0) {
            ek[  0] |=  4; ek[  9] |= 32; ek[ 18] |= 32;
            ek[ 25] |=  8; ek[ 34] |=  8; ek[ 43] |= 32;
            ek[ 49] |=  2; ek[ 58] |= 16; ek[ 74] |=  1;
            ek[ 81] |= 16; ek[ 90] |=  2; ek[ 96] |=  2;
            ek[105] |=  4; ek[115] |= 16; ek[122] |=  4;
        }
        if ((octet & 0x20) != 0) {
            ek[  2] |=  2; ek[ 19] |=  1; ek[ 24] |=  1;
            ek[ 34] |=  4; ek[ 41] |= 32; ek[ 50] |= 32;
            ek[ 57] |=  8; ek[ 64] |= 32; ek[ 73] |=  1;
            ek[ 80] |= 16; ek[ 91] |=  4; ek[106] |=  1;
            ek[113] |= 16; ek[123] |=  8;
        }
        if ((octet & 0x10) != 0) {
            ek[  3] |=  4; ek[ 10] |= 16; ek[ 16] |=  8;
            ek[ 35] |=  8; ek[ 51] |=  1; ek[ 56] |=  1;
            ek[ 67] |= 16; ek[ 72] |=  4; ek[ 91] |=  2;
            ek[ 96] |= 32; ek[105] |=  1; ek[112] |= 16;
            ek[121] |=  2;
        }
        if ((octet & 0x8) != 0) {
            ek[  4] |= 16; ek[ 15] |=  1; ek[ 22] |=  1;
            ek[ 31] |=  2; ek[ 37] |=  2; ek[ 55] |=  8;
            ek[ 62] |= 16; ek[ 69] |= 16; ek[ 76] |=  4;
            ek[ 87] |= 16; ek[ 92] |= 32; ek[102] |= 32;
            ek[109] |=  4; ek[118] |=  2; ek[125] |= 32;
        }
        if ((octet & 0x4) != 0) {
            ek[  6] |=  4; ek[ 23] |=  8; ek[ 30] |= 16;
            ek[ 39] |= 32; ek[ 45] |=  1; ek[ 54] |=  8;
            ek[ 70] |= 32; ek[ 77] |=  4; ek[ 86] |=  2;
            ek[ 92] |= 16; ek[101] |=  8; ek[116] |=  1;
            ek[125] |=  2;
        }
        if ((octet & 0x2) != 0) {
            ek[  4] |=  4; ek[ 13] |=  1; ek[ 22] |=  8;
            ek[ 36] |=  2; ek[ 47] |=  4; ek[ 53] |= 32;
            ek[ 63] |=  1; ek[ 69] |=  8; ek[ 84] |=  1;
            ek[ 94] |=  4; ek[100] |=  8; ek[117] |= 16;
            ek[127] |= 32;
        }
        octet = key[5];
        if ((octet & 0x80) != 0) {
            ek[  3] |= 32; ek[  8] |= 16; ek[ 19] |=  4;
            ek[ 34] |=  1; ek[ 41] |= 16; ek[ 50] |=  2;
            ek[ 56] |=  2; ek[ 67] |=  1; ek[ 72] |=  1;
            ek[ 82] |=  4; ek[ 89] |= 32; ek[ 98] |= 32;
            ek[105] |=  8; ek[114] |=  8; ek[121] |=  1;
        }
        if ((octet & 0x40) != 0) {
            ek[  1] |= 32; ek[ 19] |=  2; ek[ 24] |= 32;
            ek[ 33] |=  1; ek[ 40] |= 16; ek[ 51] |=  4;
            ek[ 64] |=  8; ek[ 83] |=  8; ek[ 99] |=  1;
            ek[104] |=  1; ek[114] |=  4; ek[120] |=  4;
        }
        if ((octet & 0x20) != 0) {
            ek[  8] |=  2; ek[ 17] |=  4; ek[ 27] |= 16;
            ek[ 32] |=  4; ek[ 51] |=  2; ek[ 56] |= 32;
            ek[ 66] |=  8; ek[ 75] |= 32; ek[ 81] |=  2;
            ek[ 90] |= 16; ek[ 96] |=  8; ek[115] |=  8;
            ek[122] |=  2;
        }
        if ((octet & 0x10) != 0) {
            ek[  2] |= 16; ek[ 18] |=  1; ek[ 25] |= 16;
            ek[ 34] |=  2; ek[ 40] |=  2; ek[ 49] |=  4;
            ek[ 59] |= 16; ek[ 66] |=  4; ek[ 73] |= 32;
            ek[ 82] |= 32; ek[ 89] |=  8; ek[ 98] |=  8;
            ek[107] |= 32; ek[113] |=  2; ek[123] |=  4;
        }
        if ((octet & 0x8) != 0) {
            ek[  7] |=  1; ek[ 13] |=  8; ek[ 28] |=  1;
            ek[ 38] |=  4; ek[ 44] |=  8; ek[ 61] |= 16;
            ek[ 71] |= 32; ek[ 77] |=  1; ek[ 86] |=  8;
            ek[100] |=  2; ek[111] |=  4; ek[117] |= 32;
            ek[124] |= 16;
        }
        if ((octet & 0x4) != 0) {
            ek[ 12] |=  8; ek[ 29] |= 16; ek[ 36] |=  4;
            ek[ 47] |= 16; ek[ 52] |= 32; ek[ 62] |= 32;
            ek[ 68] |=  2; ek[ 79] |=  4; ek[ 85] |= 32;
            ek[ 95] |=  1; ek[102] |=  1; ek[111] |=  2;
            ek[117] |=  2; ek[126] |=  4;
        }
        if ((octet & 0x2) != 0) {
            ek[  5] |=  1; ek[ 15] |= 16; ek[ 20] |= 32;
            ek[ 30] |= 32; ek[ 37] |=  4; ek[ 46] |=  2;
            ek[ 52] |= 16; ek[ 61] |=  8; ek[ 70] |=  1;
            ek[ 79] |=  2; ek[ 85] |=  2; ek[103] |=  8;
            ek[110] |= 16; ek[119] |= 32; ek[124] |=  4;
        }
        octet = key[6];
        if ((octet & 0x80) != 0) {
            ek[  0] |= 16; ek[  9] |=  2; ek[ 18] |= 16;
            ek[ 24] |=  8; ek[ 43] |=  8; ek[ 59] |=  1;
            ek[ 65] |=  4; ek[ 75] |= 16; ek[ 80] |=  4;
            ek[ 99] |=  2; ek[104] |= 32; ek[113] |=  1;
            ek[123] |= 32;
        }
        if ((octet & 0x40) != 0) {
            ek[ 10] |= 32; ek[ 17] |=  8; ek[ 26] |=  8;
            ek[ 35] |= 32; ek[ 41] |=  2; ek[ 50] |= 16;
            ek[ 56] |=  8; ek[ 66] |=  1; ek[ 73] |= 16;
            ek[ 82] |=  2; ek[ 88] |=  2; ek[ 97] |=  4;
            ek[107] |= 16; ek[112] |=  4; ek[121] |= 32;
        }
        if ((octet & 0x20) != 0) {
            ek[  0] |=  2; ek[ 11] |=  1; ek[ 16] |=  1;
            ek[ 26] |=  4; ek[ 33] |= 32; ek[ 42] |= 32;
            ek[ 49] |=  8; ek[ 58] |=  8; ek[ 65] |=  1;
            ek[ 72] |= 16; ek[ 83] |=  4; ek[ 98] |=  1;
            ek[105] |= 16; ek[114] |=  2;
        }
        if ((octet & 0x10) != 0) {
            ek[  8] |=  8; ek[ 27] |=  8; ek[ 43] |=  1;
            ek[ 48] |=  1; ek[ 58] |=  4; ek[ 64] |=  4;
            ek[ 83] |=  2; ek[ 88] |= 32; ek[ 97] |=  1;
            ek[104] |= 16; ek[115] |=  4; ek[122] |= 16;
        }
        if ((octet & 0x8) != 0) {
            ek[  5] |=  8; ek[ 14] |=  1; ek[ 23] |=  2;
            ek[ 29] |=  2; ek[ 47] |=  8; ek[ 54] |= 16;
            ek[ 63] |= 32; ek[ 68] |=  4; ek[ 79] |= 16;
            ek[ 84] |= 32; ek[ 94] |= 32; ek[101] |=  4;
            ek[110] |=  2; ek[116] |= 16; ek[127] |=  1;
        }
        if ((octet & 0x4) != 0) {
            ek[  4] |=  8; ek[ 15] |=  8; ek[ 22] |= 16;
            ek[ 31] |= 32; ek[ 37] |=  1; ek[ 46] |=  8;
            ek[ 60] |=  2; ek[ 69] |=  4; ek[ 78] |=  2;
            ek[ 84] |= 16; ek[ 93] |=  8; ek[108] |=  1;
            ek[118] |=  4;
        }
        if ((octet & 0x2) != 0) {
            ek[  7] |= 16; ek[ 14] |=  8; ek[ 28] |=  2;
            ek[ 39] |=  4; ek[ 45] |= 32; ek[ 55] |=  1;
            ek[ 62] |=  1; ek[ 76] |=  1; ek[ 86] |=  4;
            ek[ 92] |=  8; ek[109] |= 16; ek[116] |=  4;
            ek[125] |=  1;
        }
        octet = key[7];
        if ((octet & 0x80) != 0) {
            ek[  1] |=  2; ek[ 11] |=  4; ek[ 26] |=  1;
            ek[ 33] |= 16; ek[ 42] |=  2; ek[ 48] |=  2;
            ek[ 57] |=  4; ek[ 64] |=  1; ek[ 74] |=  4;
            ek[ 81] |= 32; ek[ 90] |= 32; ek[ 97] |=  8;
            ek[106] |=  8; ek[115] |= 32; ek[120] |= 16;
        }
        if ((octet & 0x40) != 0) {
            ek[  2] |= 32; ek[ 11] |=  2; ek[ 16] |= 32;
            ek[ 25] |=  1; ek[ 32] |= 16; ek[ 43] |=  4;
            ek[ 58] |=  1; ek[ 75] |=  8; ek[ 91] |=  1;
            ek[ 96] |=  1; ek[106] |=  4; ek[113] |= 32;
        }
        if ((octet & 0x20) != 0) {
            ek[  3] |=  1; ek[  9] |=  4; ek[ 19] |= 16;
            ek[ 24] |=  4; ek[ 43] |=  2; ek[ 48] |= 32;
            ek[ 57] |=  1; ek[ 67] |= 32; ek[ 73] |=  2;
            ek[ 82] |= 16; ek[ 88] |=  8; ek[107] |=  8;
            ek[120] |=  2;
        }
        if ((octet & 0x10) != 0) {
            ek[  0] |=  8; ek[ 10] |=  1; ek[ 17] |= 16;
            ek[ 26] |=  2; ek[ 32] |=  2; ek[ 41] |=  4;
            ek[ 51] |= 16; ek[ 56] |=  4; ek[ 65] |= 32;
            ek[ 74] |= 32; ek[ 81] |=  8; ek[ 90] |=  8;
            ek[ 99] |= 32; ek[105] |=  2; ek[114] |= 16;
        }
        if ((octet & 0x8) != 0) {
            ek[  6] |=  1; ek[ 20] |=  1; ek[ 30] |=  4;
            ek[ 36] |=  8; ek[ 53] |= 16; ek[ 60] |=  4;
            ek[ 69] |=  1; ek[ 78] |=  8; ek[ 92] |=  2;
            ek[103] |=  4; ek[109] |= 32; ek[119] |=  1;
            ek[125] |=  8;
        }
        if ((octet & 0x4) != 0) {
            ek[  7] |=  8; ek[ 21] |= 16; ek[ 28] |=  4;
            ek[ 39] |= 16; ek[ 44] |= 32; ek[ 54] |= 32;
            ek[ 61] |=  4; ek[ 71] |=  4; ek[ 77] |= 32;
            ek[ 87] |=  1; ek[ 94] |=  1; ek[103] |=  2;
            ek[109] |=  2; ek[124] |=  8;
        }
        if ((octet & 0x2) != 0) {
            ek[  6] |=  8; ek[ 12] |= 32; ek[ 22] |= 32;
            ek[ 29] |=  4; ek[ 38] |=  2; ek[ 44] |= 16;
            ek[ 53] |=  8; ek[ 71] |=  2; ek[ 77] |=  2;
            ek[ 95] |=  8; ek[102] |= 16; ek[111] |= 32;
            ek[117] |=  1; ek[127] |= 16;
        }

        expandedKey = ek;
    }
}
