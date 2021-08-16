/*
 * Copyright (c) 2002, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
 */

package sun.nio.ch;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnixDomainSocketAddress;
import java.net.StandardProtocolFamily;
import java.net.StandardSocketOptions;
import java.nio.*;
import java.nio.channels.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.channels.spi.*;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.security.PrivilegedActionException;
import java.security.SecureRandom;
import java.util.Random;


/**
 * A simple Pipe implementation based on a socket connection.
 */

class PipeImpl
    extends Pipe
{
    // Number of bytes in the secret handshake.
    private static final int NUM_SECRET_BYTES = 16;

    // Random object for handshake values
    private static final Random RANDOM_NUMBER_GENERATOR = new SecureRandom();

    // Source and sink channels
    private final SourceChannelImpl source;
    private final SinkChannelImpl sink;

    private static class Initializer
        implements PrivilegedExceptionAction<Void>
    {

        private final SelectorProvider sp;
        private IOException ioe;
        SourceChannelImpl source;
        SinkChannelImpl sink;

        private Initializer(SelectorProvider sp) {
            this.sp = sp;
        }

        @Override
        public Void run() throws IOException {
            LoopbackConnector connector = new LoopbackConnector();
            connector.run();
            if (ioe instanceof ClosedByInterruptException) {
                ioe = null;
                Thread connThread = new Thread(connector) {
                    @Override
                    public void interrupt() {}
                };
                connThread.start();
                for (;;) {
                    try {
                        connThread.join();
                        break;
                    } catch (InterruptedException ex) {}
                }
                Thread.currentThread().interrupt();
            }

            if (ioe != null)
                throw new IOException("Unable to establish loopback connection", ioe);

            return null;
        }

        private class LoopbackConnector implements Runnable {

            @Override
            public void run() {
                ServerSocketChannel ssc = null;
                SocketChannel sc1 = null;
                SocketChannel sc2 = null;
                // Loopback address
                SocketAddress sa = null;

                try {
                    // Create secret with a backing array.
                    ByteBuffer secret = ByteBuffer.allocate(NUM_SECRET_BYTES);
                    ByteBuffer bb = ByteBuffer.allocate(NUM_SECRET_BYTES);

                    for(;;) {
                        // Bind ServerSocketChannel to a port on the loopback
                        // address
                        if (ssc == null || !ssc.isOpen()) {
                            ssc = createListener();
                            sa = ssc.getLocalAddress();
                        }

                        // Establish connection (assume connections are eagerly
                        // accepted)
                        sc1 = SocketChannel.open(sa);
                        RANDOM_NUMBER_GENERATOR.nextBytes(secret.array());
                        do {
                            sc1.write(secret);
                        } while (secret.hasRemaining());
                        secret.rewind();

                        // Get a connection and verify it is legitimate
                        sc2 = ssc.accept();
                        do {
                            sc2.read(bb);
                        } while (bb.hasRemaining());
                        bb.rewind();

                        if (bb.equals(secret))
                            break;

                        sc2.close();
                        sc1.close();
                    }

                    // Create source and sink channels
                    source = new SourceChannelImpl(sp, sc1);
                    sink = new SinkChannelImpl(sp, sc2);
                } catch (IOException e) {
                    try {
                        if (sc1 != null)
                            sc1.close();
                        if (sc2 != null)
                            sc2.close();
                    } catch (IOException e2) {}
                    ioe = e;
                } finally {
                    try {
                        if (ssc != null)
                            ssc.close();
                        if (sa instanceof UnixDomainSocketAddress) {
                            Path path = ((UnixDomainSocketAddress) sa).getPath();
                            Files.deleteIfExists(path);
                        }
                    } catch (IOException e2) {}
                }
            }
        }
    }

    /**
     * Creates a Pipe implementation that supports buffering.
     */
    PipeImpl(SelectorProvider sp) throws IOException {
        this(sp, true);
    }

    /**
     * Creates Pipe implementation that supports optionally buffering.
     *
     * @implNote The pipe uses Unix domain sockets where possible. It uses a
     * loopback connection on older editions of Windows. When buffering is
     * disabled then it sets TCP_NODELAY on the sink channel.
     */
    @SuppressWarnings("removal")
    PipeImpl(SelectorProvider sp, boolean buffering) throws IOException {
        Initializer initializer = new Initializer(sp);
        try {
            AccessController.doPrivileged(initializer);
            SinkChannelImpl sink = initializer.sink;
            if (sink.isNetSocket() && !buffering) {
                sink.setOption(StandardSocketOptions.TCP_NODELAY, true);
            }
        } catch (PrivilegedActionException pae) {
            throw (IOException) pae.getCause();
        }
        this.source = initializer.source;
        this.sink = initializer.sink;
    }

    public SourceChannelImpl source() {
        return source;
    }

    public SinkChannelImpl sink() {
        return sink;
    }

    private static volatile boolean noUnixDomainSockets;

    private static ServerSocketChannel createListener() throws IOException {
        ServerSocketChannel listener = null;
        if (!noUnixDomainSockets) {
            try {
                listener = ServerSocketChannel.open(StandardProtocolFamily.UNIX);
                return listener.bind(null);
            } catch (UnsupportedOperationException | IOException e) {
                // IOException is most likely to be caused by the temporary directory
                // name being too long. Possibly should log this.
                noUnixDomainSockets = true;
                if (listener != null)
                    listener.close();
            }
        }
        listener = ServerSocketChannel.open();
        InetAddress lb = InetAddress.getLoopbackAddress();
        listener.bind(new InetSocketAddress(lb, 0));
        return listener;
    }
}
