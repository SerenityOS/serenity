/*
 * Copyright (c) 2005, 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 5092263
 * @summary GZIPInputStream o GZIPOutputStream === the identity stream
 * @author Martin Buchholz
 * @key randomness
*/

// To manually test for uncompressed streams larger than 2GB, do
// javac Accordion.java && java -server Accordion 3325349068
// javac Accordion.java && java -server Accordion 5470735564

import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class Accordion {
    private static void readFully(InputStream s, byte[] buf) throws Throwable {
        int pos = 0;
        int n;
        while ((n = s.read(buf, pos, buf.length-pos)) > 0)
            pos += n;
        if (pos != buf.length)
            throw new Exception("Unexpected EOF");
    }

    private static volatile Throwable trouble;

    public static void main(String[] args) throws Throwable {
        if (args.length > 1)
            throw new Exception("Usage: java Accordion [BYTES]");

        final long bytes = args.length == 0 ? 10001 : Long.parseLong(args[0]);
        final int bufsize = 1729;
        final long count = bytes/bufsize;
        final PipedOutputStream out = new PipedOutputStream();
        final PipedInputStream in = new PipedInputStream(out);
        final Random random = new Random();
        final byte[] data = new byte[1729];
        for (int i = 0; i < data.length; i++)
            data[i] = (byte)random.nextInt(255);
        System.out.println("count="+count);

        Thread compressor = new Thread() { public void run() {
            try (GZIPOutputStream s = new GZIPOutputStream(out)) {
                for (long i = 0; i < count; i++)
                    s.write(data, 0, data.length);
            } catch (Throwable t) { trouble = t; }}};

        Thread uncompressor = new Thread() { public void run() {
            try (GZIPInputStream s = new GZIPInputStream(in)) {
                final byte[] maybeBytes = new byte[data.length];
                for (long i = 0; i < count; i++) {
                    readFully(s, maybeBytes);
                    if (! Arrays.equals(data, maybeBytes))
                        throw new Exception("data corruption");
                }
                if (s.read(maybeBytes, 0, 1) > 0)
                    throw new Exception("Unexpected NON-EOF");
            } catch (Throwable t) { trouble = t; }}};

        compressor.start(); uncompressor.start();
        compressor.join();  uncompressor.join();

        if (trouble != null)
            throw trouble;
    }
}
