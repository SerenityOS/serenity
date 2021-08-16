/*
 * Copyright (c) 2002, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4727547
 * @summary SocksSocketImpl throws NullPointerException
 * @build SocksServer
 * @run main/othervm SocksV4Test
 */

import java.io.IOException;
import java.net.Authenticator;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

public class SocksV4Test {

    // An unresolvable host
    static final String HOSTNAME = "doesnot.exist.invalid";
    static final String USER = "johndoe";
    static final String PASSWORD = "helloworld";

    public static void main(String[] args) throws Exception {
        Authenticator.setDefault(new Auth());
        UHETest();
        getLocalPortTest();
    }

    static class Auth extends Authenticator {
        protected PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(USER, PASSWORD.toCharArray());
        }
    }

    public static void getLocalPortTest() throws Exception {
        // We actually use V5 for this test because that is the default
        // protocol version used by the client and it doesn't really handle
        // down grading very well.
        InetAddress lba = InetAddress.getLoopbackAddress();
        try (SocksServer srvr = new SocksServer(lba, 0, false);
             ServerSocket ss = new ServerSocket(0, 0, lba)) {

            srvr.addUser(USER, PASSWORD);
            int serverPort = ss.getLocalPort();
            srvr.start();
            int proxyPort = srvr.getPort();
            System.out.printf("Server port %d, Proxy port %d\n", serverPort, proxyPort);
            Proxy sp = new Proxy(Proxy.Type.SOCKS,
                    new InetSocketAddress(lba, proxyPort));
            // Let's create an unresolved address
            InetSocketAddress ad = new InetSocketAddress(lba.getHostAddress(), serverPort);
            try (Socket s = new Socket(sp)) {
                s.connect(ad, 10000);
                int pp = s.getLocalPort();
                System.out.println("Local port = " + pp);
                if (pp == serverPort || pp == proxyPort)
                    throw new RuntimeException("wrong port returned");
            } catch (UnknownHostException ex) {
                throw new RuntimeException(ex);
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
        }
    }

    public static void UHETest() throws Exception {
        // sanity before running the test
        assertUnresolvableHost(HOSTNAME);

        InetAddress lba = InetAddress.getLoopbackAddress();
        // Create a SOCKS V4 proxy
        try (SocksServer srvr = new SocksServer(lba, 0, true)) {
            srvr.start();
            Proxy sp = new Proxy(Proxy.Type.SOCKS,
                    new InetSocketAddress(lba, srvr.getPort()));
            // Let's create an unresolved address
            InetSocketAddress ad = new InetSocketAddress(HOSTNAME, 1234);
            try (Socket s = new Socket(sp)) {
                s.connect(ad, 10000);
            } catch (UnknownHostException ex) {
                // OK, that's what we expected
            } catch (NullPointerException npe) {
                // Not OK, this used to be the bug
                throw new RuntimeException("Got a NUllPointerException");
            }
        }
    }

    static void assertUnresolvableHost(String host) {
        InetAddress addr = null;
        try {
            addr = InetAddress.getByName(host);
        } catch (UnknownHostException x) {
            // OK, expected
        }
        if (addr != null)
            throw new RuntimeException("Test cannot run. resolvable address:" + addr);
    }
}
