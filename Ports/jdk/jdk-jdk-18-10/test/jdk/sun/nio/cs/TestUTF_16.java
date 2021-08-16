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
   @bug 4403848 6348426 6407730
   @summary Check correctness of the UTF-16 converter in all its flavors
 */

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.*;
import java.nio.charset.*;

public class TestUTF_16 {

    private static void testDecode(String charset,
                                   String expected,
                                   byte[] input)
    throws Exception
    {
        String out = new String(input, charset);
        if (!out.equals(expected)) {
            failureReport (out, expected);
            throw new Exception("UTF_16 Decoding test failed " + charset);
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
                throw new Exception("UTF_16 Encoding test failed " + charset);

    }

    private static void warn(String s) {
        System.err.println("FAILED Test 4403848 UTF-16" +
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

    /*
    private static void checkResult(char[] expected,
                                    String testStr,
                                    String testName)
    throws Exception
    {
        if (testStr.length() != expected.length)
            failureReport(testStr, expected);

        for (int i = 0; i < testStr.length(); i++) {
            if (testStr.charAt(i) != expected[i]) {
                failureReport(testStr, expected);
                throw new Exception ("REGTEST TestUTF16 failed: "
                                        + testName);
            }
        }
        System.err.println ("Test " + testName + " PASSED");
        return;
    }
    */

    private static void test() throws Exception  {

            // Tests: Check decoding of UTF-16.
            //        Ensures correct endian polarity
            //        of the decoders and appropriate
            //        interpretation of BOM bytes where
            //        they are required.

            // Test 1: BigEndian UTF-16 Decoding

            testDecode("UTF_16BE", "\u0092\u0093",
                        new byte[] { (byte) 0x00, (byte) 0x92,
                                     (byte) 0x00, (byte) 0x93 });

            // Test 1a: BigEndian UTF-16 Decoding. BOM bytes provided.
            testDecode("UTF_16BE", "\ufeff\u0092\u0093",
                        new byte[] { (byte) 0xfe, (byte) 0xff,
                                     (byte) 0x00, (byte) 0x92,
                                     (byte) 0x00, (byte) 0x93 });

            testDecode("UTF_16LE", "\u9200\u9300",
                        new byte[] { (byte) 0x00, (byte) 0x92,
                                     (byte) 0x00, (byte) 0x93 });

            // Test 2a: LittleEndian  UTF-16 Decoding, BOM bytes provided.
            testDecode("UTF_16LE", "\ufeff\u9200\u9300",
                        new byte[] { (byte) 0xff, (byte) 0xfe,
                                     (byte) 0x00, (byte) 0x92,
                                     (byte) 0x00, (byte) 0x93 });

            // Test 3: UTF-16 (with mandatory byte order mark) Decoding

            testDecode("UTF-16", "\u9200\u9300",
                        new byte[] { (byte) 0xfe, (byte) 0xff,
                                     (byte) 0x92, (byte) 0x00,
                                     (byte) 0x93, (byte) 0x00 });


            // Test 3a: UTF-16 BOM omitted. This should decode OK.
            testDecode("UTF-16", "\u9200\u9300",
                        new byte[] { (byte) 0x92, (byte) 0x00,
                                     (byte) 0x93, (byte) 0x00 });


            // Test 4: encoding using UTF-16
            // BOM must be emitted when encoding and must be BigEndian.

            testEncode("UTF-16", "\u0123",
                        new byte[] { (byte) 0xfe, (byte) 0xff,
                                     (byte) 0x01, (byte) 0x23 });

            // Test 5:
            if (CoderResult.OVERFLOW !=
                Charset.forName("UTF_16")
                .newDecoder()
                .decode((ByteBuffer.allocate(4)
                                     .put(new byte[]
                                          {(byte)0xd8,(byte)0x00,
                                           (byte)0xdc,(byte)0x01})
                                     .flip()),
                        CharBuffer.allocate(1),
                        true)) {
                throw new Exception ("REGTEST TestUTF16 Overflow test failed");
            }

            // Test 6: decoding using UTF_16LE_BOM/UnicodeLittle
            // UnicodeLittle should accept non-BOM byte sequence

            testDecode("UnicodeLittle", "Arial",
                        new byte[] { 'A', 0, 'r', 0, 'i', 0, 'a', 0, 'l', 0});

            System.err.println ("\nPASSED UTF-16 encoder test");

        // Reversed BOM in middle of stream Negative test.

        /*
        boolean caughtException = false;
        try {
                String out = new String(new byte[] {(byte)0x00,
                                            (byte)0x92,
                                            (byte)0xff,
                                            (byte)0xfe},
                                            "UTF-16");
        } catch (IOException e) { caughtException = true; }

        if (caughtException == false)
           throw new Exception ("Incorrectly parsed BOM in middle of input");
        */

            // Fixed included with bug 4403848 fixes buffer sizing
            // issue due to non provision of additional 2 bytes
            // headroom for initial BOM bytes for UTF-16 encoding.
          System.err.println ("OVERALL PASS OF UTF-16 Test");
   }

   public static void main (String[] args) throws Exception {
     test();
   }
}
