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
   @summary Test interaction of buffer sizes in buffered char and byte streams
 */

import java.io.*;

public class BufferSizes {

    static int min = 90;
    static int max = 110;
    static int chunk = 100;
    static int count = 1000;

    static void runBytes() throws IOException {
        for (int sz = min; sz <= max; sz++) {
            System.err.println(sz);
            InputStream in
                = new BufferedInputStream(new ABCInputStream(count, chunk), sz);
            OutputStream out
                = new BufferedOutputStream(new ABCOutputStream(count), sz);
            int n;
            byte[] buf = new byte[sz];
            while ((n = in.read(buf, 0, sz)) != -1)
                out.write(buf, 0, n);
            in.close();
            out.close();
        }
    }


    static void runChars() throws IOException {
        for (int sz = min; sz <= max; sz++) {
            System.err.println(sz);
            Reader in
                = new BufferedReader(new InputStreamReader(new ABCInputStream(count, chunk)), sz);
            Writer out
                = new BufferedWriter(new OutputStreamWriter(new ABCOutputStream(count)), sz);
            int n;
            char[] cbuf = new char[sz];

            while ((n = in.read(cbuf, 0, sz)) != -1)
                out.write(cbuf, 0, n);
            in.close();
            out.close();
        }
    }

    public static void main(String[] args) throws IOException {
        runBytes();
        runChars();
    }

}
