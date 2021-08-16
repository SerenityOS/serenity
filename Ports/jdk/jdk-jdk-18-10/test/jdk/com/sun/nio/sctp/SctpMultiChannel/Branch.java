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
import java.util.Set;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.nio.ByteBuffer;
import com.sun.nio.sctp.AbstractNotificationHandler;
import com.sun.nio.sctp.Association;
import com.sun.nio.sctp.AssociationChangeNotification;
import com.sun.nio.sctp.AssociationChangeNotification.AssocChangeEvent;
import com.sun.nio.sctp.HandlerResult;
import com.sun.nio.sctp.InvalidStreamException;
import com.sun.nio.sctp.MessageInfo;
import com.sun.nio.sctp.SctpChannel;
import com.sun.nio.sctp.SctpMultiChannel;
import com.sun.nio.sctp.ShutdownNotification;
import static java.lang.System.out;
import static java.lang.System.err;

public class Branch {
    /* Latches used to synchronize between the client and server so that
     * connections without any IO may not be closed without being accepted */
    final CountDownLatch clientFinishedLatch = new CountDownLatch(1);
    final CountDownLatch serverFinishedLatch = new CountDownLatch(1);

    void test(String[] args) {
        SocketAddress address = null;
        Server server = null;

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
                server = new Server();
                server.start();
                address = server.address();
                debug("Server started and listening on " + address);
            } catch (IOException ioe) {
                ioe.printStackTrace();
                return;
            }
        }

        doTest(address);
    }

    void doTest(SocketAddress peerAddress) {
        SctpMultiChannel channel = null;
        ByteBuffer buffer = ByteBuffer.allocate(Util.LARGE_BUFFER);
        MessageInfo info = MessageInfo.createOutgoing(null, 0);

        try {
            channel = SctpMultiChannel.open();

            /* setup an association implicitly by sending a small message */
            int streamNumber = 0;
            debug("sending to " + peerAddress + " on stream number: " + streamNumber);
            info = MessageInfo.createOutgoing(peerAddress, streamNumber);
            buffer.put(Util.SMALL_MESSAGE.getBytes("ISO-8859-1"));
            buffer.flip();
            int position = buffer.position();
            int remaining = buffer.remaining();

            debug("sending small message: " + buffer);
            int sent = channel.send(buffer, info);

            check(sent == remaining, "sent should be equal to remaining");
            check(buffer.position() == (position + sent),
                    "buffers position should have been incremented by sent");

            /* Receive the COMM_UP */
            buffer.clear();
            BranchNotificationHandler handler = new BranchNotificationHandler();
            info = channel.receive(buffer, null, handler);
            check(handler.receivedCommUp(), "COMM_UP no received");
            Set<Association> associations = channel.associations();
            check(!associations.isEmpty(),"There should be some associations");
            Association bassoc = associations.iterator().next();

            /* TEST 1: branch */
            SctpChannel bchannel = channel.branch(bassoc);

            check(!bchannel.getAllLocalAddresses().isEmpty(),
                                   "branched channel should be bound");
            check(!bchannel.getRemoteAddresses().isEmpty(),
                                   "branched channel should be connected");
            check(channel.associations().isEmpty(),
                  "there should be no associations since the only one was branched off");

            buffer.clear();
            info = bchannel.receive(buffer, null, null);
            buffer.flip();
            check(info != null, "info is null");
            check(info.streamNumber() == streamNumber,
                    "message not sent on the correct stream");
            check(info.bytes() == Util.SMALL_MESSAGE.getBytes("ISO-8859-1").
                  length, "bytes received not equal to message length");
            check(info.bytes() == buffer.remaining(), "bytes != remaining");
            check(Util.compare(buffer, Util.SMALL_MESSAGE),
              "received message not the same as sent message");

        } catch (IOException ioe) {
            unexpected(ioe);
        } finally {
            clientFinishedLatch.countDown();
            try { serverFinishedLatch.await(10L, TimeUnit.SECONDS); }
            catch (InterruptedException ie) { unexpected(ie); }
            if (channel != null) {
                try { channel.close(); }
                catch (IOException e) { unexpected (e);}
            }
        }
    }

    class Server implements Runnable
    {
        final InetSocketAddress serverAddr;
        private SctpMultiChannel serverChannel;

        public Server() throws IOException {
            serverChannel = SctpMultiChannel.open().bind(null);
            java.util.Set<SocketAddress> addrs = serverChannel.getAllLocalAddresses();
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
            ByteBuffer buffer = ByteBuffer.allocateDirect(Util.LARGE_BUFFER);
            try {
                MessageInfo info;

                /* receive a small message */
                do {
                    info = serverChannel.receive(buffer, null, null);
                    if (info == null) {
                        fail("Server: unexpected null from receive");
                            return;
                    }
                } while (!info.isComplete());

                buffer.flip();
                check(info != null, "info is null");
                check(info.streamNumber() == 0,
                        "message not sent on the correct stream");
                check(info.bytes() == Util.SMALL_MESSAGE.getBytes("ISO-8859-1").
                      length, "bytes received not equal to message length");
                check(info.bytes() == buffer.remaining(), "bytes != remaining");
                check(Util.compare(buffer, Util.SMALL_MESSAGE),
                  "received message not the same as sent message");

                check(info != null, "info is null");
                Set<Association> assocs = serverChannel.associations();
                check(assocs.size() == 1, "there should be only one association");

                /* echo the message */
                debug("Server: echoing first message");
                buffer.flip();
                MessageInfo sendInfo = MessageInfo.createOutgoing(info.association(), null, 0);
                int bytes = serverChannel.send(buffer, sendInfo);
                debug("Server: sent " + bytes + "bytes");

                clientFinishedLatch.await(10L, TimeUnit.SECONDS);
                serverFinishedLatch.countDown();
            } catch (IOException ioe) {
                unexpected(ioe);
            } catch (InterruptedException ie) {
                unexpected(ie);
            } finally {
                try { if (serverChannel != null) serverChannel.close(); }
                catch (IOException  unused) {}
            }
        }
    }

    class BranchNotificationHandler extends AbstractNotificationHandler<Object>
    {
        boolean receivedCommUp;  // false

        boolean receivedCommUp() {
            return receivedCommUp;
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

            return HandlerResult.RETURN;
        }

        /* A ShutdownNotification handler is provided to ensure that no
         * shutdown notification are being handled since we don't expect
         * to receive them. This is not part of branch testing, it just
         * fits here to test another bug. */
        @Override
        public HandlerResult handleNotification(
                ShutdownNotification notification, Object attachment) {
            debug("ShutdownNotification");
            debug("  Association: " + notification.association());

            fail("Shutdown should not be received");

            return HandlerResult.RETURN;
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
    void debug(String message) {if(debug) { System.out.println(message); }  }
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
