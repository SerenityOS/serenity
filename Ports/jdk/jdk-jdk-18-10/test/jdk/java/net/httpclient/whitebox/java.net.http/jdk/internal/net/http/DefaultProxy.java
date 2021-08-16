/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

package jdk.internal.net.http;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.List;
import org.testng.annotations.Test;
import static org.testng.Assert.assertEquals;

public class DefaultProxy {

    static ProxySelector getProxySelector() {
        return (((HttpClientFacade)HttpClient.newHttpClient()).impl).proxySelector();
    }

    @Test
    public void testDefault() {
        ProxySelector ps = getProxySelector();
        System.out.println("HttpClientImpl proxySelector:" + ps);
        assertEquals(ps, ProxySelector.getDefault());
    }

    // From the test driver
    // -Dhttp.proxyHost=foo.proxy.com
    // -Dhttp.proxyPort=9876
    // -Dhttp.nonProxyHosts=*.direct.com
    @Test
    public void testHttpProxyProps() {
        ProxySelector ps = getProxySelector();

        URI uri = URI.create("http://foo.com/example.html");
        List<Proxy> plist = ps.select(uri);
        System.out.println("proxy list for " + uri + " : " + plist);
        assertEquals(plist.size(), 1);
        Proxy proxy = plist.get(0);
        assertEquals(proxy.type(), Proxy.Type.HTTP);
        InetSocketAddress expectedAddr =
                InetSocketAddress.createUnresolved("foo.proxy.com", 9876);
        assertEquals(proxy.address(), expectedAddr);

        // nonProxyHosts
        uri = URI.create("http://foo.direct.com/example.html");
        plist = ps.select(uri);
        System.out.println("proxy list for " + uri + " : " + plist);
        assertEquals(plist.size(), 1);
        proxy = plist.get(0);
        assertEquals(proxy.type(), Proxy.Type.DIRECT);
        assertEquals(proxy.address(), null);
    }

    // From the test driver
    // -Dhttp.proxyHost=secure.proxy.com
    // -Dhttp.proxyPort=5443
    @Test
    public void testHttpsProxyProps() {
        ProxySelector ps = getProxySelector();

        URI uri = URI.create("https://foo.com/example.html");
        List<Proxy> plist = ps.select(uri);
        System.out.println("proxy list for " + uri + " : " + plist);
        assertEquals(plist.size(), 1);
        Proxy proxy = plist.get(0);
        assertEquals(proxy.type(), Proxy.Type.HTTP);
        InetSocketAddress expectedAddr =
                InetSocketAddress.createUnresolved("secure.proxy.com", 5443);
        assertEquals(proxy.address(), expectedAddr);

        // nonProxyHosts
        uri = URI.create("https://foo.direct.com/example.html");
        plist = ps.select(uri);
        System.out.println("proxy list for " + uri + " : " + plist);
        assertEquals(plist.size(), 1);
        proxy = plist.get(0);
        assertEquals(proxy.type(), Proxy.Type.DIRECT);
        assertEquals(proxy.address(), null);
    }

}
