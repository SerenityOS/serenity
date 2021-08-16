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
   @bug 4834154
   @summary Decode a file using EUC-TW, test for decode errors
 */

/*
 * Tests for decode errors in NIO EUC-TW decoder. 4734607 details
 * decoding errors which occur when the input file > 8k in size
 * and contains numerous US-ASCII range chars
 */

import java.io.*;

public class BufferUnderflowEUCTWTest {
    private static int BUFFERSIZE = 8194;

    public static void main (String[] args) throws Exception {
        int i = 0;
        byte[] b = new byte[BUFFERSIZE];

        for (; i < BUFFERSIZE - 4; i++) // pad with zeroes
            b[i] = 0;

        // Overspill a valid EUC-TW 4 byte sequence between 2
        // successive input buffers.
        b[i++] = (byte)0x8E;
        b[i++] = (byte)0xA2;
        b[i++] = (byte)0xA1;
        b[i++] = (byte)0xA6;

        ByteArrayInputStream r = new ByteArrayInputStream(b);

        try {
            InputStreamReader isr=new InputStreamReader(r, "EUC-TW");
            char[] cc = new char[BUFFERSIZE];
            int cx = isr.read(cc);
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new Exception("Array Index error: bug 4834154");
        }
    }
}
