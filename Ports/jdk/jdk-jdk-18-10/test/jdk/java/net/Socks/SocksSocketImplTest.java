/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import sun.net.spi.DefaultProxySelector;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

/**
 * @test
 * @bug 8230310
 * @summary Tests java.net.SocksSocketImpl
 * @run testng SocksSocketImplTest
 * @modules java.base/sun.net.spi:+open
 */
public class SocksSocketImplTest {

    private ProxySelector previousDefault;

    @BeforeTest
    public void beforeTest() {
        previousDefault = ProxySelector.getDefault();
        ProxySelector.setDefault(new SchemeStrippedProxySelector());
    }

    @AfterTest
    public void afterTest() {
        ProxySelector.setDefault(previousDefault);
    }

    /**
     * Creates a socket connection, which internally triggers proxy selection for the target
     * address. The test has been configured to use a {@link SchemeStrippedProxySelector ProxySelector}
     * which throws a {@link IllegalArgumentException}. This test then verifies that this IAE gets wrapped
     * by {@code java.net.SocksSocketImpl} into an {@link IOException} before being thrown
     *
     * @throws Exception
     */
    @Test
    public void testIOEOnProxySelection() throws Exception {
        final int backlog = -1;
        final int port = 0;
        try (ServerSocket ss = new ServerSocket(port, backlog, InetAddress.getLoopbackAddress());
             Socket s1 = new Socket(ss.getInetAddress(), ss.getLocalPort());
             Socket s2 = ss.accept()) {
            Assert.fail("IOException was expected to be thrown, but wasn't");
        } catch (IOException ioe) {
            // expected
            // now verify the IOE was thrown for the correct expected reason
            if (!(ioe.getCause() instanceof IllegalArgumentException)) {
                // rethrow this so that the test output failure will capture the entire/real
                // cause in its stacktrace
                throw ioe;
            }
        }
    }

    /**
     * A {@link ProxySelector} which strips the "scheme" part of the {@link URI}
     * before delegating the selection to the the {@link DefaultProxySelector}.
     * This is to ensure that the {@code DefaultProxySelector} throws an {@link IllegalArgumentException}
     * during selection of the proxy
     */
    private static final class SchemeStrippedProxySelector extends DefaultProxySelector {

        @Override
        public List<Proxy> select(final URI uri) {
            System.out.println("Proxy selection for " + uri);
            final URI schemeStrippedURI;
            try {
                // strip the scheme and pass the rest
                schemeStrippedURI = new URI(null, uri.getHost(), uri.getPath(), null);
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
            System.out.println("Scheme stripped URI " + schemeStrippedURI + " is being used to select a proxy");
            return super.select(schemeStrippedURI);
        }
    }
}
