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
 * @bug 4429227
 * @summary Ensure that OutputStreamWriter works on whole multiples
 *          of its internal buffer size
 */

import java.io.*;


public class WriteLengths {

    static PrintStream log = System.err;
    static int failures = 0;

    static ByteArrayOutputStream bos = new ByteArrayOutputStream(1 << 15);

    static void go(int len, String enc) throws Exception {
        bos.reset();
        OutputStreamWriter osw = new OutputStreamWriter(bos, enc);
        char[] cs = new char[len];
        osw.write(cs);
        osw.close();
        byte[] ba = bos.toByteArray();
        if (ba.length != len) {
            log.println("FAIL: Wrote " + len + ", got " + ba.length
                        + "; enc = " + enc);
            failures++;
        }
    }

    public static void main(String[] args) throws Exception {
        for (int i = 0; i < (1 << 15); i += 1024)
            go(i, "us-ascii");
    }

}
