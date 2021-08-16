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
 * @bug 4251646
 * @summary Make sure buffer boundary convert works
 * @modules jdk.charsets
 */

import java.nio.*;
import java.nio.charset.*;

public class TestISO2022JPEncoder {
    static char[] inputChars = {'\u0020', '\u0020', '\u0020', '\u0020',
                                '\u0020', '\u0020', '\u0020', '\u0020',
                                '\u0020', '\u4e00'};
    static byte[] expectedBytes1 = {0x20, 0x20, 0x20, 0x20, 0x20,
                                    0x20, 0x20, 0x20, 0x20};
    static byte[] expectedBytes2 = {0x1b, 0x24, 0x42, 0x30, 0x6c,
                                    0x1b, 0x28, 0x42};
    static byte[] outputBuff = new byte[10];

    public static void main(String args[]) throws Exception {
        CharsetEncoder enc = Charset.forName("ISO2022JP").newEncoder();
        CharBuffer cb = CharBuffer.wrap(inputChars);
        ByteBuffer bb = ByteBuffer.wrap(outputBuff);
        CoderResult cr = enc.encode(cb, bb, false);
        if (!cr.isOverflow())
            throw new Exception("Expected CodeResult.OVERFLOW was not returned");
        for (int i = 0; i < expectedBytes1.length; ++i) {
            //System.out.println(expectedBytes1[i] + ":" + outputBuff[i]);
            if (expectedBytes1[i] != outputBuff[i]) {
                throw new Exception("Output bytes does not match at first conversion");
            }
        }
        int nci = cb.position();
        if (nci != expectedBytes1.length)
            throw new Exception("Output length does not match at first conversion");
        bb.clear();
        cr = enc.encode(cb, bb, true);
        enc.flush(bb);
        //System.out.println(ret + "," + expectedBytes2.length);
        bb.flip();
        int len = bb.remaining();
        if (len != expectedBytes2.length)
            throw new Exception("Output length does not match at second conversion");
        for (int i = 0; i < expectedBytes2.length; ++i) {
            //System.out.println(expectedBytes2[i] + ":" + outputBuff[i]);
            if (expectedBytes2[i] != outputBuff[i]) {
                throw new Exception("Output bytes does not match at second conversion");
            }
        }
    }
}
