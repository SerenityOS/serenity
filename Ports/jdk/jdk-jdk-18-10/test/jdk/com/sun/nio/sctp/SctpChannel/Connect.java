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
import java.util.Set;
import java.util.concurrent.Callable;
import java.nio.channels.AlreadyConnectedException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ConnectionPendingException;
import java.nio.channels.NoConnectionPendingException;
import java.nio.channels.UnresolvedAddressException;
import java.nio.channels.UnsupportedAddressTypeException;
import com.sun.nio.sctp.SctpChannel;
import com.sun.nio.sctp.SctpServerChannel;
import static java.lang.System.out;
import static java.lang.System.err;

/**
 * Tests connect, finishConnect, isConnectionPending,
 * getRemoteAddresses and association.
 */
public class Connect {

    void test(String[] args) {
        if (!Util.isSCTPSupported()) {
            out.println("SCTP protocol is not supported");
            out.println("Test cannot be run");
            return;
        }

        doTest();
    }

    void doTest() {
        SctpChannel channel = null;

        try (SctpServerChannel ssc = SctpServerChannel.open()) {
            /* Create a server channel to connect to */
            ssc.bind(null);
            Set<SocketAddress> addrs = ssc.getAllLocalAddresses();
            if (addrs.isEmpty())
                debug("addrs should not be empty");
            final SocketAddress peerAddress = (InetSocketAddress) addrs.iterator().next();

            channel = SctpChannel.open();

            /* TEST 0.5 Verify default values for new/unconnected channel */
            check(channel.getRemoteAddresses().isEmpty(),
                    "non empty set for unconnected channel");
            check(channel.association() == null,
                    "non-null association for unconnected channel");
            check(!channel.isConnectionPending(),
                    "should not have a connection pending");

            /* TEST 1: non-blocking connect */
            channel.configureBlocking(false);
            if (channel.connect(peerAddress) != true) {
                debug("non-blocking connect did not immediately succeed");
                check(channel.isConnectionPending(),
                        "should return true for isConnectionPending");
                try {
                    channel.connect(peerAddress);
                    fail("should have thrown ConnectionPendingException");
                } catch (ConnectionPendingException cpe) {
                    pass();
                } catch (IOException ioe) {
                    unexpected(ioe);
                }
                channel.configureBlocking(true);
                check(channel.finishConnect(),
                        "finishConnect should have returned true");
            }

            ssc.accept();
            ssc.close();

            /* TEST 1.5 Verify after connect */
            check(!channel.getRemoteAddresses().isEmpty(),
                    "empty set for connected channel");
            check(channel.association() != null,
                    "null association for connected channel");
            check(!channel.isConnectionPending(),
                    "pending connection for connected channel");

            /* TEST 2: Verify AlreadyConnectedException thrown */
            try {
                channel.connect(peerAddress);
                fail("should have thrown AlreadyConnectedException");
            } catch (AlreadyConnectedException unused) {
                pass();
            }  catch (IOException ioe) {
                unexpected(ioe);
            }

            /* TEST 2.5: Verify AlreadyConnectedException thrown */
            try {
                channel.connect(peerAddress, 5, 5);
                fail("should have thrown AlreadyConnectedException");
            } catch (AlreadyConnectedException unused) {
                pass();
            }  catch (IOException ioe) {
                unexpected(ioe);
            }

            /* TEST 3: UnresolvedAddressException */
            channel.close();
            channel = SctpChannel.open();
            InetSocketAddress unresolved =
                    InetSocketAddress.createUnresolved("xxyyzzabc", 4567);
            try {
                channel.connect(unresolved);
                fail("should have thrown UnresolvedAddressException");
            } catch (UnresolvedAddressException unused) {
                pass();
            }  catch (IOException ioe) {
                unexpected(ioe);
            }

            /* TEST 4: UnsupportedAddressTypeException */
            SocketAddress unsupported = new UnsupportedSocketAddress();
            try {
                channel.connect(unsupported);
                fail("should have thrown UnsupportedAddressTypeException");
            } catch (UnsupportedAddressTypeException unused) {
                pass();
            }  catch (IOException ioe) {
                unexpected(ioe);
            }

            /* TEST 5: ClosedChannelException */
            channel.close();
            final SctpChannel closedChannel = channel;
            testCCE(new Callable<Void>() {
                public Void call() throws IOException {
                    closedChannel.connect(peerAddress); return null; } });

            /* TEST 5.5 getRemoteAddresses */
            testCCE(new Callable<Void>() {
                public Void call() throws IOException {
                    closedChannel.getRemoteAddresses(); return null; } });
            testCCE(new Callable<Void>() {
                public Void call() throws IOException {
                    closedChannel.association(); return null; } });
            check(!channel.isConnectionPending(),
                    "pending connection for closed channel");

            /* Run some more finishConnect tests */

            /* TEST 6: NoConnectionPendingException */
            channel = SctpChannel.open();
            try {
                channel.finishConnect();
                fail("should have thrown NoConnectionPendingException");
            } catch (NoConnectionPendingException unused) {
                pass();
            }  catch (IOException ioe) {
                unexpected(ioe);
            }

            /* TEST 7: ClosedChannelException */
            channel.close();
            final SctpChannel cceChannel = channel;
            testCCE(new Callable<Void>() {
                public Void call() throws IOException {
                    cceChannel.finishConnect(); return null; } });

            /* TEST 8: IOException: Connection refused. Exercises handleSocketError.
             *         Assumption: no sctp socket listening on 3456 */
            SocketAddress addr = new InetSocketAddress("localhost", 3456);
            channel = SctpChannel.open();
            try {
                channel.connect(addr);
                fail("should have thrown ConnectException: Connection refused");
            } catch (IOException ioe) {
                pass();
            }

        } catch (IOException ioe) {
            unexpected(ioe);
        } finally {
            try { if (channel != null) channel.close(); }
            catch (IOException unused) {}
        }
    }

    class UnsupportedSocketAddress extends SocketAddress { }

    void testCCE(Callable callable) {
        try {
            callable.call();
            fail("should have thrown ClosedChannelException");
        } catch (ClosedChannelException cce) {
           pass();
        } catch (Exception ioe) {
            unexpected(ioe);
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
