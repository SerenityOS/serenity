/*
 * Copyright (c) 2009, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6863110
 * @summary Newly connected/accepted SctpChannel should fire OP_READ if registered with a Selector
 * @author chegar
 */

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.nio.ByteBuffer;
import java.nio.channels.Selector;
import java.nio.channels.SelectionKey;
import com.sun.nio.sctp.AbstractNotificationHandler;
import com.sun.nio.sctp.AssociationChangeNotification;
import com.sun.nio.sctp.AssociationChangeNotification.AssocChangeEvent;
import com.sun.nio.sctp.HandlerResult;
import com.sun.nio.sctp.Notification;
import com.sun.nio.sctp.SctpChannel;
import com.sun.nio.sctp.SctpServerChannel;
import com.sun.nio.sctp.ShutdownNotification;
import static java.lang.System.out;
import static java.lang.System.err;
import static java.nio.channels.SelectionKey.OP_CONNECT;
import static java.nio.channels.SelectionKey.OP_READ;

public class CommUp {
    static CountDownLatch acceptLatch = new CountDownLatch(1);
    static final int TIMEOUT = 10000;

    CommUpNotificationHandler clientHandler = new CommUpNotificationHandler();
    CommUpNotificationHandler serverHandler = new CommUpNotificationHandler();
    CommUpServer server;
    Thread clientThread;

    void test(String[] args) {
        SocketAddress address = null;

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
                server = new CommUpServer();
                server.start();
                address = server.address();
                debug("Server started and listening on " + address);
            } catch (IOException ioe) {
                ioe.printStackTrace();
                return;
            }
        }

        /* store the main thread so that the server can interrupt it, if necessary */
        clientThread = Thread.currentThread();

        doClient(address);
    }

    void doClient(SocketAddress peerAddress) {
        SctpChannel sc = null;
        try {
            debug("connecting to " + peerAddress);
            sc = SctpChannel.open();
            sc.configureBlocking(false);
            check(sc.isBlocking() == false, "Should be in non-blocking mode");
            sc.connect(peerAddress);

            Selector selector = Selector.open();
            SelectionKey selectiontKey = sc.register(selector, OP_CONNECT);

            /* Expect two interest Ops */
            boolean opConnectReceived = false;
            boolean opReadReceived = false;
            for (int z=0; z<2; z++) {
                debug("select " + z);
                int keysAdded = selector.select(TIMEOUT);
                debug("returned " + keysAdded + " keys");
                if (keysAdded > 0) {
                    Set<SelectionKey> keys = selector.selectedKeys();
                    Iterator<SelectionKey> i = keys.iterator();
                    while(i.hasNext()) {
                        SelectionKey sk = i.next();
                        i.remove();
                        SctpChannel readyChannel =
                            (SctpChannel)sk.channel();

                        /* OP_CONNECT */
                        if (sk.isConnectable()) {
                            /* some trivial checks */
                            check(opConnectReceived == false,
                                  "should only received one OP_CONNECT");
                            check(opReadReceived == false,
                                  "should not receive OP_READ before OP_CONNECT");
                            check(readyChannel.equals(sc),
                                  "channels should be equal");
                            check(!sk.isAcceptable(),
                                  "key should not be acceptable");
                            check(!sk.isReadable(),
                                  "key should not be readable");
                            check(!sk.isWritable(),
                                  "key should not be writable");

                            /* now process the OP_CONNECT */
                            opConnectReceived = true;
                            check((sk.interestOps() & OP_CONNECT) == OP_CONNECT,
                                  "selection key interest ops should contain OP_CONNECT");
                            sk.interestOps(OP_READ);
                            check((sk.interestOps() & OP_CONNECT) != OP_CONNECT,
                                  "selection key interest ops should not contain OP_CONNECT");
                            check(sc.finishConnect(),
                                  "finishConnect should return true");
                        } /* OP_READ */
                          else if (sk.isReadable()) {
                            /* some trivial checks */
                            check(opConnectReceived == true,
                                  "should receive one OP_CONNECT before OP_READ");
                            check(opReadReceived == false,
                                  "should not receive OP_READ before OP_CONNECT");
                            check(readyChannel.equals(sc),
                                  "channels should be equal");
                            check(!sk.isAcceptable(),
                                  "key should not be acceptable");
                            check(sk.isReadable(),
                                  "key should be readable");
                            check(!sk.isWritable(),
                                  "key should not be writable");
                            check(!sk.isConnectable(),
                                  "key should not be connectable");

                            /* now process the OP_READ */
                            opReadReceived = true;
                            selectiontKey.cancel();

                            /* try with small buffer to see if native
                             * implementation can handle this */
                            ByteBuffer buffer = ByteBuffer.allocateDirect(1);
                            readyChannel.receive(buffer, null, clientHandler);
                            check(clientHandler.receivedCommUp(),
                                    "Client should have received COMM_UP");

                            /* dont close (or put anything on) the channel until
                             * we check that the server's accepted channel also
                             * received COMM_UP */
                            serverHandler.waitForCommUp();
                        } else {
                            fail("Unexpected selection key");
                        }
                    }
                } else {
                    fail("Client selector returned 0 ready keys");
                    /* stop the server */
                    server.thread().interrupt();
                }
            } //for

            try { sc.close(); }
            catch (IOException ioe) { unexpected(ioe); }

        } catch (IOException ioe) {
            unexpected(ioe);
        } catch (InterruptedException ie) {
            unexpected(ie);
        }
    }

    class CommUpServer implements Runnable
    {
        final InetSocketAddress serverAddr;
        private SctpServerChannel ssc;
        private Thread serverThread;

        public CommUpServer() throws IOException {
            ssc = SctpServerChannel.open().bind(null);
            java.util.Set<SocketAddress> addrs = ssc.getAllLocalAddresses();
            if (addrs.isEmpty())
                debug("addrs should not be empty");

            serverAddr = (InetSocketAddress) addrs.iterator().next();
        }

        void start() {
            serverThread = new Thread(this, "CommUpServer-"  +
                                              serverAddr.getPort());
            serverThread.start();
        }

        InetSocketAddress address () {
            return serverAddr;
        }

        Thread thread() {
            return serverThread;
        }

        @Override
        public void run() {
            Selector selector = null;
            SctpChannel sc = null;
            SelectionKey readKey = null;
            try {
                sc = ssc.accept();
                debug("accepted " + sc);

                selector = Selector.open();
                sc.configureBlocking(false);
                check(sc.isBlocking() == false, "Should be in non-blocking mode");
                readKey = sc.register(selector, SelectionKey.OP_READ);

                debug("select");
                int keysAdded = selector.select(TIMEOUT);
                debug("returned " + keysAdded + " keys");
                if (keysAdded > 0) {
                    Set<SelectionKey> keys = selector.selectedKeys();
                    Iterator<SelectionKey> i = keys.iterator();
                    while(i.hasNext()) {
                        SelectionKey sk = i.next();
                        i.remove();
                        SctpChannel readyChannel =
                            (SctpChannel)sk.channel();
                        check(readyChannel.equals(sc),
                                "channels should be equal");
                        check(!sk.isAcceptable(),
                                "key should not be acceptable");
                        check(sk.isReadable(),
                                "key should be readable");
                        check(!sk.isWritable(),
                                "key should not be writable");
                        check(!sk.isConnectable(),
                                "key should not be connectable");

                        /* block until we check if the client has received its COMM_UP*/
                        clientHandler.waitForCommUp();

                        ByteBuffer buffer = ByteBuffer.allocateDirect(1);
                        sc.receive(buffer, null, serverHandler);
                        check(serverHandler.receivedCommUp(),
                                "Accepted channel should have received COMM_UP");
                    }
                } else {
                   fail("Server selector returned 0 ready keys");
                   /* stop the client */
                   clientThread.interrupt();
            }
            } catch (IOException ioe) {
                ioe.printStackTrace();
            } catch (InterruptedException unused) {
            } finally {
                if (readKey != null) readKey.cancel();
                try { if (selector != null) selector.close(); }
                catch (IOException  ioe) { unexpected(ioe); }
                try { if (ssc != null) ssc.close(); }
                catch (IOException  ioe) { unexpected(ioe); }
                try { if (sc != null) sc.close(); }
                catch (IOException  ioe) { unexpected(ioe); }
            }
        }
    }

    class CommUpNotificationHandler extends AbstractNotificationHandler<Object>
    {
        private boolean receivedCommUp;  // false

        public synchronized boolean receivedCommUp() {
            return receivedCommUp;
        }

        public synchronized boolean waitForCommUp() throws InterruptedException {
            while (receivedCommUp == false) {
                wait();
            }

            return false;
        }

        @Override
        public HandlerResult handleNotification(
                Notification notification, Object attachment) {
            fail("Unknown notification type");
            return HandlerResult.CONTINUE;
        }

        @Override
        public synchronized HandlerResult handleNotification(
                AssociationChangeNotification notification, Object attachment) {
            AssocChangeEvent event = notification.event();
            debug("AssociationChangeNotification");
            debug("  Association: " + notification.association());
            debug("  Event: " + event);

            if (event.equals(AssocChangeEvent.COMM_UP)) {
                receivedCommUp = true;
                notifyAll();
            }

            return HandlerResult.RETURN;
        }

        @Override
        public HandlerResult handleNotification(
                ShutdownNotification notification, Object attachment) {
            debug("ShutdownNotification");
            debug("  Association: " + notification.association());
            return HandlerResult.RETURN;
        }
    }

        //--------------------- Infrastructure ---------------------------
    boolean debug = true;
    volatile int passed = 0, failed = 0;
    void pass() {passed++;}
    void fail() {failed++; Thread.dumpStack();}
    void fail(String msg) {err.println(msg); fail();}
    void unexpected(Throwable t) {failed++; t.printStackTrace();}
    void check(boolean cond) {if (cond) pass(); else fail();}
    void check(boolean cond, String failMessage) {if (cond) pass(); else fail(failMessage);}
    void debug(String message) {if(debug) { out.println(Thread.currentThread().getName() + ": " + message); }  }
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
