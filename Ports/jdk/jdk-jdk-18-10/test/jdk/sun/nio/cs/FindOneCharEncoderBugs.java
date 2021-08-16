/*
 * Copyright (c) 2008, 2014, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 5058133 6233345 6381699 6381702 6381705 6381706
 * @summary Check that all one-char sequences can be encoded by all charsets
 * @run main/timeout=1200 FindOneCharEncoderBugs
 * @author Martin Buchholz
 */

import java.util.*;
import java.nio.*;
import java.nio.charset.*;

public class FindOneCharEncoderBugs {
    final static String[] brokenCharsets = {
        // Delete the following lines when these charsets are fixed!
        "x-IBM970",
        "x-COMPOUND_TEXT", // Direct buffers not supported
    };

    private static boolean equals(byte[] ba, ByteBuffer bb) {
        if (ba.length != bb.limit())
            return false;
        for (int i = 0; i < ba.length; i++)
            if (ba[i] != bb.get(i))
                return false;
        return true;
    }

    private static String toString(byte[] bytes) {
        final StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            if (sb.length() != 0) sb.append(' ');
            sb.append(String.format("%02x", (int)b));
        }
        return sb.toString();
    }

    private static String toString(ByteBuffer bb) {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bb.limit(); i++) {
            if (sb.length() != 0) sb.append(' ');
            sb.append(String.format("%02x", (int)bb.get(i)));
        }
        return sb.toString();
    }

    private static ByteBuffer convert(Charset cs, char c, CharBuffer cb) throws Throwable {
        cb.clear(); cb.put(c); cb.flip();
        return cs.newEncoder()
            .onUnmappableCharacter(CodingErrorAction.REPLACE)
            .onMalformedInput(CodingErrorAction.REPLACE)
            .encode(cb);
    }

    /** Returns a direct CharBuffer with the same capacity as ordinary CharBuffer ocb */
    private static CharBuffer directCharBuffer(CharBuffer ocb) {
        final CharBuffer dcb =
            ByteBuffer.allocateDirect(ocb.capacity() * Character.SIZE / Byte.SIZE)
            .asCharBuffer();
        check(! ocb.isDirect());
        check(  dcb.isDirect());
        equal(ocb.capacity(), dcb.capacity());
        return dcb;
    }

    private static void testChar(byte[] expected, CharBuffer cb, Charset cs, char c) {
        try {
            final ByteBuffer bb = convert(cs, c, cb);
            if (! equals(expected, bb))
                fail("bytes differ charset=%s direct=%s char=\\u%04x%n%s%n%s",
                     cs, cb.isDirect(), (int)c,
                     toString(expected), toString(bb));
        } catch (Throwable t) {
            System.out.printf("Unexpected exception charset=%s direct=%s char=\\u%04x%n",
                              cs, cb.isDirect(), (int)c);
            unexpected(t);
            failed++;
        }
    }

    private static void testCharset(Charset cs) throws Throwable {
        if (! cs.canEncode())
            return;

        final String csn = cs.name();

        for (String n : brokenCharsets)
            if (csn.equals(n)) {
                System.out.printf("Skipping possibly broken charset %s%n", csn);
                return;
            }
        System.out.println(csn);

        final char[] theChar = new char[1];
        final CharBuffer ocb = CharBuffer.allocate(1);
        final CharBuffer dcb = directCharBuffer(ocb);
        final int maxFailuresPerCharset = 5;
        final int failed0 = failed;

        for (char c = '\u0000';
             (c+1 != 0x10000) && (failed - failed0 < maxFailuresPerCharset);
             c++) {
            theChar[0] = c;
            byte[] bytes = new String(theChar).getBytes(csn);
            if (bytes.length == 0)
                fail("Empty output?! charset=%s char=\\u%04x", cs, (int)c);
            testChar(bytes, ocb, cs, c);
            testChar(bytes, dcb, cs, c);
        }
    }

    private static void realMain(String[] args) {
        for (Charset cs : Charset.availableCharsets().values()) {
            try { testCharset(cs); }
            catch (Throwable t) { unexpected(t); }
        }
    }

    //--------------------- Infrastructure ---------------------------
    static volatile int passed = 0, failed = 0;
    static void pass() {passed++;}
    static void fail() {failed++; Thread.dumpStack();}
    static void fail(String format, Object... args) {
        System.out.println(String.format(format, args)); failed++;}
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
    private static abstract class CheckedThread extends Thread {
        abstract void realRun() throws Throwable;
        public void run() {
            try {realRun();} catch (Throwable t) {unexpected(t);}}}
}
