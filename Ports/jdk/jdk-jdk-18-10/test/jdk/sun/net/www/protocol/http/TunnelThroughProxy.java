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

/**
 * @test
 * @bug 4620362
 * @modules java.base/sun.net.www
 * @build ProxyTunnelServer
 * @run main/othervm TunnelThroughProxy
 * @summary JSSE not returning proper exception on unknown host
 */

import java.net.*;
import java.io.*;

public class TunnelThroughProxy {
    public static void main(String[] args) throws Exception {
        nonexistingHostTest();
        getLocalPortTest();
    }

    static void nonexistingHostTest() throws Exception {
        ProxyTunnelServer proxy = setupProxy(false);
        try {
            URL u = new URL("https://www.nonexistent-site.com/");
            URLConnection uc = u.openConnection();
            InputStream is = uc.getInputStream();
            is.close();
        } catch (Exception e) {
            if (!e.getMessage().matches(".*HTTP\\/.*500.*")) {
                throw new RuntimeException(e);
            }
        } finally {
            proxy.terminate();
        }
    }


    static void getLocalPortTest() throws Exception {
        ProxyTunnelServer proxy = setupProxy(true);
        try {
            int proxyPort = proxy.getPort();
            InetAddress proxyAddress = proxy.getInetAddress();
            ServerSocket server = new ServerSocket(0, 0, InetAddress.getLoopbackAddress());
            int serverPort = server.getLocalPort();

            Socket sock;
            sock = new Socket(new Proxy(Proxy.Type.HTTP,
                    new InetSocketAddress(proxyAddress, proxyPort)));
            InetSocketAddress dest = new InetSocketAddress(server.getInetAddress(), serverPort);
            sock.connect(dest);
            int localPort = sock.getLocalPort();
            if (localPort == proxyPort)
                throw new RuntimeException("Fail: socket has wrong local port");
            // check that tunnel really works
            Socket sock1 = server.accept();
            OutputStream os = sock1.getOutputStream();
            os.write(99);
            os.flush();
            if (sock.getInputStream().read() != 99)
                throw new RuntimeException("Tunnel does not work");
        } finally {
            proxy.terminate();
        }
    }

    static ProxyTunnelServer setupProxy(boolean makeTunnel) throws IOException {
        InetAddress proxyAddress = InetAddress.getLoopbackAddress();
        ProxyTunnelServer pserver = new ProxyTunnelServer(proxyAddress);
        pserver.doTunnel(makeTunnel);
        int proxyPort = pserver.getPort();

        // disable proxy authentication
        pserver.needUserAuth(false);
        pserver.start();
        System.out.printf("Setting https.proxyHost='%s'%n", proxyAddress.getHostAddress());
        System.setProperty("https.proxyHost", proxyAddress.getHostAddress());
        System.out.printf("Setting https.proxyPort='%s'%n", String.valueOf(proxyPort));
        System.setProperty("https.proxyPort", String.valueOf(proxyPort));
        return pserver;
    }
}
