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
 * @bug 6226510
 * @summary Check that ISO-2022-JP's encoder correctly resets to ASCII mode
 * @modules jdk.charsets
 * @author Martin Buchholz
 */

import java.nio.*;
import java.nio.charset.*;

public class ResetISO2022JP {

    public static void main(String[] args) throws Exception {
        if (! (encode(true).equals(encode(false))))
            throw new Exception("Mismatch!");
    }

    static String encode(boolean reuseEncoder) {
        String s = "\u3042\u3043\u3044";

        CharsetEncoder e = Charset.forName("ISO-2022-JP").newEncoder();

        if (reuseEncoder) {
            // I'm turning japanese. Yes I'm turning japanese.  Yes I think so!
            e.encode(CharBuffer.wrap(s), ByteBuffer.allocate(64), true);

            // Should put encoder back into ASCII mode
            e.reset();
        }

        ByteBuffer bb = ByteBuffer.allocate(64);
        e.encode(CharBuffer.wrap(s), bb, true);
        e.flush(bb);
        bb.flip();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bb.limit(); i++)
            sb.append(String.format("%02x ", bb.get(i)));
        System.out.println(sb);
        return sb.toString();
    }
}
