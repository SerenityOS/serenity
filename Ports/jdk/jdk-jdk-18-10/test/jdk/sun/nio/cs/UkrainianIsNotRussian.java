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
   @bug 4890726
   @summary Check the correctness of KOI8_U by comparing to KOI8_R
 */

import java.util.*;
import static java.lang.Character.UnicodeBlock;

public class UkrainianIsNotRussian {
    private static String decode(byte[] bytes, String encoding) throws Throwable {
        String s = new String(bytes, encoding);
        equal(s.length(), 1);
        check(Arrays.equals(s.getBytes(encoding), bytes));
        return s;
    }

    private static void realMain(String[] args) throws Throwable {
        final byte[] bytes = new byte[1];
        int differences = 0;
        for (int i = 0; i < 0xff; i++) {
            bytes[0] = (byte) i;
            final String r = decode(bytes, "KOI8_R");
            final String u = decode(bytes, "KOI8_U");
            if (! r.equals(u)) {
                differences++;
                final char rc = r.charAt(0);
                final char uc = u.charAt(0);
                final UnicodeBlock rcb = UnicodeBlock.of(rc);
                final UnicodeBlock ucb = UnicodeBlock.of(uc);
                System.out.printf("%02x => %04x %s, %04x %s%n",
                                  i, (int) rc, rcb, (int) uc, ucb);
                check(rcb == UnicodeBlock.BOX_DRAWING &&
                      ucb == UnicodeBlock.CYRILLIC);
            }
        }
        equal(differences, 8);
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
