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
 * @bug 4179800
 * @summary Make sure JIS0208.Decoder really works
 */

import java.nio.*;
import java.nio.charset.*;

public class TestJIS0208Decoder {
    static String outputString = "\u65e5\u672c\u8a9e\u30c6\u30ad\u30b9\u30c8";
    static byte [] inputBytes = new byte[] {(byte)'F', (byte)'|', (byte)'K', (byte)'\\',
                                     (byte)'8', (byte)'l', (byte)'%', (byte)'F',
                                     (byte)'%', (byte)'-', (byte)'%', (byte)'9',
                                     (byte)'%', (byte)'H'};

    public static void main(String args[])
        throws Exception
    {
        test();
    }

    private static void test()
        throws Exception
    {
        CharsetDecoder dec = Charset.forName("JIS0208").newDecoder();
        try {
            String ret = dec.decode(ByteBuffer.wrap(inputBytes)).toString();
            if (ret.length() != outputString.length()
                || ! outputString.equals(ret)){
                throw new Exception("ByteToCharJIS0208 does not work correctly");
            }
        }
        catch (Exception e){
            throw new Exception("ByteToCharJIS0208 does not work correctly");
        }
    }
}
