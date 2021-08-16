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

/*
 * @test
 * @bug 4054043
 * @summary Test bufferedinputstream when stream is interrupted
 *
 */

import java.io.*;

/**
 * This class tests to see if bufferinputstream updates count
 * when the stream is interrupted and restarted
 * It was adapted from a test class provided in the bug report
 *
 */

public class CountUpdate {

    public static void main(String[] args) throws Exception {
        BufferBreaker breaker = new BufferBreaker();
        BufferedInputStream in = new BufferedInputStream(breaker, 1000);

        byte b[] = new byte[100];
        int total = 0;

        for (int i=0; i<5; i++) {

            if (i>0) breaker.breakIt = true;
            try {
                int n = in.read(b);
                total += n;
                //System.out.print("read "+n+" bytes: [");
                //System.out.write(b, 0, n);
                //System.out.println("]");
            }
            catch (IOException e) {
                //System.out.println(e);
            }
        }

        if (total>7)
            throw new RuntimeException(
                            "BufferedInputStream did not reset count.");
    }
}

class BufferBreaker extends InputStream {
    public boolean breakIt = false;

    public int read() {
        return 'x';
    }

    public static final byte[] buffer = {(byte)'a',
                                         (byte)'b',
                                         (byte)'c',
                                         (byte)'d',
                                         (byte)'e',
                                         (byte)'f',
                                         (byte)'g'};

    public int read(byte b[]) throws IOException {
        return read(b, 0, b.length);
    }

    public int read(byte b[], int off, int len) throws IOException {
        if (breakIt) throw new IOException("BREAK");
        if (len > buffer.length) len = buffer.length;
        System.arraycopy(buffer, 0, b, off, len);
        return len;
    }

    public long skip(long n) {
        return 0;
    }

    public int available() {
        return 0;
    }

}
