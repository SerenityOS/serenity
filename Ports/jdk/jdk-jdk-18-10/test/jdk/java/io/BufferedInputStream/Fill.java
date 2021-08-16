/*
 * Copyright (c) 1998, 2006, Oracle and/or its affiliates. All rights reserved.
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
   @summary Ensure that BufferedInputStream's read method will fill the target
            array whenever possible
 */


import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedInputStream;


public class Fill {

    /**
     * A simple InputStream that is always ready but may read fewer than the
     * requested number of bytes
     */
    static class Source extends InputStream {

        int shortFall;
        byte next = 0;

        Source(int shortFall) {
            this.shortFall = shortFall;
        }

        public int read() throws IOException {
            return next++;
        }

        public int read(byte[] buf, int off, int len) throws IOException {
            int n = len - shortFall;
            for (int i = off; i < n; i++)
                buf[i] = next++;
            return n;
        }

        public int available() {
            return Integer.MAX_VALUE;
        }

        public void close() throws IOException {
        }

    }

    /**
     * Test BufferedInputStream with an underlying source that always reads
     * shortFall fewer bytes than requested
     */
    static void go(int shortFall) throws Exception {

        InputStream r = new BufferedInputStream(new Source(shortFall), 10);
        byte[] buf = new byte[8];

        int n1 = r.read(buf);
        int n2 = r.read(buf);
        System.err.println("Shortfall " + shortFall
                           + ": Read " + n1 + ", then " + n2 + " bytes");
        if (n1 != buf.length)
            throw new Exception("First read returned " + n1);
        if (n2 != buf.length)
            throw new Exception("Second read returned " + n2);

    }

    public static void main(String[] args) throws Exception {
        for (int i = 0; i < 8; i++) go(i);
    }

}
