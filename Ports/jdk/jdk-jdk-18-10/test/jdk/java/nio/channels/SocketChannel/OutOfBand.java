/*
 * Copyright (c) 2010, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Test socket adapter sendUrgentData method
 * @bug 6963907
 * @key randomness
 */

import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.io.IOException;
import java.util.Random;

public class OutOfBand {

    private static final Random rand = new Random();

    public static void main(String[] args) throws Exception {
        ServerSocketChannel ssc = null;
        SocketChannel sc1 = null;
        SocketChannel sc2 = null;

        try {

            // establish loopback connection
            ssc = ServerSocketChannel.open().bind(new InetSocketAddress(0));
            InetAddress lh = InetAddress.getLocalHost();
            SocketAddress remote =
                new InetSocketAddress(lh, ssc.socket().getLocalPort());
            sc1 = SocketChannel.open(remote);
            sc2 = ssc.accept();

            // enable SO_OOBLINE on server side
            sc2.socket().setOOBInline(true);

            // run tests
            test1(sc1, sc2);
            test2(sc1, sc2);
            test3(sc1, sc2);
        } finally {
            if (sc1 != null) sc1.close();
            if (sc2 != null) sc2.close();
            if (ssc != null) ssc.close();
        }
    }

    /**
     * Basic test to check that OOB/TCP urgent byte is received.
     */
    static void test1(SocketChannel client, SocketChannel server)
        throws Exception
    {
        assert server.socket().getOOBInline();
        ByteBuffer bb = ByteBuffer.allocate(100);
        for (int i=0; i<1000; i++) {
            int b1 = -127 + rand.nextInt(384);
            client.socket().sendUrgentData(b1);

            bb.clear();
            if (server.read(bb) != 1)
                throw new RuntimeException("One byte expected");
            bb.flip();
            byte b2 = bb.get();
            if ((byte)b1 != b2)
                throw new RuntimeException("Unexpected byte");
        }
    }

    /**
     * Basic test to check that OOB/TCP urgent byte is received, maybe with
     * OOB mark changing.
     */
    static void test2(final SocketChannel client, SocketChannel server)
        throws Exception
    {
        assert server.socket().getOOBInline();
        Runnable sender = new Runnable() {
            public void run() {
                try {
                    for (int i=0; i<256; i++)
                        client.socket().sendUrgentData(i);
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
        };
        Thread thr = new Thread(sender);
        thr.start();

        ByteBuffer bb = ByteBuffer.allocate(256);
        while (bb.hasRemaining()) {
            if (server.read(bb) < 0)
                throw new RuntimeException("Unexpected EOF");
        }
        bb.flip();
        byte expect = 0;
        while (bb.hasRemaining()) {
            if (bb.get() != expect)
                throw new RuntimeException("Unexpected byte");
            expect++;
        }

        thr.join();
    }

    /**
     * Test that is close to some real world examples where an urgent byte is
     * used to "cancel" a long running query or transaction on the server.
     */
    static void test3(SocketChannel client, final SocketChannel server)
        throws Exception
    {
        final int STOP = rand.nextInt(256);

        assert server.socket().getOOBInline();
        Runnable reader = new Runnable() {
            public void run() {
                ByteBuffer bb = ByteBuffer.allocate(100);
                try {
                    int n = server.read(bb);
                    if (n != 1) {
                        String msg = (n < 0) ? "Unexpected EOF" :
                                               "One byte expected";
                        throw new RuntimeException(msg);
                    }
                    bb.flip();
                    if (bb.get() != (byte)STOP)
                        throw new RuntimeException("Unexpected byte");
                    bb.flip();
                    server.write(bb);
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }

            }
        };

        Thread thr = new Thread(reader);
        thr.start();

        // "stop" server
        client.socket().sendUrgentData(STOP);

        // wait for server reply
        ByteBuffer bb = ByteBuffer.allocate(100);
        int n = client.read(bb);
        if (n != 1)
            throw new RuntimeException("Unexpected number of bytes");
        bb.flip();
        if (bb.get() != (byte)STOP)
            throw new RuntimeException("Unexpected reply");

        thr.join();
    }
}
