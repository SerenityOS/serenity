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
 * @bug 4821286
 * @summary Check correctness of CharsetEncoder.isLegalReplacement(byte[])
 */

import java.io.*;
import java.nio.*;
import java.nio.charset.*;
import java.util.*;


public class IsLegalReplacement {

    static PrintStream out = System.err;
    static int errors = 0;

    static String toString(byte[] ba) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < ba.length; i++) {
            byte b = ba[i];
            if (i > 0)
                sb.append(' ');
            sb.append(Integer.toHexString((b >> 4) & 0xf));
            sb.append(Integer.toHexString((b >> 0) & 0xf));
        }
        return sb.toString();
    }

    static CoderResult ilr(String csn, byte[] repl) {
        CharsetDecoder dec = Charset.forName(csn).newDecoder();
        dec.onMalformedInput(CodingErrorAction.REPORT);
        dec.onUnmappableCharacter(CodingErrorAction.REPORT);
        ByteBuffer bb = ByteBuffer.wrap(repl);
        CharBuffer cb = CharBuffer.allocate((int)(bb.remaining()
                                                  * dec.maxCharsPerByte()));
        return dec.decode(bb, cb, true);
    }

    static void test(String csn, byte[] repl, boolean expected)
        throws Exception
    {
        CharsetEncoder enc = Charset.forName(csn).newEncoder();
        out.print(csn + ": " + toString(repl) + ": ");
        if (enc.isLegalReplacement(repl) == expected) {
            out.print("Okay");
        } else {
            out.print("Wrong: Expected " + expected);
            errors++;
        }
        out.println(" (" + ilr(csn, repl) + ")");
    }

    public static void main(String[] args) throws Exception {

        test("UTF-16", new byte [] { (byte)0xd8, 0, (byte)0xdc, 0 }, true);
        test("UTF-16", new byte [] { (byte)0xdc, 0, (byte)0xd8, 0 }, false);
        test("UTF-16", new byte [] { (byte)0xd8, 0 }, false);
        test("UTF-16BE", new byte [] { (byte)0xd8, 0, (byte)0xdc, 0 }, true);
        test("UTF-16BE", new byte [] { (byte)0xdc, 0, (byte)0xd8, 0 }, false);
        test("UTF-16BE", new byte [] { (byte)0xd8, 0 }, false);
        test("UTF-16LE", new byte [] { 0, (byte)0xd8, 0, (byte)0xdc }, true);
        test("UTF-16LE", new byte [] { 0, (byte)0xdc, 0, (byte)0xd8 }, false);
        test("UTF-16LE", new byte [] { 0, (byte)0xd8 }, false);

        if (errors > 0)
            throw new Exception(errors + " error(s) occurred");

    }

}
