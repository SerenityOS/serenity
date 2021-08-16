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

/* @test
   @bug 4508058 6378911
   @summary Check if UTF8 decoder handles BOM correctly
 */

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.*;
import java.nio.charset.*;

/* The fix for 6378911 is to backout the change we made for 4508058,
   so this regtest is modified accordingly to leave the beginning
   BOM untouched during decoding.
 */
public class TestUTF8BOM {
    private static ByteBuffer bf = ByteBuffer.allocateDirect(1000);
    private static void testDecode(String expected, byte[] input)
        throws Exception
    {
        String out = new String(input, "utf8");
        if (!out.equals(expected)) {
            failureReport (out, expected);
            throw new Exception("UTF_8 Decoding test failed");
        }

        //try directBuffer.
        bf.clear();
        bf.put(input).flip();
        out = Charset.forName("UTF-8")
                     .decode(bf)
                     .toString();
        if (!out.equals(expected)) {
            failureReport (out, expected);
            throw new Exception("UTF_8 Decoding test failed(directbuffer)");
        }
    }

    private static void failureReport(String testStr,
                                      String expected) {

        System.err.println ("Expected Characters:");
        for (int i = 0; i < expected.length() ; i++) {
            System.out.println("expected char[" + i + "] : " +
                              Integer.toHexString((int)expected.charAt(i)) +
                              "  obtained char[" + i + "] : " +
                              Integer.toHexString((int)testStr.charAt(i)));
        }
    }

    public static void main (String[] args) throws Exception {
            // Test 1: with BOM at beginning
            testDecode("\ufeff\u0092\u0093",
                        new byte[] { (byte) 0xef, (byte) 0xbb, (byte) 0xbf,
                                     (byte) 0xc2, (byte) 0x92,
                                     (byte) 0xc2, (byte) 0x93 });
            // Test 2: with BOM at middle
            testDecode("\u9200\ufeff\u9300",
                        new byte[] { (byte) 0xe9, (byte) 0x88, (byte) 0x80,
                                     (byte) 0xef, (byte) 0xbb, (byte) 0xbf,
                                     (byte) 0xe9, (byte) 0x8c, (byte) 0x80 });

            // Test 3: with BOM at end
            testDecode("\u9200\u9300\ufeff",
                        new byte[] { (byte) 0xe9, (byte) 0x88, (byte) 0x80,
                                     (byte) 0xe9, (byte) 0x8c, (byte) 0x80,
                                     (byte) 0xef, (byte) 0xbb, (byte) 0xbf });
            System.err.println ("\nPASSED UTF-8 decode BOM test");
   }
}
