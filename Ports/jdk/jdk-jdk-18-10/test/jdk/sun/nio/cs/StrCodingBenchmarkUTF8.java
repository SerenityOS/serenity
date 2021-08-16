/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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

import java.util.*;
import java.nio.*;
import java.nio.charset.*;

public class StrCodingBenchmarkUTF8 {

    public static void main(String[] args) throws Throwable {

        final int itrs = Integer.getInteger("iterations", 100000);
        final int size = 2048;
        final int subsize    = Integer.getInteger("subsize", 128);
        final Random rnd = new Random();
        final int maxchar    = 0x7f;

        Charset charset = Charset.forName("UTF-8");
        final String csn = charset.name();
        final Charset cs = charset;

        int[] starts = new int[] { 0, 0x80, 0x800, 0x10000};
        for (int nb = 1; nb <= 4; nb++) {

            final CharsetEncoder enc = cs.newEncoder();

            char[] cc = new char[size];
            int i = 0;
            while (i < size - 3) {
                i += Character.toChars(starts[nb - 1] + rnd.nextInt(maxchar), cc, i);
            }

            final String string = new String(cc);
            final byte[] bytes  = string.getBytes(cs);

            System.out.printf("%n--------%s[nb=%d]---------%n", csn, nb);
            int sz = 12;
            while (sz < size) {
                System.out.printf("   [len=%d]%n", sz);
                final byte[] bs  = Arrays.copyOf(bytes, sz);
                final String str = new String(bs, csn);
                StrCodingBenchmark.Job[] jobs = {
                    new StrCodingBenchmark.Job("String decode: csn") {
                    public void work() throws Throwable {
                        for (int i = 0; i < itrs; i++)
                            new String(bs, csn);
                    }},

                    new StrCodingBenchmark.Job("String decode: cs") {
                    public void work() throws Throwable {
                        for (int i = 0; i < itrs; i++)
                            new String(bs, cs);
                    }},

                    new StrCodingBenchmark.Job("String encode: csn") {
                    public void work() throws Throwable {
                        for (int i = 0; i < itrs; i++)
                                str.getBytes(csn);
                    }},

                    new StrCodingBenchmark.Job("String encode: cs") {
                    public void work() throws Throwable {
                         for (int i = 0; i < itrs; i++)
                          str.getBytes(cs);
                    }},
                };
                StrCodingBenchmark.time(StrCodingBenchmark.filter(null, jobs));
                sz <<= 1;
            }
        }
    }
}
