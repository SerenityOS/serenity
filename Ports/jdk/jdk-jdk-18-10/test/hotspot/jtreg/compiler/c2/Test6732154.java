/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6732154
 * @summary REG: Printing an Image using image/gif doc flavor crashes the VM, Solsparc
 *
 * @run main/othervm -Xcomp
 *      -XX:CompileCommand=compileonly,compiler.c2.Test6732154::ascii85Encode
 *      compiler.c2.Test6732154
 */

package compiler.c2;

public class Test6732154 {

    // Exact copy of sun.print.PSPrinterJob.ascii85Encode([b)[b
    private byte[] ascii85Encode(byte[] inArr) {
        byte[]  outArr = new byte[((inArr.length+4) * 5 / 4) + 2];
        long p1 = 85;
        long p2 = p1*p1;
        long p3 = p1*p2;
        long p4 = p1*p3;
        byte pling = '!';

        int i = 0;
        int olen = 0;
        long val, rem;

        while (i+3 < inArr.length) {
            val = ((long)((inArr[i++]&0xff))<<24) +
                  ((long)((inArr[i++]&0xff))<<16) +
                  ((long)((inArr[i++]&0xff))<< 8) +
                  ((long)(inArr[i++]&0xff));
            if (val == 0) {
                outArr[olen++] = 'z';
            } else {
                rem = val;
                outArr[olen++] = (byte)(rem / p4 + pling); rem = rem % p4;
                outArr[olen++] = (byte)(rem / p3 + pling); rem = rem % p3;
                outArr[olen++] = (byte)(rem / p2 + pling); rem = rem % p2;
                outArr[olen++] = (byte)(rem / p1 + pling); rem = rem % p1;
                outArr[olen++] = (byte)(rem + pling);
            }
        }
        // input not a multiple of 4 bytes, write partial output.
        if (i < inArr.length) {
            int n = inArr.length - i; // n bytes remain to be written

            val = 0;
            while (i < inArr.length) {
                val = (val << 8) + (inArr[i++]&0xff);
            }

            int append = 4 - n;
            while (append-- > 0) {
                val = val << 8;
            }
            byte []c = new byte[5];
            rem = val;
            c[0] = (byte)(rem / p4 + pling); rem = rem % p4;
            c[1] = (byte)(rem / p3 + pling); rem = rem % p3;
            c[2] = (byte)(rem / p2 + pling); rem = rem % p2;
            c[3] = (byte)(rem / p1 + pling); rem = rem % p1;
            c[4] = (byte)(rem + pling);

            for (int b = 0; b < n+1 ; b++) {
                outArr[olen++] = c[b];
            }
        }

        // write EOD marker.
        outArr[olen++]='~'; outArr[olen++]='>';

        /* The original intention was to insert a newline after every 78 bytes.
         * This was mainly intended for legibility but I decided against this
         * partially because of the (small) amount of extra space, and
         * partially because for line breaks either would have to hardwire
         * ascii 10 (newline) or calculate space in bytes to allocate for
         * the platform's newline byte sequence. Also need to be careful
         * about where its inserted:
         * Ascii 85 decoder ignores white space except for one special case:
         * you must ensure you do not split the EOD marker across lines.
         */
        byte[] retArr = new byte[olen];
        System.arraycopy(outArr, 0, retArr, 0, olen);
        return retArr;
    }

    public static void main(String[] args) {
        new Test6732154().ascii85Encode(new byte[0]);
        System.out.println("Test passed.");
    }
}
