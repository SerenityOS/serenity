/*
 * Copyright (c) 1998, Oracle and/or its affiliates. All rights reserved.
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
   @bug 4090383
   @summary Ensure that BufferedReader's read method will fill the target array
            whenever possible
 */


import java.io.IOException;
import java.io.Reader;
import java.io.BufferedReader;


public class Fill {

    /**
     * A simple Reader that is always ready but may read fewer than the
     * requested number of characters
     */
    static class Source extends Reader {

        int shortFall;
        char next = 0;

        Source(int shortFall) {
            this.shortFall = shortFall;
        }

        public int read(char[] cbuf, int off, int len) throws IOException {
            int n = len - shortFall;
            for (int i = off; i < n; i++)
                cbuf[i] = next++;
            return n;
        }

        public boolean ready() {
            return true;
        }

        public void close() throws IOException {
        }

    }

    /**
     * Test BufferedReader with an underlying source that always reads
     * shortFall fewer characters than requested
     */
    static void go(int shortFall) throws Exception {

        Reader r = new BufferedReader(new Source(shortFall), 10);
        char[] cbuf = new char[8];

        int n1 = r.read(cbuf);
        int n2 = r.read(cbuf);
        System.err.println("Shortfall " + shortFall
                           + ": Read " + n1 + ", then " + n2 + " chars");
        if (n1 != cbuf.length)
            throw new Exception("First read returned " + n1);
        if (n2 != cbuf.length)
            throw new Exception("Second read returned " + n2);

    }

    public static void main(String[] args) throws Exception {
        for (int i = 0; i < 8; i++) go(i);
    }

}
