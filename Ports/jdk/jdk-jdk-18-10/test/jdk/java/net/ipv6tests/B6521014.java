/*
 * Copyright (c) 2007, 2019, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 6521014 6543428
 * @summary IOException thrown when Socket tries to bind to an local IPv6 address on SuSE Linux
 * @library /test/lib
 * @build jdk.test.lib.NetworkConfiguration
 *        jdk.test.lib.Platform
 * @run main B6521014
 */

import java.net.*;
import java.io.*;
import java.util.*;
import jdk.test.lib.NetworkConfiguration;

/*
 *
 * What this testcase is to test is a (weird) coupling through the
 * cached_scope_id field of java.net.Inet6Address. Native method
 * NET_InetAddressToSockaddr as in Linux platform will try to write
 * and read this field, therefore Inet6Address becomes 'stateful'.
 * So the coupling. Certain executive order, e.g. two methods use
 * the same Inet6Address instance as illustrated in this test case,
 * will show side effect of such coupling.
 *
 * And on Windows, NET_InetAddressToSockaddr() did not assign appropriate
 * sin6_scope_id value to sockaddr_in6 structure if there's no one coming
 * with Inet6Address instance, which caused bind exception. This test use
 * link-local address without %scope suffix, so it is also going to test
 * that.
 *
 */
public class B6521014 {

    static Inet6Address removeScope(Inet6Address addr) {
        try {
            return (Inet6Address)InetAddress.getByAddress(addr.getAddress());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static Optional<Inet6Address> getLocalAddr() throws Exception {
        return NetworkConfiguration.probe()
                .ip6Addresses()
                .filter(Inet6Address::isLinkLocalAddress)
                .findFirst();
    }

    static void test1(Inet6Address sin) throws Exception {
        try (ServerSocket ssock = createBoundServer(sin);
             Socket sock = new Socket()) {
            int port = ssock.getLocalPort();
            sock.connect(new InetSocketAddress(sin, port), 100);
        } catch (SocketTimeoutException e) {
            // time out exception is okay
            System.out.println("timed out when connecting.");
        }
    }

    static void test2(Inet6Address sin) throws Exception {
        try (ServerSocket ssock = createBoundServer(sin);
             Socket sock = new Socket()) {
            int port = ssock.getLocalPort();
            ssock.setSoTimeout(100);
            sock.bind(new InetSocketAddress(sin, 0));
            sock.connect(new InetSocketAddress(sin, port), 100);
        } catch (SocketTimeoutException expected) {
            // time out exception is okay
            System.out.println("timed out when connecting.");
        }
    }

    static ServerSocket createBoundServer(Inet6Address sin) throws IOException {
        ServerSocket ss = new ServerSocket();
        InetSocketAddress address = new InetSocketAddress(sin, 0);
        ss.bind(address);
        return ss;
    }

    public static void main(String[] args) throws Exception {
        Optional<Inet6Address> oaddr = getLocalAddr();
        if (!oaddr.isPresent()) {
            System.out.println("Cannot find a link-local address.");
            return;
        }

        Inet6Address addr = oaddr.get();
        System.out.println("Using " + addr);
        test1(addr);
        test2(addr);
    }
}
