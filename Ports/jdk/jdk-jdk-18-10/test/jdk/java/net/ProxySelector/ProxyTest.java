/*
 * Copyright (c) 2003, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4696512
 * @summary HTTP client: Improve proxy server configuration and selection
 * @modules java.base/sun.net.www
 * @library ../../../sun/net/www/httptest/ /test/lib
 * @build ClosedChannelList TestHttpServer HttpTransaction HttpCallback
 * @compile ProxyTest.java
 * @run main/othervm -Dhttp.proxyHost=inexistant -Dhttp.proxyPort=8080 ProxyTest
 */

import java.net.*;
import java.io.*;
import java.util.List;
import jdk.test.lib.net.URIBuilder;

public class ProxyTest implements HttpCallback {
    static TestHttpServer server;

    public ProxyTest() {
    }

    public void request(HttpTransaction req) {
        req.setResponseEntityBody("Hello .");
        try {
            req.sendResponse(200, "Ok");
            req.orderlyClose();
        } catch (IOException e) {
        }
    }

    static public class MyProxySelector extends ProxySelector {
        private static volatile URI lastURI;
        private final static List<Proxy> NO_PROXY = List.of(Proxy.NO_PROXY);

        public java.util.List<Proxy> select(URI uri) {
            System.out.println("Selecting no proxy for " + uri);
            lastURI = uri;
            return NO_PROXY;
        }

        public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
        }

        public static URI lastURI() { return lastURI; }
    }

    public static void main(String[] args) {
        ProxySelector defSelector = ProxySelector.getDefault();
        if (defSelector == null)
            throw new RuntimeException("Default ProxySelector is null");
        ProxySelector.setDefault(new MyProxySelector());
        try {
            InetAddress loopback = InetAddress.getLoopbackAddress();
            server = new TestHttpServer(new ProxyTest(), 1, 10, loopback, 0);
            URL url = URIBuilder.newBuilder()
                      .scheme("http")
                      .loopback()
                      .port(server.getLocalPort())
                      .toURL();
            System.out.println("client opening connection to: " + url);
            HttpURLConnection urlc = (HttpURLConnection)url.openConnection();
            InputStream is = urlc.getInputStream();
            is.close();
            URI lastURI = MyProxySelector.lastURI();
            if (!String.valueOf(lastURI).equals(url + "/")) {
                throw new AssertionError("Custom proxy was not used: last URI was " + lastURI);
            }
        } catch (Exception e) {
                throw new RuntimeException(e);
        } finally {
            if (server != null) {
                server.terminate();
            }
        }
    }
}
