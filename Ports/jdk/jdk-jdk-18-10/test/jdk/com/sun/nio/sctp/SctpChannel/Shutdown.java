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
import java.util.concurrent.CountDownLatch;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NotYetConnectedException;
import com.sun.nio.sctp.AbstractNotificationHandler;
import com.sun.nio.sctp.HandlerResult;
import com.sun.nio.sctp.MessageInfo;
import com.sun.nio.sctp.SctpChannel;
import com.sun.nio.sctp.SctpServerChannel;
import com.sun.nio.sctp.ShutdownNotification;
import static java.lang.System.out;
import static java.lang.System.err;

public class Shutdown {
    static CountDownLatch finishedLatch = new CountDownLatch(1);
    static CountDownLatch sentLatch = new CountDownLatch(1);

    void test(String[] args) {
        SocketAddress address = null;
        ShutdownServer server = null;

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
                server = new ShutdownServer();
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
        ByteBuffer buffer = ByteBuffer.allocate(Util.SMALL_BUFFER);
        MessageInfo info;

        try {
            channel = SctpChannel.open();

            /* TEST 1: Verify NotYetConnectedException thrown */
            debug("Test 1: NotYetConnectedException");
            try {
                channel.shutdown();
                fail("shutdown not throwing expected NotYetConnectedException");
            } catch (NotYetConnectedException unused) {
                pass();
            }  catch (IOException ioe) {
                unexpected(ioe);
            }

            channel.connect(peerAddress);
            sentLatch.await();
            channel.shutdown();

            /* TEST 2: receive data sent before shutdown */
            do {
                debug("Test 2: invoking receive");
                info = channel.receive(buffer, null, null);
                if (info == null) {
                    fail("unexpected null from receive");
                    return;
                }
            } while (!info.isComplete());

            buffer.flip();
            check(info != null, "info is null");
            check(info.bytes() == Util.SMALL_MESSAGE.getBytes("ISO-8859-1").
                  length, "bytes received not equal to message length");
            check(info.bytes() == buffer.remaining(), "bytes != remaining");
            check(Util.compare(buffer, Util.SMALL_MESSAGE),
                  "received message not the same as sent message");

            buffer.clear();

            /* TEST 3: receive notifications on the SCTP stack */
            debug("Test 3: receive notifications");
            while ((info = channel.receive(buffer, null, null )) != null &&
                    info.bytes() != -1 );


            /* TEST 4: If the channel is already shutdown then invoking this
             * method has no effect. */
            debug("Test 4: no-op");
            try {
                channel.shutdown();
                pass();
            } catch (IOException ioe) {
                unexpected(ioe);
            }

            /* TEST 5: Further sends will throw ClosedChannelException */
            debug("Test 5: ClosedChannelException");
            info = MessageInfo.createOutgoing(null, 1);
            try {
                channel.send(buffer, info);
                fail("shutdown not throwing expected ClosedChannelException");
            } catch (ClosedChannelException unused) {
                pass();
            } catch (IOException ioe) {
                unexpected(ioe);
            }

            /* TEST 6: getRemoteAddresses */
            debug("Test 6: getRemoteAddresses");
            try {
                java.util.Set<SocketAddress> remoteAddrs = channel.getRemoteAddresses();
                check(remoteAddrs.isEmpty(),
                         "A shutdown channel should not have remote addresses");
            } catch (IOException ioe) {
                unexpected(ioe);
            }
        } catch (IOException ioe) {
            unexpected(ioe);
        } catch (InterruptedException ie) {
            unexpected(ie);
        }finally {
            finishedLatch.countDown();
            try { if (channel != null) channel.close(); }
            catch (IOException e) { unexpected(e);}
        }
    }

    class ShutdownServer implements Runnable
    {
        final InetSocketAddress serverAddr;
        private SctpServerChannel ssc;

        public ShutdownServer() throws IOException {
            ssc = SctpServerChannel.open().bind(null);
            //serverAddr = (InetSocketAddress)(ssc.getAllLocalAddresses().iterator().next());

            java.util.Set<SocketAddress> addrs = ssc.getAllLocalAddresses();
            if (addrs.isEmpty())
                debug("addrs should not be empty");

            serverAddr = (InetSocketAddress) addrs.iterator().next();

        }

        public void start() {
            (new Thread(this, "ShutdownServer-"  + serverAddr.getPort())).start();
        }

        public InetSocketAddress address() {
            return serverAddr;
        }

        @Override
        public void run() {
            SctpChannel sc = null;
            try {
                sc = ssc.accept();

                /* send a message */
                MessageInfo info = MessageInfo.createOutgoing(null, 1);
                ByteBuffer buf = ByteBuffer.allocateDirect(Util.SMALL_BUFFER);
                buf.put(Util.SMALL_MESSAGE.getBytes("ISO-8859-1"));
                buf.flip();
                sc.send(buf, info);

                /* notify client that the data has been sent */
                sentLatch.countDown();

                /* wait until after the client has finished its tests */
                finishedLatch.await();

                buf.clear();
                ShutdownNotificationHandler handler =
                        new ShutdownNotificationHandler();
                BooleanWrapper bool = new BooleanWrapper();
                sc.configureBlocking(false);
                sc.receive(buf, bool, handler);
                check(bool.booleanValue(), "SHUTDOWN not received on Server");

            } catch (IOException ioe) {
                ioe.printStackTrace();
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            } finally {
                try { if (ssc != null) ssc.close(); }
                catch (IOException  ioe) { unexpected(ioe); }
                try { if (sc != null) sc.close(); }
                catch (IOException  ioe) { unexpected(ioe); }
            }
        }
    }

    class BooleanWrapper {
        boolean bool;

        boolean booleanValue() {
            return bool;
        }

        void booleanValue(boolean value) {
            bool = value;
        }
    }

    class ShutdownNotificationHandler extends AbstractNotificationHandler<BooleanWrapper>
    {
        @Override
        public HandlerResult handleNotification(
                ShutdownNotification sn, BooleanWrapper bool)
        {
            bool.booleanValue(true);
            debug(sn.toString());
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
