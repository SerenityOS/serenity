/*
 * Copyright (c) 2008, 2020, Oracle and/or its affiliates. All rights reserved.
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
 *
 * @summary converted from VM Testbase jit/misctests/t5.
 * VM Testbase keywords: [jit, quick]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm jit.misctests.t5.t5
 */

package jit.misctests.t5;

import nsk.share.TestFailure;

public class t5
{
    public static void main (String [] args)
    {
        byte[] data = new byte[16];
        //foo (data, 0, 16, data, 0);
        foo();

    }

    public static int FF (int a, int b, int c, int d, int x, int s, int ac)
    {
        return (((a += ((b & c) | (~b & d)) + x + ac) << s) | (a >>> (32-s))
+ b);
    }

    //static void foo (byte[] data, int off, int len, byte[] to, int pos)
    static void foo ()
    {
        System.out.println ("Starting foo ...");
        int[] W = new int[16];

        int   a = 0x67452301;
        int   b = 0xefcdab89;
        int   c = 0x98badcfe;
        int   d = 0x10325476;
        for (int i = 0; i < 16; i++) {
            a = FF (a, b, c, d, W[0],   7, 0xd76aa478);
            d = FF (d, a, b, c, W[1],  12, 0xe8c7b756);
            c = FF (c, d, a, b, W[2],  17, 0x242070db);
            b = FF (b, c, d, a, W[3],  22, 0xc1bdceee);
            a = FF (a, b, c, d, W[4],   7, 0xf57c0faf);
            d = FF (d, a, b, c, W[5],  12, 0x4787c62a);
            c = FF (c, d, a, b, W[6],  17, 0xa8304613);
            b = FF (b, c, d, a, W[7],  22, 0xfd469501);
        }
        System.out.println ("foo ended.");
    }
}
