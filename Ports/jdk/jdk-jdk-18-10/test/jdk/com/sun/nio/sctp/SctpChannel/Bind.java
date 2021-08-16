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
 * @bug 4927640
 * @summary Tests the SCTP protocol implementation
 * @author chegar
 */

import java.net.*;
import java.io.*;
import java.util.List;
import java.util.Set;
import java.util.Iterator;
import java.nio.ByteBuffer;
import java.nio.channels.AlreadyBoundException;
import java.nio.channels.AlreadyConnectedException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.UnsupportedAddressTypeException;
import com.sun.nio.sctp.AssociationChangeNotification;
import com.sun.nio.sctp.AbstractNotificationHandler;
import com.sun.nio.sctp.HandlerResult;
import com.sun.nio.sctp.IllegalUnbindException;
import com.sun.nio.sctp.MessageInfo;
import com.sun.nio.sctp.PeerAddressChangeNotification;
import com.sun.nio.sctp.SctpChannel;
import com.sun.nio.sctp.SctpServerChannel;
import com.sun.nio.sctp.ShutdownNotification;
import static java.lang.System.out;

/**
 * Tests bind, bindAddress, unbindAddress, getLocalAddress, and
 * getAllLocalAddresses.
 */
public class Bind {
     void test(String[] args) {
        if (!Util.isSCTPSupported()) {
            out.println("SCTP protocol is not supported");
            out.println("Test cannot be run");
            return;
        }

        /* Simply bind tests */
        testBind();

        /* Test unconnected */
        testBindUnbind(false);

        /* Test connected */
        /* Adding/Removing addresses from a connected association is optional.
         * This test can be run on systems that support dynamic address
         * reconfiguration */
        //testBindUnbind(true);
    }

    void testBind() {
        SctpChannel channel = null;
        try {
            channel = SctpChannel.open();

            /* TEST 1: empty set if channel is not bound */
            check(channel.getAllLocalAddresses().isEmpty(),
                    "getAllLocalAddresses returned non empty set for unbound channel");

            /* TEST 2: null to bind the channel to an automatically assigned
             *         socket address */
            channel.bind(null);

            /* TEST 3: non empty set if the channel is bound */
            check(!channel.getAllLocalAddresses().isEmpty(),
                    "getAllLocalAddresses returned empty set for bound channel");
            debug("getAllLocalAddresses on channel bound to the wildcard:\n"
                    + channel.getAllLocalAddresses());

            /* TEST 4: AlreadyBoundException if this channel is already bound */
            try { channel.bind(null); }
            catch (AlreadyBoundException unused) { pass(); }
            catch (IOException ioe) { unexpected(ioe); }

            /* TEST 5: UnsupportedAddressTypeException */
            try {
                channel.close();  /* open a new unbound channel for test */
                channel = SctpChannel.open();
                channel.bind(new UnsupportedSocketAddress());
                fail("UnsupportedSocketAddress expected");
            } catch (UnsupportedAddressTypeException unused) { pass();
            } catch (IOException ioe) { unexpected(ioe); }

            /* TEST 6: AlreadyConnectedException */
            try {
                channel.close();  /* open a new unbound channel for test */
                channel = SctpChannel.open();
                try (var peer = connectChannel(channel)) {
                    channel.bind(null);
                    fail("AlreadyConnectedException expected");
                }
            } catch (AlreadyConnectedException unused) { pass();
            } catch (IOException ioe) { unexpected(ioe); }

            /* TEST 7: ClosedChannelException - If this channel is closed */
            try {
                channel.close();  /* open a new unbound channel for test */
                channel = SctpChannel.open();
                channel.close();
                channel.bind(null);
                fail("ClosedChannelException expected");
            } catch (ClosedChannelException unused) { pass();
            } catch (IOException ioe) { unexpected(ioe); }

            /* TEST 8: ClosedChannelException if channel is closed */
            try {
                channel.getAllLocalAddresses();
                fail("should have thrown ClosedChannelException");
            } catch (ClosedChannelException cce) {
               pass();
            } catch (Exception ioe) {
                unexpected(ioe);
            }
        } catch (IOException ioe) {
            unexpected(ioe);
        } finally {
            try { channel.close(); }
            catch (IOException ioe) { unexpected(ioe); }
        }
    }

    void testBindUnbind(boolean connected) {
        SctpChannel channel = null;
        SctpChannel peerChannel = null;

        debug("testBindUnbind, connected: " + connected);
        try {
            channel = SctpChannel.open();

            List<InetAddress> addresses = Util.getAddresses(true, false);
            Iterator iterator = addresses.iterator();
            InetSocketAddress a = new InetSocketAddress((InetAddress)iterator.next(), 0);
            debug("channel.bind( " + a + ")");
            channel.bind(a);
            while (iterator.hasNext()) {
                InetAddress ia = (InetAddress)iterator.next();
                debug("channel.bindAddress(" + ia + ")");
                channel.bindAddress(ia);
            }
            if (debug) {Util.dumpAddresses(channel, out);}

            if (connected) {
                /* Test with connected channel */
                peerChannel = connectChannel(channel);
            }

            /* TEST 1: bind/unbindAddresses on the system addresses */
            debug("bind/unbindAddresses on the system addresses");
            List<InetAddress> addrs = Util.getAddresses(true, false);
            for (InetAddress addr : addrs) {
                try {
                    debug("unbindAddress: " + addr);
                    check(boundAddress(channel, addr), "trying to remove address that is not bound");
                    channel.unbindAddress(addr);
                    if (debug) {Util.dumpAddresses(channel, out);}
                    check(!boundAddress(channel, addr), "address was not removed");

                    debug("bindAddress: " + addr);
                    channel.bindAddress(addr);
                    if (debug) {Util.dumpAddresses(channel, out);}
                    check(boundAddress(channel, addr), "address is not bound");
                } catch (IOException ioe) {
                    unexpected(ioe);
                }
            }

            /* TEST 2: bindAddress - already bound address. */
            InetAddress againAddress = addrs.get(0);
            try {
                debug("bind already bound address " + againAddress);
                channel.bindAddress(againAddress);
            } catch (AlreadyBoundException unused) {
                debug("Caught AlreadyBoundException - OK");
                pass();
            } catch (IOException ioe) {
                unexpected(ioe);
            }

            /* TEST 3: bind non local address */
            try {
                InetAddress nla = InetAddress.getByName("123.123.123.123");
                debug("bind non local address " + nla);
                channel.bindAddress(nla);
            } catch (IOException ioe) {
                debug("Informative only " + ioe);
            }

            /* TEST 4: unbind address that is not bound */
            try {
                debug("unbind address that is not bound " + againAddress);
                /* remove address first then again */
                channel.unbindAddress(againAddress);
                channel.unbindAddress(againAddress);
            } catch (IllegalUnbindException unused) {
                debug("Caught IllegalUnbindException - OK");
                pass();
            } catch (IOException ioe) {
                unexpected(ioe);
            }

            /* TEST 5: unbind address that is not bound */
            try {
                InetAddress nla = InetAddress.getByName("123.123.123.123");
                debug("unbind address that is not bound " + nla);
                channel.unbindAddress(nla);

            } catch (IllegalUnbindException unused) {
                debug("Caught IllegalUnbindException - OK");
                pass();
            } catch (IOException ioe) {
                unexpected(ioe);
            }

            if (connected) {
                channel.shutdown();

                BindNotificationHandler handler = new BindNotificationHandler();
                ByteBuffer buffer = ByteBuffer.allocate(10);
                MessageInfo info;
                while((info = peerChannel.receive(buffer, null, handler)) != null) {
                    if (info != null) {
                        if (info.bytes() == -1) {
                            debug("peerChannel Reached EOF");
                            break;
                        }
                    }
                }

                while((info = channel.receive(buffer, null, handler)) != null) {
                    if (info != null) {
                        if (info.bytes() == -1) {
                            debug("channel Reached EOF");
                            break;
                        }
                    }
                }
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } finally {
            try { if (channel != null) channel.close(); }
            catch (IOException ioe) { unexpected(ioe); }
            try { if (peerChannel != null) peerChannel.close(); }
            catch (IOException ioe) { unexpected(ioe); }
         }
    }

    boolean boundAddress(SctpChannel channel, InetAddress addr)
        throws IOException {
        for (SocketAddress boundAddr : channel.getAllLocalAddresses()) {
            if (((InetSocketAddress) boundAddr).getAddress().equals(addr))
                return true;
        }
        return false;
    }

    SctpChannel connectChannel(SctpChannel channel)
        throws IOException {
        debug("connecting channel...");
        try {
            SctpServerChannel ssc = SctpServerChannel.open();
            ssc.bind(null);
            Set<SocketAddress> addrs = ssc.getAllLocalAddresses();
            Iterator<SocketAddress> iterator = addrs.iterator();
            SocketAddress addr = iterator.next();
            debug("using " + addr + "...");
            channel.connect(addr);
            SctpChannel peerChannel = ssc.accept();
            ssc.close();
            debug("connected");
            return peerChannel;
        } catch (IOException ioe) {
            debug("Cannot connect channel");
            unexpected(ioe);
            throw ioe;
        }
    }

    class BindNotificationHandler extends AbstractNotificationHandler<Object>
    {
        @Override
        public HandlerResult handleNotification(
                AssociationChangeNotification acn, Object unused)
        {
            debug("AssociationChangeNotification: " +  acn);
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
                ShutdownNotification sn, Object unused)
        {
            debug("ShutdownNotification: " +  sn);
            return HandlerResult.CONTINUE;
        }
    }

    class UnsupportedSocketAddress extends SocketAddress { }

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
