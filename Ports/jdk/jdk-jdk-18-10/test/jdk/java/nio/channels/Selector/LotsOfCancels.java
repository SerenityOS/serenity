/*
 * Copyright 2009, 2019, Google Inc.  All Rights Reserved.
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

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Reproduces O(N^2) behavior of JDK6/7 select() call. This happens when
 * a selector has many unprocessed updates to its interest set (e.g. adding
 * OP_READ on a bunch of newly accepted sockets). The O(N^2) is triggered
 * by cancelling a number of selection keys (or just closing a few sockets).
 * In this case, select() will first go through the list of cancelled keys
 * and try to deregister them. That deregistration is O(N^2) over the list
 * of unprocessed updates to the interest set.
 *
 * <p> This O(N^2) behavior is a BUG in JVM and should be fixed.
 *
 * <p> The test first creates initCount connections, and adds them
 * to the server epoll set. It then creates massCount connections,
 * registers interest (causing updateList to be populated with massCount*2
 * elements), but does not add them to epoll set (that would've cleared
 * updateList). The test then closes initCount connections, thus populating
 * deregistration queue. The subsequent call to selectNow() will first process
 * deregistration queue, performing O(N^2) over updateList size,
 * equal to massCount * 2.
 *
 * <p> Note that connect rate is artificially slowed down to compensate
 * for what I believe is a Linux bug, where too high of a connection rate
 * ends up in SYN's being dropped and then slow retransmits.
 *
 * @author Igor Chernyshev
 */
public class LotsOfCancels {

    static long testStartTime;

    public static void main(String[] args) throws Exception {
        // the final select should run in less than 1000ms.
        runTest(500, 2700, 1000);
    }

    static void log(String msg) {
        System.out.println(getLogPrefix() + msg);
    }

    static String getLogPrefix() {
        return durationMillis(testStartTime) + ": ";
    }

    /**
     * Returns the elapsed time since startNanos, in milliseconds.
     * @param startNanos the start time; this must be a value returned
     * by {@link System.nanoTime}
     */
    static long durationMillis(long startNanos) {
        return (System.nanoTime() - startNanos) / (1000L * 1000L);
    }

    static void runTest(int initCount, int massCount, int maxSelectTime)
            throws Exception {
        testStartTime = System.nanoTime();

        InetSocketAddress address = new InetSocketAddress(InetAddress.getLoopbackAddress(), 7359);

        // Create server channel, add it to selector and run epoll_ctl.
        log("Setting up server");
        Selector serverSelector = Selector.open();
        ServerSocketChannel server = ServerSocketChannel.open();
        server.configureBlocking(false);
        server.socket().bind(address, 5000);
        server.register(serverSelector, SelectionKey.OP_ACCEPT);
        serverSelector.selectNow();

        log("Setting up client");
        ClientThread client = new ClientThread(address);
        client.start();
        Thread.sleep(100);

        // Set up initial set of client sockets.
        log("Starting initial client connections");
        client.connectClients(initCount);
        Thread.sleep(500);  // Wait for client connections to arrive

        // Accept all initial client sockets, add to selector and run
        // epoll_ctl.
        log("Accepting initial connections");
        List<SocketChannel> serverChannels1 =
            acceptAndAddAll(serverSelector, server, initCount);
        if (serverChannels1.size() != initCount) {
            throw new Exception("Accepted " + serverChannels1.size() +
                                " instead of " + initCount);
        }
        serverSelector.selectNow();

        // Set up mass set of client sockets.
        log("Requesting mass client connections");
        client.connectClients(massCount);
        Thread.sleep(500);  // Wait for client connections to arrive

        // Accept all mass client sockets, add to selector and do NOT
        // run epoll_ctl.
        log("Accepting mass connections");
        List<SocketChannel> serverChannels2 =
            acceptAndAddAll(serverSelector, server, massCount);
        if (serverChannels2.size() != massCount) {
            throw new Exception("Accepted " + serverChannels2.size() +
                                " instead of " + massCount);
        }

        // Close initial set of sockets.
        log("Closing initial connections");
        closeAll(serverChannels1);

        // Now get the timing of select() call.
        log("Running the final select call");
        long startTime = System.nanoTime();
        serverSelector.selectNow();
        long duration = durationMillis(startTime);
        log("Init count = " + initCount +
            ", mass count = " + massCount +
            ", duration = " + duration + "ms");

        if (duration > maxSelectTime) {
            System.out.println
                ("\n\n\n\n\nFAILURE: The final selectNow() took " +
                 duration + "ms " +
                 "- seems like O(N^2) bug is still here\n\n");
            System.exit(1);
        }
    }

    static List<SocketChannel> acceptAndAddAll(Selector selector,
                                               ServerSocketChannel server,
                                               int expected)
            throws Exception {
        int retryCount = 0;
        int acceptCount = 0;
        List<SocketChannel> channels = new ArrayList<SocketChannel>();
        while (channels.size() < expected) {
            SocketChannel channel = server.accept();
            if (channel == null) {
                log("accept() returned null " +
                    "after accepting " + acceptCount + " more connections");
                acceptCount = 0;
                if (retryCount < 10) {
                    // See if more new sockets got stacked behind.
                    retryCount++;
                    Thread.sleep(500);
                    continue;
                }
                break;
            }
            retryCount = 0;
            acceptCount++;
            channel.configureBlocking(false);
            channel.register(selector, SelectionKey.OP_READ);
            channels.add(channel);
        }
        // Cause an additional updateList entry per channel.
        for (SocketChannel channel : channels) {
            channel.register(selector, SelectionKey.OP_WRITE);
        }
        return channels;
    }

    static void closeAll(List<SocketChannel> channels)
            throws Exception {
        for (SocketChannel channel : channels) {
            channel.close();
        }
    }

    static class ClientThread extends Thread {
        private final SocketAddress address;
        private final Selector selector;
        private int connectionsNeeded;
        private int totalCreated;

        ClientThread(SocketAddress address) throws Exception {
            this.address = address;
            selector = Selector.open();
            setDaemon(true);
        }

        void connectClients(int count) throws Exception {
            synchronized (this) {
                connectionsNeeded += count;
            }
            selector.wakeup();
        }

        @Override
        public void run() {
            try {
                handleClients();
            } catch (Throwable e) {
                e.printStackTrace();
                System.exit(1);
            }
        }

        private void handleClients() throws Exception {
            int selectCount = 0;
            while (true) {
                int createdCount = 0;
                synchronized (this) {
                    if (connectionsNeeded > 0) {

                        while (connectionsNeeded > 0 && createdCount < 20) {
                            connectionsNeeded--;
                            createdCount++;
                            totalCreated++;

                            SocketChannel channel = SocketChannel.open();
                            channel.configureBlocking(false);
                            channel.connect(address);
                            if (!channel.finishConnect()) {
                                channel.register(selector,
                                                 SelectionKey.OP_CONNECT);
                            }
                        }

                        log("Started total of " +
                            totalCreated + " client connections");
                        Thread.sleep(200);
                    }
                }

                if (createdCount > 0) {
                    selector.selectNow();
                } else {
                    selectCount++;
                    long startTime = System.nanoTime();
                    selector.select();
                    long duration = durationMillis(startTime);
                    log("Exited clientSelector.select(), loop #"
                        + selectCount + ", duration = " + duration + "ms");
                }

                int keyCount = -1;
                Iterator<SelectionKey> keys =
                    selector.selectedKeys().iterator();
                while (keys.hasNext()) {
                    SelectionKey key = keys.next();
                    synchronized (key) {
                        keyCount++;
                        keys.remove();
                        if (!key.isValid()) {
                            log("Ignoring client key #" + keyCount);
                            continue;
                        }
                        int readyOps = key.readyOps();
                        if (readyOps == SelectionKey.OP_CONNECT) {
                            key.interestOps(0);
                            ((SocketChannel) key.channel()).finishConnect();
                        } else {
                            log("readyOps() on client key #" + keyCount +
                                " returned " + readyOps);
                        }
                    }
                }
            }
        }
    }
}
