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

/*
 * @test
 * @bug 8224973
 * @summary Basic test for the default behavior of openConnection(URL,Proxy)
 * @run testng TestDefaultBehavior
 */

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import org.testng.annotations.Test;
import static java.net.Proxy.*;
import static org.testng.Assert.expectThrows;

public class TestDefaultBehavior {

    static final Class<IllegalArgumentException> IAE = IllegalArgumentException.class;
    static final Class<UnsupportedOperationException> UOE = UnsupportedOperationException.class;

    static final InetSocketAddress ADDR = InetSocketAddress.createUnresolved("proxy.com", 80);
    static final URI uri = URI.create("http://example.com:80/");

    @Test
    public void testDefaultBehavior() {
        CustomURLStreamHandler handler = new CustomURLStreamHandler();

        expectThrows(IAE, () -> handler.openConnection(null, null));
        expectThrows(IAE, () -> handler.openConnection(null, NO_PROXY));
        expectThrows(IAE, () -> handler.openConnection(null, new Proxy(Type.SOCKS, ADDR)));
        expectThrows(IAE, () -> handler.openConnection(null, new Proxy(Type.HTTP, ADDR)));
        expectThrows(IAE, () -> handler.openConnection(uri.toURL(), null));

        expectThrows(UOE, () -> handler.openConnection(uri.toURL(), NO_PROXY));
        expectThrows(UOE, () -> handler.openConnection(uri.toURL(), new Proxy(Type.SOCKS, ADDR)));
        expectThrows(UOE, () -> handler.openConnection(uri.toURL(), new Proxy(Type.HTTP, ADDR)));
    }

    // A URLStreamHandler that delegates the overloaded openConnection that
    // takes a proxy, to the default java.net.URLStreamHandler implementation.
    static class CustomURLStreamHandler extends URLStreamHandler {

        @Override
        public URLConnection openConnection(URL url, Proxy proxy) throws IOException {
            return super.openConnection(url, proxy);
        }

        @Override
        protected URLConnection openConnection(URL u) throws IOException {
            return null;
        }
    }
}
