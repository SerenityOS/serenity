/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.ReadOnlyBufferException;

/*
 * @test
 * @bug 8266078
 * @summary Tests that attempting to read into a read-only CharBuffer
 *          does not advance the Reader position
 * @run main ReadIntoReadOnlyBuffer
 */
public class ReadIntoReadOnlyBuffer {
    private static final String THE_STRING = "123";
    public static void main(String[] args) throws Exception {
        CharBuffer buf = CharBuffer.allocate(8).asReadOnlyBuffer();
        StringReader r = new StringReader(THE_STRING);
        read(r, buf);
        buf = ByteBuffer.allocateDirect(16).asCharBuffer().asReadOnlyBuffer();
        r = new StringReader(THE_STRING);
        read(r, buf);
    }

    private static void read(Reader r, CharBuffer b) throws IOException {
        try {
            r.read(b);
            throw new RuntimeException("ReadOnlyBufferException expected");
        } catch (ReadOnlyBufferException expected) {
        }

        char[] c = new char[3];
        int n = r.read(c);
        if (n != c.length) {
            throw new RuntimeException("Expected " + c.length + ", got " + n);
        }

        String s = new String(c);
        if (!s.equals(THE_STRING)) {
            throw new RuntimeException("Expected " + THE_STRING + ", got " + s);
        }
    }
}
