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
 * @bug 4262894 6233303
 * @summary Testing substitute character Escape sequence
 * @modules jdk.charsets
 */

import java.nio.*;
import java.nio.charset.*;

public class TestISO2022JPSubBytes {
    /* \U2460 is not valid character in ISO2022JP and will be substituted
     * with replacement character. If the replacement character is not the
     * "current charset" character, correct escape sequence should be output
     * for changing character set.
     */
    static char[][] in = { {'\u25cb', '\u2460', '\u25cb'},
                           {'\u0061', '\u2460', '\u0061'},
                           {'\u25cb', '\u2460', '\u25cb'},
                           {'\u0061', '\u2460', '\u0061'},
                         };
    static byte[][] expected = { {0x1b, 0x24, 0x42, 0x21, 0x7b,
                                  0x21, 0x29,
                                  0x21, 0x7b,
                                  0x1b, 0x28, 0x42},
                                 {0x61,
                                  0x1b, 0x24, 0x42, 0x21, 0x29,
                                  0x1b, 0x28, 0x42, 0x61},
                                 {0x1b, 0x24, 0x42, 0x21, 0x7b,
                                  0x1b, 0x28, 0x42, 0x3f,
                                  0x1b, 0x24, 0x42, 0x21, 0x7b,
                                  0x1b, 0x28, 0x42},
                                 {0x61,
                                  0x3f,
                                  0x61}
                                };

    public static void main(String args[]) throws Exception {
        CharsetEncoder enc = Charset.forName("ISO2022JP")
          .newEncoder()
          .onUnmappableCharacter(CodingErrorAction.REPLACE);

        test(enc, in[0], expected[0]);

        enc.reset();
        test(enc, in[1], expected[1]);

        enc.reset();
        enc.replaceWith(new byte[]{(byte)'?'});
        test(enc, in[2], expected[2]);

        enc.reset();
        test(enc, in[3], expected[3]);
    }

    public static void test (CharsetEncoder enc,
                             char[] inputChars,
                             byte[] expectedBytes) throws Exception
    {
        ByteBuffer bb = ByteBuffer.allocate(expectedBytes.length);
        enc.encode(CharBuffer.wrap(inputChars), bb, true);
        enc.flush(bb);
        bb.flip();
        byte[] outputBuff = bb.array();
        int outputLen = bb.limit();
        if (outputLen != expectedBytes.length) {
            throw new Exception("Output bytes does not match");
        }
        for (int i = 0; i < outputLen; ++i) {
            System.out.printf("<%x:%x> ",
                              expectedBytes[i] & 0xff,
                              outputBuff[i] & 0xff);
            if (expectedBytes[i] != outputBuff[i]) {
                System.out.println("...");
                throw new Exception("Output bytes does not match");
            }
        }
        System.out.println();
    }
}
