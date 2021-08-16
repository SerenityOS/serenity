/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4691425
 * @summary Test the read and write of GZIPInput/OutputStream, including
 *          concatenated .gz inputstream
 * @key randomness
 */

import java.io.*;
import java.util.*;
import java.util.zip.*;

public class GZIPInputStreamRead {
    public static void main(String[] args) throws Throwable {
        Random rnd = new Random();
        for (int i = 1; i < 100; i++) {
            int members = rnd.nextInt(10) + 1;

            ByteArrayOutputStream srcBAOS = new ByteArrayOutputStream();
            ByteArrayOutputStream dstBAOS = new ByteArrayOutputStream();
            for (int j = 0; j < members; j++) {
                byte[] src = new byte[rnd.nextInt(8192) + 1];
                rnd.nextBytes(src);
                srcBAOS.write(src);

                try (GZIPOutputStream gzos = new GZIPOutputStream(dstBAOS)) {
                    gzos.write(src);
                }
            }
            byte[] srcBytes = srcBAOS.toByteArray();
            byte[] dstBytes = dstBAOS.toByteArray();
            // try different size of buffer to read the
            // GZIPInputStream
            /* just for fun when running manually
            for (int j = 1; j < 10; j++) {
                test(srcBytes, dstBytes, j);
            }
            */
            for (int j = 0; j < 10; j++) {
                int readBufSZ = rnd.nextInt(2048) + 1;
                test(srcBytes,
                     dstBytes,
                     readBufSZ,
                     512);    // the defualt buffer size
                test(srcBytes,
                     dstBytes,
                     readBufSZ,
                     rnd.nextInt(4096) + 1);
            }
        }
    }

    private static void test(byte[] src, byte[] dst,
                             int readBufSize, int gzisBufSize)
        throws Throwable
    {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(dst);
             GZIPInputStream gzis = new GZIPInputStream(bais, gzisBufSize))
        {
            byte[] result = new byte[src.length + 10];
            byte[] buf = new byte[readBufSize];
            int n = 0;
            int off = 0;

            while ((n = gzis.read(buf, 0, buf.length)) != -1) {
                System.arraycopy(buf, 0, result, off, n);
                off += n;
                // no range check, if overflow, let it fail
            }
            if (off != src.length || gzis.available() != 0 ||
                !Arrays.equals(src, Arrays.copyOf(result, off))) {
                throw new RuntimeException(
                    "GZIPInputStream reading failed! " +
                    ", src.len=" + src.length +
                    ", read=" + off);
            }
        }
    }
}
