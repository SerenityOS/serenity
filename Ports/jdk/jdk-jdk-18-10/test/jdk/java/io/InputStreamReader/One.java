/*
 * Copyright (c) 2001, Oracle and/or its affiliates. All rights reserved.
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
   @bug 4401798
   @summary Check that single-character reads work properly
 */


import java.io.*;


public class One {

    private abstract static class Test {

        InputStreamReader isr;
        StringBuffer sb;
        String expect;

        Test(byte[] in, String expect) throws Exception {
            isr = new InputStreamReader(new ByteArrayInputStream(in), "UTF-8");
            sb = new StringBuffer(expect.length());
            this.expect = expect;
            go();
        }

        void go() throws Exception {
            read();
            if (!expect.equals(sb.toString()))
                throw new Exception("Expected " + expect
                                    + ", got " + sb.toString());
        }

        abstract void read() throws IOException;

    }


    private static void test(String expect) throws Exception {
        byte[] in = expect.getBytes("UTF-8");

        new Test(in, expect) {
                public void read() throws IOException {
                    for (;;) {
                        int c;
                        if ((c = isr.read()) == -1)
                            break;
                        sb.append((char)c);
                    }
                }};

        new Test(in, expect) {
                public void read() throws IOException {
                    for (;;) {
                        char[] cb = new char[1];
                        if (isr.read(cb) == -1)
                            break;
                        sb.append(cb[0]);
                    }
                }};

        new Test(in, expect) {
                public void read() throws IOException {
                    for (;;) {
                        char[] cb = new char[2];
                        int n;
                        if ((n = isr.read(cb)) == -1)
                            break;
                        sb.append(cb[0]);
                        if (n == 2)
                            sb.append(cb[1]);
                    }
                }};

    }

    public static void main(String[] args) throws Exception {
        test("x");
        test("xy");
        test("xyz");
        test("\ud800\udc00");
        test("x\ud800\udc00");
    }

}
