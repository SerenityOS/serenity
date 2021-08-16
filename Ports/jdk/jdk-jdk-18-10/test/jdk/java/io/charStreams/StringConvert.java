/*
 * Copyright (c) 1997, 2000, Oracle and/or its affiliates. All rights reserved.
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
   @summary General tests of String constructors and methods that convert
            between character encodings.  This should really be in
            java/lang/String, but it shares code with the I/O charStream
            tests.
 */

import java.io.*;


public class StringConvert {

    static String enc = "UTF8";
    static int limit = 500;
    static int max = 0xffff;

    static void fail(String s) {
        throw new RuntimeException(s);
    }

    public static void main(String[] args) throws Exception {
        PrintStream log = System.err;
        IntGenerator ig = new IntGenerator();
        CharGenerator cg;
        StringGenerator sg;
        String s;
        int i = 0;

        /* String(byte[] bytes, String enc)
           getBytes(String enc)
         */
        log.println("-- String(byte[], String), getBytes(String)");
        i = 0;
        cg = new CharGenerator(ig, 0, max);
        sg = new StringGenerator(ig, cg, limit);
        while ((s = sg.next()) != null) {
            byte[] b = s.getBytes(enc);
            String t = new String(b, enc);
            if (!s.equals(t)) {
                int n = Math.min(s.length(), t.length());
                for (int j = 0; j < n; j++) {
                    if (s.charAt(j) != t.charAt(j)) {
                        log.println("Mismatch: " + j + " "
                                    + Integer.toHexString(s.charAt(j))
                                    + " != "
                                    + Integer.toHexString(t.charAt(j)));
                    }
                }
                fail("Conversion failure");
            }
            log.println("[" + i + "] " + s.length());
            i++;
        }

        /* String(byte[] bytes)
           getBytes()
         */
        log.println("-- String(byte[]), getBytes()");
        i = 0;
        cg = new CharGenerator(ig, 0x20, 0x7e);
        sg = new StringGenerator(ig, cg, limit);
        while ((s = sg.next()) != null) {
            log.println("[" + i + "] \"" + s + "\"");
            byte[] b = s.getBytes();
            String t = new String(b);
            if (! s.equals(t))
                fail("Conversion failure");
            i++;
        }

        /* String(byte[] bytes, int offset, int length)
           getBytes()
         */
        log.println("-- String(byte[], int, int), getBytes()");
        i = 0;
        cg = new CharGenerator(ig, 0x20, 0x7e);
        sg = new StringGenerator(ig, cg, limit);
        while ((s = sg.next()) != null) {
            log.println("[" + i + "] \"" + s + "\"");
            byte[] b = s.getBytes();
            int o = ig.next(s.length() - 1);
            int n = ig.next(s.length() - o);
            String t = new String(b, o, n);
            if (! s.substring(o, o + n).equals(t))
                fail("Conversion failure");
            i++;
        }

        /* String(byte[] bytes, int offset, int length, String enc)
           getBytes(String enc)
         */
        log.println("-- String(byte[], int, int, String), getBytes(String)");
        i = 0;
        cg = new CharGenerator(ig);
        sg = new StringGenerator(ig, cg, limit);
        while ((s = sg.next()) != null) {
            log.println("[" + i + "] " + s.length());
            byte[] b = s.getBytes(enc);
            int o = ig.next(100);
            byte[] b2 = new byte[b.length + o];
            System.arraycopy(b, 0, b2, o, b.length);
            String t = new String(b2, o, b.length, enc);
            if (! s.equals(t))
                fail("Conversion failure");
            i++;
        }

        /* Substrings */
        log.println("-- Substrings");
        i = 0;
        cg = new CharGenerator(ig, 0x20, 0x7e);
        sg = new StringGenerator(ig, cg, limit);
        while ((s = sg.next()) != null) {
            log.println("[" + i + "] \"" + s + "\"");
            int o = ig.next(s.length() - 1);
            int n = ig.next(s.length() - o);
            String s2 = s.substring(o, o + n);
            byte[] b = s2.getBytes();
            String t = new String(b);
            if (! s2.equals(t))
                fail("Conversion failure");
            i++;
        }

    }

}
