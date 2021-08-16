/*
 * Copyright (c) 1999, 2005, Oracle and/or its affiliates. All rights reserved.
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

/* @test
   @bug 4241440 4220470
   @summary Test the various two-byte Unicode encodings

   @run main Unicode UnicodeLittle little true
   @run main Unicode UnicodeBig big true
   @run main Unicode UnicodeLittleUnmarked little false
   @run main Unicode UnicodeBigUnmarked big false
   @run main Unicode iso-10646-ucs-2 big false
   @run main Unicode x-utf-16be big false
   @run main Unicode x-utf-16le little false
 */

import java.io.ByteArrayOutputStream;


public class Unicode {

    static final int BOM_HIGH = 0xfe;
    static final int BOM_LOW  = 0xff;

    static final int BIG = 0;
    static final int LITTLE = 1;


    static void fail(String enc, String msg, int e0, int e1, int b0, int b1)
        throws Exception
    {
        throw new Exception(enc + ": " + msg
                            + ": Expected "
                            + Integer.toHexString(e0)
                            + " " + Integer.toHexString(e1)
                            + ", got "
                            + Integer.toHexString(b0)
                            + " " + Integer.toHexString(b1));
    }


    /* Chars to bytes */
    static void encode(String enc, int byteOrder, boolean markExpected)
        throws Exception
    {
        String s = "abc";
        byte[] b = s.getBytes(enc);
        int i = 0;
        if (markExpected) {
            int b0 = b[i++] & 0xff;
            int b1 = b[i++] & 0xff;
            int e0 = 0, e1 = 0;
            if (byteOrder == BIG) {
                e0 = BOM_HIGH;
                e1 = BOM_LOW;
            } else if (byteOrder == LITTLE) {
                e0 = BOM_LOW;
                e1 = BOM_HIGH;
            }
            if ((b0 != e0) || (b1 != e1))
                fail(enc, "Incorrect or missing byte-order mark",
                     e0, e1, b0, b1);
        }
        for (int j = 0; j < s.length(); j++) {
            char c = s.charAt(j);
            int b0 = b[i++] & 0xff;
            int b1 = b[i++] & 0xff;
            int e0 = 0, e1 = 0;
            if (byteOrder == BIG) {
                e0 = c >> 8;
                e1 = c & 0xff;
            } else if (byteOrder == LITTLE) {
                e0 = c & 0xff;
                e1 = c >> 8;
            }
            if ((b0 != e0) || (b1 != e1))
                fail(enc, "Incorrect content at index " + j,
                     e0, e1, b0, b1);
        }
    }


    /* Bytes to chars */
    static void decode(String enc, int byteOrder, boolean markit)
        throws Exception
    {
        String s = "abc";
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        if (markit) {
            if (byteOrder == BIG) {
                bo.write(BOM_HIGH);
                bo.write(BOM_LOW);
            } else if (byteOrder == LITTLE) {
                bo.write(BOM_LOW);
                bo.write(BOM_HIGH);
            }
        }
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (byteOrder == BIG) {
                bo.write(c >> 8);
                bo.write(c & 0xff);
            } else if (byteOrder == LITTLE) {
                bo.write(c & 0xff);
                bo.write(c >> 8);
            }
        }
        byte[] b = bo.toByteArray();
        String s2 = new String(b, enc);
        if (!s.equals(s2))
            throw new Exception(enc + ": Decode error");
    }


    public static void main(String[] args) throws Exception {
        String enc = args[0];
        String bos = args[1];
        boolean markExpected = Boolean.valueOf(args[2]).booleanValue();
        int byteOrder = -1;
        if (bos.equals("big")) byteOrder = BIG;
        if (bos.equals("little")) byteOrder = LITTLE;

        /* We run each test twice in order to check the repeatability of
           String.getBytes and String(byte[], String) (4220470) */
        encode(enc, byteOrder, markExpected);
        encode(enc, byteOrder, markExpected);
        decode(enc, byteOrder, markExpected);
        decode(enc, byteOrder, markExpected);
    }

}
