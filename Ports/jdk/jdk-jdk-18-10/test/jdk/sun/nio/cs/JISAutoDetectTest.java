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
 * @test
 * @bug 4087261 4184592
 * @summary Make sure to determine Japanese text encoding as correctly
 * as possible.
 * @modules jdk.charsets
 */

import java.nio.charset.*;
import java.nio.*;

public class JISAutoDetectTest {

    class TestData {
        byte[]  input;
        byte[]  input2;                 // for second call
        String  expectedCharset;
    }
    TestData[] data = new TestData[50];

    public static void main(String[] argv) throws Exception {
        JISAutoDetectTest test =  new JISAutoDetectTest();
        test.execute();
    }

    void execute() throws Exception {
        CharBuffer output = CharBuffer.allocate(128);
        CharBuffer expectedOutput = CharBuffer.allocate(128);

        for (int i = 0; i < data.length; i++) {
            if (data[i] == null)
                break;

            CharsetDecoder autoDetect = Charset.forName("JISAutoDetect").newDecoder();
            CharsetDecoder dec = Charset.forName(data[i].expectedCharset).newDecoder();
            CoderResult ncr, mcr;
            output.clear();
            expectedOutput.clear();
            ncr = autoDetect.decode(ByteBuffer.wrap(data[i].input),
                                    output,
                                    true);
            mcr = dec.decode(ByteBuffer.wrap(data[i].input),
                             expectedOutput,
                             true);

            if (data[i].input2 != null) {
                ncr = autoDetect.decode(ByteBuffer.wrap(data[i].input2),
                                       output,
                                       true);
                mcr = dec.decode(ByteBuffer.wrap(data[i].input2),
                                 expectedOutput,
                                 true);
            }
            String testNumber = " (test#: " + i + ")";
            if (ncr != mcr)
                throw new Exception("JISAutoDetect returned a wrong result");
            output.flip();
            expectedOutput.flip();
            if (output.limit() != expectedOutput.limit())
                throw new Exception("JISAutoDetect returned a wrong length"+testNumber);

            for (int x = 0; x < output.limit(); x++) {
                if (expectedOutput.charAt(x) != output.charAt(x))
                    throw new Exception("JISAutoDetect returned a wrong string"+testNumber);
            }
        }
    }

    public JISAutoDetectTest() {
        int i = 0;

        // 0
        data[i] = new TestData();
        data[i].input = new byte[] { (byte)'C', (byte)'o', (byte)'p', (byte)'y',
                                     (byte)'r', (byte)'i', (byte)'g', (byte)'h',
                                     (byte)'t', (byte)' ', (byte)0xa9, (byte)' ',
                                     (byte)'1', (byte)'9', (byte)'9', (byte)'8' };
        data[i].expectedCharset = "SJIS";

        // 1
        i++;
        data[i] = new TestData();
        data[i].input = new byte[] { (byte)0xbb, (byte)0xdd, (byte)0xcf, (byte)0xb2,
                                     (byte)0xb8, (byte)0xdb, (byte)0xbc, (byte)0xbd,
                                     (byte)0xc3, (byte)0xd1, (byte)0xbd, (byte)0xde,
                                     (byte)0x82, (byte)0xc5, (byte)0x82, (byte)0xb7 };
        data[i].expectedCharset = "SJIS";

        // 2
        i++;
        data[i] = new TestData();
        data[i].input = new byte[] { (byte)0xbb, (byte)0xdd, (byte)0xcf, (byte)0xb2,
                                     (byte)0xb8, (byte)0xdb, (byte)0xbc, (byte)0xbd,
                                     (byte)0xc3, (byte)0xd1, (byte)0xbd, (byte)0xde};
        data[i].expectedCharset = "SJIS";

        // 3
        i++;
        data[i] = new TestData();
        data[i].input = new byte[] { (byte)0xbb, (byte)0xdd, (byte)0xcf, (byte)0xb2,
                                     (byte)0xb8, (byte)0xdb, (byte)0xbc, (byte)0xbd,
                                     (byte)0xc3, (byte)0xd1, (byte)0xbd };
        data[i].expectedCharset = "SJIS";

        // 4
        i++;
        data[i] = new TestData();
        data[i].input = new byte[] { (byte)0x8f, (byte)0xa1, (byte)0xaa };
        data[i].expectedCharset = "SJIS";

        // 5
        i++;
        data[i] = new TestData();
        data[i].input = new byte[] { (byte)0xa4, (byte)0xd2, (byte)0xa4, (byte)0xe9,
                                     (byte)0xa4, (byte)0xac, (byte)0xa4, (byte)0xca };
        data[i].expectedCharset = "EUC_JP";

        // 6
        i++;
        data[i] = new TestData();
        data[i].input = new byte[] { (byte)0xbb, (byte)0xdd, (byte)0xcf, (byte)0xb2,
                                     (byte)0xb8, (byte)0xdb, (byte)0xbc, (byte)0xbd,
                                     (byte)0xc3, (byte)0xd1, (byte)0xbd, (byte)0xde,
                                     (byte)0xa4, (byte)0xc7, (byte)0xa4, (byte)0xb9 };
        data[i].expectedCharset = "EUC_JP";

        // 7 (for 4184592)
        i++;
        data[i] = new TestData();
        data[i].input = new byte[] { (byte)'a', (byte)'b', (byte)'c' };
        data[i].input2 = new byte[] { (byte)0x1b, (byte)'$', (byte)'B',
                                      (byte)'#', (byte)'4', (byte)'$', (byte)'5',
                                      (byte)0x1b, (byte)'(', (byte)'B' };
        data[i].expectedCharset = "ISO2022JP";
    }
}
