/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7129312
 * @requires (sun.arch.data.model == "64" & os.maxMemory > 4g)
 * @summary BufferedInputStream calculates negative array size with large
 *          streams and mark
 * @run main/othervm -Xmx4G -Xlog:gc,gc+heap,gc+ergo+heap -XX:+CrashOnOutOfMemoryError
                     -XX:+IgnoreUnrecognizedVMOptions -XX:+G1ExitOnExpansionFailure
                     -Xlog:cds=debug
                     LargeCopyWithMark
 */

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class LargeCopyWithMark {

    static final int BUFF_SIZE = 8192;
    static final int BIS_BUFF_SIZE = Integer.MAX_VALUE / 2 + 100;
    static final long BYTES_TO_COPY = 2L * Integer.MAX_VALUE;

    static {
        assert BIS_BUFF_SIZE * 2 < 0 : "doubling must overflow";
    }

    public static void main(String[] args) throws Exception {
        byte[] buff = new byte[BUFF_SIZE];

        try (InputStream myis = new MyInputStream(BYTES_TO_COPY);
             InputStream bis = new BufferedInputStream(myis, BIS_BUFF_SIZE);
             OutputStream myos = new MyOutputStream()) {

            // will require a buffer bigger than BIS_BUFF_SIZE
            bis.mark(BIS_BUFF_SIZE + 100);

            for (;;) {
                int count = bis.read(buff, 0, BUFF_SIZE);
                if (count == -1)
                    break;
                myos.write(buff, 0, count);
            }
        }
    }
}

class MyInputStream extends InputStream {
    private long bytesLeft;
    public MyInputStream(long bytesLeft) {
        this.bytesLeft = bytesLeft;
    }
    @Override public int read() throws IOException {
        return 0;
    }
    @Override public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }
    @Override public int read(byte[] b, int off, int len) throws IOException {
        if (bytesLeft <= 0)
            return -1;
        long result = Math.min(bytesLeft, (long)len);
        bytesLeft -= result;
        return (int)result;
    }
    @Override public int available() throws IOException {
        return (bytesLeft > 0) ? 1 : 0;
    }
}

class MyOutputStream extends OutputStream {
    @Override public void write(int b) throws IOException {}
    @Override public void write(byte[] b) throws IOException {}
    @Override public void write(byte[] b, int off, int len) throws IOException {}
}
