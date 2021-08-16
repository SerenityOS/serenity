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
import java.util.Set;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.Arrays;
import java.util.Iterator;
import java.nio.channels.ClosedChannelException;
import com.sun.nio.sctp.SctpChannel;
import com.sun.nio.sctp.SctpServerChannel;
import com.sun.nio.sctp.SctpSocketOption;
import java.security.AccessController;
import java.security.PrivilegedAction;
import static com.sun.nio.sctp.SctpStandardSocketOptions.*;
import static java.lang.System.out;

public class SocketOptionTests {
    final String osName = AccessController.doPrivileged(
                    (PrivilegedAction<String>)() -> System.getProperty("os.name"));

    <T> void checkOption(SctpChannel sc, SctpSocketOption<T> name,
            T expectedValue) throws IOException {
        T value = sc.getOption(name);
        check(value.equals(expectedValue), name + ": value (" + value +
                ") not as expected (" + expectedValue + ")");
       }

    <T> void optionalSupport(SctpChannel sc, SctpSocketOption<T> name,
            T value) {
        try {
            sc.setOption(name, value);
            checkOption(sc, name, value);
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

        try (SctpChannel sc = SctpChannel.open()) {

            /* check supported options */
            Set<SctpSocketOption<?>> options = sc.supportedOptions();
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
            sc.setOption(SCTP_INIT_MAXSTREAMS, streams);
            checkOption(sc, SCTP_INIT_MAXSTREAMS, streams);
            streams = sc.getOption(SCTP_INIT_MAXSTREAMS);
            check(streams.maxInStreams() == 1024, "Max in streams: value: "
                    + streams.maxInStreams() + ", expected 1024 ");
            check(streams.maxOutStreams() == 1024, "Max out streams: value: "
                    + streams.maxOutStreams() + ", expected 1024 ");

            optionalSupport(sc, SCTP_DISABLE_FRAGMENTS, true);
            optionalSupport(sc, SCTP_EXPLICIT_COMPLETE, true);
            optionalSupport(sc, SCTP_FRAGMENT_INTERLEAVE, 1);

            sc.setOption(SCTP_NODELAY, true);
            checkOption(sc, SCTP_NODELAY, true);
            sc.setOption(SO_SNDBUF, 16*1024);
            checkOption(sc, SO_SNDBUF, 16*1024);
            sc.setOption(SO_RCVBUF, 16*1024);
            checkOption(sc, SO_RCVBUF, 16*1024);
            checkOption(sc, SO_LINGER, -1);  /* default should be negative */
            sc.setOption(SO_LINGER, 2000);
            checkOption(sc, SO_LINGER, 2000);

            /* SCTP_PRIMARY_ADDR */
            sctpPrimaryAddr();

            /* NullPointerException */
            try {
                sc.setOption(null, "value");
                fail("NullPointerException not thrown for setOption");
            } catch (NullPointerException unused) {
                pass();
            }
            try {
               sc.getOption(null);
               fail("NullPointerException not thrown for getOption");
            } catch (NullPointerException unused) {
               pass();
            }

            /* ClosedChannelException */
            sc.close();
            try {
               sc.setOption(SCTP_INIT_MAXSTREAMS, streams);
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
        System.out.println("TESTING SCTP_PRIMARY_ADDR");
        SctpChannel sc = SctpChannel.open();
        SctpServerChannel ssc = SctpServerChannel.open().bind(null);
        Set<SocketAddress> addrs = ssc.getAllLocalAddresses();
        if (addrs.isEmpty())
            debug("addrs should not be empty");
        debug("Listening on " + addrs);

        InetSocketAddress serverAddr = (InetSocketAddress) addrs.iterator().next();
        debug("connecting to " + serverAddr);
        sc.connect(serverAddr);
        SctpChannel peerChannel = ssc.accept();
        ssc.close();
        Set<SocketAddress> remoteAddresses = sc.getRemoteAddresses();
        debug("Remote Addresses: ");
        for (Iterator<SocketAddress> it = remoteAddresses.iterator(); it.hasNext(); ) {
            InetSocketAddress addr = (InetSocketAddress)it.next();
            debug("\t" + addr);
        }

        SocketAddress primaryAddr = sc.getOption(SCTP_PRIMARY_ADDR);
        System.out.println("SCTP_PRIMARY_ADDR returned: " + primaryAddr);
        /* Verify that this is one of the remote addresses */
        check(remoteAddresses.contains(primaryAddr), "SCTP_PRIMARY_ADDR returned bogus address!");

        for (Iterator<SocketAddress> it = remoteAddresses.iterator(); it.hasNext(); ) {
            InetSocketAddress addrToSet = (InetSocketAddress) it.next();
            System.out.println("SCTP_PRIMARY_ADDR try set to: " + addrToSet);
            sc.setOption(SCTP_PRIMARY_ADDR, addrToSet);
            System.out.println("SCTP_PRIMARY_ADDR set to    : " + addrToSet);
            primaryAddr = sc.getOption(SCTP_PRIMARY_ADDR);
            System.out.println("SCTP_PRIMARY_ADDR returned  : " + primaryAddr);
            check(addrToSet.equals(primaryAddr), "SCTP_PRIMARY_ADDR not set correctly");
        }
        sc.close();
        peerChannel.close();
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
