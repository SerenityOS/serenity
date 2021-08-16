/*
 * Copyright (c) 2000, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8191025
 * @summary Test socketchannel vector IO (use -Dseed=X to set PRNG seed)
 * @library .. /test/lib
 * @build jdk.test.lib.RandomFactory
 * @run main VectorIO
 * @key randomness
 */

import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;
import jdk.test.lib.RandomFactory;

public class VectorIO {

    private static Random generator = RandomFactory.getRandom();

    static int testSize;

    // whether to use the write/read variant with a length parameter
    static boolean setLength;

    public static void main(String[] args) throws Exception {
        testSize = 1;
        setLength = false;
        runTest();
        for(int i=15; i<18; i++) {
            testSize = i;
            setLength = !setLength;
            runTest();
        }
    }

    static void runTest() throws Exception {
        System.err.println("Length " + testSize);
        Server sv = new Server(testSize);
        sv.start();
        bufferTest(sv.port());
        if (sv.finish(8000) == 0)
            throw new Exception("Failed: Length = " + testSize);
    }

    static void bufferTest(int port) throws Exception {
        ByteBuffer[] bufs = new ByteBuffer[testSize];
        long total = 0L;
        for(int i=0; i<testSize; i++) {
            String source = "buffer" + i;
            if (generator.nextBoolean())
                bufs[i] = ByteBuffer.allocateDirect(source.length());
            else
                bufs[i] = ByteBuffer.allocate(source.length());

            bufs[i].put(source.getBytes("8859_1"));
            bufs[i].flip();
            total += bufs[i].remaining();
        }

        ByteBuffer[] bufsPlus1 = new ByteBuffer[bufs.length + 1];
        System.arraycopy(bufs, 0, bufsPlus1, 0, bufs.length);

        // Get a connection to the server
        InetAddress lh = InetAddress.getLocalHost();
        InetSocketAddress isa = new InetSocketAddress(lh, port);
        SocketChannel sc = SocketChannel.open();
        sc.connect(isa);
        sc.configureBlocking(generator.nextBoolean());

        // Write the data out
        long rem = total;
        while (rem > 0L) {
            long bytesWritten;
            if (setLength) {
                bytesWritten = sc.write(bufsPlus1, 0, bufs.length);
            } else {
                bytesWritten = sc.write(bufs);
            }
            if (bytesWritten == 0) {
                if (sc.isBlocking()) {
                    throw new RuntimeException("write did not block");
                } else {
                    System.err.println("Non-blocking write() wrote zero bytes");
                }
                Thread.sleep(50);
            } else {
                rem -= bytesWritten;
            }
        }

        // Clean up
        sc.close();
    }

    static class Server
        extends TestThread
    {
        final int testSize;
        final ServerSocketChannel ssc;

        Server(int testSize) throws IOException {
            super("Server " + testSize);
            this.testSize = testSize;
            this.ssc = ServerSocketChannel.open().bind(new InetSocketAddress(0));
        }

        int port() {
            return ssc.socket().getLocalPort();
        }

        void go() throws Exception {
            bufferTest();
        }

        void bufferTest() throws Exception {
            long total = 0L;
            ByteBuffer[] bufs = new ByteBuffer[testSize];
            for(int i=0; i<testSize; i++) {
                String source = "buffer" + i;
                if (generator.nextBoolean())
                    bufs[i] = ByteBuffer.allocateDirect(source.length());
                else
                    bufs[i] = ByteBuffer.allocate(source.length());
                total += bufs[i].capacity();
            }

            ByteBuffer[] bufsPlus1 = new ByteBuffer[bufs.length + 1];
            System.arraycopy(bufs, 0, bufsPlus1, 0, bufs.length);

            // Get a connection from client
            SocketChannel sc = null;

            try {

                ssc.configureBlocking(false);

                for (;;) {
                    sc = ssc.accept();
                    if (sc != null) {
                        System.err.println("accept() succeeded");
                        break;
                    }
                    Thread.sleep(50);
                }

                sc.configureBlocking(generator.nextBoolean());

                // Read data into multiple buffers
                long avail = total;
                while (avail > 0) {
                    long bytesRead;
                    if (setLength) {
                        bytesRead = sc.read(bufsPlus1, 0, bufs.length);
                    } else {
                        bytesRead = sc.read(bufs);
                    }
                    if (bytesRead < 0)
                        break;
                    if (bytesRead == 0) {
                        if (sc.isBlocking()) {
                            throw new RuntimeException("read did not block");
                        } else {
                            System.err.println
                                ("Non-blocking read() read zero bytes");
                        }
                        Thread.sleep(50);
                    }
                    avail -= bytesRead;
                }

                // Check results
                for(int i=0; i<testSize; i++) {
                    String expected = "buffer" + i;
                    bufs[i].flip();
                    int size = bufs[i].capacity();
                    byte[] data = new byte[size];
                    for(int j=0; j<size; j++)
                        data[j] = bufs[i].get();
                    String message = new String(data, "8859_1");
                    if (!message.equals(expected))
                        throw new Exception("Wrong data: Got "
                                            + message + ", expected "
                                            + expected);
                }

            } finally {
                // Clean up
                ssc.close();
                if (sc != null)
                    sc.close();
            }

        }

    }

}
