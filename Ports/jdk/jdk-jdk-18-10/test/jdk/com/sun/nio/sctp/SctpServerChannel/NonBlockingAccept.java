/*
 * Copyright (c) 2009, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4927640
 * @summary Tests the SCTP protocol implementation
 * @author chegar
 */

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.nio.channels.AlreadyConnectedException;
import java.nio.channels.Selector;
import java.nio.channels.SelectionKey;
import com.sun.nio.sctp.SctpChannel;
import com.sun.nio.sctp.SctpServerChannel;
import static java.lang.System.out;
import static java.lang.System.err;

public class NonBlockingAccept {
    static CountDownLatch acceptLatch = new CountDownLatch(1);
    static final int SEL_TIMEOUT = 10000;
    static final int NUM_TEST_CONNECTIONS = 10;

    void test(String[] args) {
        SocketAddress address = null;
        NonblockingServer server;

        if (!Util.isSCTPSupported()) {
            out.println("SCTP protocol is not supported");
            out.println("Test cannot be run");
            return;
        }

        if (args.length == 2) {
            /* requested to connecct to a specific address */
            try {
                int port = Integer.valueOf(args[1]);
                address = new InetSocketAddress(args[0], port);
            } catch (NumberFormatException nfe) {
                err.println(nfe);
            }
        } else {
            /* start server on local machine, default */
            try {
                server = new NonblockingServer();
                server.start();
                address = server.address();
                debug("Server started and listening on " + address);
            } catch (IOException ioe) {
                ioe.printStackTrace();
                return;
            }
        }

        doClient(address);
    }

    void doClient(SocketAddress peerAddress) {
        Set<SctpChannel> channels = new HashSet<SctpChannel>(NUM_TEST_CONNECTIONS);

        try {
            for (int i=0; i<NUM_TEST_CONNECTIONS;) {
                debug("connecting " + ++i);
                channels.add(SctpChannel.open(peerAddress, 0, 0));
                sleep(100);
            }

            /* don't close the channels until they have been accepted */
            acceptLatch.await();

            for(SctpChannel sc: channels)
                sc.close();
        } catch (IOException ioe) {
            unexpected(ioe);
        } catch (InterruptedException ie) {
            unexpected(ie);
        }
    }

    class NonblockingServer implements Runnable
    {
        final InetSocketAddress serverAddr;
        private SctpServerChannel ssc;
        private Thread serverThread;

        public NonblockingServer() throws IOException {
            ssc = SctpServerChannel.open().bind(null);
            java.util.Set<SocketAddress> addrs = ssc.getAllLocalAddresses();
            if (addrs.isEmpty())
                debug("addrs should not be empty");

            serverAddr = (InetSocketAddress) addrs.iterator().next();
        }

        void start() {
            serverThread = new Thread(this, "NonblockingServer-"  +
                                              serverAddr.getPort());
            serverThread.start();
        }

        InetSocketAddress address () {
            return serverAddr;
        }

        @Override
        public void run() {
            Selector acceptSelector = null;
            SelectionKey acceptKey = null;

            try {
                acceptSelector = Selector.open();
                ssc.configureBlocking(false);
                check(ssc.isBlocking() == false, "Should be in non-blocking mode");
                acceptKey = ssc.register(acceptSelector, SelectionKey.OP_ACCEPT);

                int connectionsAccepted = 0;
                while (connectionsAccepted < NUM_TEST_CONNECTIONS) {
                    int keysAdded = acceptSelector.select(SEL_TIMEOUT);
                    if (keysAdded > 0) {
                        Set<SelectionKey> keys = acceptSelector.selectedKeys();
                        Iterator<SelectionKey> i = keys.iterator();
                        while(i.hasNext()) {
                            SelectionKey sk = i.next();
                            i.remove();
                            SctpServerChannel nextReady =
                                (SctpServerChannel)sk.channel();
                            check(nextReady.equals(ssc),
                                    "channels should be equal");
                            check(sk.isAcceptable(),
                                    "key should be acceptable");
                            check(!sk.isReadable(),
                                    "key should not be readable");
                            check(!sk.isWritable(),
                                    "key should not be writable");
                            check(!sk.isConnectable(),
                                    "key should not be connectable");
                            SctpChannel acceptsc = nextReady.accept();
                            connectionsAccepted++;
                            debug("Accepted " + connectionsAccepted + " connections");
                            check(acceptsc != null,
                                    "Accepted channel should not be null");
                            if (acceptsc != null) {
                                checkAcceptedChannel(acceptsc);
                                acceptsc.close();
                            }
                        } /* while */
                    } /* if */
                } /* while */
            } catch (IOException ioe) {
                ioe.printStackTrace();
            } finally {
                acceptLatch.countDown();
                if (acceptKey != null) acceptKey.cancel();
                try { if (acceptSelector != null) acceptSelector.close(); }
                catch (IOException  ioe) { unexpected(ioe); }
                try { if (ssc != null) ssc.close(); }
                catch (IOException  ioe) { unexpected(ioe); }
            }
        }
    }

    void checkAcceptedChannel(SctpChannel sc) {
        try {
            debug("Checking accepted SctpChannel");
            check(sc.association() != null,
                  "accepted channel should have an association");
            check(!(sc.getRemoteAddresses().isEmpty()),
                  "accepted channel should be connected");
            check(!(sc.isConnectionPending()),
                  "accepted channel should not have a connection pending");
            check(sc.isBlocking(),
                  "accepted channel should be blocking");
            try { sc.connect(new TestSocketAddress()); fail(); }
            catch (AlreadyConnectedException unused) { pass(); }
            try { sc.bind(new TestSocketAddress()); fail(); }
            catch (AlreadyConnectedException unused) { pass(); }
        } catch (IOException unused) { fail(); }
    }

    static class TestSocketAddress extends SocketAddress {}

        //--------------------- Infrastructure ---------------------------
    boolean debug = true;
    volatile int passed = 0, failed = 0;
    void pass() {passed++;}
    void fail() {failed++; Thread.dumpStack();}
    void fail(String msg) {err.println(msg); fail();}
    void unexpected(Throwable t) {failed++; t.printStackTrace();}
    void check(boolean cond) {if (cond) pass(); else fail();}
    void check(boolean cond, String failMessage) {if (cond) pass(); else fail(failMessage);}
    void debug(String message) {if(debug) { out.println(message); }  }
    void sleep(long millis) { try { Thread.currentThread().sleep(millis); }
                          catch(InterruptedException ie) { unexpected(ie); }}
    public static void main(String[] args) throws Throwable {
        Class<?> k = new Object(){}.getClass().getEnclosingClass();
        try {k.getMethod("instanceMain",String[].class)
                .invoke( k.newInstance(), (Object) args);}
        catch (Throwable e) {throw e.getCause();}}
    public void instanceMain(String[] args) throws Throwable {
        try {test(args);} catch (Throwable t) {unexpected(t);}
        out.printf("%nPassed = %d, failed = %d%n%n", passed, failed);
        if (failed > 0) throw new AssertionError("Some tests failed");}

}
