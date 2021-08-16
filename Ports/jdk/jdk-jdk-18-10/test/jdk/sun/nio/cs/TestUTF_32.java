/*
 * Copyright (c) 2008, Oracle and/or its affiliates. All rights reserved.
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
   @bug 6346419
   @summary Check correctness of the UTF-32 and its variant charsets
 */

import java.io.*;
import java.nio.*;
import java.nio.charset.*;

public class TestUTF_32 {
    private static void testDecode(String charset,
                                   String expected,
                                   byte[] input)
        throws Exception
    {
        String out = new String(input, charset);
        if (!out.equals(expected)) {
            failureReport (out, expected);
            throw new Exception("UTF_32 Decoding test failed: " + charset);
        }
    }

    private static void testEncode(String charset,
                                   String input,
                                   byte[] expected)
        throws Exception
    {
        byte[] testBytes = input.getBytes(charset);
        for (int i = 0; i< expected.length; i++)
            if (testBytes[i] != expected[i])
                throw new Exception("UTF_32 Encoding test failed: [" + i + "]"+ charset);

    }

    private static void warn(String s) {
        System.err.println("FAILED Test UTF-32:" +
                            s) ;
    }

    private static void failureReport(String testStr,
                                      String expected) {
        System.err.println ("Expected Characters:");
        for (int i = 0; i < expected.length() ; i++) {
            warn("expected char[" + i + "] : " +
                  Integer.toHexString((int)expected.charAt(i)) +
                 "obtained char[" + i + "] : " +
                  Integer.toHexString((int)testStr.charAt(i)));
        }
    }

    private static void writeInt(OutputStream os, int i, boolean isBig)
        throws Exception
    {
        if (isBig) {
            os.write((i>>24) & 0xff);
            os.write((i>>16) & 0xff);
            os.write((i>>8) & 0xff);
            os.write(i & 0xff);
        } else {
            os.write(i & 0xff);
            os.write((i>>8) & 0xff);
            os.write((i>>16) & 0xff);
            os.write((i>>24) & 0xff);
        }
    }

    private static byte[] getBytes(boolean doBOM, boolean isBig)
        throws Exception
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(1024*1024);
        if (doBOM)
           writeInt(baos, 0xfeff, isBig);

        for (int i = 0; i < 0xffff; i++) {
            if (i < Character.MIN_SURROGATE ||
                i > Character.MAX_SURROGATE)
            writeInt(baos, i, isBig);
        }
        for (int i = 0x10000; i < 0x1ffff; i++) {
            writeInt(baos, i, isBig);
        }

        for (int i = 0x100000; i < 0x10ffff; i++) {
            writeInt(baos, i, isBig);
        }
        byte[] bb = baos.toByteArray();
        baos.close();
        return bb;
    }

    public static void main (String[] args) throws Exception {
        byte[] bb;
        String s;

        // Test 1: UTF_32 BigEndian
        bb = getBytes(false, true);
        s = new String(bb, "UTF-32");
        testDecode("UTF_32", s, bb);
        testEncode("UTF_32", s, bb);

        // Test 2: UTF_32 LittleEndian Decoding With BOM and
        //         BigEndian Encoding
        bb = getBytes(true, false);
        s = new String(bb, "UTF-32");
        bb = getBytes(false, true);
        testDecode("UTF_32", s, bb);
        testEncode("UTF_32", s, bb);


        // Test 3: UTF_32BE
        bb = getBytes(false, true);
        s = new String(bb, "UTF-32BE");
        testDecode("UTF_32BE", s, bb);
        testEncode("UTF_32BE", s, bb);


        // Test 4: UTF_32LE
        bb = getBytes(false, false);
        s = new String(bb, "UTF-32LE");
        testDecode("UTF_32LE", s, bb);
        testEncode("UTF_32LE", s, bb);

        // Test 5: UTF_32BE_BOM
        bb = getBytes(true, true);
        s = new String(bb, "UTF-32BE-BOM");
        testDecode("UTF_32BE_BOM", s, bb);
        testEncode("UTF_32BE_BOM", s, bb);


        // Test 6: UTF_32LE_BOM
        bb = getBytes(true, false);
        s = new String(bb, "UTF-32LE-BOM");
        testDecode("UTF_32LE_BOM", s, bb);
        testEncode("UTF_32LE_BOM", s, bb);

        s = "\u4e00\ufffd\u4e01";
        // Test 7: BigEndian with reverse BOM in middle
        bb = new byte[] {
            (byte)0x00,(byte)0x00,(byte)0x4e,(byte)0x00,
            (byte)0xff,(byte)0xfe,(byte)0x00,(byte)0x00,
            (byte)0x00,(byte)0x00,(byte)0x4e,(byte)0x01
        };
        if (!s.equals(new String(bb, "UTF_32")) ||
            !s.equals(new String(bb, "UTF_32BE")) ||
            !s.equals(new String(bb, "UTF_32BE_BOM")))
            throw new Exception("UTF_32 Decoding test failed: ");

        // Test 7: LittleEndian with reverse BOM in middle
        bb = new byte[] {
            (byte)0xff,(byte)0xfe,(byte)0x00,(byte)0x00,
            (byte)0x00,(byte)0x4e,(byte)0x00,(byte)0x00,
            (byte)0x00,(byte)0x00,(byte)0xfe,(byte)0xff,
            (byte)0x01,(byte)0x4e,(byte)0x00,(byte)0x00
        };
        if (!s.equals(new String(bb, "UTF_32")) ||
            !s.equals(new String(bb, "UTF_32LE")) ||
            !s.equals(new String(bb, "UTF_32LE_BOM")))
            throw new Exception("UTF_32 Decoding test failed: ");

        // Test 8: Overflow
        if (CoderResult.OVERFLOW !=
            Charset.forName("UTF_32")
            .newDecoder()
            .decode((ByteBuffer.allocate(4)
                                 .put(new byte[]
                                      {(byte)0,(byte)1, (byte)0,(byte)01})
                                 .flip()),
                    CharBuffer.allocate(1),
                    true)) {
            throw new Exception ("Test UTF-32 Overflow test failed");
        }
        System.err.println ("OVERALL PASS OF UTF-32 Test");
    }
}
