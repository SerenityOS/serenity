/*
 * Copyright (c) 2008, 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6379808 8166339
 * @summary Check all Cp933 SBCS characters are not supported in Cp834
 * @modules jdk.charsets
 */

import java.io.*;
import java.nio.*;
import java.nio.charset.*;

public class TestCp834_SBCS {
    public static void main(String args[]) throws Exception {
        // The correctness of 1:1 mapping is Coverted by CoderTest.java
        // and TestConv.java, we only need to verify that SBCS characters
        // are not supported by this charset.
        CharsetEncoder enc834 = Charset.forName("Cp834")
                                       .newEncoder()
                                       .onUnmappableCharacter(CodingErrorAction.REPLACE)
                                       .onMalformedInput(CodingErrorAction.REPLACE);

        CharsetDecoder dec834 = Charset.forName("Cp834")
                                       .newDecoder()
                                       .onUnmappableCharacter(CodingErrorAction.REPLACE)
                                       .onMalformedInput(CodingErrorAction.REPLACE);

        CharsetDecoder dec933 = Charset.forName("Cp933")
                                       .newDecoder()
                                       .onUnmappableCharacter(CodingErrorAction.REPLACE)
                                       .onMalformedInput(CodingErrorAction.REPLACE);
        byte[] ba = new byte[1];
        byte[] ba2 = new byte[2];
        ByteBuffer dbb = ByteBuffer.allocateDirect(10);
        char[] ca = new char[1];
        char c;
        for (int i = 0; i <= 0xff; i++) {
            if (i != 0xe && i != 0xf) {   // no SI/SO
                ba[0] = (byte)i;
                CharBuffer cb = dec933.decode(ByteBuffer.wrap(ba));
                if ((c = cb.get()) != '\ufffd') {
                    // OK, this is a SBCS character in Cp933
                    if (dec834.decode(ByteBuffer.wrap(ba)).get() != '\ufffd')
                        throw new RuntimeException("SBCS is supported in IBM834 decoder");

                    if (enc834.canEncode(c))
                        throw new RuntimeException("SBCS can be encoded in IBM834 encoder");

                    ca[0] = c;
                    ByteBuffer bb = enc834.encode(CharBuffer.wrap(ca));
                    if (bb.get() != (byte)0xfe || bb.get() != (byte)0xfe)
                        throw new RuntimeException("SBCS is supported in IBM834 encoder");
                }
            }
        }

        // 8166339: cp834 should handle unmappable bytes as dobule-byte pair.
        if (! new String("\ufffd".getBytes("cp834"), "cp834").equals("\ufffd")) {
            throw new RuntimeException("u+fffd roundtrip failed");
        }

        if (! new String("a".getBytes("cp834"), "cp834").equals("\ufffd") ||
            ! new String(new byte[] { 0x41, 0x40}, "cp834").equals("\ufffd")) {
            throw new RuntimeException("decoding unmappable don't return u+fffd");
        }

        CoderResult cr = Charset.forName("Cp834").newDecoder().decode(
            ByteBuffer.wrap(new byte[] { 0x41, 0x40}), CharBuffer.wrap(new char[2]), true);
        if (cr.isError() && cr.length() != 2) {
            throw new RuntimeException("decoding unmappable don't return unmmappable(2)");
        }

    }
}
