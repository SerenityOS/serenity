/*
 * Copyright (c) 2008, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6448457
 * @summary Test Channels.newOutputStream returns OutputStream that handles
 *     short writes from the underlying channel
 * @key randomness
 */

import java.io.OutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Random;

public class ShortWrite {

    static Random rand = new Random();
    static int bytesWritten = 0;

    public static void main(String[] args) throws IOException {

        WritableByteChannel wbc = new WritableByteChannel() {
            public int write(ByteBuffer src) {
                int rem = src.remaining();
                if (rem > 0) {
                    // short write
                    int n = rand.nextInt(rem) + 1;
                    src.position(src.position() + n);
                    bytesWritten += n;
                    return n;
                } else {
                    return 0;
                }
            }
            public void close() throws IOException {
                throw new RuntimeException("not implemented");
            }
            public boolean isOpen() {
                throw new RuntimeException("not implemented");
            }
        };

        // wrap Channel with OutputStream
        OutputStream out = Channels.newOutputStream(wbc);


        // write 100, 99, 98, ... 1
        // and check that the expected number of bytes is written
        int expected = 0;
        byte[] buf = new byte[100];
        for (int i=0; i<buf.length; i++) {
            int len = buf.length-i;
            out.write(buf, i, len);
            expected += len;
        }
        System.out.format("Bytes written: %d, expected: %d\n", bytesWritten,
            expected);
        if (bytesWritten != expected)
            throw new RuntimeException("incorrect number of bytes written");

    }
}
