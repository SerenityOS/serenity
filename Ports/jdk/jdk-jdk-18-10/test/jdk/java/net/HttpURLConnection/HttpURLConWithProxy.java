/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8161016 8183369
 * @library /test/lib
 * @summary When proxy is set HttpURLConnection should not use DIRECT connection.
 * @run main/othervm HttpURLConWithProxy
 */
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URL;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import jdk.test.lib.net.URIBuilder;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogRecord;

public class HttpURLConWithProxy {

    private static Logger logger =
        Logger.getLogger("sun.net.www.protocol.http.HttpURLConnection");

    public static void main(String... arg) throws Exception {
        // Remove the default nonProxyHosts to use localhost for testing
        System.setProperty("http.nonProxyHosts", "");

        System.setProperty("http.proxyHost", "1.1.1.1");
        System.setProperty("http.proxyPort", "1111");

        // Use the logger to help verify the Proxy was used
        logger.setLevel(Level.ALL);
        Handler h = new ProxyHandler();
        h.setLevel(Level.ALL);
        logger.addHandler(h);

        ServerSocket ss;
        URL url;
        HttpURLConnection con;
        InetAddress loopback = InetAddress.getLoopbackAddress();
        InetSocketAddress address = new InetSocketAddress(loopback, 0);

        // Test1: using Proxy set by System Property:
        try {
            ss = new ServerSocket();
            ss.bind(address);
            url = URIBuilder.newBuilder()
                .scheme("http")
                .loopback()
                .port(ss.getLocalPort())
                .toURL();
            con = (HttpURLConnection) url.openConnection();
            con.setConnectTimeout(10 * 1000);
            con.connect();
            if(con.usingProxy()){
                System.out.println("Test1 Passed with: Connection succeeded with proxy");
            } else {
                throw new RuntimeException("Shouldn't use DIRECT connection "
                        + "when proxy is invalid/down");
            }
        } catch (IOException ie) {
            if(!ProxyHandler.proxyRetried) {
                throw new RuntimeException("Connection not retried with proxy");
            }
            System.out.println("Test1 Passed with: " + ie.getMessage());
        }

        // Test2: using custom ProxySelector implementation
        ProxyHandler.proxyRetried = false;
        MyProxySelector myProxySel = new MyProxySelector();
        ProxySelector.setDefault(myProxySel);
        try {
            ss = new ServerSocket();
            ss.bind(address);
            url = URIBuilder.newBuilder()
                .scheme("http")
                .loopback()
                .port(ss.getLocalPort())
                .toURL();
            con = (HttpURLConnection) url.openConnection();
            con.setConnectTimeout(10 * 1000);
            con.connect();
            if(con.usingProxy()){
                System.out.println("Test2 Passed with: Connection succeeded with proxy");
            } else {
                throw new RuntimeException("Shouldn't use DIRECT connection "
                        + "when proxy is invalid/down");
            }
        } catch (IOException ie) {
            if(!ProxyHandler.proxyRetried) {
                throw new RuntimeException("Connection not retried with proxy");
            }
            System.out.println("Test2 Passed with: " + ie.getMessage());
        }
    }
}


class MyProxySelector extends ProxySelector {

    List<Proxy> proxies = new ArrayList<>();

    MyProxySelector() {
        Proxy p1 = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("2.2.2.2", 2222));
        Proxy p2 = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("3.3.3.3", 3333));
        proxies.add(p1);
        proxies.add(p2);
    }

    @Override
    public List<Proxy> select(URI uri) {
        return proxies;
    }

    @Override
    public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
        // System.out.println("MyProxySelector.connectFailed(): "+sa);
    }
}

class ProxyHandler extends Handler {
    public static boolean proxyRetried = false;

    @Override
    public void publish(LogRecord record) {
        if (record.getMessage().contains("Retrying with proxy")) {
            proxyRetried = true;
        }
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() throws SecurityException {
    }
}
