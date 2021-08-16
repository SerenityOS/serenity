/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8034181
 * @summary SIGBUS in SctpChannelImpl receive
 * @author chegar
 */

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.io.IOException;
import java.nio.ByteBuffer;
import com.sun.nio.sctp.AbstractNotificationHandler;
import com.sun.nio.sctp.AssociationChangeNotification;
import com.sun.nio.sctp.AssociationChangeNotification.AssocChangeEvent;
import com.sun.nio.sctp.HandlerResult;
import com.sun.nio.sctp.MessageInfo;
import com.sun.nio.sctp.Notification;
import com.sun.nio.sctp.PeerAddressChangeNotification;
import com.sun.nio.sctp.SctpChannel;
import com.sun.nio.sctp.SctpServerChannel;
import com.sun.nio.sctp.ShutdownNotification;
import static java.lang.System.out;
import static java.lang.System.err;
import static java.nio.charset.StandardCharsets.US_ASCII;

public class ReceiveIntoDirect {
    /* suitably small message to NOT overrun small buffers */
    final byte[] msgBytes =  "Hello".getBytes(US_ASCII);

    /* number of client connections/combinations (accepted by the server) */
    final int NUM_CONNECTIONS = 75;

    void test(String[] args) throws IOException {
        SocketAddress address = null;
        Server server;

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
            server = new Server();
            server.start();
            address = server.address();
            debug("Server started and listening on " + address);
        }

        /* many combinations with varing buffer sizes, and offsets */
        runWithManyOffsets(address, 20);
        runWithManyOffsets(address, 49);
        runWithManyOffsets(address, 50);
        runWithManyOffsets(address, 51);
        runWithManyOffsets(address, 1024);
    }

    void runWithManyOffsets(SocketAddress addr, int bufferSize)
        throws IOException
    {
        doTest(addr, bufferSize, 1);
        doTest(addr, bufferSize, 2);
        doTest(addr, bufferSize, 3);
        doTest(addr, bufferSize, 4);
        doTest(addr, bufferSize, 5);
        doTest(addr, bufferSize, 6);
        doTest(addr, bufferSize, 7);
        doTest(addr, bufferSize, 8);
        doTest(addr, bufferSize, 9);
        doTest(addr, bufferSize, 10);
        doTest(addr, bufferSize, 11);
        doTest(addr, bufferSize, 12);
        doTest(addr, bufferSize, 13);
        doTest(addr, bufferSize, 14);
        doTest(addr, bufferSize, 15);
    }

    void doTest(SocketAddress peerAddress, int bufferSize, int bufferOffset)
        throws IOException
    {
        debug("\n\nTesting with bufferSize " + bufferSize + " and offset " + bufferOffset);
        assert bufferOffset + msgBytes.length <= bufferSize :
               "buffer offset + message length greater than buffer size ";

        ByteBuffer buffer = ByteBuffer.allocateDirect(bufferSize);
        MessageInfo info;

        try (SctpChannel channel = SctpChannel.open()) {
            channel.connect(peerAddress);

            ReceiveNotificationHandler handler =
                new ReceiveNotificationHandler();

            /* TEST 1: Assoc/peer change notif into direct buffer with offest */
            do {
                debug("Test 1: Assoc/peer change with offset " + bufferOffset);
                buffer.position(bufferOffset);
                info = channel.receive(buffer, null, handler);
                if (info == null) {
                    fail("unexpected null from receive");
                    return;
                }
            } while (!info.isComplete());

            buffer.flip().position(bufferOffset);
            check(handler.receivedCommUp(), "SCTP_COMM_UP not received");
            check(info != null, "info is null");
            check(info.address() != null, "address is null");
            check(info.association() != null, "association is null");
            check(info.isComplete(), "message is not complete");
            check(info.isUnordered() != true,
                  "message should not be unordered");
            check(info.streamNumber() >= 0, "invalid stream number");
            check(info.bytes() == msgBytes.length,
                  "bytes received not equal to message length");
            check(info.bytes() == buffer.remaining(), "bytes != remaining");
            check(Util.compare(buffer, msgBytes),
                  "received message not the same as sent message");

            /* TEST 2: shutdown notification with offset */
            debug("Test 2: shutdown notif with offset " + bufferOffset);
            buffer.clear().position(bufferOffset);
            while ((info = channel.receive(buffer, null, handler )) != null &&
                    info.bytes() != -1 );
        }
    }

    class Server implements Runnable
    {
        private final InetSocketAddress serverAddr;
        private final SctpServerChannel ssc;

        public Server() throws IOException {
            ssc = SctpServerChannel.open().bind(null);
            java.util.Set<SocketAddress> addrs = ssc.getAllLocalAddresses();
            if (addrs.isEmpty())
                debug("addrs should not be empty");

            serverAddr = (InetSocketAddress) addrs.iterator().next();
        }

        public void start() {
            (new Thread(this, "Server-"  + serverAddr.getPort())).start();
        }

        public InetSocketAddress address() {
            return serverAddr;
        }

        @Override
        public void run() {
            try {
                for (int i=0; i<NUM_CONNECTIONS; i++) {
                    SctpChannel sc = ssc.accept();

                    /* send a small message */
                    MessageInfo info = MessageInfo.createOutgoing(null, 0);
                    ByteBuffer buf = ByteBuffer.allocateDirect(Util.SMALL_BUFFER);
                    buf.put(msgBytes);
                    buf.flip();

                    debug("sending small message: " + buf);
                    sc.send(buf, info);

                    sc.shutdown();
                    sc.close();
                }
            } catch (IOException x) {
                unexpected(x);
            } finally {
                try { ssc.close(); }
                catch (IOException x) { unexpected(x); }
            }
        }
    }

    class ReceiveNotificationHandler extends AbstractNotificationHandler<Object>
    {
        boolean receivedCommUp;  // false

        public ReceiveNotificationHandler() { }

        public boolean receivedCommUp() {
            return receivedCommUp;
        }

        @Override
        public HandlerResult handleNotification(
                Notification notification, Object attachment) {
            fail("Unknown notification type");
            return HandlerResult.CONTINUE;
        }

        @Override
        public HandlerResult handleNotification(
                AssociationChangeNotification notification, Object attachment) {
            AssocChangeEvent event = notification.event();
            debug("AssociationChangeNotification");
            debug("  Association: " + notification.association());
            debug("  Event: " + event);

            if (event.equals(AssocChangeEvent.COMM_UP))
                receivedCommUp = true;

            return HandlerResult.CONTINUE;
        }

        @Override
        public HandlerResult handleNotification(
                PeerAddressChangeNotification pacn, Object unused)
        {
            debug("PeerAddressChangeNotification: " +  pacn);
            return HandlerResult.CONTINUE;
        }

        @Override
        public HandlerResult handleNotification(
                ShutdownNotification notification, Object attachment) {
            debug("ShutdownNotification");
            debug("  Association: " + notification.association());
            return HandlerResult.CONTINUE;
        }
    }
        //--------------------- Infrastructure ---------------------------
    boolean debug = true;
    volatile int passed = 0, failed = 0;
    void pass() {passed++;}
    void fail() {failed++; Thread.dumpStack();}
    void fail(String msg) {System.err.println(msg); fail();}
    void unexpected(Throwable t) {failed++; t.printStackTrace();}
    void check(boolean cond) {if (cond) pass(); else fail();}
    void check(boolean cond, String failMessage) {if (cond) pass(); else fail(failMessage);}
    void debug(String message) {if(debug) {
          System.out.println(Thread.currentThread() + " " + message); } }
    public static void main(String[] args) throws Throwable {
        Class<?> k = new Object(){}.getClass().getEnclosingClass();
        try {k.getMethod("instanceMain",String[].class)
                .invoke( k.newInstance(), (Object) args);}
        catch (Throwable e) {throw e.getCause();}}
    public void instanceMain(String[] args) throws Throwable {
        try {test(args);} catch (Throwable t) {unexpected(t);}
        System.out.printf("%nPassed = %d, failed = %d%n%n", passed, failed);
        if (failed > 0) throw new AssertionError("Some tests failed");}

}
