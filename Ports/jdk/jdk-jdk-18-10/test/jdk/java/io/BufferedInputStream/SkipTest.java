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
 * @test 1.1 98/01/12
 * @bug 4022294
 * @summary Test bufferedinputstream for data loss during skip
 *
 */

import java.io.*;
import java.util.*;

/**
 * This class tests to see if bufferinputstream can be reset
 * to recover data that was skipped over when the buffer did
 * not contain all the bytes to be skipped
 */
public class SkipTest {

    public static void main(String[] args) throws Exception {
        long skipped = 0;

        // Create a tiny buffered stream so it can be easily
        // set up to contain only some of the bytes to skip
        DataSupplier source = new DataSupplier();
        BufferedInputStream in = new BufferedInputStream(source, 4);

        // Set up data to be skipped and recovered
        // the skip must be longer than the buffer size
        in.mark(30);
        while (skipped < 15) {
            skipped += in.skip(15-skipped);
        }
        int nextint = in.read();
        in.reset();

        // Resume reading and see if data was lost
        nextint = in.read();

        if (nextint != 'a')
            throw new RuntimeException("BufferedInputStream skip lost data");
    }
}


class DataSupplier extends InputStream {

    private int aposition=0;

    public int read() {
        return 'x';
    }

    public long skip(long n) {
        aposition += (int) n;
        return n;
    }

    public static final byte[] buffer = {(byte)'a',(byte)'b',(byte)'c',
(byte)'d',(byte)'e',(byte)'f',(byte)'g',(byte)'h',(byte)'i',
(byte)'j',(byte)'k',(byte)'l',(byte)'m',(byte)'n',(byte)'o',
(byte)'p',(byte)'q',(byte)'r',(byte)'s',(byte)'t',(byte)'u',
(byte)'v',(byte)'w',(byte)'x',(byte)'y',(byte)'z'
                                         };

    public int read(byte b[]) throws IOException {
        return read(b, 0, b.length);
    }

    public int read(byte b[], int off, int len) throws IOException {
        if (len > buffer.length) len = buffer.length;
        System.arraycopy(buffer, aposition, b, off, len);
        return len;
    }

}
