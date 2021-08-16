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
 * @bug 6797305
 * @summary Add LoadUB and LoadUI opcode class
 *
 * @run main/othervm -Xcomp
 *      -XX:CompileCommand=compileonly,compiler.codegen.Test6797305::load*
 *      compiler.codegen.Test6797305
 */

package compiler.codegen;

public class Test6797305 {
    static final byte[]  ba = new byte[]  { -1 };
    static final short[] sa = new short[] { -1 };
    static final int[]   ia = new int[]   { -1 };
    static final long[]  la = new long[]  { -1 };

    public static void main(String[] args)
    {
        long b = loadB(ba);
        if (b != -1)
            throw new InternalError("loadB failed: " + b + " != " + -1);

        long b2l = loadB2L(ba);
        if (b2l != -1L)
            throw new InternalError("loadB2L failed: " + b2l + " != " + -1L);

        int ub = loadUB(ba);
        if (ub != 0xFF)
            throw new InternalError("loadUB failed: " + ub + " != " + 0xFF);

        int ubmask = loadUBmask(ba);
        if (ubmask != 0xFE)
            throw new InternalError("loadUBmask failed: " + ubmask + " != " + 0xFE);

        long ub2l = loadUB2L(ba);
        if (ub2l != 0xFFL)
            throw new InternalError("loadUB2L failed: " + ub2l + " != " + 0xFFL);

        int s = loadS(sa);
        if (s != -1)
            throw new InternalError("loadS failed: " + s + " != " + -1);

        long s2l = loadS2L(sa);
        if (s2l != -1L)
            throw new InternalError("loadS2L failed: " + s2l + " != " + -1L);

        int us = loadUS(sa);
        if (us != 0xFFFF)
            throw new InternalError("loadUS failed: " + us + " != " + 0xFFFF);

        int usmask = loadUSmask(sa);
        if (usmask != 0xFFFE)
            throw new InternalError("loadUBmask failed: " + ubmask + " != " + 0xFFFE);

        long us2l = loadUS2L(sa);
        if (us2l != 0xFFFFL)
            throw new InternalError("loadUS2L failed: " + us2l + " != " + 0xFFFFL);

        int i = loadI(ia);
        if (i != -1)
            throw new InternalError("loadI failed: " + i + " != " + -1);

        long i2l = loadI2L(ia);
        if (i2l != -1L)
            throw new InternalError("loadI2L failed: " + i2l + " != " + -1L);

        long ui2l = loadUI2L(ia);
        if (ui2l != 0xFFFFFFFFL)
            throw new InternalError("loadUI2L failed: " + ui2l + " != " + 0xFFFFFFFFL);

        long l = loadL(la);
        if (l != -1L)
            throw new InternalError("loadL failed: " + l + " != " + -1L);
    }

    static int  loadB     (byte[] ba)  { return ba[0];               }
    static long loadB2L   (byte[] ba)  { return ba[0];               }
    static int  loadUB    (byte[] ba)  { return ba[0] & 0xFF;        }
    static int  loadUBmask(byte[] ba)  { return ba[0] & 0xFE;        }
    static long loadUB2L  (byte[] ba)  { return ba[0] & 0xFF;        }

    static int  loadS     (short[] sa) { return sa[0];               }
    static long loadS2L   (short[] sa) { return sa[0];               }
    static int  loadUS    (short[] sa) { return sa[0] & 0xFFFF;      }
    static int  loadUSmask(short[] sa) { return sa[0] & 0xFFFE;      }
    static long loadUS2L  (short[] sa) { return sa[0] & 0xFFFF;      }

    static int  loadI     (int[] ia)   { return ia[0];               }
    static long loadI2L   (int[] ia)   { return ia[0];               }
    static long loadUI2L  (int[] ia)   { return ia[0] & 0xFFFFFFFFL; }

    static long loadL     (long[] la)  { return la[0];               }
}
