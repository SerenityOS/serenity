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
import java.nio.channels.AlreadyConnectedException;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.NotYetBoundException;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ClosedChannelException;
import com.sun.nio.sctp.SctpChannel;
import com.sun.nio.sctp.SctpServerChannel;
import static java.lang.System.out;
import static java.lang.System.err;

public class Accept {
    static CountDownLatch acceptLatch = new CountDownLatch(1);
    static CountDownLatch closeByIntLatch = new CountDownLatch(1);
    static CountDownLatch asyncCloseLatch = new CountDownLatch(1);
    AcceptServer server = null;

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
                server = new AcceptServer();
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
        SctpChannel channel = null;

        try {
            channel = SctpChannel.open(peerAddress, 0, 0);
            acceptLatch.await();

            /* for test 4 */
            closeByIntLatch.await();
            sleep(500);
            server.thread().interrupt();

            /* for test 5 */
            asyncCloseLatch.await();
            sleep(500);
            server.channel().close();

            /* wait for the server thread to finish */
            join(server.thread(), 10000);
        } catch (IOException ioe) {
            unexpected(ioe);
        } catch (InterruptedException ie) {
            unexpected(ie);
        } finally {
            try { if (channel != null) channel.close(); }
            catch (IOException e) { unexpected(e);}
        }
    }

    class AcceptServer implements Runnable
    {
        final InetSocketAddress serverAddr;
        private SctpServerChannel ssc;
        private Thread serverThread;

        public AcceptServer() throws IOException {
            ssc = SctpServerChannel.open();

            /* TEST 1: NotYetBoundException */
            debug("TEST 1: NotYetBoundException");
            try {
                ssc.accept();
                fail();
            } catch (NotYetBoundException nybe) {
                debug("  caught NotYetBoundException");
                pass();
            } catch (IOException ioe) {
                unexpected(ioe);
            }

            ssc.bind(null);
            java.util.Set<SocketAddress> addrs = ssc.getAllLocalAddresses();
            if (addrs.isEmpty())
                debug("addrs should not be empty");

            serverAddr = (InetSocketAddress) addrs.iterator().next();

            /* TEST 2: null if this channel is in non-blocking mode and no
             *         association is available to be accepted  */
            ssc.configureBlocking(false);
            debug("TEST 2: non-blocking mode null");
            try {
                SctpChannel sc = ssc.accept();
                check(sc == null, "non-blocking mode should return null");
            } catch (IOException ioe) {
                unexpected(ioe);
            } finally {
                ssc.configureBlocking(true);
            }
        }

        void start() {
            serverThread = new Thread(this, "AcceptServer-"  +
                                              serverAddr.getPort());
            serverThread.start();
        }

        InetSocketAddress address() {
            return serverAddr;
        }

        SctpServerChannel channel() {
            return ssc;
        }

        Thread thread() {
            return serverThread;
        }

        @Override
        public void run() {
            SctpChannel sc = null;
            try {
                /* TEST 3: accepted channel */
                debug("TEST 3: accepted channel");
                sc = ssc.accept();

                checkAcceptedChannel(sc);
                acceptLatch.countDown();

                /* TEST 4: ClosedByInterruptException */
                debug("TEST 4: ClosedByInterruptException");
                try {
                    closeByIntLatch.countDown();
                    ssc.accept();
                    fail();
                } catch (ClosedByInterruptException unused) {
                    debug("  caught ClosedByInterruptException");
                    pass();
                }

                /* TEST 5: AsynchronousCloseException */
                debug("TEST 5: AsynchronousCloseException");
                /* reset thread interrupt status */
                Thread.currentThread().interrupted();

                ssc = SctpServerChannel.open().bind(null);
                try {
                    asyncCloseLatch.countDown();
                    ssc.accept();
                    fail();
                } catch (AsynchronousCloseException unused) {
                    debug("  caught AsynchronousCloseException");
                    pass();
                }

                /* TEST 6: ClosedChannelException */
                debug("TEST 6: ClosedChannelException");
                try {
                    ssc.accept();
                    fail();
                } catch (ClosedChannelException unused) {
                    debug("  caught ClosedChannelException");
                    pass();
                }
                ssc = null;
            } catch (IOException ioe) {
                ioe.printStackTrace();
            } finally {
                try { if (ssc != null) ssc.close(); }
                catch (IOException  ioe) { unexpected(ioe); }
                try { if (sc != null) sc.close(); }
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
    void join(Thread thread, long millis) { try { thread.join(millis); }
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
