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
 * @bug 6419791
 * @summary
 * @author Martin Buchholz
 */

import java.io.*;
import java.util.*;
import java.nio.charset.*;
import java.nio.*;

public class ISO8859x {
    final static byte[] lowBytes = new byte[0xa0];
    final static char[] lowChars = new char[0xa0];
    final static String lowString;
    static {
        for (int i = 0; i < 0xa0; i++) {
            lowBytes[i] = (byte) i;
            lowChars[i] = (char) i;
        }
        lowString = new String(lowChars);
    }

    private static void testCharset(Charset cs) throws Throwable {
        String csn = cs.name();
        System.out.println(csn);

        check(cs.canEncode());
        check(Arrays.equals(lowString.getBytes(csn), lowBytes));
        check(new String(lowBytes, csn).equals(lowString));

        CharsetEncoder encoder = cs.newEncoder();
        CharsetDecoder decoder = cs.newDecoder();
        decoder.onUnmappableCharacter(CodingErrorAction.REPORT)
               .onMalformedInput(CodingErrorAction.REPORT);
        encoder.onUnmappableCharacter(CodingErrorAction.REPORT)
               .onMalformedInput(CodingErrorAction.REPORT);

        byte[] bytes = new byte[1];
        for (int c = 0xa0; c < 0x100; c++) {
            try {
                bytes[0] = (byte) c;
                char[] chars;
                try { chars = decoder.decode(ByteBuffer.wrap(bytes)).array(); }
                catch (UnmappableCharacterException x) { continue; }
                equal(chars.length, 1);
                byte[] bytes2 = encoder.encode(CharBuffer.wrap(chars)).array();
                check(Arrays.equals(bytes2, bytes));
            } catch (Throwable t) {
                System.out.printf("cs=%s c=%02x%n", cs, c);
                unexpected(t);
            }
        }
    }

    private static void realMain(String[] args) throws Throwable {
        for (Map.Entry<String,Charset> e
                 : Charset.availableCharsets().entrySet()) {
            String csn = e.getKey();
            Charset cs = e.getValue();
            if (csn.matches(".*(8859).*"))
                try { testCharset(cs); }
                catch (Throwable t) { unexpected(t); }
        }
    }

    //--------------------- Infrastructure ---------------------------
    static volatile int passed = 0, failed = 0;
    static void pass() {passed++;}
    static void fail() {failed++; Thread.dumpStack();}
    static void fail(String msg) {System.out.println(msg); fail();}
    static void unexpected(Throwable t) {failed++; t.printStackTrace();}
    static void check(boolean cond) {if (cond) pass(); else fail();}
    static void equal(Object x, Object y) {
        if (x == null ? y == null : x.equals(y)) pass();
        else fail(x + " not equal to " + y);}
    public static void main(String[] args) throws Throwable {
        try {realMain(args);} catch (Throwable t) {unexpected(t);}
        System.out.printf("%nPassed = %d, failed = %d%n%n", passed, failed);
        if (failed > 0) throw new AssertionError("Some tests failed");}
}
