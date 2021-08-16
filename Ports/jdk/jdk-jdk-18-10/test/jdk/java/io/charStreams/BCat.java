/*
 * Copyright (c) 1997, Oracle and/or its affiliates. All rights reserved.
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
   @summary Simple stream-copy test for BufferedReader and BufferedWriter
 */

import java.io.*;

public class BCat {

    public static int count = 10000;
    public static int chunk = 512;

    public static void main(String[] args) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(new ABCInputStream(count, chunk)),
                                               1025);
        Writer out = new BufferedWriter(new OutputStreamWriter(new ABCOutputStream(count)));
        char[] buf = new char[119];
        int n;

        while ((n = in.read(buf)) != -1) {
            out.write(buf, 0, n);
            System.err.print(" " + n);
        }
        in.close();
        out.close();
        System.err.println();
    }

}
