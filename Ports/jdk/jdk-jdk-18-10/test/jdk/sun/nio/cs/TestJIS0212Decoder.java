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
   @bug 4179800 8042125
   @summary Make sure JIS0212.Decoder really works
 */

import java.nio.*;
import java.nio.charset.*;

public class TestJIS0212Decoder {
    static String outputString = "\u4e02\u4e04\u4e05\u4e0c\u4e12\u4e1f\u4e23";
    static char [] outputChars = new char[8];
    static byte [] inputBytes = new byte[] {(byte)0x30, (byte)0x21, (byte)0x30, (byte)0x22,
                                            (byte)0x30, (byte)0x23, (byte)0x30, (byte)0x24,
                                            (byte)0x30, (byte)0x25, (byte)0x30, (byte)0x26,
                                            (byte)0x30, (byte)0x27};

    public static void main(String args[]) throws Exception {
        test();
    }

    private static void test() throws Exception {
        CharsetDecoder dec = Charset.forName("JIS0212").newDecoder();
        try {
            String ret = dec.decode(ByteBuffer.wrap(inputBytes)).toString();
            if (ret.length() != outputString.length()
                || ! outputString.equals(ret)){
                throw new Exception("JIS0212 decoder does not work correctly");
            }
        } catch (Exception e){
            throw new Exception("JIS0212 encoder does not work correctly");
        }

        // test 0x742c -> u2116 mapping
        if (!"\u2116".equals(new String(new byte[] { (byte)0x8f, (byte)0xf4, (byte)0xac },
                                        "x-eucJP-Open"))) {
            throw new RuntimeException("JIS0212_Solaris nr mapping failed");
        }
    }
}
