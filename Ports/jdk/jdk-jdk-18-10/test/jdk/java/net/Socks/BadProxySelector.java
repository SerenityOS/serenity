/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7178362
 * @run main/othervm BadProxySelector
 */

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.io.*;

public class BadProxySelector {
    public static void main(String[] args) throws Exception {
        ProxySelector.setDefault(new HTTPProxySelector());
        try (ServerSocket ss = new ServerSocket(0, 0, InetAddress.getLocalHost());
             Socket s1 = new Socket(ss.getInetAddress(), ss.getLocalPort());
             Socket s2 = ss.accept()) {
        }

       ProxySelector.setDefault(new NullHTTPProxySelector());
        try (ServerSocket ss = new ServerSocket(0, 0, InetAddress.getLocalHost());
             Socket s1 = new Socket(ss.getInetAddress(), ss.getLocalPort());
             Socket s2 = ss.accept()) {
        }
    }

    // always returns bogus HTTP proxies
    private static class HTTPProxySelector extends ProxySelector {
        @Override
        public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {}

        @Override
        public List<Proxy> select(URI uri) {
            System.out.println(this.getClass().getSimpleName() + " called for " + uri);
            List<Proxy> proxies = new ArrayList<>();
            proxies.add(new Proxy(Proxy.Type.HTTP,
                                  new InetSocketAddress("localhost", 0)));
            proxies.add(new Proxy(Proxy.Type.HTTP,
                                  new InetSocketAddress("localhost", 0)));
            return proxies;
        }
    }

    private static class NullHTTPProxySelector extends ProxySelector {
        @Override
        public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {}

        @Override
        public List<Proxy> select(URI uri) {
            System.out.println(this.getClass().getSimpleName() + " called for " + uri);
            List<Proxy> proxies = new ArrayList<>();
            proxies.add(null);
            proxies.add(new Proxy(Proxy.Type.HTTP,
                                  new InetSocketAddress("localhost", 0)));
            return proxies;
        }
    }
}
