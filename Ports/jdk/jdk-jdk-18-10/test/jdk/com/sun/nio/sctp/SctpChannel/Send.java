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
 * @bug 4927640
 * @summary Tests the SCTP protocol implementation
 * @author chegar
 */

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.nio.ByteBuffer;
import java.nio.channels.NotYetConnectedException;
import java.nio.channels.ClosedChannelException;
import com.sun.nio.sctp.AbstractNotificationHandler;
import com.sun.nio.sctp.Association;
import com.sun.nio.sctp.AssociationChangeNotification;
import com.sun.nio.sctp.AssociationChangeNotification.AssocChangeEvent;
import com.sun.nio.sctp.HandlerResult;
import com.sun.nio.sctp.InvalidStreamException;
import com.sun.nio.sctp.MessageInfo;
import com.sun.nio.sctp.Notification;
import com.sun.nio.sctp.SctpChannel;
import com.sun.nio.sctp.SctpServerChannel;
import static java.lang.System.out;
import static java.lang.System.err;

public class Send {
    /* Latches used to synchronize between the client and server so that
     * connections without any IO may not be closed without being accepted */
    final CountDownLatch clientFinishedLatch = new CountDownLatch(1);
    final CountDownLatch serverFinishedLatch = new CountDownLatch(1);

    SendNotificationHandler handler = new SendNotificationHandler();

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
        SctpChannel channel = null;
        ByteBuffer buffer = ByteBuffer.allocate(Util.LARGE_BUFFER);
        MessageInfo info = MessageInfo.createOutgoing(null, 0);

        try {
            channel = SctpChannel.open();

            /* TEST 1: Verify NotYetConnectedException thrown */
            try {
                channel.send(buffer, info);
                fail("should have thrown NotYetConnectedException");
            } catch (NotYetConnectedException unused) {
                pass();
            }  catch (IOException ioe) {
                unexpected(ioe);
            }

            channel.connect(peerAddress);
            /* Receive CommUp */
            channel.receive(buffer, null, handler);

            /* TEST 2: send small message */
            int streamNumber = 0;
            debug("sending on stream number: " + streamNumber);
            info = MessageInfo.createOutgoing(null, streamNumber);
            buffer.put(Util.SMALL_MESSAGE.getBytes("ISO-8859-1"));
            buffer.flip();
            int position = buffer.position();
            int remaining = buffer.remaining();

            debug("sending small message: " + buffer);
            int sent = channel.send(buffer, info);

            check(sent == remaining, "sent should be equal to remaining");
            check(buffer.position() == (position + sent),
                    "buffers position should have been incremented by sent");

            buffer.clear();

            /* TEST 3: send large message */
            streamNumber = handler.maxOutStreams() - 1;
            debug("sending on stream number: " + streamNumber);
            info = MessageInfo.createOutgoing(null, streamNumber);
            buffer.put(Util.LARGE_MESSAGE.getBytes("ISO-8859-1"));
            buffer.flip();
            position = buffer.position();
            remaining = buffer.remaining();

            debug("sending large message: " + buffer);
            sent = channel.send(buffer, info);

            check(sent == remaining, "sent should be equal to remaining");
            check(buffer.position() == (position + sent),
                    "buffers position should have been incremented by sent");

            /* TEST 4: InvalidStreamExcepton */
            streamNumber = handler.maxInStreams;
            info = MessageInfo.createOutgoing(null, streamNumber);
            buffer.clear();
            buffer.put(Util.SMALL_MESSAGE.getBytes("ISO-8859-1"));
            buffer.flip();
            position = buffer.position();
            remaining = buffer.remaining();

            debug("sending on stream number: " + streamNumber);
            debug("sending small message: " + buffer);
            try {
                sent = channel.send(buffer, info);
                fail("should have thrown InvalidStreamExcepton");
            } catch (InvalidStreamException ise){
                pass();
            } catch (IOException ioe) {
                unexpected(ioe);
            }
            check(buffer.remaining() == remaining,
                    "remaining should not be changed");
            check(buffer.position() == position,
                    "buffers position should not be changed");

            /* TEST 5: Non blocking send should return zero if there is
               insufficient room in the underlying output buffer */
            buffer.clear();
            channel.configureBlocking(false);
            info = MessageInfo.createOutgoing(null, 1);
            buffer.put(Util.LARGE_MESSAGE.getBytes("ISO-8859-1"));
            buffer.flip();

            int count = 0;  // do not loop forever
            do {
                position = buffer.position();
                remaining = buffer.remaining();
                debug("sending large message: " + buffer);
                sent = channel.send(buffer, info);
                if (sent == 0) {
                    check(buffer.remaining() == remaining,
                          "remaining should not be changed");
                    check(buffer.position() == position,
                          "buffers position should not be changed");
                }
                buffer.rewind();
            } while (sent != 0 && count++ < 100);

            /* TEST 6: ClosedChannelException */
            channel.close();
            try {
                channel.send(buffer, info);
                fail("should have thrown ClosedChannelException");
            } catch (ClosedChannelException cce) {
               pass();
            } catch (IOException ioe) {
                unexpected(ioe);
            }

            /* TEST 7: send without previous receive.
             * Verify that send can still throw InvalidStreamExcepton */
            debug("Opening new channel.");
            channel = SctpChannel.open(peerAddress, 0, 0);
            streamNumber = Short.MAX_VALUE - 1;
            info = MessageInfo.createOutgoing(null, streamNumber);
            buffer.clear();
            buffer.put(Util.SMALL_MESSAGE.getBytes("ISO-8859-1"));
            buffer.flip();
            position = buffer.position();
            remaining = buffer.remaining();

            debug("sending on stream number: " + streamNumber);
            debug("sending small message: " + buffer);
            try {
                sent = channel.send(buffer, info);
                fail("should have thrown InvalidStreamExcepton");
            } catch (InvalidStreamException ise){
                pass();
            } catch (IOException ioe) {
                unexpected(ioe);
            }
            check(buffer.remaining() == remaining,
                    "remaining should not be changed");
            check(buffer.position() == position,
                    "buffers position should not be changed");

            /* Receive CommUp */
            channel.receive(buffer, null, handler);
            check(handler.receivedCommUp(), "should have received COMM_UP");

            /* TEST 8: Send to an invalid preferred SocketAddress */
            SocketAddress addr = new InetSocketAddress("123.123.123.123", 3456);
            info = MessageInfo.createOutgoing(addr, 0);
            debug("sending to " + addr);
            debug("sending small message: " + buffer);
            try {
                sent = channel.send(buffer, info);
                fail("Invalid address should have thrown an Exception.");
            } catch (Exception e){
                pass();
                debug("OK, caught " + e);
            }

            /* TEST 9: Send from heap buffer to force implementation to
             * substitute with a native buffer, then check that its position
             * is updated correctly */
            buffer.clear();
            info = MessageInfo.createOutgoing(null, 0);
            buffer.put(Util.SMALL_MESSAGE.getBytes("ISO-8859-1"));
            buffer.flip();
            final int offset = 1;
            buffer.position(offset);
            remaining = buffer.remaining();

            debug("sending small message: " + buffer);
            try {
                sent = channel.send(buffer, info);

                check(sent == remaining, "sent should be equal to remaining");
                check(buffer.position() == (offset + sent),
                        "buffers position should have been incremented by sent");
            } catch (IllegalArgumentException iae) {
                fail(iae + ", Error updating buffers position");
            }

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
        private SctpServerChannel ssc;

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
            ByteBuffer buffer = ByteBuffer.allocateDirect(Util.LARGE_BUFFER);
            SctpChannel sc1 = null, sc2 = null;
            try {
                sc1 = ssc.accept();

                /* receive a small message */
                MessageInfo info;
                do {
                    info = sc1.receive(buffer, null, null);
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

                /* receive a large message */
                buffer.clear();
                do {
                    info = sc1.receive(buffer, null, null);
                    if (info == null) {
                        fail("Server: unexpected null from receive");
                            return;
                    }
                } while (!info.isComplete());

                buffer.flip();
                check(info != null, "info is null");
                check(info.streamNumber() == handler.maxOutStreams() - 1,
                        "message not sent on the correct stream");
                check(info.bytes() == Util.LARGE_MESSAGE.getBytes("ISO-8859-1").
                      length, "bytes received not equal to message length");
                check(info.bytes() == buffer.remaining(), "bytes != remaining");
                check(Util.compare(buffer, Util.LARGE_MESSAGE),
                  "received message not the same as sent message");

                /* TEST 7 ++ */
                sc2 = ssc.accept();

                /* TEST 9 */
                ByteBuffer expected = ByteBuffer.allocate(Util.SMALL_BUFFER);
                expected.put(Util.SMALL_MESSAGE.getBytes("ISO-8859-1"));
                expected.flip();
                final int offset = 1;
                expected.position(offset);
                buffer.clear();
                do {
                    info = sc2.receive(buffer, null, null);
                    if (info == null) {
                        fail("Server: unexpected null from receive");
                        return;
                    }
                } while (!info.isComplete());

                buffer.flip();
                check(info != null, "info is null");
                check(info.streamNumber() == 0, "message not sent on the correct stream");
                check(info.bytes() == expected.remaining(),
                      "bytes received not equal to message length");
                check(info.bytes() == buffer.remaining(), "bytes != remaining");
                check(expected.equals(buffer),
                    "received message not the same as sent message");

                clientFinishedLatch.await(10L, TimeUnit.SECONDS);
                serverFinishedLatch.countDown();
            } catch (IOException ioe) {
                unexpected(ioe);
            } catch (InterruptedException ie) {
                unexpected(ie);
            } finally {
                try { if (ssc != null) ssc.close(); }
                catch (IOException  unused) {}
                try { if (sc1 != null) sc1.close(); }
                catch (IOException  unused) {}
                try { if (sc2 != null) sc2.close(); }
                catch (IOException  unused) {}
            }
        }
    }

    class SendNotificationHandler extends AbstractNotificationHandler<Void>
    {
        boolean receivedCommUp;  // false
        int maxInStreams;
        int maxOutStreams;

        public boolean receivedCommUp() {
            return receivedCommUp;
        }

        public int maxInStreams() {
            return maxInStreams;
        }

        public int maxOutStreams(){
            return maxOutStreams;
        }

        @Override
        public HandlerResult handleNotification(
                Notification notification, Void attachment) {
            fail("Unknown notification type");
            return HandlerResult.CONTINUE;
        }

        @Override
        public HandlerResult handleNotification(
                AssociationChangeNotification notification, Void attachment) {
            AssocChangeEvent event = notification.event();
            Association association = notification.association();
            debug("AssociationChangeNotification");
            debug("  Association: " + notification.association());
            debug("  Event: " + event);

            if (event.equals(AssocChangeEvent.COMM_UP))
                receivedCommUp = true;

            this.maxInStreams = association.maxInboundStreams();
            this.maxOutStreams = association.maxOutboundStreams();

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
