/*
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
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
   @bug 6227339
   @summary Check if returned CoderResult.unmappableForLength has correct
            length value.
 */

import java.nio.charset.*;
import java.nio.*;

public class TestUnmappableForLength {
    public static void main(String[] argv) throws CharacterCodingException {
            byte[] ba = {(byte)0xa2, (byte)0xff};
            //EUC_TW has its own decodeArrayLoop()
            testDecode("EUC_TW", ba, 2);

            //EUC_CN uses DoubleByteDecoder's decodeArrayLoop()
            testDecode("EUC_CN", ba, 2);
    }

    static void testDecode(String csName, byte[] ba, int expected)
        throws CharacterCodingException
    {
        try {
            CoderResult cr = Charset
                .forName(csName)
                .newDecoder()
                .decode(ByteBuffer.wrap(ba), CharBuffer.allocate(4), true);
            if (cr.isUnmappable() && cr.length() != expected) {
                throw new CharacterCodingException();
            }
        } catch (IllegalArgumentException x){
            x.printStackTrace();
        }
    }

}
