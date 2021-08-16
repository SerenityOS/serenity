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
   @bug 4160949
   @summary Verify that left over high surrogate does not
   cause an UnknownCharacterException when substitutition mode is turned on.
 */

import java.nio.*;
import java.nio.charset.*;

public class LeftOverSurrogate {

    public static void main(String args[]) throws Exception {
        String s = "abc\uD800\uDC00qrst"; // Valid surrogate
        char[] c = s.toCharArray();
        CharsetEncoder enc = Charset.forName("ISO8859_1").newEncoder()
          .onUnmappableCharacter(CodingErrorAction.REPLACE);
        /* Process the first 4 characters, including the high surrogate
           which should be stored */
        ByteBuffer bb = ByteBuffer.allocate(10);
        CharBuffer cb = CharBuffer.wrap(c);
        cb.limit(4);
        enc.encode(cb, bb, false);
        cb.limit(7);
        enc.encode(cb, bb, true);
        byte[] first = bb.array();
        for(int i = 0; i < 7; i++)
            System.err.printf("[%d]=%d was %d\n",
                              i,
                              (int) first[i] &0xffff,
                              (int) c[i] & 0xffff);
    }
}
