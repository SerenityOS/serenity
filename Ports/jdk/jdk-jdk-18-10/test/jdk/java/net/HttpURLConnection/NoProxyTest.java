/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8144008
 * @library /test/lib
 * @summary Setting NO_PROXY on HTTP URL connections does not stop proxying
 * @run main/othervm NoProxyTest
 */

import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import jdk.test.lib.net.URIBuilder;

public class NoProxyTest {

    static class NoProxyTestSelector extends ProxySelector {
        @Override
        public List<Proxy> select(URI uri) {
            throw new RuntimeException("Should not reach here as proxy==Proxy.NO_PROXY");
        }
        @Override
        public void connectFailed(URI u, SocketAddress s, IOException e) { }
    }

    public static void main(String args[]) throws MalformedURLException {
        ProxySelector.setDefault(new NoProxyTestSelector());

        URL url = URIBuilder.newBuilder()
            .scheme("http")
            .loopback()
            .path("/")
            .toURLUnchecked();
        System.out.println("URL: " + url);
        URLConnection connection;
        try {
            connection = url.openConnection(Proxy.NO_PROXY);
            connection.connect();
        } catch (IOException ignore) {
            //ignore
        }
    }
}
