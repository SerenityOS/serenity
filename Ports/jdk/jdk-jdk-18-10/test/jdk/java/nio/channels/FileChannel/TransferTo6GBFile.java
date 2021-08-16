/*
 * Copyright (c) 2001, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6253145
 * @summary Test FileChannel.transferTo with file positions up to 8GB
 * @run testng/timeout=300 TransferTo6GBFile
 */

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.TimeUnit;

import org.testng.annotations.Test;

public class TransferTo6GBFile {

    private static PrintStream err = System.err;
    private static PrintStream out = System.out;

    // Test transferTo with file positions larger than 2 and 4GB
    @Test
    public void xferTest08() throws Exception { // for bug 6253145
        final long G = 1024L * 1024L * 1024L;

        // Create 6GB file

        File file = File.createTempFile("source", null);
        file.deleteOnExit();

        RandomAccessFile raf = new RandomAccessFile(file, "rw");
        FileChannel fc = raf.getChannel();

        out.println("  Writing large file...");
        long t0 = System.nanoTime();
        try {
            fc.write(ByteBuffer.wrap("0123456789012345".getBytes("UTF-8")), 6*G);
            long t1 = System.nanoTime();
            out.printf("  Wrote large file in %d ns (%d ms) %n",
            t1 - t0, TimeUnit.NANOSECONDS.toMillis(t1 - t0));
        } catch (IOException x) {
            err.println("  Unable to create test file:" + x);
            fc.close();
            return;
        }

        // Setup looback connection and echo server

        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.socket().bind(new InetSocketAddress(0));

        InetAddress lh = InetAddress.getLocalHost();
        InetSocketAddress isa = new InetSocketAddress(lh, ssc.socket().getLocalPort());
        SocketChannel source = SocketChannel.open(isa);
        SocketChannel sink = ssc.accept();

        Thread thr = new Thread(new EchoServer(sink));
        thr.start();

        // Test data is array of positions and counts

        long testdata[][] = {
            { 2*G-1,    1 },
            { 2*G-1,    10 },       // across 2GB boundary
            { 2*G,      1 },
            { 2*G,      10 },
            { 2*G+1,    1 },
            { 4*G-1,    1 },
            { 4*G-1,    10 },       // across 4GB boundary
            { 4*G,      1 },
            { 4*G,      10 },
            { 4*G+1,    1 },
            { 5*G-1,    1 },
            { 5*G-1,    10 },
            { 5*G,      1 },
            { 5*G,      10 },
            { 5*G+1,    1 },
            { 6*G,      1 },
        };

        ByteBuffer sendbuf = ByteBuffer.allocateDirect(100);
        ByteBuffer readbuf = ByteBuffer.allocateDirect(100);

        try {
            byte value = 0;
            for (int i=0; i<testdata.length; i++) {
                long position = testdata[(int)i][0];
                long count = testdata[(int)i][1];

                // generate bytes
                for (long j=0; j<count; j++) {
                    sendbuf.put(++value);
                }
                sendbuf.flip();

                // write to file and transfer to echo server
                fc.write(sendbuf, position);
                t0 = System.nanoTime();
                fc.transferTo(position, count, source);
                out.printf("  transferTo(%d, %2d, source): %d ns%n",
                    position, count, System.nanoTime() - t0);

                // read from echo server
                long nread = 0;
                while (nread < count) {
                    int n = source.read(readbuf);
                    if (n < 0)
                        throw new RuntimeException("Premature EOF!");
                    nread += n;
                }

                // check reply from echo server
                readbuf.flip();
                sendbuf.flip();
                if (!readbuf.equals(sendbuf))
                    throw new RuntimeException("Echoed bytes do not match!");
                readbuf.clear();
                sendbuf.clear();
            }
        } finally {
            source.close();
            ssc.close();
            fc.close();
            file.delete();
        }
    }

    /**
     * Simple in-process server to echo bytes read by a given socket channel
     */
    static class EchoServer implements Runnable {
        private SocketChannel sc;

        public EchoServer(SocketChannel sc) {
            this.sc = sc;
        }

        public void run() {
            ByteBuffer bb = ByteBuffer.allocateDirect(1024);
            try {
                for (;;) {
                    int n = sc.read(bb);
                    if (n < 0)
                        break;

                    bb.flip();
                    while (bb.remaining() > 0) {
                        sc.write(bb);
                    }
                    bb.clear();
                }
            } catch (IOException x) {
                x.printStackTrace();
            } finally {
                try {
                    sc.close();
                } catch (IOException ignore) { }
            }
        }
    }
}
