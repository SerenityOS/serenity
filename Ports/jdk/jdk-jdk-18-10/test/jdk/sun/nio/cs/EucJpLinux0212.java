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
 * @bug 6350021
 * @summary Consistency checks when input buffer contains JISX0212 characters
 * @author Martin Buchholz
 */

import java.io.*;
import java.util.*;
import java.nio.*;
import java.nio.charset.*;

public class EucJpLinux0212 {
    private static void equal(CharBuffer b1, CharBuffer b2) {
        equal(b1.position(), b2.position());
        equal(b1.limit(), b2.limit());
        System.out.printf("positions=%d %d%n", b1.position(), b2.position());
        System.out.printf("limits=%d %d%n", b1.limit(), b2.limit());
        for (int i = b1.position(); i < b1.limit(); i++)
            equal((int)b1.get(i), (int)b2.get(i));
    }

    private static void realMain(String[] args) throws Throwable {
        List<ByteBuffer> bbs = Arrays.asList(
            ByteBuffer.allocate(10),
            ByteBuffer.allocateDirect(10));
        List<CharBuffer> cbs = new ArrayList<CharBuffer>();

        for (ByteBuffer bb : bbs) {
            bb.put(new byte[]{ (byte)0x8f, 0x01, 0x02,
                               (byte)0xa1, (byte)0xc0,
                               0x02, 0x03});
            bb.flip();
            CharsetDecoder decoder = Charset.forName("EUC_JP_LINUX").newDecoder();
            decoder.onUnmappableCharacter(CodingErrorAction.REPLACE);
            CharBuffer cb = decoder.decode(bb);
            cbs.add(cb);
        }
        equal(cbs.get(0), cbs.get(1));
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
