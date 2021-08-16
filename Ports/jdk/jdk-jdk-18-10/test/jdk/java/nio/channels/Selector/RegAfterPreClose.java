/*
 * Copyright (c) 2009, 2010, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6693490
 * @summary Pre-close file descriptor may inadvertently get registered with
 *     epoll during close
 */

import java.net.*;
import java.nio.channels.*;
import java.util.concurrent.*;
import java.util.*;
import java.io.IOException;

public class RegAfterPreClose {

    static final int TEST_ITERATIONS = 300;
    static volatile boolean done;

    /**
     * A task that continuously connects to a given address and immediately
     * closes the connection.
     */
    static class Connector implements Runnable {
        private final SocketAddress sa;
        Connector(int port) throws IOException {
            InetAddress lh = InetAddress.getLocalHost();
            this.sa = new InetSocketAddress(lh, port);
        }
        public void run() {
            while (!done) {
                try {
                    SocketChannel.open(sa).close();
                } catch (IOException x) {
                    // back-off as probably resource related
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException  ignore) { }
                }
            }
        }
    }

    /**
     * A task that closes a channel.
     */
    static class Closer implements Runnable {
        private final Channel channel;
        Closer(Channel sc) {
            this.channel = sc;
        }
        public void run() {
            try {
                channel.close();
            } catch (IOException ignore) { }
        }
    }

    public static void main(String[] args) throws Exception {
        // create listener
        InetSocketAddress isa = new InetSocketAddress(0);
        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.socket().bind(isa);

        // register with Selector
        final Selector sel = Selector.open();
        ssc.configureBlocking(false);
        SelectionKey key = ssc.register(sel, SelectionKey.OP_ACCEPT);

        ThreadFactory factory = new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setDaemon(true);
                return t;
            }
        };

        // create Executor that handles tasks that closes channels
        // "asynchronously" - this creates the conditions to provoke the bug.
        ExecutorService executor = Executors.newFixedThreadPool(2, factory);

        // submit task that connects to listener
        executor.execute(new Connector(ssc.socket().getLocalPort()));

        // loop accepting connections until done (or an IOException is thrown)
        int remaining = TEST_ITERATIONS;
        while (remaining > 0) {
            sel.select();
            if (key.isAcceptable()) {
                SocketChannel sc = ssc.accept();
                if (sc != null) {
                    remaining--;
                    sc.configureBlocking(false);
                    sc.register(sel, SelectionKey.OP_READ);
                    executor.execute(new Closer(sc));
                }
            }
            sel.selectedKeys().clear();
        }
        done = true;
        sel.close();
        executor.shutdown();
    }
}
