/*
 * Copyright (c) 2009, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Iterator;
import java.util.Set;
import java.util.List;
import java.util.Arrays;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import com.sun.nio.sctp.AbstractNotificationHandler;
import com.sun.nio.sctp.Association;
import com.sun.nio.sctp.AssociationChangeNotification;
import com.sun.nio.sctp.AssociationChangeNotification.AssocChangeEvent;
import com.sun.nio.sctp.HandlerResult;
import com.sun.nio.sctp.MessageInfo;
import com.sun.nio.sctp.SctpChannel;
import com.sun.nio.sctp.SctpMultiChannel;
import com.sun.nio.sctp.SctpServerChannel;
import com.sun.nio.sctp.SctpSocketOption;
import java.security.AccessController;
import java.security.PrivilegedAction;
import static com.sun.nio.sctp.SctpStandardSocketOptions.*;
import static java.lang.System.out;

public class SocketOptionTests {
    final String osName = AccessController.doPrivileged(
                    (PrivilegedAction<String>)() -> System.getProperty("os.name"));

    <T> void checkOption(SctpMultiChannel smc, SctpSocketOption<T> name,
            T expectedValue) throws IOException {
        T value = smc.getOption(name, null);
        check(value.equals(expectedValue), name + ": value (" + value +
                ") not as expected (" + expectedValue + ")");
       }

    <T> void optionalSupport(SctpMultiChannel smc, SctpSocketOption<T> name,
            T value) {
        try {
            smc.setOption(name, value, null);
            checkOption(smc, name, value);
        } catch (IOException e) {
            /* Informational only, not all options have native support */
            out.println(name + " not supported. " + e);
        }
    }

    void test(String[] args) {
        if (!Util.isSCTPSupported()) {
            out.println("SCTP protocol is not supported");
            out.println("Test cannot be run");
            return;
        }

        try {
            SctpMultiChannel smc = SctpMultiChannel.open();

            /* check supported options */
            Set<SctpSocketOption<?>> options = smc.supportedOptions();
            List<? extends SctpSocketOption<?>> expected = Arrays.<SctpSocketOption<?>>asList(
                    SCTP_DISABLE_FRAGMENTS, SCTP_EXPLICIT_COMPLETE,
                    SCTP_FRAGMENT_INTERLEAVE, SCTP_INIT_MAXSTREAMS,
                    SCTP_NODELAY, SCTP_PRIMARY_ADDR, SCTP_SET_PEER_PRIMARY_ADDR,
                    SO_SNDBUF, SO_RCVBUF, SO_LINGER);

            for (SctpSocketOption opt: expected) {
                if (!options.contains(opt))
                    fail(opt.name() + " should be supported");
            }

            InitMaxStreams streams = InitMaxStreams.create(1024, 1024);
            smc.setOption(SCTP_INIT_MAXSTREAMS, streams, null);
            checkOption(smc, SCTP_INIT_MAXSTREAMS, streams);
            streams = smc.getOption(SCTP_INIT_MAXSTREAMS, null);
            check(streams.maxInStreams() == 1024, "Max in streams: value: "
                    + streams.maxInStreams() + ", expected 1024 ");
            check(streams.maxOutStreams() == 1024, "Max out streams: value: "
                    + streams.maxOutStreams() + ", expected 1024 ");

            optionalSupport(smc, SCTP_DISABLE_FRAGMENTS, true);
            optionalSupport(smc, SCTP_EXPLICIT_COMPLETE, true);
            optionalSupport(smc, SCTP_FRAGMENT_INTERLEAVE, 1);

            smc.setOption(SCTP_NODELAY, true, null);
            checkOption(smc, SCTP_NODELAY, true);
            smc.setOption(SO_SNDBUF, 16*1024, null);
            smc.setOption(SO_RCVBUF, 16*1024, null);

            checkOption(smc, SO_LINGER, -1);  /* default should be negative */

            smc.setOption(SO_LINGER, 2000, null);
            checkOption(smc, SO_LINGER, 2000);

            /* SCTP_PRIMARY_ADDR */
            sctpPrimaryAddr();

            /* NullPointerException */
            try {
                smc.setOption(null, "value", null);
                fail("NullPointerException not thrown for setOption");
            } catch (NullPointerException unused) {
                pass();
            }
            try {
               smc.getOption(null, null);
               fail("NullPointerException not thrown for getOption");
            } catch (NullPointerException unused) {
               pass();
            }

            /* ClosedChannelException */
            smc.close();
            try {
               smc.setOption(SCTP_INIT_MAXSTREAMS, streams, null);
               fail("ClosedChannelException not thrown");
            } catch (ClosedChannelException unused) {
                pass();
            }
        } catch (IOException ioe) {
            unexpected(ioe);
        }
    }

    /* SCTP_PRIMARY_ADDR */
    void sctpPrimaryAddr() throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(Util.SMALL_BUFFER);

        System.out.println("TESTING SCTP_PRIMARY_ADDR");

        /* create listening channel */
        SctpServerChannel ssc = SctpServerChannel.open().bind(null);
        Set<SocketAddress> addrs = ssc.getAllLocalAddresses();
        if (addrs.isEmpty())
            debug("addrs should not be empty");

        InetSocketAddress serverAddr = (InetSocketAddress) addrs.iterator().next();

        /* setup an association implicitly by sending a small message */
        int streamNumber = 0;
        debug("sending to " + serverAddr + " on stream number: " + streamNumber);
        MessageInfo info = MessageInfo.createOutgoing(serverAddr, streamNumber);
        buffer.put(Util.SMALL_MESSAGE.getBytes("ISO-8859-1"));
        buffer.flip();

        debug("sending small message: " + buffer);
        SctpMultiChannel smc = SctpMultiChannel.open();
        int sent = smc.send(buffer, info);

        /* Receive the COMM_UP */
        buffer.clear();
        SOTNotificationHandler handler = new SOTNotificationHandler();
        info = smc.receive(buffer, null, handler);
        check(handler.receivedCommUp(), "COMM_UP no received");
        Set<Association> associations = smc.associations();
        check(!associations.isEmpty(),"There should be some associations");
        Association assoc = associations.iterator().next();

        SctpChannel peerChannel = ssc.accept();
        ssc.close();
        Set<SocketAddress> remoteAddresses = smc.getRemoteAddresses(assoc);
        debug("Remote Addresses: ");
        for (Iterator<SocketAddress> it = remoteAddresses.iterator(); it.hasNext(); ) {
            InetSocketAddress addr = (InetSocketAddress)it.next();
            debug("\t" + addr);
        }

        SocketAddress primaryAddr = smc.getOption(SCTP_PRIMARY_ADDR, assoc);
        System.out.println("SCTP_PRIMARY_ADDR returned: " + primaryAddr);
        /* Verify that this is one of the remote addresses */
        check(remoteAddresses.contains(primaryAddr), "SCTP_PRIMARY_ADDR returned bogus address!");

        for (Iterator<SocketAddress> it = remoteAddresses.iterator(); it.hasNext(); ) {
            InetSocketAddress addrToSet = (InetSocketAddress) it.next();
            System.out.println("SCTP_PRIMARY_ADDR try set to: " + addrToSet);
            smc.setOption(SCTP_PRIMARY_ADDR, addrToSet, assoc);
            System.out.println("SCTP_PRIMARY_ADDR set to    : " + addrToSet);
            primaryAddr = smc.getOption(SCTP_PRIMARY_ADDR, assoc);
            System.out.println("SCTP_PRIMARY_ADDR returned  : " + primaryAddr);
            check(addrToSet.equals(primaryAddr), "SCTP_PRIMARY_ADDR not set correctly");
        }
        smc.close();
        peerChannel.close();
    }

    class SOTNotificationHandler extends AbstractNotificationHandler<Object>
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
