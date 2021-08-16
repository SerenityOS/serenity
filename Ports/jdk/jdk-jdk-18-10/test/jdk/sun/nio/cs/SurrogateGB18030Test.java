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
   @bug 4896454
   @summary Check GB18030 surrogate encoding/decoding handling
 */

import java.nio.*;
import java.nio.charset.*;

public class SurrogateGB18030Test {
    public static void main(String[] args) throws Exception {
        SurrogateGB18030Test test = new SurrogateGB18030Test();

        test.roundtripTest();

        /**
         * Valid Surrogate pair and 4 byte GB18030 representation
         */

        String inputString =  "\uD800\uDC00";

        byte[] expectedBytes = { (byte)0x90,
                                 (byte)0x30,
                                 (byte)0x81,
                                 (byte)0x30
                               };
        test.encodeTest(inputString, expectedBytes);

        /**
         * Vice-versa : check that 4 byte GB18030 value encodes correctly
         */

        String expectedStr = "\uDBFF\uDFFF";

        byte[] inputBytes = { (byte)0xe3,
                              (byte)0x32,
                              (byte)0x9a,
                              (byte)0x35
                              };


        test.decodeTest(inputBytes, expectedStr);

    }

    private void roundtripTest() throws Exception
    {
        byte[] ba;
        char[] pair = new char[2];
        for (char high = '\ud800'; high <= '\udbff'; high++) {
            for (char low = '\udc00'; low <= '\udfff'; low++) {
                pair[0] = high;
                pair[1] = low;
                String s = new String(pair);
                if (!s.equals(new String(s.getBytes("gb18030"), "gb18030")))
                    throw new Exception ("GB18030 roundtrip failure");
            }
        }

    }

    private void encodeTest(String inputString, byte[] expectedBytes)
        throws Exception
    {
        byte[] encoded = inputString.getBytes("GB18030");

        CharBuffer cb = CharBuffer.wrap(inputString.toCharArray());
        ByteBuffer bb = ByteBuffer.allocate(4);

        CharsetEncoder encoder = Charset.forName("GB18030").newEncoder();
        encoder.encode(cb, bb, true);

        bb.flip();
        for (int i = 0 ; i < expectedBytes.length; i++) {
            if (encoded[i] != expectedBytes[i]
                || bb.get() != expectedBytes[i])
                    throw new Exception ("GB18030 encode failure");
        }
    }

    private void decodeTest(byte[] inputBytes, String expectedStr)
        throws Exception
    {
        String s2 = new String(inputBytes, "GB18030");

        CharsetDecoder decoder = Charset.forName("GB18030").newDecoder();

        ByteBuffer bb = ByteBuffer.wrap(inputBytes);
        CharBuffer cb = CharBuffer.allocate(2);
        decoder.decode(bb, cb, true);

        cb.flip();
        for (int i = 0 ; i < expectedStr.length(); i++) {
            if (expectedStr.charAt(i) != cb.get()
                || s2.charAt(i) != expectedStr.charAt(i))
                    throw new Exception ("GB18030 encode failure");
        }
    }
}
